package de.hopp.generator.backends.server.virtex6.ise;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printBuffer;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.*;
import static de.hopp.generator.utils.Files.copy;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.server.virtex6.ProjectBackendIF;
import de.hopp.generator.backends.server.virtex6.ise.sdk.Virtex6BDLVisitor;
import de.hopp.generator.backends.server.virtex6.ise.xps.XPSBDLVisitor;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.frontend.BDLFilePos;

public abstract class ISE implements ProjectBackendIF {

    protected XPSBDLVisitor visit;
    
    public static File sourceDir = new File(new File("deploy", "server"), "virtex6");
    
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // deploy the necessary sources
        deployBITSources(board, config, errors);
        deployELFSources(board, config, errors);
        
        // stop here, if it was only a dryrun
        if(config.dryrun()) return;
        
        // generate .bit and .elf files
        generateBITFile();
        generateELFFile();
    }

    /** 
     * Generates necessary Sources for generation of the BIT file
     */
    protected void deployBITSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        IOHandler IO = config.IOHANDLER();
        
        visit.visit(board);

        StringBuffer buffer  = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);
        unparser.visit(visit.getMHSFile());
        
        if(errors.hasErrors()) return;
        
        // if this is only a dryrun, return
        if(config.dryrun()) return;
        
        try {
            // deploy generated .mhs file
            File target = new File(edkDir(config), "system.mhs");
            IO.debug("deploying " + target.getPath());
            printBuffer(buffer, target);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
        
        // deploy core sources
        for(File target : visit.getCoreSources().keySet()) {
            try {
                IO.debug("copying " + visit.getCoreSources().get(target) + " to "+ target.getPath());
                copyFile(new File(visit.getCoreSources().get(target)), target);
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
        }
        
        // deploy core pao files
        for(File target : visit.getPAOFiles().keySet()) {
            try {
                IO.debug("deploying " + target);
                write(target, visit.getPAOFiles().get(target));
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
        }
        
        // deploy core mpd files
        for(File target : visit.getMPDFiles().keySet()) {
            try {
                IO.debug("deploying " + target);
                
                buffer   = new StringBuffer();
                unparser = new MHSUnparser(buffer);
                unparser.visit(visit.getMPDFiles().get(target));
                
                printBuffer(buffer, target);
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
        }
    }

    /** Starts whatever external tool is responsible for generation of the BIT file */
    protected void generateBITFile() {
        
    }

    /** 
     * Generates necessary Sources for generation of the ELF file
     */
    protected void deployELFSources(BDLFilePos board, Configuration config, ErrorCollection errors) {
        IOHandler IO = config.IOHANDLER();
        
        // generate board-specific MFiles
        Virtex6BDLVisitor visit = new Virtex6BDLVisitor(config, errors);
        visit.visit(board);
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
        
        // if this is only a dryrun, return
        if(config.dryrun()) return;
        
        // deploy board-independent files and directories
        try { 
            copy(new File(sourceDir, "generic").getPath(), sdkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic sources due to:\n" + e.getMessage()));
        }
        
        // deploy board-dependent, generic files
        for(File source : visit.getFiles().keySet()) {
            try {
                copy(source.getPath(), visit.getFiles().get(source), IO);
            } catch (IOException e) {
                errors.addError(new GenerationFailed("Failed to deploy generic source " +
                        source + " due to:\n" + e.getMessage()));
            }
        }
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
    
        // unparse & deploy the generated MFiles (note, that several (semi-)generic files are already deployed)
        printMFile(visit.getConstants(),  UnparserType.HEADER, errors);
        printMFile(visit.getComponents(), UnparserType.HEADER, errors);
        printMFile(visit.getComponents(), UnparserType.C,      errors);
    
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
        
        // generate api-specification
        IO.println("  generate server-side api specification ... ");
        doxygen(sdkDir(config), IO, errors);
    }
    
    /** Starts whatever external tool is responsible for generation of the ELF file */
    protected void generateELFFile() {
        
    }
}
