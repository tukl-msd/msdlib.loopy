package de.hopp.generator.backends.host.cpp;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.utils.Files.deploy;

import java.io.IOException;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.host.AbstractHostBackend;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.model.BDLFilePos;

/**
 * Generation backend for a host-side C++ driver.
 * This visitor generates a C++ API for communication with an arbitrary board-side driver.
 * @author Thomas Fischer
 */
public class CPP extends AbstractHostBackend {

    public CPP() {

    }

    public String getName() {
        return "c++";
    }

    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {

        IOHandler IO = config.IOHANDLER();

        /* ************************ ANALYSIS & GENERATION ************************ */

        // generate  board-specific MFiles
        CPPBDLVisitor visit = new CPPBDLVisitor(config, errors);
        visit.visit(board);

        if(errors.hasErrors()) return;

        // return, if this is only a dry run
        if(config.dryrun()) return;

        /* ****************************** DEPLOYMENT ****************************** */

        // deploy generic client code
        try {
            deploy("deploy/client/cpp", config.hostDir(), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(""));
            return;
        }

        // unparse & deploy the generated MFiles
        try {
            printMFile(visit.consts, UnparserType.HEADER);
            printMFile(visit.comps,  UnparserType.HEADER);
            printMFile(visit.comps,  UnparserType.CPP);
            printMFile(visit.logger, UnparserType.CPP);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to deploy non-generic client sources due to:\n"
                + e.getMessage()));
            return;
        } catch (InvalidConstruct e) {
            throw new IllegalStateException("Encountered invalid construct in C model unparser");
        }

        // generate api specification
        IO.println("  generate client-side api specification ... ");
        doxygen(config.hostDir(), IO, errors);
    }
}
