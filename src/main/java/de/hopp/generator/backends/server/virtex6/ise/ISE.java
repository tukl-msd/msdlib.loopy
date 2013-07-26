package de.hopp.generator.backends.server.virtex6.ise;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printBuffer;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.edkDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkAppDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkBSPDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkDir;
import static de.hopp.generator.backends.server.virtex6.ise.xps.UCF.deployUCF;
import static de.hopp.generator.utils.Files.copy;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;

import java.io.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.SDKGenerationFailed;
import de.hopp.generator.backends.XPSGenerationFailed;
import de.hopp.generator.backends.server.virtex6.ProjectBackendIF;
import de.hopp.generator.backends.server.virtex6.ise.sdk.SDK;
import de.hopp.generator.backends.server.virtex6.ise.xps.IPCores;
import de.hopp.generator.backends.server.virtex6.ise.xps.MHSGenerator;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.frontend.BDLFilePos;
import de.hopp.generator.frontend.Core;

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
public abstract class ISE implements ProjectBackendIF {

    /** Source directory of sdk sources for a virtex6 board. */
    public static final File sdkSourceDir = new File(new File("deploy", "server"), "virtex6");
    /** Source directory of edk sources for the ISE workflow. */
    public static final File edkSourceDir = new File(new File("deploy", "server"), "ISE");

    protected static final String arch       = "virtex6";
    protected static final String device     = "xc6vlx240t";
    protected static final String pack       = "ff1156";
    protected static final String speedgrade = "-1";

    protected abstract MHSGenerator xps();
    protected abstract SDK sdk();

    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // deploy the necessary sources
        if(!config.sdkOnly()) deployBITSources(board, config, errors);
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
        if(!config.sdkOnly()) generateBITFile(config, errors);
        // abort, if errors occurred
        if(errors.hasErrors()) return;

        // generate .elf file
        generateELFFile(config, errors);
        // abort, if errors occurred
        if(errors.hasErrors()) return;

        // deploy bit and elf files
        try {
            File bitFile = new File(new File(edkDir(config), "implementation"), "system.bit");
            copyFileToDirectory(bitFile, config.serverDir());
        } catch (IOException e) {
            errors.addWarning(new Warning("could not deploy .bit file due to: " + e.getMessage()));
        }
        try {
            File elfFile = new File(new File(new File(sdkDir(config), "app"), "Debug"), "app.elf");
            copyFileToDirectory(elfFile, config.serverDir());
        } catch (IOException e) {
            errors.addWarning(new Warning("could not deploy .elf file due to: " + e.getMessage()));
        }

        // load .elf into .bit file
//        runBitInit(config, errors);
    }

    /**
     * Generates necessary Sources for generation of the BIT file
     */
    protected void deployBITSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        IOHandler IO = config.IOHANDLER();

        StringBuffer buffer  = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);
        unparser.visit(xps().generateMHSFile(board));

        if(errors.hasErrors()) return;

        // generate and deploy core sources
        for(Core core : board.cores().term()) {
            try {
                IPCores.deployCore(core, config);
            } catch (UsageError e) {
                errors.addError(e);
            } catch (IOException e) {
                errors.addError(new GenerationFailed(
                    "Failed to deploy source files for core " + core.name() +
                        " due to:\n" + e.getMessage()));
            }
        }

        // if this is only a dryrun, return
        if(config.dryrun()) return;

        // deploy board-independent files and directories (mig project file, queue and resizer cores
        try {
            copy(edkSourceDir.getPath(), edkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic edk sources due to:\n" + e.getMessage()));
        }

        // deploy board-dependent files
        try {
            deployUCF(board.term(), config);
        } catch (ParserError e) {
            errors.addError(e);
        } catch (IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
        try {
            // deploy generated .mhs file
            File target = new File(edkDir(config), "system.mhs");
            IO.debug("deploying " + target.getPath());
            printBuffer(buffer, target);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }

        // deploy generated ucf file
    }

    /**
     * Generates necessary Sources for generation of the ELF file
     */
    protected void deployELFSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        IOHandler IO = config.IOHANDLER();

        // generate board-specific MFiles
        //        SDK visit = new SDK(config, errors);
        sdk().visit(board);

        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;

        // if this is only a dryrun, return
        if(config.dryrun()) return;

        // deploy board-independent files and directories
        try {
            copy(new File(sdkSourceDir, "generic").getPath(), sdkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic sdk sources due to:\n" + e.getMessage()));
        }

        // deploy board-dependent, generic files
        for(File source : sdk().getFiles().keySet())
            try {
                copy(source.getPath(), sdk().getFiles().get(source), IO);
            } catch (IOException e) {
                errors.addError(new GenerationFailed("Failed to deploy generic source " +
                    source + " due to:\n" + e.getMessage()));
            }

        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;

        // unparse & deploy the generated MFiles (note, that several (semi-)generic files are already deployed)
        printMFile(sdk().getConstants(),  UnparserType.HEADER, errors);
        printMFile(sdk().getComponents(), UnparserType.HEADER, errors);
        printMFile(sdk().getComponents(), UnparserType.C,      errors);
        printMFile(sdk().getScheduler(),  UnparserType.HEADER, errors);
        printMFile(sdk().getScheduler(),  UnparserType.C,      errors);

        printMFile(sdk().getMSS(), new File(sdkBSPDir(config), "system.mss"), errors);

        try {
            FileUtils.writeStringToFile(new File(new File(sdkAppDir(config), "src"), "lscript.ld"), sdk().getLScript());
        } catch (IOException e) {
            errors.addError(new GenerationFailed("Failed to deploy linker script due to:\n" + e.getMessage()));
        }

        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;

        // generate api-specification
        IO.println("  generate server-side api specification ... ");
        doxygen(sdkAppDir(config), IO, errors);
    }
    /**
     * Starts whatever external tool is responsible for generation of the BIT file
     */
    protected void generateBITFile(Configuration config, ErrorCollection errors) {
        config.IOHANDLER().println("running xps synthesis (this may take some time) ...");

        try {
            // setup process builder (all of this would be soooo much easier with Java 7 ;)
            ProcessBuilder pb = new ProcessBuilder("xps", "-nw").directory(edkDir(config));
            String[] args = new String[] {
                "xload new system.xmp " + arch + " " + device + " " + pack + " " + speedgrade,
                "save proj",
                "xset parallel_synthesis yes",
                "xset sdk_export_dir " + sdkDir(config).getCanonicalPath(),
                "run bits",
                "run exporttosdk",
                "exit"
            };

            // start XPS process
            Process p = startProcess(pb.redirectErrorStream(true), args);

            // start stream reader threads for this process
            PrintStream[] writers = writers(config, "xps.log");
            Thread output = new Thread(new XPSReader(errors, p.getInputStream(), writers));
            output.start();

            // wait for the process to terminate and store the result
            int rslt = p.waitFor();

            // if something went wrong, print a warning
            if(p.waitFor() != 0) errors.addWarning(
                new Warning("failed to correctly terminate XPS process (returned " + rslt + ")")
            );

        } catch (IOException e) {
            errors.addError(new XPSGenerationFailed("failed to generate execute XPS build\n" + e.getMessage()));
        } catch (InterruptedException e) {
            errors.addError(new XPSGenerationFailed("failed to execute XPS build thread listener\n" + e.getMessage()));
        }
    }

    /** Starts whatever external tool is responsible for generation of the ELF file */
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
          if(p.waitFor() != 0) errors.addError(new SDKGenerationFailed("XSDK process terminated with result " + rslt));

        } catch(Exception e) {
            errors.addError(new SDKGenerationFailed("Failed to generate .elf file: " + e.getMessage()));
        }
    }

    /**
     * Initialises the bitfile with the elf file
     */
    private void runBitInit(Configuration config, ErrorCollection errors) {
        try {
            // create required directories
            if(!config.serverDir().exists() && !config.serverDir().mkdirs())
                errors.addError(new GenerationFailed("Could not generate required directories"));

            if(errors.hasErrors()) return;

            // build and run bitinit process
            ProcessBuilder pb = new ProcessBuilder(
                "bitinit", "system.mhs",
                "-p", device,
                "-pe", "microblaze_0", "../sdk/app/Debug/app.elf",
                "-o", config.serverDir().getCanonicalPath() + "/download.bit"
            ).directory(edkDir(config));
//            runProcess(pb, config, errors);
//        } catch (GenerationFailed e) {
//            errors.addError(e);
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
