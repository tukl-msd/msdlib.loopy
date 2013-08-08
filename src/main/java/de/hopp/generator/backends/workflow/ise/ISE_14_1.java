package de.hopp.generator.backends.workflow.ise;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_1;
import de.hopp.generator.backends.workflow.ise.sdk.SDK;
import de.hopp.generator.frontend.BDLFilePos;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class ISE_14_1 extends ISE {

    @Override
    public String getName() {
        return "ise14.1";
    }
    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        xps = ((ISEBoard_14_1)config.board()).getMHS_14_1(errors);
        sdk = new SDK(config, errors);
        super.generate(board, config, errors);
    }
}
