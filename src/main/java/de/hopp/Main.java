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
        System.out.println(" -h --help             show this help.");
        System.out.println(" -n <name>");
        System.out.println(" --name <name>         generate classfile with filename <name>.");
        System.out.println("                       If this is not set, the default name");
        System.out.println("                       " + "" + " is used.");
        System.out.println(" -d <dir>");
        System.out.println(" --dest <dir>          generate classfile to <dir>.");
        System.out.println("                       If this is not set, the classfile is generated to");
        System.out.println("                       the current working directory.");
        System.out.println(" -p <dir>");
        System.out.println(" --package <package>   adds a package declaration with the given name");
        System.out.println("                       to the generated classfile. If this is not set,");
        System.out.println("                       files are generated to the default package.");
        System.out.println();
    }

    private void run(String[] args) throws ExecutionFailed {
        
        // parse all cli parameters
        String schema = "";//parseParameters(args);
        File schemaFile = new File(schema);
        
        // check if the given string indeed references an existing file
//        if(!schemaFile.exists()) {
//            System.err.println("ERROR: Could not find file " + schemaFile.getPath());
//            throw new ExecutionFailed();
//        } else if(!schemaFile.isFile()) {
//            System.err.println("ERROR: " + schemaFile.getPath() + " is no file");
//            throw new ExecutionFailed();
//        }
        config.setSourceFile(schemaFile);
        
        // TODO parse source file and generate a board depending on the results
        File mhs = new File("sample.mhs");

        System.out.println();
        System.out.println("starting parser");
        Board board = new Parser2(config).parse(mhs);
        System.out.println("parser finished");
        System.out.println("got the following board: " + board.toString());
        
        // instantiate and run generator with this configuration
        System.out.println();
        System.out.println("starting c/c++ generator");
        Generator generator = new Generator(config, board);
        generator.generate();
        System.out.println("generator finished");
        
        // finished
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

            // SCHEMANAME flags
            if(args[i].equals("-n") || args[i].equals("--name")) {
                
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
//                config.setName(args[++i]);
                
            // DESTDIR flags
            } else if(args[i].equals("-d") || args[i].equals("--dest")) {
                
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                config.setDestDir(new File(args[++i]));
            
            // PACKAGE flags
            } else if(args[i].equals("-p") || args[i].equals("--package")) {
                
                if(i + 1 >= args.length) {
                    System.err.println("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
//                config.setPackageName(args[++i]);
            
            // usage help
            } else if(args[i].equals("-h") || args[i].equals("--help")) {
                showUsage();
                throw new ExecutionFailed();
            } else remaining.add(args[i]);
        }
        
        return remaining.toArray(new String[0]);
    }
}
