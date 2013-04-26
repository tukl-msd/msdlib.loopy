package de.hopp.generator.backends.server.virtex6.ise;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printBuffer;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.edkDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkBSPDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkDir;
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
import de.hopp.generator.backends.server.virtex6.ise.sdk.SDK;
import de.hopp.generator.backends.server.virtex6.ise.xps.XPS;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.frontend.BDLFilePos;

/**
 * Abstract project backend for Xilinx ISE projects for the Virtex6 board.
 * 
 * This backend is responsible for the generation of .elf and .bit files.
 * To that purpose, the ISE workflow is used.
 * The workflow implies creation of an XPS project for .bit file generation
 * and a subsequent SDK project for .elf file generation.
 * Respective backends are used to generate the project files.
 * Afterwards, the required parts of the Xilinx toolsuite are called.
 *
 * The abstract class describes the workflow. Steps of the workflow are
 * required to be overridden in the implementations.
 *
 * 
 * 
 * This flow is assumed to cover all versions of ISE.
 * If this proves not to be the case in some earlier or later, unsupported version,
 * introduce a new ProjectBackendIF subclass with the adjusted flow and
 * rename this flow accordingly to the earliest version number,
 * this workflow is compatible with (e.g. ISE14).
 *
 * @author Thomas Fischer
 */
public abstract class ISE implements ProjectBackendIF {

    public static File sdkSourceDir = new File(new File("deploy", "server"), "virtex6");
    public static File edkSourceDir = new File(new File("deploy", "server"), "ISE");
    
    protected abstract XPS xps();
    protected abstract SDK sdk();
    
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
        
        xps().visit(board);
        
        StringBuffer buffer  = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);
        unparser.visit(xps().getMHSFile());
        
        if(errors.hasErrors()) return;
        
        // if this is only a dryrun, return
        if(config.dryrun()) return;
        
        // deploy board-independent files and directories
        try { 
            copy(edkSourceDir.getPath(), edkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic edk sources due to:\n" + e.getMessage()));
        }
        
        try {
            // deploy generated .mhs file
            File target = new File(edkDir(config), "system.mhs");
            IO.debug("deploying " + target.getPath());
            printBuffer(buffer, target);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
        
        // deploy core sources
        for(File target : xps().getCoreSources().keySet()) {
            try {
                IO.debug("copying " + xps().getCoreSources().get(target) + " to "+ target.getPath());
                copyFile(new File(xps().getCoreSources().get(target)), target);
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
        }
        
        // deploy core pao files
        for(File target : xps().getPAOFiles().keySet()) {
            try {
                IO.debug("deploying " + target);
                write(target, xps().getPAOFiles().get(target));
            } catch(IOException e) {
                errors.addError(new GenerationFailed(e.getMessage()));
            }
        }
        
        // deploy core mpd files
        for(File target : xps().getMPDFiles().keySet()) {
            try {
                IO.debug("deploying " + target);
                
                buffer   = new StringBuffer();
                unparser = new MHSUnparser(buffer);
                unparser.visit(xps().getMPDFiles().get(target));
                
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
//        SDK visit = new SDK(config, errors);
        sdk().visit(board);
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
        
        // if this is only a dryrun, return
        if(config.dryrun()) return;
        
        // deploy board-independent files and directories
        try { 
            copy(new File(sdkSourceDir, "generic").getPath(), sdkDir(config), IO);
        } catch(IOException e) {
            e.printStackTrace();
            errors.addError(new GenerationFailed("Failed to deploy generic sdk sources due to:\n" + e.getMessage()));
        }
        
        // deploy board-dependent, generic files
        for(File source : sdk().getFiles().keySet()) {
            try {
                copy(source.getPath(), sdk().getFiles().get(source), IO);
            } catch (IOException e) {
                errors.addError(new GenerationFailed("Failed to deploy generic source " +
                        source + " due to:\n" + e.getMessage()));
            }
        }
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
    
        // unparse & deploy the generated MFiles (note, that several (semi-)generic files are already deployed)
        printMFile(sdk().getConstants(),  UnparserType.HEADER, errors);
        printMFile(sdk().getComponents(), UnparserType.HEADER, errors);
        printMFile(sdk().getComponents(), UnparserType.C,      errors);
        printMFile(sdk().getScheduler(),  UnparserType.HEADER, errors);
        printMFile(sdk().getScheduler(),  UnparserType.C,      errors);
    
        printMFile(sdk().getMSS(), new File(sdkBSPDir(config), "system.mss"), errors);
        
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
