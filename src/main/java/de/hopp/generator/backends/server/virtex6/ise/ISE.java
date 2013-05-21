package de.hopp.generator.backends.server.virtex6.ise;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printBuffer;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.edkDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkAppDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkBSPDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkDir;
import static de.hopp.generator.utils.Files.copy;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.write;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.server.virtex6.ProjectBackendIF;
import de.hopp.generator.backends.server.virtex6.ise.sdk.SDK;
import de.hopp.generator.backends.server.virtex6.ise.xps.XPS;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.frontend.BDLFilePos;

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

    protected abstract XPS xps();
    protected abstract SDK sdk();

    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // deploy the necessary sources
        deployBITSources(board, config, errors);
        deployELFSources(board, config, errors);

        // stop here, if it was only a dryrun
        // deployment phases also do not generate but only generate models...
        if(config.dryrun()) return;

        // stop also, if bitfile generation should be skipped
        // in this case, deployment phases ran through
        if(config.noBitGen()) return;

        // generate .bit and .elf files
        generateBITFile(config, errors);
        generateELFFile(config, errors);

        // load .elf into .bit file
        runBitInit(config, errors);
    }

    /**
     * Generates necessary Sources for generation of the BIT file
     */
    protected void deployBITSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        IOHandler IO = config.IOHANDLER();

        xps().visit(board);

        StringBuffer buffer  = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);
        unparser.visit(xps().getMHSFile());

        if(errors.hasErrors()) return;

        // if this is only a dryrun, return
        if(config.dryrun()) return;

        // deploy board-independent files and directories
        try {
            copy(edkSourceDir.getPath(), edkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic edk sources due to:\n" + e.getMessage()));
        }
        try {
            File target = new File(edkDir(config), "data/system.ucf");
            printBuffer(new StringBuffer(xps().getUCFFile()), target);
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

        // deploy core sources
        for(File target : xps().getCoreSources().keySet())
            try {
                IO.debug("copying " + xps().getCoreSources().get(target) + " to "+ target.getPath());
                copyFile(new File(xps().getCoreSources().get(target)), target);
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }

        // deploy core pao files
        for(File target : xps().getPAOFiles().keySet())
            try {
                IO.debug("deploying " + target);
                write(target, xps().getPAOFiles().get(target));
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }

        // deploy core mpd files
        for(File target : xps().getMPDFiles().keySet())
            try {
                IO.debug("deploying " + target);

                buffer   = new StringBuffer();
                unparser = new MHSUnparser(buffer);
                unparser.visit(xps().getMPDFiles().get(target));

                printBuffer(buffer, target);
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
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
            runProcess(pb, args, config, errors);

        } catch (GenerationFailed e) {
            errors.addWarning(new Warning("failed to correctly terminate xps process"));
        } catch (IOException e) {
            errors.addWarning(new Warning("failed to generate .bit file\n" + e.getMessage()));
        } catch (InterruptedException e) {
            errors.addWarning(new Warning("failed to generate .bit file\n" + e.getMessage()));
        }
    }

    /** Starts whatever external tool is responsible for generation of the ELF file */
    protected void generateELFFile(Configuration config, ErrorCollection errors) {
        config.IOHANDLER().println("running sdk to generate elf file (this may take some time) ...");

        try {
            // import all required projects to workspace
            runProcess(new ProcessBuilder(sdkCommand("import", "hw")).directory(sdkDir(config)),
                new String[0], config, errors);
            runProcess(new ProcessBuilder(sdkCommand("import", "app_bsp")).directory(sdkDir(config)),
                new String[0], config, errors);
            runProcess(new ProcessBuilder(sdkCommand("import", "app")).directory(sdkDir(config)),
                new String[0], config, errors);

            //build the project
            runProcess(new ProcessBuilder(sdkCommand("build", "app")).directory(sdkDir(config)),
                new String[0], config, errors);
        } catch(GenerationFailed e) {
            errors.addError(e);
        } catch(Exception e) {
            errors.addError(new GenerationFailed("Failed to generate .elf file: " + e.getMessage()));
        }
    }

    /**
     * Initialises the bitfile with the elf file
     */
    private void runBitInit(Configuration config, ErrorCollection errors) {
        try {
            // create required directories
            if(!config.serverDir().mkdirs())
                errors.addError(new GenerationFailed("Could not generate required directories"));

            if(errors.hasErrors()) return;

            // build and run bitinit process
            ProcessBuilder pb = new ProcessBuilder(
                "bitinit", "system.mhs",
                "-p", device,
                "-pe", "microblaze_0", "../sdk/app/Debug/app.elf",
                "-o", config.serverDir().getCanonicalPath()
            ).directory(edkDir(config));
            runProcess(pb, new String[0], config, errors);
        } catch (GenerationFailed e) {
            errors.addError(e);
        } catch (Exception e) {
            errors.addError(new GenerationFailed("Could not initialise bit file with elf file: " + e.getMessage()));
        }
    }

    private static void runProcess(ProcessBuilder pb, String[] args, Configuration config, ErrorCollection errors)
            throws IOException, InterruptedException, GenerationFailed{

        BufferedReader input = null;

        // if the run is verbose, merge error and input streams
        if(config.VERBOSE()) pb = pb.redirectErrorStream(true);

        try {
            // start the process
            Process p = pb.start();

            // get output stream of the process (which is an input stream for this program)
            if(config.VERBOSE())
                input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            else
                input = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // feed the required commands to the spawned process
            PrintWriter pWriter = new PrintWriter(p.getOutputStream());
            for(String s : args) pWriter.println(s);
            pWriter.close();

            // print the output stream of the process
            String line;
            while((line = input.readLine()) != null)
                config.IOHANDLER().println(line);

            // wait for the process to terminate and store the result
            int rslt = p.waitFor();

            // if something went wrong, print a warning
            if(rslt != 0) throw new GenerationFailed("Process terminated with result " + rslt);
        } finally {
            try {
                if(input != null) input.close();
            } catch(IOException e) { /* well... memory leak... */ }
        }
    }

    private static String[] sdkCommand(String command, String project) {
        return new String[] {
            "xsdk",
            "-eclipseargs",
//            "-vm",
//            "/software/Xilinx_14.4/14.4/ISE_DS/ISE/java6/lin64/jre/bin",
            "-nosplash",
            "-application",
            "org.eclipse.cdt.managedbuilder.core.headlessbuild",
            "-" + command,
            project,
            "-data",
            ".",
            "-vmargs",
            "-Dorg.eclipse.cdt.core.console=org.eclipse.cdt.core.systemConsole"
        };
    }
}
