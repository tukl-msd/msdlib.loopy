package de.hopp.generator.backends.workflow.ise.xps;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;

/**
 * Generation backend for a project for Xilinx XPS version 14.6. This includes
 * an .mhs file describing the board as well as several default components like
 * parameterised queues and DeMUXes.
 *
 * @author Thomas Fischer
 */
public abstract class MHS_14_6 extends MHS_14_4 {

    /**
     * Creates an XPS 14.6 project backend for a Virtex 6 board.
     *
     * Initialises version strings of components from the Xilinx catalogue.
     *
     * @param board ISE compatible board model of the selected board
     * @param errors ErrorCollection for this backend and the related generator run.
     */
    public MHS_14_6(ISEBoard board, ErrorCollection errors) {
        super(board, errors);

        // not sure if these are 14.6 changes or if they are available in an
        // earlier version
        // shouldn't the core version be independent of the xps version
        // anyways...?
        version_microblaze        = "8.50.b";

        version_axi_intc          = "1.04.a";
    }
}
