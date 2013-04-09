//package de.hopp.tests;
//
//import static de.hopp.generator.board.BoardSpec.Board;
//import static de.hopp.generator.board.BoardSpec.Components;
//import static de.hopp.generator.board.BoardSpec.ETHERNET_LITE;
//import static de.hopp.generator.board.BoardSpec.UART;
//import static de.hopp.generator.model.Model.*;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//
//import de.hopp.generator.backends.unparser.CUnparser;
//import de.hopp.generator.board.Board;
//import de.hopp.generator.exceptions.ExecutionFailed;
//import de.hopp.generator.exceptions.InvalidConstruct;
//import de.hopp.generator.model.MClass;
//import de.hopp.generator.model.MFile;
//import de.hopp.generator.model.MMethod;
//
//
//public class Test {
//    
//    public static void main (String[] args) {
//        System.out.println("some primitive test");
//        
//        MMethod m = MProcedure(MDocumentation(Strings()), MModifiers(), MType("a"), "b", MParameters(), MCode(Strings("abc")));
//        MClass c = MClass(MDocumentation(Strings()), MModifiers(), "a", MExtends(), MStructs(), MEnums(), MAttributes(), MMethods(m));
//        MFile file = MFile(MDocumentation(Strings()), "name", MDefinitions(), MStructs(), MEnums(),
//                    MAttributes(), MProcedures(), MClasses(c));
//        
//        File outFile = new File("out");
//        StringBuffer output = new StringBuffer(16384);
//        
//        try {
//            new CUnparser(output, "Driver").visit(MFileInFile(file));
//        } catch (InvalidConstruct e) {
//            System.err.println("encountered invalid construct for plain c unparser");
//            throw new ExecutionFailed();
//        }
//        
//        // open file and unparse data in it
//        try {
//            FileWriter fileWriter = new FileWriter(outFile);
//            new BufferedWriter(fileWriter).append(output).flush();
//            fileWriter.close();
//        } catch (IOException e) {
//            System.err.println("io error on file "+outFile);
//            throw new IllegalStateException(e);
//        }
//    }
//	
//    public static Board defaultBoard() {
////        return Board(UART(),  ETHERNET_LITE(IP(192,168,1,10),IP(255,255,255,0), IP(192,168,1,1), 8844));
//        return Board(Components(UART(), ETHERNET_LITE()));
//    }
//}
