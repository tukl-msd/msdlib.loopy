package de.hopp;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

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
        System.out.println(" --mac <mac>           modify mac address of the board.");
        System.out.println(" --ip <ip>             modify ip address of the board.");
        System.out.println(" --mask <mask>         modify the network mask of the board.");
        System.out.println("                       The default value is 255.255.255.0");
        System.out.println(" --gw <gw>             modify standard gateway of the board.");
        System.out.println(" --port <port>         modify the standard port of the board.");
        System.out.println("                       The default port ist 8844");
        System.out.println();
        System.out.println(" --debug               enables debug mode for the generated driver,");
        System.out.println("                       which will cause THE DRIVER to produce additional");
        System.out.println("                       console output.");
        System.out.println(" -v --verbose          makes the driver generator produce additional");
        System.out.println("                       console output.");
//        System.out.println(" -n <name>");
//        System.out.println(" --name <name>         generate classfile with filename <name>.");
//        System.out.println("                       If this is not set, the default name");
//        System.out.println("                       " + "" + " is used.");
        System.out.println(" -d <dir>");
        System.out.println(" --dest <dir>          generate classfile to <dir>.");
        System.out.println("                       If this is not set, the classfile is generated to");
        System.out.println("                       the current working directory.");
        System.out.println(" -h --help             show this help.");
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
        System.out.println("  client folder    : " + config.clientDir());
        System.out.println("  server folder    : " + config.serverDir());
        System.out.println("  MAC address      : " + unparseMAC(config.getMAC()));
        System.out.println("  IP address       : " + unparseIP( config.getIP()));
        System.out.println("  network mask     : " + unparseIP( config.getMask()));
        System.out.println("  standard gateway : " + unparseIP( config.getGW()));
        System.out.println("  used port        : " + config.getPort());

        // start parser and 
        System.out.println();
        System.out.println("starting parser");
        Board board = new Parser2(config).parse(schemaFile);
        System.out.println("parser finished");
        System.out.println("  DEBUG: got the following board: " + board.toString());
        
        // instantiate and run generator with this configuration
        System.out.println();
        System.out.println("starting c/c++ generator");
        Generator generator = new Generator(config, board);
        generator.generate();
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

            // ETHERNET CONFIG flags
            if(args[i].equals("--mac")) {
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
                
            // DEBUG flag
            } else if(args[i].equals("--debug")) {
                config.enableDebug();
             
            // VERBOSE flags
            } else if(args[i].equals("-v") || args[i].equals("--verbose")) {
                config.enableVerbose();
                
//            // SCHEMANAME flags
//            } else if(args[i].equals("-n") || args[i].equals("--name")) {
//                    
//                if(i + 1 >= args.length) {
//                    System.err.println("no argument left for "+args[i]);
//                    throw new ExecutionFailed();
//                }
//              config.setName(args[++i]);
  
            // DESTDIR flags
            } else if(args[i].equals("-c") || args[i].equals("--client")) {
              
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
//            } else if(args[i].equals("-d") || args[i].equals("--dest")) {
//              
//              if(i + 1 >= args.length) {
//                  System.err.println("no argument left for "+args[i]);
//                  throw new ExecutionFailed();
//              }
//              config.setDestDir(new File(args[++i]));
                
            // USAGE HELP flag
            } else if(args[i].equals("-h") || args[i].equals("--help")) {
                showUsage();
                throw new ExecutionFailed();
            } else remaining.add(args[i]);
        }
        
        return remaining.toArray(new String[0]);
    }
    
    private String unparseIP(int[] ip) {
        return java.util.Arrays.toString(ip).replace(", ", ".").replace("[", "").replace("]", "");
    }
    private String unparseMAC(String[] ip) {
        return java.util.Arrays.toString(ip).replace(", ", ":").replace("[", "").replace("]", "");
    }
}
