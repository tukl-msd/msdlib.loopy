package de.hopp.generator.backends.client.cpp;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.utils.Files.copy;

import java.io.File;
import java.io.IOException;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.client.ClientBackend;
import de.hopp.generator.frontend.BDLFilePos;

/**
 * Generation backend for a host-side C++ driver.
 * This visitor generates a C++ API for communication with an arbitrary board-side driver.
 * @author Thomas Fischer
 */
public class CPP implements ClientBackend {

    public CPP() {

    }
    
    public String getName() {
        return "c++";
    }
    
    public void printUsage(IOHandler IO) {
        IO.println(" no parameters");
    }
    
    public Configuration parseParameters(Configuration config, String[] args) {
        // parse backend specific parameters...
        // basically everything, that was not handled by the system...
        config.setUnusued(args);
        return config;
    }

    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        
        IOHandler IO = config.IOHANDLER();
        
        // generate  board-specific MFiles
        CPPBDLVisitor visit = new CPPBDLVisitor(config);
        visit.visit(board);
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
        
        // return, if this is only a dry run
        if(config.dryrun()) return;
        
        // deploy generic client code
        try {
            copy("deploy/client/cpp", config.clientDir(), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(""));
            return;
        }
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;

        // unparse & deploy the generated MFiles (note, that several (semi-)generic files are already deployed)
        printMFile(visit.consts, UnparserType.HEADER, errors);
        printMFile(visit.comps,  UnparserType.HEADER, errors);
        printMFile(visit.comps,  UnparserType.CPP, errors);
        
        // abort, if errors occurred TODO add another exception
        if(errors.hasErrors()) return;
        
        
        // generate api specification
        IO.println("  generate client-side api specification ... ");
        doxygen(config.clientDir(), IO, errors);
    }
}
