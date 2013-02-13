package de.hopp.generator;

import static de.hopp.generator.utils.BoardUtils.printBoard;
import static de.hopp.generator.utils.Ethernet.unparseIP;
import static de.hopp.generator.utils.Ethernet.unparseMAC;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.hopp.generator.backends.Backend;
import de.hopp.generator.board.Board;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.frontend.ClientBackend;
import de.hopp.generator.frontend.Parser2;
import de.hopp.generator.frontend.ProjectBackend;
import de.hopp.generator.frontend.ServerBackend;

public class Main {
    
    public final static String version = "0.0.1";
    
    private Configuration config;
    private final IOHandler IO;
    private final ErrorCollection errors;
    
    private Backend clientBackend;
    private Backend serverBackend;
    
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
    
        // show flags
        IO.println("Options:");
        IO.println();
        IO.println(" ---------- directory related ----------");
        IO.println(" -s <dir> ");
        IO.println(" --server <dir>        generate server files fo <dir>. If this is not set,");
        IO.println("                       the server files are generated to ./" +
                Configuration.defaultServerDir + "\".");
        IO.println(" -c <dir> ");
        IO.println(" --client <dir>        generate client files fo <dir>. If this is not set, ");
        IO.println("                       the client files are generated to ./" +
                Configuration.defaultClientDir + "\".");
        IO.println();
        IO.println(" ---------- console logging ----------");
        IO.println(" -q --quiet            suppresses console output from driver generator.");
        IO.println(" -v --verbose          sets the log level to verbose resulting in additional");
        IO.println("                       console output from the driver generator.");
        IO.println("                       Note that the log level flags overwrite each other.");
        IO.println(" -d --debug            enables debug mode for the generated driver,");
        IO.println("                       which will cause THE DRIVER to produce additional");
        IO.println("                       console output.");
        IO.println();
        IO.println(" ---------- miscellaneous ----------");
        IO.println("    --config <file>    supplies the generator with a config file containing");
        IO.println("                       all information configurable with command line parameters.");
        IO.println("                       This will immediately start the generator ignoring all");
        IO.println("                       other command line parameters");
        IO.println("    --gui              starts the graphical user interface of the generator.");
        IO.println("                       With this interface, config files can easily be created");
        IO.println("                       and directly executed. Other command line parameters");
        IO.println("                       will be ignored.");
        IO.println(" -h --help             show this help.");
        IO.println();
        IO.println(" ---------- ethernet related ----------");
        IO.println(" --mac <mac>           modifies mac address of the board.");
        IO.println("                       The default value is " + unparseMAC(Configuration.defaultMAC));
        IO.println(" --ip <ip>             modifies ip address of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultIP));
        IO.println(" --mask <mask>         modifies the network mask of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultMask));
        IO.println(" --gw <gw>             modifies standard gateway of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultGW));
        IO.println(" --port <port>         modifies the standard port of the board.");
        IO.println("                       The default port is " + Configuration.defaultPort);
        IO.println();
    }

    private void run(String[] args) {

        // first of all show the copyright information
        IO.showCopyright();
        
        // parse all cli parameters
        String schema = parseParameters(args);

        // check if any errors occurred already
        showStatus();
        
        // check, if backens are specified
        if(clientBackend == null) errors.addError(new UsageError("No client backend selected"));
        if(serverBackend == null) errors.addError(new UsageError("No server backend selected"));

        // check if any errors occurred already
        showStatus();
        
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
        IO.println("- source .mhs file : " + schemaFile);
        config.printConfig();
        
        // start parser
        IO.println();
        IO.println("starting parser");
        Board board = new Parser2(config).parse(schemaFile);
        IO.debug(printBoard(board));
        
        // if there are errors abort here
        showStatus();
        
        // unparse generated server models to corresponding files
        IO.println();
        IO.println("generating server side driver files ...");
        
        serverBackend.generate(board, config, errors);
        
        // abort if any errors occurred
        showStatus();
        
        // unparse generated client models to corresponding files
        IO.println();
        IO.println("generating client side driver files ...");
        clientBackend.generate(board, config, errors);
        
        // abort if any errors occurred
        showStatus();
        
        // run doxygen generation
        // TODO this also shouldn't be done for all board types, I guess...?
        // Maybe there are boards using Java and therefore javadoc generation should be applied
        // (although doxygen would work to in these cases;)
        
        // finished
        showStatus();
        IO.println();
        
        // show that the build was successful
        IO.println("BUILD SUCCESSFUL");
    }

    private String parseParameters(String[] args) throws ExecutionFailed {
    
        // take away system configuration options
        args = parseOptions(args);

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
            
            // DESTDIR flags TODO push these into backends
            if(args[i].equals("-C") || args[i].equals("--clientDir")) {
            
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setClientDir(new File(args[++i]));
            } else if(args[i].equals("-S") || args[i].equals("--serverDir")) {
            
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setServerDir(new File(args[++i]));
            } else
            // BACKEND flags
            if(args[i].equals("-c") || args[i].equals("--client")) {
              
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    clientBackend = ClientBackend.fromName(args[++i]).getInstance();
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
            } else if(args[i].equals("-s") || args[i].equals("--server")) {
              
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    serverBackend = ServerBackend.fromName(args[++i]).getInstance();
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
            } else if(args[i].equals("-p") || args[i].equals("--project")) {
                // project backend - currently only xps14.1
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    ProjectBackend.fromName(args[++i]);
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
            // LOGGING flags
            } else if(args[i].equals("-d") || args[i].equals("--debug")) {
                config.enableDebug();
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                config.enableVerbose();
            } else if(args[i].equals("-q") || args[i].equals("--quiet")) {
                config.enableQuiet();
                
            } else if(args[i].equals("--config")) {
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
                
             // ETHERNET CONFIG flags
            } else if(args[i].equals("--mac")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setMac(args[++i].split(":"));
            } else if(args[i].equals("--ip")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setIP(args[++i].split("[.]"));
            } else if(args[i].equals("--mask")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setMask(args[++i].split("[.]"));
            } else if(args[i].equals("--gw")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setGW(args[++i].split("[.]"));
            } else if(args[i].equals("--port")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    int port = Integer.valueOf(args[++i]);
                    if(port < 0) throw new NumberFormatException();
                    config.setPort(port);
                } catch(NumberFormatException e) {
                    throw new IllegalArgumentException("invalid value for port. Has to be an integer >= 0");
                }
                
            } else remaining.add(args[i]);
        }
        
        return remaining.toArray(new String[0]);
    }
    
    /**
     * Show the current status, end program if there were errors
     * @param checkpoint whether to show the status for a checkpoint only or to summarize all we can
     * @throws ExecutionFailed if the error collection contains error, this method will abort execution
     */
    private void showStatus() throws ExecutionFailed {
       
        // TODO remove this... put it somewhere else
        if(errors.hasErrors() || errors.hasWarnings())
            IO.println();

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
        
        // so there are no errors, look if there are some warning and print them
        if(errors.hasWarnings()) errors.showWarnings(IO);
    }
}
