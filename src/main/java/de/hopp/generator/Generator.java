package de.hopp.generator;

import static de.hopp.generator.model.Model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
    
    private static final MFile DUMMY_FILE = MFile("", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
    
    private enum UnparserType { HEADER, C, CPP }

    public Generator(Configuration config, Board board) {
        this.config = config;
        this.board  = board;
    }
    
    public void generate() {

        // generate the board and host drivers
        System.out.println("  generating board side driver model ...");
        MFileInFile boardDriver = MFileInFile(generateBoardDriver());
        System.out.println("  generating host side driver model ...");
        MFileInFile hostDriver  = MFileInFile(generateHostDriver());
        
        // setup output file
        System.out.println("  finished model generation");
        System.out.println();

        System.out.println("    generating board side driver files ...");
        System.out.println("      generating header file ...");
        printMFile(boardDriver, UnparserType.HEADER);
        
        System.out.println("      generating source file ...");
        printMFile(boardDriver, UnparserType.C);
        
        System.out.println("    generating host side driver files ...");
        System.out.println("      generating header file ...");
        printMFile(hostDriver, UnparserType.HEADER);
        
        System.out.println("      generating source file ...");
        printMFile(hostDriver, UnparserType.CPP);
    }
    
    private MFile generateHostDriver() {
        MFile hostDriver = DUMMY_FILE.replaceName("Host");
        
        for(Component comp : board.components()) {
            hostDriver = mergeFiles(hostDriver, comp.Switch(new Component.Switch<MFile, NE>() {
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
                                Strings(
                                    "// set LED register accordingly"
                                )
                            ));
                    
                    // generate method to read LED register
                    MMethod getLEDs = MMethod(
                            MDocumentation(Strings()), MModifiers(), MVoid(), "setLEDs", MParameters(
                                MParameter(VALUE(), MArrayType(MType("boolean"), 8), "state")
                            ), MCode(
                                Strings(
                                    "// read LED register and put it into array"
                                )
                            ));
                    
                    // put methods into returned file
                    ledsFile.replaceMethods(MMethods(setLEDs, getLEDs));
                    
                    return ledsFile;
                }
                public MFile CaseSWITCHES(SWITCHES term)           { return DUMMY_FILE; }
                public MFile CaseBUTTONS(BUTTONS term)             { return DUMMY_FILE; }
            }));
        }
        
        return hostDriver;
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

    private void printMFile(MFileInFile mfile, UnparserType type) {
        
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
        File target = new File(".");;
        if(config.getDest() != null)
            target = new File(config.getDest(), target.getName());

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
     * @return the "merged" file containing all components and the name of the first file
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
