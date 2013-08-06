package de.hopp.generator.backends.workflow.ise.xps;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public abstract class MHS_14_1 extends MHSGenerator {

    /**
     * Creates an XPS 14.1 project backend for a Virtex 6 board.
     *
     * Initialises version strings of components from the Xilinx catalogue.
     * @param board ISE compatible board model of the selected board
     * @param errors ErrorCollection for this backend and the related generator run.
     */
    public MHS_14_1(ISEBoard board, ErrorCollection errors) {
        this.board  = board;
        this.errors = errors;
        // version strings
        version                   = "2.1.0";

        version_microblaze        = "8.30.a";

        version_axi_intc          = "1.02.a";
        version_axi_interconnect  = "1.06.a";
        version_axi_timer         = "1.03.a";
        version_axi_v6_ddrx       = "1.05.a";
        version_bram_block        = "1.00.a";
        version_clock_generator   = "4.03.a";
        version_lmb_bram_if_cntlr = "3.00.b";
        version_lmb_v10           = "2.00.b";
        version_mdm               = "2.00.b";
        version_proc_sys_reset    = "3.00.a";

        version_axi_uartlite      = "1.02.a";
        version_axi_ethernetlite  = "1.01.b";

        version_gpio_leds         = "1.01.b";
        version_gpio_buttons      = "1.01.b";
        version_gpio_switches     = "1.01.b";
    }
}
