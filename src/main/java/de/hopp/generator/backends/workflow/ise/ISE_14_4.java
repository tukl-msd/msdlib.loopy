package de.hopp.generator.backends.workflow.ise;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_4;
import de.hopp.generator.model.BDLFilePos;

/**
 * Generation backend for a project for Xilinx XPS version 14.4.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class ISE_14_4 extends ISE {

    @Override
    public String getName() {
        return "ise14.4";
    }
    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // check if the supplied board is compatible with this workflow
        if( !(config.board() instanceof ISEBoard_14_4)) {
            errors.addError(new GenerationFailed(config.board().getName() +
                " board incompatible with " + config.flow().getName() + " workflow"));
            return;
        }

        xps = ((ISEBoard_14_4)config.board()).getMHS_14_4(errors);
        sdk = ((ISEBoard_14_4)config.board()).getSDK_14_4(config, errors);
        super.generate(board, config, errors);
    }

    public Class<ISEBoard_14_4> getBoardInterface() {
        return ISEBoard.ISEBoard_14_4.class;
    }
}
