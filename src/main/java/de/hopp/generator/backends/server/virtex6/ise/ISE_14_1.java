package de.hopp.generator.backends.server.virtex6.ise;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.server.virtex6.ProjectBackendIF;
import de.hopp.generator.backends.server.virtex6.ise.xps.XPS_14_1BDLVisitor;
import de.hopp.generator.frontend.BDLFilePos;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class ISE_14_1 extends ISE implements ProjectBackendIF {

    @Override
    public String getName() {
        return "ise14.1";
    }
    
    @Override
    public void printUsage(IOHandler IO) {
        IO.println(" no parameters");
    }

    @Override
    public Configuration parseParameters(Configuration config, String[] args) {
        config.setUnusued(args);
        return config;
    }
    
    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        visit = new XPS_14_1BDLVisitor(config, errors);
        super.generate(board, config, errors);
    }
}
