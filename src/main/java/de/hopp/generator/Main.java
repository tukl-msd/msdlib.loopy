package de.hopp.generator;

import static de.hopp.generator.frontend.BDL.BDLFilePos;
import static de.hopp.generator.utils.BoardUtils.printBoard;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.frontend.BDLFilePos;
import de.hopp.generator.frontend.ClientBackend;
import de.hopp.generator.frontend.Parser;
import de.hopp.generator.frontend.ServerBackend;

/**
 * Main class of the generator.
 * Marks the entry point for all calls.
 * Responsible for parameter parsing, top level console output and running frontend and backends.
 * @author Thomas Fischer
 */
public class Main {
    
    public final static String version = "0.0.1";
    
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
            main.run(args);
        } catch(ExecutionFailed e) {
            System.exit(1);
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
        IO.println(" -s --server <name>");
        IO.println("    --board <name>     selects the backend for generation of");
        IO.println("                       the board-side (server-side) driver.");
        IO.println(" -c --client <name>");
        IO.println("    --host <name>      selects the backend for generation of");
        IO.println("                       the host-side (client-side) driver");
        IO.println(" ---------- directory related ----------");
        IO.println(" -S --serverDir <dir>");
        IO.println("    --boardDir <dir>   generate files for the board to <dir>.");
        IO.println("                       If this is not set, the board files");
        IO.println("                       are generated to ./" + Configuration.defaultServerDir + "\".");
        IO.println(" -C --clientDir <dir>");
        IO.println("    --hostDir <dir>    generate files for the board to <dir>.");
        IO.println("                       If this is not set, the host files");
        IO.println("                       are generated to ./" + Configuration.defaultClientDir + "\".");
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
//        enables debug mode for the generated driver,");
//        IO.println("                       which will cause THE DRIVER to produce additional");
//        IO.println("                       console output.");
        IO.println();
        IO.println(" ---------- miscellaneous ----------");
        IO.println(" -o --parseonly        check only for parser errors");
        IO.println(" -n --dryrun           don't execute the generator phase of");
        IO.println("                       the backends, i.e. analyze only");
        IO.println("    --config <file>    supplies the generator with a config file containing");
        IO.println("                       all information configurable with cli parameters.");
        IO.println("                       This will immediately start the generator ignoring all");
        IO.println("                       further command line parameters");
        IO.println("    --gui              starts the graphical user interface of the generator.");
        IO.println("                       With this interface, config files can easily be created");
        IO.println("                       and directly executed. Further command line parameters");
        IO.println("                       will be ignored.");
        IO.println(" -h --help             show this help.");
        IO.println();
        
        for(ClientBackend backend : ClientBackend.values()) {

            // show backend name
            IO.println("Host Backend: " + backend.getInstance().getName());
            IO.println();

            // show backend usage and flags
            backend.getInstance().printUsage(IO);
            IO.println();
        }
        
        for(ServerBackend backend : ServerBackend.values()) {

            // show backend name
            IO.println("Server Backend: " + backend.getInstance().getName());
            IO.println();

            // show backend usage and flags
            backend.getInstance().printUsage(IO);
            IO.println();
        }
    }

    private void run(String[] args) {

        // first of all show the copyright information
        IO.showCopyright();
        
        // parse all cli parameters
        String schema = parseParameters(args);

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
        
        // abort if any errors occurred
        showStatus(false);
        
        // print the board (verbose only)
        IO.verbose(printBoard(board) + "\n");
        
        // print the raw board term (debug only)
        IO.debug(board.term().toString() + "\n");

        // if this is a parseonly run, stop here
        if(config.parseonly()) {
            showStatus(true);
            return;
        }
        
        // unparse generated server models to corresponding files
        if(config.server() != null) {
            IO.println("starting up " + config.server().getName() + " board backend ...");
        
            config.server().generate(board, config, errors);
        
            // abort if any errors occurred
            IO.println();
            showStatus(false);
            
            IO.println("backend finished");
        }
        
        if(config.client() != null) {
            // unparse generated client models to corresponding files
            IO.println();
            IO.println("starting up " + config.client().getName() + " host backend ...");
        
            config.client().generate(board, config, errors);
            
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
        if(config.server() != null) {
            config = config.server().parseParameters(config, args);
            args   = config.UNUSED();
            config.setUnusued(new String[0]);
        }
        if(config.client() != null) {
            config = config.client().parseParameters(config, args);
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
                    config.setClient(ClientBackend.fromName(args[++i]).getInstance());
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
                
            } else if(args[i].equals("-s") || args[i].equals("--server") || args[i].equals("--board")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    config.setServer(ServerBackend.fromName(args[++i]).getInstance());
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
                
            // DESTDIR flags TODO push these into backends
            } else if(args[i].equals("-C") || args[i].equals("--clientDir") || args[i].equals("--hostDir")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setClientDir(new File(args[++i]));
                
            } else if(args[i].equals("-S") || args[i].equals("--serverDir") || args[i].equals("--boardDir")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setServerDir(new File(args[++i]));
                
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
                
            } else if(args[i].equals("-c") || args[i].equals("--config")) {
                // TODO run generator with the provided config
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
            } else if(args[i].equals("--gui")) {
                // TODO run generator gui
            // USAGE HELP flag
            } else if(args[i].equals("-h") || args[i].equals("--help")) {
                showUsage();
                throw new ExecutionFailed();
                
            } else remaining.add(args[i]);
        }
        
        return remaining.toArray(new String[0]);
    }
    
    /**
     * Show the current status, end program if there were errors
     * @param checkpoint whether to show the status for a checkpoint only or to summarize all we can
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
