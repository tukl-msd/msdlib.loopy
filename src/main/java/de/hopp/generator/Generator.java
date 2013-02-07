package de.hopp.generator;

import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.Files.copy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import de.hopp.generator.board.*;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.model.*;
import de.hopp.generator.unparser.CppUnparser;
import de.hopp.generator.unparser.CUnparser;
import de.hopp.generator.unparser.HUnparser;

public class Generator {

    private Configuration config;
    private IOHandler IO;
    private ErrorCollection errors;
    private Board board;
    
    private enum UnparserType { HEADER, C, CPP }

    public Generator(Main main, Board board) {
        this.config = main.config();
        this.IO     = main.io();
        this.errors = main.errors();
        this.board  = board;
    }
    
    public void generate() {

        // setup required folders
        IO.println("  setting up required folders ...");
        if(! config.serverDir().exists()) config.serverDir().mkdirs();
        if(! config.clientDir().exists()) config.clientDir().mkdirs();
        
        // if there are errors abort here
        if(errors.hasErrors()) return;
        
        // copy generic code parts
        IO.println("  deploying generic client and server code ...");
        
        try {
            copy("deploy/client", config.clientDir(), IO);
            
            // server root directory
            copy("deploy/server/doxygen.cfg",       new File(config.serverDir(), "doxygen.cfg"), IO);
            
            // server src directory
            File serverSrc = new File(config.serverDir(), "src");
            copy("deploy/server/main.c",            new File(serverSrc, "main.c"), IO);
            copy("deploy/server/platform_config.h", new File(serverSrc, "platform_config.h"), IO);
            copy("deploy/server/platform_mb.c",     new File(serverSrc, "platform_mb.c"), IO);
//            copy("deploy/server/platform_ppc.c",    new File(serverSrc, "platform_ppc.c"), IO);
//            copy("deploy/server/platform_zynq.c",   new File(serverSrc, "platform_zynq.c"), IO);
            copy("deploy/server/platform.c",        new File(serverSrc, "platform.c"), IO);
            copy("deploy/server/platform.h",        new File(serverSrc, "platform.h"), IO);
            
            // generic subfolder parts
            copy("deploy/server/medium/protocol", new File(new File(serverSrc, "medium"), "protocol"), IO);
            File componentDir = new File(serverSrc, "components");
            copy("deploy/server/components/interrupts.h", new File(componentDir, "interrupts.h"), IO);
            copy("deploy/server/components/interrupts.c", new File(componentDir, "interrupts.c"), IO);
                        
            IO.verbose("");
        } catch (IOException e) {
            errors.addError(new UsageError(e.getMessage()));
        }

        // abort if any errors occurred
        if(errors.hasErrors()) return;
        
        // unparse generated server models to corresponding files
        IO.println("  generating server side driver files ...");
        generateBoardDriver();
        
        // abort if any errors occurred
        if(errors.hasErrors()) return;
        
        // unparse generated client models to corresponding files
        IO.println("  generating client side driver files ...");
        generateClientDriver();
        
        // generate the constants file with the debug flag
        printMFile(MFileInFile(MFile(MDocumentation(Strings(
                    "Defines several constants used by the client."
                )), "constants", MDefinitions(
                    MDefinition(MDocumentation(Strings(
                            "If set, enables additional console output for debugging purposes"
                    )), MModifiers(PUBLIC()), "DEBUG", config.debug() ? "1" : "0"
                )), MStructs(), MEnums(), MAttributes(), MProcedures())),
                new File(config.clientDir(), "src"), UnparserType.HEADER);
        
        // abort if any errors occurred
        if(errors.hasErrors()) return;
        
        // run doxygen generation
        IO.println("  generate api specification ...");
        IO.println("    generate client-side api specification ... ");
        doxygen(config.clientDir());
        IO.println("    generate server-side api specification ... ");
        doxygen(config.serverDir());            

    }
    
    private void doxygen(File dir) {
        BufferedReader input = null;
        
        try {
            String line;
            // TODO probably need .exe extension for windows?
            // TODO would be cleaner to do this with scripts, i guess (but this would require parameter passing...)
            // run doxygen in the provided directory
            Process p = new ProcessBuilder("doxygen", "doxygen.cfg").directory(dir).redirectErrorStream(true).start();
            input     = new BufferedReader(new InputStreamReader(p.getInputStream()));

            // wait for the process to terminate and store the result
            int rslt = p.waitFor();
            
            // if something went wrong, print a warning
            if(rslt != 0) {
                errors.addWarning(new Warning("failed to generate api specification at " +
                        dir.getPath() + ".run in verbose mode for more information"));
                if(! config.VERBOSE()) return;
            }
            
            // if verbose, echo the output of doxygen
            while ((line = input.readLine()) != null)
                IO.verbose("      " + line);
            
        } catch (IOException | InterruptedException e) {
            errors.addWarning(new Warning("failed to generate api specification at " + 
                    dir.getPath() + "due to: " + e.getMessage()));
        } finally {
            try { 
                if(input != null) input.close();
            } catch(IOException e) { /* well... memory leak... */ }
        }
    }

    private void generateClientDriver() {
        ClientVisitor visit = new ClientVisitor(config);
        visit.visit(board);
        
        File clientSrc = new File(config.clientDir(), "src");
        printMFile(MFileInFile(visit.getCompsFile()), clientSrc, UnparserType.HEADER);
        printMFile(MFileInFile(visit.getCompsFile()), clientSrc, UnparserType.CPP);
    }

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
    private void generateBoardDriver() {
        DriverVisitor visit = new DriverVisitor(config);
        try {
            visit.visit(board);
            
            File serverSrc = new File(config.serverDir(), "src");
            File comp = new File(serverSrc, "components");
            printMFile(MFileInFile(visit.getConstantsFile()),  serverSrc, UnparserType.HEADER);
            printMFile(MFileInFile(visit.getComponentsFile()), comp, UnparserType.HEADER);
            printMFile(MFileInFile(visit.getComponentsFile()), comp, UnparserType.C);
        } catch (IOException e) {
            errors.addError(new UsageError(e.getMessage()));
        }
    }

    private void printMFile(MFileInFile mfile, File target, UnparserType type) {
        
        // setup buffer
        StringBuffer buf = new StringBuffer(16384);
        
        // get unparser instance
        MFileInFile.Visitor<InvalidConstruct> visitor = createUnparser(type, buf, mfile.name().term());
        
        // unparse to buffer
        try {
            visitor.visit(mfile);
        } catch (InvalidConstruct e) {
            errors.addError(new UsageError(e.getMessage()));
        }
               
        // append model name and file extension according to used unparser
        switch(type) {
        case HEADER : target = new File(target, mfile.name().term() +   ".h"); break;
        case C      : target = new File(target, mfile.name().term() +   ".c"); break;
        case CPP    : target = new File(target, mfile.name().term() + ".cpp"); break;
        default     : throw new IllegalStateException("invalid unparser");
        }
        
        // print buffer contents to file
        try {
            printBuffer(buf, target);
        } catch(IOException e) {
            errors.addError(new UsageError(e.getMessage()));
        }
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
    private static void printBuffer(StringBuffer buf, File target) throws IOException {
        // create the files and parent directories if they don't exist
        if(target.getParentFile() != null && ! target.getParentFile().exists())
            target.getParentFile().mkdirs();
        if(! target.exists())
            target.createNewFile();
            
        // write output into file
        FileWriter fileWriter = new FileWriter(target);
        new BufferedWriter(fileWriter).append(buf).flush();
        fileWriter.close();
    }

    /**
     * Creates a new file by adding all components of a file to another file.
     * @param file1 file to which components should be added
     * @param file2 file which MDefinitioncomponents should be added
     * @return the "merged" file with the name and documentation of the first file
     *         and all components from both files
     */
    private static MFile mergeFiles(MFile file1, MFile file2) {
        MFile file = MFile(file1.doc(), file1.name(),
                file1.defs().addAll(file2.defs()),
                file1.structs().addAll(file2.structs()), 
                file1.enums().addAll(file2.enums()),
                file1.attributes().addAll(file2.attributes()),
                file1.procedures().addAll(file2.procedures()),
                file1.classes().addAll(file2.classes()));
        return file;
    }
}
