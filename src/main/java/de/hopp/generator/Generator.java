package de.hopp.generator;

import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.Files.copy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import katja.common.NE;

import de.hopp.Configuration;
import de.hopp.generator.board.*;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;
import de.hopp.generator.unparser.CppUnparser;
import de.hopp.generator.unparser.CUnparser;
import de.hopp.generator.unparser.HUnparser;
public class Generator {

    private Configuration config;
    private Board board;
    
    private static final MFile DUMMY_FILE = 
            MFile("", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
    
    private enum UnparserType { HEADER, C, CPP }

    public Generator(Configuration config, Board board) {
        this.config = config;
        this.board  = board;
    }
    
    public void generate() {

        // setup required folders
        System.out.println("  setting up required folders ...");
//        if(! config.getDest().exists()) config.getDest().mkdirs();
        if(! config.serverDir().exists()) config.serverDir().mkdirs();
        if(! config.clientDir().exists()) config.clientDir().mkdirs();
        
        // copy fixed parts (i.e. client side api and doxygen configs)
        System.out.println("  deploying generic client and server code ...");
        
        try {
            copy("deploy/client", config.clientDir(), config.verbose());
            copy("deploy/server", config.serverDir(), config.verbose());
            if(config.verbose()) System.out.println();
        } catch (IOException e) {
            System.out.println("    ERROR: " + e.getMessage());
            throw new ExecutionFailed();
        }
        
        // generate the board and client drivers
        System.out.println("  generating board side driver model ...");
        MFileInFile boardDriver = MFileInFile(generateBoardDriver());
        System.out.println("  generating client side driver model ...");
        @SuppressWarnings("unused")
        MFileInFile clientDriver  = MFileInFile(generateClientDriver());
        
        // setup output file
        System.out.println("  finished model generation");
        System.out.println();

        System.out.println("  generating board side driver files ...");
        System.out.println("    generating header file ...");
        printMFile(boardDriver, false, UnparserType.HEADER);
        
        System.out.println("    generating source file ...");
        printMFile(boardDriver, false, UnparserType.C);
//        
        System.out.println("    generating client side driver files ...");
        // generate the constants file with the debug flag
        printMFile(MFileInFile(MFile("constants", MDefinitions(
                    MDefinition(MDocumentation(Strings(
                            "If set, enables additional console output for debugging purposes"
                    )), "DEBUG", "1"
                )), MStructs(), MEnums(), MAttributes(), MMethods())),
                true, UnparserType.C);
        
//        System.out.println("      generating header file ...");
//        printMFile(clientDriver, true, UnparserType.HEADER);
//        
//        System.out.println("      generating source file ...");
//        printMFile(clientDriver, true, UnparserType.CPP);
        
        System.out.println("  generate documentation ...");
        doxygen(config.clientDir());
//        doxygen(config.serverDir());
    }
    
    private static void doxygen(File dir) {
        try {
            String line;
            // TODO probably need .exe extension for windows?
            Process p = new ProcessBuilder("doxygen", "doxygen.cfg").directory(dir).redirectErrorStream(true).start();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MFile generateClientDriver() {
        MFile clientDriver = DUMMY_FILE.replaceName("Client");
        
        for(Component comp : board.components()) {
            clientDriver = mergeFiles(clientDriver, comp.Switch(new Component.Switch<MFile, NE>() {
                public MFile CaseUART(UART term)                   { return DUMMY_FILE; }
                public MFile CaseETHERNET_LITE(ETHERNET_LITE term) { return DUMMY_FILE; }
                public MFile CaseETHERNET(ETHERNET term)           { return DUMMY_FILE; }
                public MFile CasePCIE(PCIE term)                   { return DUMMY_FILE; }
                public MFile CaseLEDS(LEDS term)                   {
                    MFile ledsFile = DUMMY_FILE;
                    
                    // generate method to set LED register
                    MMethod setLEDs = MMethod(
                            MDocumentation(Strings()), MModifiers(), MVoid(), "setLEDs", MParameters(
                                MParameter(VALUE(), MArrayType(MType("boolean"), 8), "state")
                            ), MCode(
                                Strings("// set LED register accordingly")
                            ));
                    
                    // generate method to read LED register
                    MMethod getLEDs = MMethod(
                            MDocumentation(Strings()), MModifiers(), MVoid(), "setLEDs", MParameters(
                                MParameter(VALUE(), MArrayType(MType("boolean"), 8), "state")
                            ), MCode(
                                Strings("// read LED register and put it into array")
                            ));
                    
                    // put methods into returned file
                    ledsFile.replaceMethods(MMethods(setLEDs, getLEDs));
                    
                    return ledsFile;
                }
                public MFile CaseSWITCHES(SWITCHES term)           { return DUMMY_FILE; }
                public MFile CaseBUTTONS(BUTTONS term)             { return DUMMY_FILE; }
            }));
        }
        
        return clientDriver;
    }

    private MFile generateBoardDriver() {
        
        /* 
         * Generally there are two options for generating the board:
         * a) add stuff for each component individually
         * b) cycle through methods and add parts for all components there
         * 
         * the first will probably result in a cleaner generator, the second in a cleaner generated file
         * 
         * a) also requires forward definitions, i.e. two visitors...
         * 
         * we'll go with a slight mix of the two, focusing on a),
         * but using b) for generation of the init method and necessary definitions 
         */
        DriverVisitor visit = new DriverVisitor(config);
        visit.visit(board);
        return visit.getFile().replaceName("Driver");
    }

    private void printMFile(MFileInFile mfile, boolean client, UnparserType type) {
        
        // setup buffer
        StringBuffer buf = new StringBuffer(16384);
        
        // get unparser instance
        MFileInFile.Visitor<InvalidConstruct> visitor = createUnparser(type, buf, mfile.name().term());
        
        // unparse to buffer
        try {
            visitor.visit(mfile);
        } catch (InvalidConstruct e) {
           System.err.println("encountered invalid construct for plain c unparser:");
           System.err.println("  " + e.getMessage());
           throw new ExecutionFailed();
        }
               
        // set output file
        File target = client ? config.clientDir() : config.serverDir();
             target = new File(target, "src");
             target = new File(target, new File(".").getName());
        
//        if(client) target = new File(config.clientDir(), target.getName());
//        if(config.getDest() != null)
//            target = new File(config.getDest(), target.getName());

        // append file extension according to used unparser
        switch(type) {
        case HEADER : target = new File(target, mfile.name().term() +   ".h"); break;
        case C      : target = new File(target, mfile.name().term() +   ".c"); break;
        case CPP    : target = new File(target, mfile.name().term() + ".cpp"); break;
        default     : throw new IllegalStateException();
        }
        
        // print buffer contents to file
        printBuffer(buf, target);
    }

    private static MFileInFile.Visitor<InvalidConstruct> createUnparser(UnparserType type, StringBuffer buf, String name) {
        switch(type) {
        case HEADER : return new   HUnparser(buf, name);
        case C      : return new   CUnparser(buf, name);
        case CPP    : return new CppUnparser(buf, name);
        }
        throw new IllegalStateException();
    }

    /**
     * prints the content of a StringBuffer into a file.
     * Creates the file and directories if required.
     * @param buf the buffer to be printed
     * @param target file into which the buffer content should be printed
     * @throws IOException error during file creation or the print
     */
    private static void printBuffer(StringBuffer buf, File target) {
        try {
            // create the files and parent directories if they don't exist
            if(target.getParentFile() != null && ! target.getParentFile().exists())
                target.getParentFile().mkdirs();
            if(! target.exists())
                target.createNewFile();
            
            // write output into file
            FileWriter fileWriter = new FileWriter(target);
            new BufferedWriter(fileWriter).append(buf).flush();
            fileWriter.close();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("io error on file " + target.getPath());
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a new file by adding all components of a file to another file.
     * @param file1 file to which components should be added
     * @param file2 file which components should be added
     * @return the "merged" file with the name of the first file containing
     *         all components from both files
     */
    private static MFile mergeFiles(MFile file1, MFile file2) {
        MFile file = MFile(file1.name(),
                file1.defs().addAll(file2.defs()),
                file1.structs().addAll(file2.structs()), 
                file1.enums().addAll(file2.enums()),
                file1.attributes().addAll(file2.attributes()),
                file1.methods().addAll(file2.methods()),
                file1.classes().addAll(file2.classes()));
        return file;
    }
}
