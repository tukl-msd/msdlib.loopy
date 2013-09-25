package de.hopp.generator.backends.workflow.ise;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.backends.workflow.ise.ISEUtils.edkDir;
import static de.hopp.generator.backends.workflow.ise.ISEUtils.sdkAppDir;
import static de.hopp.generator.backends.workflow.ise.ISEUtils.sdkBSPDir;
import static de.hopp.generator.backends.workflow.ise.ISEUtils.sdkDir;
import static de.hopp.generator.utils.BoardUtils.totalMemorySize;
import static de.hopp.generator.utils.Files.deploy;
import static de.hopp.generator.utils.Files.deployContent;
import static de.hopp.generator.utils.Files.getResource;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.SDKGenerationFailed;
import de.hopp.generator.backends.XPSGenerationFailed;
import de.hopp.generator.backends.workflow.WorkflowBackend;
import de.hopp.generator.backends.workflow.ise.sdk.SDK;
import de.hopp.generator.backends.workflow.ise.xps.IPCores;
import de.hopp.generator.backends.workflow.ise.xps.MHS;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.model.BDLFilePos;
import de.hopp.generator.model.Core;
import de.hopp.generator.model.unparser.MHSUnparser;

/**
 * Abstract project backend for Xilinx ISE projects for the Virtex6 board.
 *
 * This backend is responsible for the generation of .elf and .bit files.
 * To that purpose, the ISE workflow is used.
 * The workflow implies creation of an XPS project for .bit file generation
 * and a subsequent SDK project for .elf file generation.
 * Respective backends are used to generate the project files.
 * Afterwards, the required parts of the Xilinx toolsuite are called.
 *
 * The abstract class describes the workflow. Steps of the workflow are
 * required to be overridden in the implementations.
 *
 *
 *
 * This flow is assumed to cover all versions of ISE.
 * If this proves not to be the case in some earlier or later, unsupported version,
 * introduce a new ProjectBackendIF subclass with the adjusted flow and
 * rename this flow accordingly to the earliest version number,
 * this workflow is compatible with (e.g. ISE14).
 *
 * @author Thomas Fischer
 */
public abstract class ISE implements WorkflowBackend {

    /** Truly generic ISE sources, that have to be deployed independent of board and design */
    public static final File genericXPSSourceDir = new File("deploy/board/generic/xps");
    /** Truly generic SDK sources, that have to be deployed independent of board and design */
    public static final File genericSDKSourceDir = new File("deploy/board/generic/sdk/generic");
    // All other files are not considered generic at THIS point and are referenced when required.

    protected MHS xps;
    protected SDK sdk;

    @Override
    public void printUsage(IOHandler IO) {
        IO.println(" no parameters");
    }

    @Override
    public Configuration parseParameters(Configuration config, String[] args) {
        config.setUnusued(args);
        return config;
    }

    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // check if the supplied board is compatible with this workflow (currently this means ISE 14)
        if( !(config.board() instanceof ISEBoard)) {
            errors.addError(new GenerationFailed(config.board().getName() +
                " board incompatible with " + config.flow().getName() + " workflow"));
            return;
        }

        boolean newFiles = false;

        // deploy the necessary sources
        if(!config.sdkOnly()) newFiles = deployBITSources(board, config, errors);
        deployELFSources(board, config, errors);
        // abort, if errors occurred
        if(errors.hasErrors()) return;

        // stop here, if it was only a dryrun
        // deployment phases also do not generate but only generate models...
        if(config.dryrun()) return;
        // stop also, if bitfile generation should be skipped
        // in this case, deployment phases ran through
        if(config.noGen()) return;

        // generate .bit file (if sdk only flag is not set)
        if(!config.sdkOnly()) generateBITFile(newFiles, config, errors);
        // abort, if errors occurred
        if(errors.hasErrors()) return;

        // generate .elf file
        generateELFFile(config, errors);
        // abort, if errors occurred
        if(errors.hasErrors()) return;

        for(File source : ((ISEBoard)config.board()).boardFiles(config)) {
            try {
                copyFileToDirectory(source, config.boardDir());
            } catch (IOException e) {
                errors.addWarning(new Warning("could not deploy " +
                    FilenameUtils.getName(source.getPath()) +
                    " file due to: " + e.getMessage()));
            }
        }

        // load .elf into .bit file
//        runBitInit(config, errors);
    }

    /**
     * Generates necessary Sources for generation of the BIT file
     * @param board bdl file for which .bit sources should be generated
     * @param config the configuration for this generator run
     * @param errors the error collection of this generator run
     * @return true if new files have been deployed, that did not exist before,
     *      false otherwise
     */
    protected boolean deployBITSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        ISEBoard iseBoard = (ISEBoard)config.board();
        IOHandler IO = config.IOHANDLER();

        StringBuffer buffer  = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);

        boolean newFiles = false;

        /* ************************ ANALYSIS & GENERATION ************************ */

        // generate design-specific Models
        unparser.visit(xps.generateMHSFile(board));

        if(errors.hasErrors()) return newFiles;

        // generate and deploy core sources
        for(Core core : board.cores().term()) {
            try {
                newFiles = IPCores.deployCore(core, config) || newFiles;
            } catch (UsageError e) {
                errors.addError(e);
            } catch (IOException e) {
                errors.addError(new GenerationFailed(
                    "Failed to deploy source files for core " + core.name() +
                        " due to:\n" + e.getMessage()));
            }
        }

        // if this is a dryrun, skip deployment phase
        if(config.dryrun()) return newFiles;


        /* ****************************** DEPLOYMENT ****************************** */

        try {
            // deploy board-independent XPS files and directories (i.e. the pcores)
            IO.verbose("  deploying board-independent xps files");
            newFiles = deploy(genericXPSSourceDir.getPath(), edkDir(config), IO) || newFiles;
            IO.verbose();
            // deploy design-independent XPS files and directories (i.e. the mig project file)
            IO.verbose("  deploying design-independent xps files");
            newFiles = deploy(new File(iseBoard.xpsSources(), "generic").getPath(), edkDir(config), IO) || newFiles;
            IO.verbose();
        } catch(IOException e) {
            e.printStackTrace(); // TODO only if debug or something like that...
            errors.addError(new GenerationFailed("Failed to deploy generic edk sources due to:\n" + e.getMessage()));
        }

        // FIXME merge these two blocks for better readability?...
        // deploy design-dependent files
        try {
            IO.verbose("  deploying dependent files");
            for(Entry<String, String> entry : iseBoard.getData(board.term()).entrySet())
                newFiles = deployContent(entry.getValue(),
                    new File(new File(edkDir(config), "data"), entry.getKey()), IO) || newFiles;
        } catch (ParserError e) {
            errors.addError(e);
        } catch (IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
        try {
            // deploy generated .mhs file
            File target = new File(edkDir(config), "system.mhs");
            newFiles = deployContent(buffer, target, IO) || newFiles;
            IO.verbose();
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }

        // deploy generated ucf file

        return newFiles;
    }

    /**
     * Generates necessary Sources for generation of the ELF file
     * @param board bdl file for which .elf sources should be generated
     * @param config the configuration for this generator run
     * @param errors the error collection of this generator run
     */
    protected void deployELFSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        ISEBoard iseBoard = (ISEBoard)config.board();
        IOHandler IO = config.IOHANDLER();


        /* ************************ ANALYSIS & GENERATION ************************ */

        // generate board-specific MFiles
        sdk.generate(board);

        // abort, if errors occurred
        if(errors.hasErrors()) return;

        // if this is a dryrun, skip deployment phase
        if(config.dryrun()) return;


        /* ****************************** DEPLOYMENT ****************************** */

        try {
            // deploy independent SDK files and directories
            IO.verbose("  deploying independent sdk files");
            deploy(genericSDKSourceDir.getPath(), sdkDir(config), IO);
            IO.verbose();
            // deploy design-independent SDK files and directories
            IO.verbose("  deploying design-independent sdk files");
            deploy(new File(iseBoard.sdkSources(), "generic").getPath(), sdkDir(config), IO);
            IO.verbose();
            // deploy board-independent SDK files and directories
            IO.verbose("  deploying board-independent sdk files");
            for(File source : sdk.getFiles().keySet())
                deploy(source.getPath(), sdk.getFiles().get(source), IO);
            IO.verbose();
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to deploy generic sdk sources due to:\n"
                + e.getMessage()));
            return;
        }

        try {
            // deploy mss file
            printMFile(sdk.getMSS(), new File(sdkBSPDir(config), "system.mss"));
            // deploy linker script
            FileUtils.writeStringToFile(new File(new File(ISEUtils.sdkAppDir(config), "src"), "lscript.ld"),
                setupLinkerScript(board, iseBoard, config));
        } catch (IOException e) {
            e.getStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy non-generic sdk sources due to:\n"
                + e.getMessage()));
            return;
        }

        // unparse & deploy the generated MFiles
        try {
            printMFile(sdk.getConstants(),  UnparserType.HEADER);
            printMFile(sdk.getComponents(), UnparserType.HEADER);
            printMFile(sdk.getComponents(), UnparserType.C);
            printMFile(sdk.getScheduler(),  UnparserType.HEADER);
            printMFile(sdk.getScheduler(),  UnparserType.C);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to deploy generic SDK sources due to:\n"
                + e.getMessage()));
            return;
        } catch (InvalidConstruct e) {
            throw new IllegalStateException("Encountered invalid construct in C model unparser");
        }

        // generate api-specification
        IO.println("  generate server-side api specification ... ");
        doxygen(sdkAppDir(config), IO, errors);
    }
    /**
     * Starts whatever external tool is responsible for generation of the BIT file.
     * @param newFiles indicates if new files have been deployed in the generation phase.
     * @param config the configuration for this generator run.
     * @param errors the error collection of this generator run.
     */
    protected void generateBITFile(boolean newFiles, Configuration config, ErrorCollection errors) {
        ISEBoard board = (ISEBoard)config.board();
        config.IOHANDLER().println("running xps synthesis (this may take some time) ...");

        try {
            // setup process builder (all of this would be soooo much easier with Java 7 ;)
            ProcessBuilder pb = new ProcessBuilder("xps", "-nw").directory(edkDir(config));

            List<String> args = new LinkedList<String>();
            if(newFiles || ! new File(edkDir(config), "system.xmp").exists()) {
                // if there is no xmp file or something changed, rebuild the project file
                args.add("xload new system.xmp " + board.getArch() + " " + board.getDev() + " " +
                        board.getPack() + " " + board.getSpeed());
                args.add("save proj");
            } else {
                // otherwise just load it
                args.add("xload xmp system.xmp");
            }

            args.add("xset parallel_synthesis yes");
            args.add("xset sdk_export_dir " + sdkDir(config).getCanonicalPath());
            args.add("run bits");
            args.add("run exporttosdk");
            args.add("exit");

            // start XPS process
            Process p = startProcess(pb.redirectErrorStream(true), args.toArray(new String[args.size()]));

            // start stream reader threads for this process
            PrintStream[] writers = writers(config, "xps.log");
            Thread output = new Thread(new XPSReader(errors, p.getInputStream(), writers));
            output.start();

            // wait for the process to terminate and store the result
            int rslt = p.waitFor();

            // if something went wrong, print a warning
            if(rslt != 0) errors.addWarning(
                new Warning("failed to correctly terminate XPS process (returned " + rslt + ")")
            );

        } catch (IOException e) {
            errors.addError(new XPSGenerationFailed("failed to generate execute XPS build\n" + e.getMessage()));
        } catch (InterruptedException e) {
            errors.addError(new XPSGenerationFailed("failed to execute XPS build thread listener\n" + e.getMessage()));
        }
    }

    /**
     * Starts whatever external tool is responsible for generation of the ELF file
     * @param config the configuration for this generator run
     * @param errors the error collection of this generator run
     */
    protected void generateELFFile(Configuration config, ErrorCollection errors) {
        config.IOHANDLER().println("running sdk to generate elf file (this may take some time) ...");

        try {
          // start XSDK process -> import & build all required projects
          Process p = startProcess(new ProcessBuilder(sdkCommand).directory(sdkDir(config)));

          // start stream reader threads for this process
          PrintStream[] writers = writers(config, "xsdk.log");
          Thread output = new Thread(new      StreamReader(errors, p.getInputStream(), writers));
          Thread error  = new Thread(new ErrorStreamReader(errors, p.getErrorStream(), writers));
          output.start();
          error.start();

          // wait for the reader threads to terminate (since the build seems to run asynchronously...)
          while(output.isAlive()) Thread.sleep(100);
          while(error.isAlive())  Thread.sleep(100);

          // the build process should also be terminated now
          int rslt = p.waitFor();

          // if something went wrong, print a warning
          if(rslt != 0) errors.addError(new SDKGenerationFailed("XSDK process terminated with result " + rslt));

        } catch(Exception e) {
            errors.addError(new SDKGenerationFailed("Failed to generate .elf file: " + e.getMessage()));
        }
    }

    private String setupLinkerScript(BDLFilePos file, ISEBoard iseBoard, Configuration config) throws IOException {
        InputStream input = null;
        try {
            input = getResource(new File(iseBoard.sdkSources(), "lscript.ld").getPath(), config.IOHANDLER()).openStream();
            String lScript = IOUtils.toString(input);

            // TODO This will allocate to much memory, since there is no distinction between stack and heap here...
            lScript = lScript.replaceFirst("%STACK_SIZE", "0x" + Integer.toHexString(totalMemorySize(file)*4));
            lScript = lScript.replaceFirst("%HEAP_SIZE",  "0x" + Integer.toHexString(totalMemorySize(file)*4));

            return lScript;
        } finally {
            if(input != null) IOUtils.closeQuietly(input);
        }
    }

    /**
     * Initialises the bitfile with the elf file
     * @param config the configuration for this generator run
     * @param errors the error collection of this generator run
     */
    private void runBitInit(Configuration config, ErrorCollection errors) {
        ISEBoard board = (ISEBoard)config.board();
        try {
            // create required directories
            if(!config.boardDir().exists() && !config.boardDir().mkdirs())
                errors.addError(new GenerationFailed("Could not generate required directories"));

            if(errors.hasErrors()) return;

            // build and run bitinit process
            ProcessBuilder pb = new ProcessBuilder(
                "bitinit", "system.mhs",
                "-p", board.getDev(),
                "-pe", "microblaze_0", "../sdk/app/Debug/app.elf",
                "-o", config.boardDir().getCanonicalPath() + "/download.bit"
            ).directory(edkDir(config));
            Process p = startProcess(pb);

            int rslt = p.waitFor();

            if(rslt != 0) errors.addWarning(
                new Warning("failed to correctly terminate XPS process (returned " + rslt + ")")
            );

        } catch (Exception e) {
            errors.addError(new GenerationFailed("Could not initialise bit file with elf file: " + e.getMessage()));
        }
    }

    private PrintStream[] writers(Configuration config, String log) throws FileNotFoundException {
        if(config.VERBOSE()) {
            return new PrintStream[] {
                System.out,
                new PrintStream(new File(config.tempDir(), log))
            };
        } else {
            return new PrintStream[] {
                new PrintStream(new File(config.tempDir(), log))
            };
        }
    }

    private Process startProcess(ProcessBuilder pb, String... args) throws IOException {
        // start the process
        Process p = pb.start();

        // feed the required commands to the spawned process
        if(args.length > 0) {
            PrintWriter argWriter = new PrintWriter(p.getOutputStream());
            for(String s : args) argWriter.println(s);
            argWriter.close();
        }

        return p;
    }

    private static String[] sdkCommand = new String[] {
        "xsdk",
        "-eclipseargs",
        "-nosplash",
        "-data",
        ".",
        "-application",
        "org.eclipse.cdt.managedbuilder.core.headlessbuild",
        "-importAll",
        ".",
        "-cleanBuild",
        "all",
        "--launcher.suppressErrors",
        "-vmargs",
        "-Declipse.exitdata=\"XSDK build encountered errors.\""
//        "-Dorg.eclipse.cdt.core.console=org.eclipse.cdt.core.systemConsole"
    };

    class StreamReader implements Runnable {
        InputStream in;
        PrintStream[] out;
        ErrorCollection errors;
        public StreamReader(ErrorCollection errors, InputStream in, PrintStream... out) {
            this.in  = in;
            this.out = out;
            this.errors = errors;
        }

        protected void read(String line) throws de.hopp.generator.exceptions.Error {
            for(PrintStream s : out) s.println(line);
        }

        public void run() {
            if(in == null) return;

            BufferedReader input = new BufferedReader(new InputStreamReader(in));
            try {
                String line;
                while((line = input.readLine()) != null)
                    try {
                        read(line);
                    } catch (de.hopp.generator.exceptions.Error e) {
                        if(!errors.hasErrors()) errors.addError(e);
                    }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }

    class ErrorStreamReader extends StreamReader {
        public ErrorStreamReader(ErrorCollection errors, InputStream in, PrintStream... out) {
            super(errors, in, out);
        }

        @Override
        protected void read(String line) throws de.hopp.generator.exceptions.Error {
            super.read(line);
            throw new SDKGenerationFailed(
                "Could not generate .elf file, since SDK build encountered errors. " +
                "Check the SDK log for details."
            );
        }
    }

    class XPSReader extends StreamReader {
        public XPSReader(ErrorCollection errors, InputStream in, PrintStream... out) {
            super(errors, in, out);
        }

        @Override
        protected void read(String line) throws de.hopp.generator.exceptions.Error {
            super.read(line);
            if(line.startsWith("ERROR:")) throw new XPSGenerationFailed(
                "Could not generate .bit file, since XPS build encountered errors. " +
                "Check the XPS log for details."
            );
        }
    }
}
