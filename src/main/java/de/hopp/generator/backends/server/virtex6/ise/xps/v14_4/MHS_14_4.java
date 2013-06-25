package de.hopp.generator.backends.server.virtex6.ise.xps.v14_4;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.server.virtex6.ise.xps.v14_1.MHS_14_1;

/**
 * Generation backend for a project for Xilinx XPS version 14.4. This includes
 * an .mhs file describing the board as well as several default components like
 * parameterised queues and DeMUXes.
 * 
 * @author Thomas Fischer
 */
public class MHS_14_4 extends MHS_14_1 {

    /**
     * Creates an XPS 14.4 project backend for a Virtex 6 board.
     * 
     * Initialises version strings of components from the Xilinx catalogue.
     * 
     * @param config Configuration for this backend and the related generator run.
     * @param errors ErrorCollection for this backend and the related generator run.
     */
    public MHS_14_4(Configuration config, ErrorCollection errors) {
        super(config, errors);

        // not sure if these are 14.4 changes or if they are available in an
        // earlier version
        // shouldn't the core version be independent of the xps version
        // anyways...?
        version_microblaze        = "8.40.a";

        version_axi_intc          = "1.03.a";
        version_lmb_bram_if_cntlr = "3.10.c";
        version_mdm               = "2.10.a";
        version_axi_v6_ddrx       = "1.06.a";
    }
}
