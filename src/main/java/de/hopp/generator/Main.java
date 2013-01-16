package de.hopp.generator;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static de.hopp.generator.utils.Ethernet.*;
import static de.hopp.generator.utils.BoardUtils.printBoard;

import de.hopp.generator.board.Board;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.parser.Parser2;

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
        
        try{
            main.run(args);
        } catch(ExecutionFailed e){
            System.exit(1);
        } 
    }
    
    private void showUsage() {
        config.setPrintLevel(Configuration.LOG_INFO);
         // show usage line
        IO.println("Usage: java de.xcend.binding.Main [options] <filename>");
        IO.println();
    
        // show flags
        IO.println("Options:");
        IO.println();
        IO.println(" ---------- directory related ----------");
        IO.println(" -s <dir> ");
        IO.println(" --server <dir>        generate server files fo <dir>. If this is not set,");
        IO.println("                       the server files are generated to ./" +
                Configuration.defaultServerDir + ".");
        IO.println(" -c <dir> ");
        IO.println(" --client <dir>        generate client files fo <dir>. If this is not set, ");
        IO.println("                       the client files are generated to ./" +
                Configuration.defaultClientDir + ".");
        IO.println();
        IO.println(" ---------- miscellaneous ----------");
        IO.println(" -q --quite            suppresses console output from driver generator.");
        IO.println(" -v --verbose          makes the driver generator produce additional");
        IO.println("                       console output.");
        IO.println("                       Note that these flags overwrite each other.");
        IO.println(" -d --debug            enables debug mode for the generated driver,");
        IO.println("                       which will cause THE DRIVER to produce additional");
        IO.println("                       console output.");
        IO.println(" -h --help             show this help.");
        IO.println();
        IO.println(" ---------- ethernet related ----------");
        IO.println(" --mac <mac>           modify mac address of the board.");
        IO.println("                       The default value is " + unparseMAC(Configuration.defaultMAC));
        IO.println(" --ip <ip>             modify ip address of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultIP));
        IO.println(" --mask <mask>         modify the network mask of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultMask));
        IO.println(" --gw <gw>             modify standard gateway of the board.");
        IO.println("                       The default value is " + unparseIP(Configuration.defaultGW));
        IO.println(" --port <port>         modify the standard port of the board.");
        IO.println("                       The default port is " + Configuration.defaultPort);
        IO.println();
    }

    private void run(String[] args) {

        // first of all show the copyright information
        IO.showCopyright();
        
        // parse all cli parameters
        String schema = parseParameters(args);

        // check if any errors occurred already
        showStatus(true);
        
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
        
        // instantiate and run generator with this configuration
        IO.println();
        IO.println("starting c/c++ generator");
        Generator generator = new Generator(this, board);
        generator.generate();
        
        // finished
        showStatus(false);
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
            
          // DESTDIR flags
          if(args[i].equals("-c") || args[i].equals("--client")) {
            
            if(i + 1 >= args.length) {
                IO.error("no argument left for "+args[i]);
                throw new ExecutionFailed();
            }
            config.setClientDir(new File(args[++i]));
          } else if(args[i].equals("-s") || args[i].equals("--server")) {
            
            if(i + 1 >= args.length) {
                IO.error("no argument left for "+args[i]);
                throw new ExecutionFailed();
            }
            config.setServerDir(new File(args[++i]));
//          } else if(args[i].equals("-d") || args[i].equals("--dest")) {
//            
//            if(i + 1 >= args.length) {
//                IO.error("no argument left for "+args[i]);
//                throw new ExecutionFailed();
//            }
//            config.setDestDir(new File(args[++i]));
//            
//          // SCHEMANAME flags
//          } else if(args[i].equals("-n") || args[i].equals("--name")) {
//                  
//              if(i + 1 >= args.length) {
//                  IO.error("no argument left for "+args[i]);
//                  throw new ExecutionFailed();
//              }
//            config.setName(args[++i]);
            
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
                
            // LOGGING flags
            } else if(args[i].equals("-d") || args[i].equals("--debug")) {
                config.enableDebug();
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                config.enableVerbose();
            } else if(args[i].equals("-q") || args[i].equals("--quiet")) {
                config.enableQuiet();
                
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
    private void showStatus(boolean checkpoint) throws ExecutionFailed {
       
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
            IO.println(errors.numWarnings()+" warnings");
            IO.println(errors.numErrors()+" errors");

            // show that the build has failed
            IO.println();
            IO.println("BUILD FAILED");

            // show the time and exit
            IO.println();
            throw new ExecutionFailed();
        }
        
        // so there are no errors, look if there are some warning and print them
        if(errors.hasWarnings()) errors.showWarnings(IO);

        // if this was only a checkpoint, continue right now
        if(checkpoint) return;

        IO.println();
        
        // show that the build was successful
        IO.println("BUILD SUCCESSFUL");

        // show time and continue
        IO.println();
    }
}
