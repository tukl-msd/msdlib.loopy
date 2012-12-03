package de.hopp.generator;

import static de.hopp.generator.model.Model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.hopp.Configuration;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.*;
import de.hopp.generator.unparser.HUnparser;
import de.hopp.generator.unparser.CUnparser;

public class Generator {

    private Configuration config;
    
    public Generator(Configuration config) {
        this.config = config;
    }
    
    public void generate() {
        
        // TODO Do some generation stuff... this should be rather fixed
        //      i.e. produce some fixed methods and classes for each component of the board
//        MMethod m = MMethod(MModifiers(), MType("a"), "b", MParameters(), MCode(Strings("abc")));
//        MClass c = MClass(MModifiers(), "a", MStructs(), MEnums(), MAttributes(), MMethods(m));
//        MFile file = MFile("name", MStructs(), MEnums(), MAttributes(), MMethods(), MClasses(c));
        
        
        // unparse the generated file into output string buffer
        StringBuffer houtput = new StringBuffer(16384);
        StringBuffer coutput = new StringBuffer(16384);
        
        try {
            new HUnparser(houtput, "Driver").visit(testFile());
            new CUnparser(coutput, "Driver").visit(testFile());
        } catch (InvalidConstruct e) {
            System.err.println("encountered invalid construct for plain c unparser:");
            System.err.println("  " + e.getMessage());
            throw new ExecutionFailed();
        }
            
        // set output file
        File houtFile = new File("Driver.h");
        File coutFile = new File("Driver.c");
        
        // append destination path, if any
        if(config.getDest() != null) {
            houtFile = new File(config.getDest(), houtFile.getName());
            coutFile = new File(config.getDest(), coutFile.getName());
        }
        
        try {
            // create the files and parent directories if they don't exist
            if(houtFile.getParentFile() != null && ! houtFile.getParentFile().exists())
                houtFile.getParentFile().mkdirs();
            if(! houtFile.exists())
                houtFile.createNewFile();
            if(! coutFile.exists())
                coutFile.createNewFile();
            
            // write output into file
            FileWriter fileWriter = new FileWriter(houtFile);
            new BufferedWriter(fileWriter).append(houtput).flush();
            fileWriter.close();
            
            fileWriter = new FileWriter(coutFile);
            new BufferedWriter(fileWriter).append(coutput).flush();
            fileWriter.close();
            
        } catch (IOException e) {
            System.err.println("io error on file " + coutFile.getPath());
            throw new IllegalStateException(e);
        }        
    }
    
    private static MFileInFile testFile() {
        MMethod m = MMethod(MModifiers(), MType("a"), "b", MParameters(), MCode(Strings()));
        MMethod m2 = MMethod(MModifiers(), MPointerType(MType("int")), "c", MParameters(
                    MParameter(CONSTREF(), MPointerType(MArrayType(MPointerType(MType("int")), new Integer(5))), "p")
                ), MCode(Strings()));
//        MClass c = MClass(MModifiers(), "a", MStructs(), MEnums(), MAttributes(), MMethods(m));
        MFile file = MFile("name", MStructs(), MEnums(), MAttributes(), MMethods(m, m2), MClasses());
        
        return MFileInFile(file);
    }
}
