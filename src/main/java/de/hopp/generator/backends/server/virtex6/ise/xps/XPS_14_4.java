package de.hopp.generator.backends.server.virtex6.ise.xps;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;

/**
 * Generation backend for a project for Xilinx XPS version 14.4.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class XPS_14_4 extends XPS_14_1 {

    /**
     * Creates an XPS 14.1 project backend for a Virtex 6 board.
     * 
     * Initialises version strings of components from the Xilinx catalogue.
     * @param config Configuration for this backend and the related generator run.
     * @param errors ErrorCollection for this backend and the related generator run.
     */
    public XPS_14_4(Configuration config, ErrorCollection errors) {
        super(config, errors);
    }
}
