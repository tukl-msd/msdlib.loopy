package de.hopp.generator;

import static de.hopp.generator.frontend.BDL.BDLFilePos;
import static de.hopp.generator.utils.BoardUtils.printBoard;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.frontend.BDLFilePos;
import de.hopp.generator.frontend.Board;
import de.hopp.generator.frontend.Host;
import de.hopp.generator.frontend.Parser;
//import static de.upb.hni.vmagic.parser.VhdlParser.parseFile;
//import de.upb.hni.vmagic.VhdlFile;
//import de.upb.hni.vmagic.libraryunit.Entity;
//import de.upb.hni.vmagic.libraryunit.LibraryUnit;
//import de.upb.hni.vmagic.parser.VhdlParserException;
import de.hopp.generator.frontend.Workflow;

/**
 * Main class of the generator.
 * Marks the entry point for all calls.
 * Responsible for parameter parsing, top level console output and running frontend and backends.
 * @author Thomas Fischer
 */
public class Main {

    public final static String version = "0.2";

    private Configuration config;
    private final IOHandler IO;
    private final ErrorCollection errors;

    public Configuration config()   { return config; }
    public IOHandler io()           { return IO; }
    public ErrorCollection errors() { return errors; }

    public Main() {
        this.config = new Configuration();
        IO = config.IOHANDLER();
        errors = new ErrorCollection();
    }

    public static void main(String[] args) {
        Main main = new Main();

        try {
//            VhdlFile file = parseFile("coresources/myQueue.vhd");
//            Entity e = null;
//
//            for(LibraryUnit u : file.getElements()) {
//                if(u instanceof Entity)
//                    if(e == null) e = (Entity)u;
//                    else throw new RuntimeException("multiple entities found in file");
//            }
//
//            if(e == null) throw new RuntimeException("no entity found in sourcefile");
//
//            System.out.println("Entity " + e.getIdentifier());

//            for(VhdlObjectProvider<Constant> vop : e.getGeneric()) {
//                for(Constant c : vop.getVhdlObjects()) {
//                    System.out.println(c.getIdentifier() + " = " + c.getDefaultValue());
//                }
//            }
//
//            for(VhdlObjectProvider<Signal> vop : e.getPort()) {
//                for(Signal s : vop.getVhdlObjects()) {
//                    System.out.println("  signal: " + s.getIdentifier());
//                    System.out.println("    kind: " + s.getKind().toString());
//                    System.out.println("    mode: " + s.getMode().toString());
//                    if(s.getType() instanceof IndexSubtypeIndication) {
//                        IndexSubtypeIndication isi = (IndexSubtypeIndication) s.getType();
//                        for(DiscreteRange<?> range : isi.getRanges()) {
//                            if(range instanceof Range) {
//                                Range r = (Range) range;
//                                System.out.println("    range [" +
//                                    unparseExpression(r.getFrom()) + ":" +
//                                    unparseExpression(r.getTo()) + "]");
//                            }
//                        }
//                    }
//
//                    System.out.println("  default value: " +
//                        (s.getDefaultValue() == null ? "null" : s.getDefaultValue().toString()));
//                }
//            }
            main.run(args);
        } catch(ExecutionFailed e) {
            System.exit(1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (VhdlParserException e) {
//            e.printStackTrace();
        }
    }

    private void showUsage() {
        config.setPrintLevel(Configuration.LOG_INFO);

        // show usage line
        IO.println("Usage: java de.hopp.generator.Main [options] <filename>");
        IO.println();

        // problem with naming of short parameters ):
        //  -client/server is misleading and should be avoided --> usage of board/host...
        //  -h is already used for help (and that should not be changed, as it is rather the default)
        //  -backend/frontend of driver also seems weird (since we select backends of the generator...)
        //  -usage of generic backend here seems out of the question, since we require also the destdir parameter for both
        // alternative: do not allow different client and server directories,
        // use -d for destination, -b for backend, distinguish between host and board backends here

        // show flags
        IO.println("Options:");
        IO.println();
        IO.println(" ---------- backend selection ----------");
        IO.println(" -c --client <name>");
        IO.println("    --host <name>      selects the backend for generation of");
        IO.println("                       the host-side (client-side) driver");
        IO.println(" -s --server <name>");
        IO.println(" -b --board <name>     selects the board, for which a driver");
        IO.println("                       should be generated.");
        IO.println(" -w --workflow <name>  selects the workflow that should be used");
        IO.println("                       for hardware synthesis.");
        IO.println(" ---------- directory related ----------");
        IO.println(" -S --serverDir <dir>");
        IO.println(" -B --boardDir <dir>   generate files for the board to <dir>.");
        IO.println("                       If this is not set, the board files");
        IO.println("                       are generated to ./" + Configuration.defaultBoardDir + "\".");
        IO.println(" -C --clientDir <dir>");
        IO.println(" -H --hostDir <dir>    generate files for the host to <dir>.");
        IO.println("                       If this is not set, the host files");
        IO.println("                       are generated to ./" + Configuration.defaultHostDir + "\".");
        IO.println(" -t --temp <dir>       generate temporary files into <dir>.");
        IO.println("                       If this is not set, the termporary files");
        IO.println("                       are generated to ./" + Configuration.defaultTempDir + "\".");
        IO.println();
        IO.println(" ---------- console logging ----------");
        IO.println(" -q --quiet            suppresses console output from driver generator.");
        IO.println(" -v --verbose          sets the log level to verbose resulting in additional");
        IO.println("                       console output from the driver generator.");
        IO.println(" -d --debug            sets the log level to debug resulting in additional");
        IO.println("                       console output from the driver generator.");
        IO.println("                       Note that the log level flags overwrite each other.");
        IO.println();
        IO.println(" ---------- miscellaneous ----------");
        IO.println(" -o --parseonly        check only for parser errors");
        IO.println(" -n --dryrun           don't execute the generator phase of");
        IO.println("                       the backends, i.e. analyze only.");
        IO.println("    --nogen            disable generation of .bit and .elf files.");
        IO.println("    --sdkonly          disable generation of .bit file and sources.");
        IO.println("    --config <file>    supplies the generator with a config file containing");
        IO.println("                       all information configurable with cli parameters.");
        IO.println("                       This will immediately start the generator ignoring all");
        IO.println("                       further command line parameters");
        IO.println("    --gui              starts the graphical user interface of the generator.");
        IO.println("                       With this interface, config files can easily be created");
        IO.println("                       and directly executed. Further command line parameters");
        IO.println("                       will be ignored.");
        IO.println(" -h --help             show this help. Append a backend to get its help.");
        IO.println();

        // Print backends
        IO.println("Supported host languages:");
        for(Host backend : Host.values()) {
            // FIXME comma separated list?
            IO.println(" - " + backend.getInstance().getName());
        }

        IO.println("Supported boards:");
        for(Board backend : Board.values()) {
            // FIXME comma separated list?
            IO.println(" - " + backend.getInstance().getName());
        }

        IO.println("Supported workflows:");
        for(Workflow backend : Workflow.values()) {
            // FIXME comma separated list?
            IO.println(" - " + backend.getInstance().getName());
        }

        IO.println();
    }

    private void run(String[] args) {

        // first of all show the copyright information
        IO.showCopyright();

        // parse all cli parameters
        String schema = parseParameters(args);

//        if(config.startGUI()) {
            // if GUI mode has been started, inject all parsed parameters and ignore invalid flags
//            de.hopp.generator.gui.GUI.main(new String[0]);
//            return;
//        }

        // abort if any errors occurred
        showStatus(false);

        // check, if backends are specified
//        if(clientBackend == null) errors.addError(new UsageError("No client backend selected"));
//        if(serverBackend == null) errors.addError(new UsageError("No server backend selected"));
//
//         // abort if any errors occurred
//        showStatus();

        File schemaFile = new File(schema);

        // check if the given string references an existing file
        if(!schemaFile.exists()) {
            IO.error("Could not find file " + schemaFile.getPath());
            throw new ExecutionFailed();
        } else if(!schemaFile.isFile()) {
            IO.error(schemaFile.getPath() + " is no file");
            throw new ExecutionFailed();
        }

        // print parsed cli parameters
        IO.println();
        IO.println("HOPP Driver Generator executed with:");
        IO.println("- source bdf file : " + schemaFile);
        config.printConfig();

        // start parser
        IO.println();
        IO.println("starting parser");
        BDLFilePos board = BDLFilePos(new Parser(errors).parse(schemaFile));
        IO.println();

        if(board != null && board.term() != null) {
            // print the raw board term (debug only)
            IO.debug(board + "\n");

            // print the board (verbose only)
            IO.verbose(printBoard(board) + "\n");
        }

        // abort if any errors occurred
        showStatus(false);

        // if this is a parseonly run, stop here
        if(config.parseonly()) {
            showStatus(true);
            return;
        }

        // unparse generated server models to corresponding files
        if(config.flow() != null) {
            IO.println("starting up " + config.flow().getName() + " board backend ...");

            config.flow().generate(board, config, errors);

            // abort if any errors occurred
            IO.println();
            showStatus(false);

            IO.println("backend finished");
        }

        // unparse generated client models to corresponding files
        if(config.host() != null) {
            IO.println();
            IO.println("starting up " + config.host().getName() + " host backend ...");

            config.host().generate(board, config, errors);

            // abort if any errors occurred
            IO.println();
            showStatus(false);

            IO.println("backend finished");
        }

        // finished
        IO.println();
        showStatus(true);

    }

//    if(args[i].equals("-p")) {
//        // basically: do this in each and every option... ): doesn't seem to be a nice solution...
//        if(curBackend != null) {
//          for(String flag : curBackend.parseParameters(backendArgs)) {
//            // check if any unused flags remain... (cf current parseParameters())
//          }
//         curBackend == null;
//        }
//        config.board() = getInstance();
//        curBackend = config.board();
//        backendArgs = new LinkedList<String>;
//    } else (backendArgs.add(args[i]);

//    alternative: normal for-loop, list containing all valid system params
//    at backend parameter check if i+1 is in the list (with a while loop)
//      if not add to backend param list and i++
//      if it is parse the backend param list with the backend and continue

//    alternative attempt without list:
//    add backend parameter to option parse
//    if it is null, ignore it and throw errors for unknown flags
//    if it is not null, pass everything unknown to the backend
//    if it is not null and we find a known flag let the backend parse the block and parse the known flag (maybe with a null backend again)
//    if the end of the input is reached... well... add whatever you get to the remaining list...
    private String parseParameters(String[] args) throws ExecutionFailed {

        // take away system configuration options
        args = parseOptions(args);

        // pass remaining flags to backends
        if(config.flow() != null) {
            config = config.flow().parseParameters(config, args);
            args   = config.UNUSED();
            config.setUnusued(new String[0]);
        }
        if(config.host() != null) {
            config = config.host().parseParameters(config, args);
            args   = config.UNUSED();
            config.setUnusued(new String[0]);
        }

        // check if any unused flags remain
        for(String flag : args) {
            if(flag.startsWith("-")) {

                // print an error message and exit
                IO.println();
                IO.error("unknown flag: " + flag);
                IO.println();
                showUsage();
                throw new ExecutionFailed();
            }
        }

        // check if there is a file name left
        if(args.length < 1) {
            IO.println();
            IO.error("no file name given");
            IO.println();
            showUsage();
            throw new ExecutionFailed();
        }

        // check if there is a file name left
        if(args.length > 1) {
            IO.println();
            IO.error("too many file names given");
            IO.println();
            showUsage();
            throw new ExecutionFailed();
        }

        // return the argument
        return args[0];
    }

    private String[] parseOptions(String[] args) throws ExecutionFailed {

        // store remaining arguments
        List<String> remaining = new LinkedList<String>();

        // go through all parameters
        for(int i = 0; i < args.length; i++) {
            // BACKEND flags
            if(args[i].equals("-c") || args[i].equals("--client") || args[i].equals("--host")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    config.setHost(Host.fromName(args[++i]).getInstance());
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }

            } else if(args[i].equals("-b") || args[i].equals("--board")
                   || args[i].equals("-s") || args[i].equals("--server")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    config.setBoard(Board.fromName(args[++i]).getInstance());
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
            } else if(args[i].equals("-w") || args[i].equals("--workflow")) {
             if(i + 1 >= args.length) {
                 IO.error("no argument left for "+args[i]);
                 throw new ExecutionFailed();
             }
             try {
                 config.setFlow(Workflow.fromName(args[++i]).getInstance());
             } catch(IllegalArgumentException e) {
                 IO.error(e.getMessage());
                 throw new ExecutionFailed();
             }

            // DESTDIR flags TODO push these into backends?
            } else if(args[i].equals("-H") || args[i].equals("--hostDir")
                   || args[i].equals("-C") || args[i].equals("--clientDir")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setHostDir(new File(args[++i]));

            } else if(args[i].equals("-B") || args[i].equals("--boardDir")
                   || args[i].equals("-S") || args[i].equals("--serverDir")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setBoardDir(new File(args[++i]));

            } else if(args[i].equals("-t") || args[i].equals("--temp")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setTempDir(new File(args[++i]));

            // LOGGING flags
            } else if(args[i].equals("-d") || args[i].equals("--debug")) {
                config.enableDebug();
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                config.enableVerbose();
            } else if(args[i].equals("-q") || args[i].equals("--quiet")) {
                config.enableQuiet();

            // PROGRESS flags
            } else if(args[i].equals("-o") || args[i].equals("--parseonly")) {
                config.enableParseonly();
            } else if(args[i].equals("-n") || args[i].equals("--dryrun")) {
                config.enableDryrun();
            } else if(args[i].equals("--nogen")) {
                config.enableNoGen();
            } else if(args[i].equals("--sdkonly")) {
                config.enableSDKOnly();

            } else if(args[i].equals("--config")) {
                // TODO run generator with the provided config
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
//            } else if(args[i].equals("--gui")) {
//                config.enableGUI();
            // USAGE HELP flag
            } else if(args[i].equals("-h") || args[i].equals("--help")) {
                if(i + 1 < args.length) {
                    for(Host backend : Host.values())
                        if(backend.getInstance().getName().equals(args[i+1])) {
                            IO.println("Usage help of " + backend.getInstance().getName() + " host backend:");
                            backend.getInstance().printUsage(IO);
                            IO.println();
                            throw new ExecutionFailed();
                        }

                    for(Board backend : Board.values())
                        if(backend.getInstance().getName().equals(args[i+1])) {
                            IO.println("Usage help of " + backend.getInstance().getName() + " board backend:");
                            backend.getInstance().printUsage(IO);
                            IO.println();
                            throw new ExecutionFailed();
                        }

                    for(Workflow backend : Workflow.values())
                        if(backend.getInstance().getName().equals(args[i+1])) {
                            IO.println("Usage help of " + backend.getInstance().getName() + " workflow backend:");
                            backend.getInstance().printUsage(IO);
                            IO.println();
                            throw new ExecutionFailed();
                        }
                }

                showUsage();
                throw new ExecutionFailed();

            } else remaining.add(args[i]);
        }

        return remaining.toArray(new String[0]);
    }

    /**
     * Show the current status, end program if there were errors
     * @param done whether to show the status for a checkpoint only or to summarize all we can
     * @throws ExecutionFailed if the error collection contains error, this method will abort execution
     */
    private void showStatus(boolean done) throws ExecutionFailed {

        // check if there were errors
        if(errors.hasErrors()) {

            // print errors and warnings
            if(errors.hasWarnings()) errors.showWarnings(IO);
            errors.showErrors(IO);

            // give an account of their numbers
            IO.println();
            IO.println(errors.numWarnings() + " warnings");
            IO.println(errors.numErrors()   + " errors");

            // show that the build has failed
            IO.println();
            IO.println("BUILD FAILED");

            // show the time and exit
            IO.println();
            throw new ExecutionFailed();
        }

        // stop here, if the call will continue (we want all warnings printed at the end of the call!)
        if(!done) return;

        // so there are no errors, look if there are some warning and print them
        if(errors.hasWarnings()) errors.showWarnings(IO);

        IO.println();

        // show that the build was successful
        IO.println("BUILD SUCCESSFUL");
    }

}
