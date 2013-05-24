package de.hopp.generator.backends.server.virtex6.ise;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.server.virtex6.ProjectBackendIF;
import de.hopp.generator.backends.server.virtex6.ise.xps.v14_4.MHS_14_4;
import de.hopp.generator.frontend.BDLFilePos;

/**
 * Generation backend for a project for Xilinx XPS version 14.4.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class ISE_14_4 extends ISE_14_1 implements ProjectBackendIF {

    @Override
    public String getName() {
        return "ise14.4";
    }
    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        xps = new MHS_14_4(config, errors);
        
        super.generate(board, config, errors);
    }
}
