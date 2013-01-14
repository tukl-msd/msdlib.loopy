package de.hopp;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static de.hopp.generator.utils.Ethernet.*;
import static de.hopp.generator.utils.BoardUtils.printBoard;

import de.hopp.generator.Generator;
import de.hopp.generator.board.Board;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.parser.Parser2;

public class Main {
    
    private final static String version = "0.0.1";
    
    private Configuration config;
    
    public Main() {
        this.config = new Configuration();
    }
    
    public static void main(String[] args) {
        System.out.println("HOPP Driver Generator (Version " + version + ")");
        Main main = new Main();
        
        try{
            main.run(args);
        } catch(ExecutionFailed e){
            System.out.println("FAILED");
            System.exit(1);
        } 
    }
    
    private static void showUsage() {
         // show usage line
        System.out.println("Usage: java de.xcend.binding.Main [options] <filename>");
        System.out.println();
    
        // show flags
        System.out.println("Options:");
        System.out.println();
        System.out.println(" ---------- directory related ----------");
//        System.out.println(" -n <name>");
//        System.out.println(" --name <name>         generate classfile with filename <name>.");
//        System.out.println("                       If this is not set, the default name");
//        System.out.println("                       " + "" + " is used.");
//        System.out.println(" -d <dir>");
//        System.out.println(" --dest <dir>          generate classfile to <dir>.");
//        System.out.println("                       If this is not set, the classfile is generated to");
//        System.out.println("                       the current working directory.");

        System.out.println(" -s <dir> ");
        System.out.println(" --server <dir>        generate server files fo <dir>. If this is not set,");
        System.out.println("                       the server files are generated to ./" +
                Configuration.defaultServerDir + ".");
        System.out.println(" -c <dir> ");
        System.out.println(" --client <dir>        generate client files fo <dir>. If this is not set, ");
        System.out.println("                       the client files are generated to ./" +
                Configuration.defaultClientDir + ".");
        System.out.println();
        System.out.println(" ---------- miscellaneous ----------");
        System.out.println(" -d --debug            enables debug mode for the generated driver,");
        System.out.println("                       which will cause THE DRIVER to produce additional");
        System.out.println("                       console output.");
        System.out.println(" -v --verbose          makes the driver generator produce additional");
        System.out.println("                       console output.");
        System.out.println(" -h --help             show this help.");
        System.out.println();
        System.out.println(" ---------- ethernet related ----------");
        System.out.println(" --mac <mac>           modify mac address of the board.");
        System.out.println("                       The default value is " + unparseMAC(Configuration.defaultMAC));
        System.out.println(" --ip <ip>             modify ip address of the board.");
        System.out.println("                       The default value is " + unparseIP(Configuration.defaultIP));
        System.out.println(" --mask <mask>         modify the network mask of the board.");
        System.out.println("                       The default value is " + unparseIP(Configuration.defaultMask));
        System.out.println(" --gw <gw>             modify standard gateway of the board.");
        System.out.println("                       The default value is " + unparseIP(Configuration.defaultGW));
        System.out.println(" --port <port>         modify the standard port of the board.");
        System.out.println("                       The default port is " + Configuration.defaultPort);
        System.out.println();
    }

    private void run(String[] args) throws ExecutionFailed {
        
        // parse all cli parameters
        String schema = parseParameters(args);
        
        File schemaFile = new File(schema);
        
        // check if the given string references an existing file
        if(!schemaFile.exists()) {
            System.err.println("ERROR: Could not find file " + schemaFile.getPath());
            throw new ExecutionFailed();
        } else if(!schemaFile.isFile()) {
            System.err.println("ERROR: " + schemaFile.getPath() + " is no file");
            throw new ExecutionFailed();
        }
        
        // print parsed cli parameters
        System.out.println();
        System.out.println("started HOPP Driver Generator with the following command line parameters:");
        System.out.println("  source .mhs file : " + schemaFile);
        Configuration.printConfig(config);
        
        // start parser
        System.out.println();
        System.out.println("starting parser");
        Board board = new Parser2(config).parse(schemaFile);
        System.out.println("parser finished");
        if(config.verbose()) System.out.println("  DEBUG: " + printBoard(board));
        
        // instantiate and run generator with this configuration
        System.out.println();
        System.out.println("starting c/c++ generator");
        Generator generator = new Generator(config, board);
        generator.generate();
        System.out.println();
        System.out.println("generator finished");
        
        // finished
        System.out.println();
        System.out.println("Done");
    }

    private String parseParameters(String[] args) throws ExecutionFailed {
    
        // take away system configuration options
        args = parseOptions(args);

        // check if any unused flags remain
        for(String flag : args) {
            if(flag.startsWith("-")) {

                // print an error message and exit
                System.err.println();
                System.err.println("unknown flag: " + flag);
                System.err.println();
                showUsage();
                throw new ExecutionFailed();
            }
        }
        
        // check if there is a file name left
        if(args.length < 1) {
            System.err.println();
            System.err.println("no file name given");
            System.err.println();
            showUsage();
            throw new ExecutionFailed();
        }

        // check if there is a file name left
        if(args.length > 1) {
            System.err.println();
            System.err.println("too many file names given");
            System.err.println();
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
                System.err.println("no argument left for "+args[i]);
                throw new ExecutionFailed();
            }
            config.setClientDir(new File(args[++i]));
          } else if(args[i].equals("-s") || args[i].equals("--server")) {
            
            if(i + 1 >= args.length) {
                System.err.println("no argument left for "+args[i]);
                throw new ExecutionFailed();
            }
            config.setServerDir(new File(args[++i]));
//          } else if(args[i].equals("-d") || args[i].equals("--dest")) {
//            
//            if(i + 1 >= args.length) {
//                System.err.println("no argument left for "+args[i]);
//                throw new ExecutionFailed();
//            }
//            config.setDestDir(new File(args[++i]));
//            
//          // SCHEMANAME flags
//          } else if(args[i].equals("-n") || args[i].equals("--name")) {
//                  
//              if(i + 1 >= args.length) {
//                  System.err.println("no argument left for "+args[i]);
//                  throw new ExecutionFailed();
//              }
//            config.setName(args[++i]);
            
            // ETHERNET CONFIG flags
            } else if(args[i].equals("--mac")) {
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setMac(args[++i].split(":"));
            } else if(args[i].equals("--ip")) {
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setIP(args[++i].split("[.]"));
            } else if(args[i].equals("--mask")) {
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setMask(args[++i].split("[.]"));
            } else if(args[i].equals("--gw")) {
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setGW(args[++i].split("[.]"));
            } else if(args[i].equals("--port")) {
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    int port = Integer.valueOf(args[++i]);
                    if(port < 0) throw new NumberFormatException();
                    config.setPort(port);
                } catch(NumberFormatException e) {
                    throw new IllegalArgumentException("invalid value for port. Has to be an integer >= 0");
                }
                
            // DEBUG flags
            } else if(args[i].equals("-d") || args[i].equals("--debug")) {
                config.enableDebug();
             
            // VERBOSE flags
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                config.enableVerbose();

            // USAGE HELP flag
            } else if(args[i].equals("-h") || args[i].equals("--help")) {
                showUsage();
                throw new ExecutionFailed();
            } else remaining.add(args[i]);
        }
        
        return remaining.toArray(new String[0]);
    }
}
