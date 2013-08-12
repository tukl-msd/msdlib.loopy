package de.hopp.generator.backends.workflow.ise;

import java.io.File;
import java.util.Map;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.board.BoardIF;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.sdk.SDKGenerator;
import de.hopp.generator.backends.workflow.ise.xps.MHS;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.BDLFile;

/**
 * General interface for boards that can be synthesised using an ise workflow.
 * Do not implement boards against this interface, but against the more specialised
 * board interfaces for concrete ise versions.
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public interface ISEBoard extends BoardIF {

    public String getArch();
    public String getDev();
    public String getPack();
    public String getSpeed();

    /** The path to generic source files for the xps build */
    public File xpsSources();
    /** The path to generic source files for the xps build */
    public File sdkSources();

    /**
     * Data folder to deploy for the xps build.
     * @param bdlFile bdl model of the board.
     * @return Map from file names to file contents for the data folder.
     * @throws ParserError If some unknown component is encountered in the bdlFile,
     *      e.g. an unknown gpio component.
     */
    public Map<String, String> getData(BDLFile bdlFile) throws ParserError;

    public GpioComponent getGpio(String name);

    /**
     * Interface for boards that can be synthesised with ise version 14.1
     * @author Thomas Fischer
     */
    public interface ISEBoard_14_1 extends ISEBoard {
        /**
         * Generate an MHS visitor for the board and an ise 14.1 workflow.
         * The visitor has to be capable of creating an .mhs file for ise 14.1
         * from which the .bit file can be synthesised.
         * @param errors ErrorCollection to use by the visitor to store errors occurring
         *      during generation.
         * @return The MHS file for the board and ise 14.1.
         */
        public MHS getMHS_14_1(ErrorCollection errors);

        public SDKGenerator getSDK_14_1(Configuration config, ErrorCollection errors);
    }

    /**
     * Interface for boards that can be synthesised with ise version 14.4
     * @author Thomas Fischer
     */
    public interface ISEBoard_14_4 extends ISEBoard {
        /**
         * Generate an MHS visitor for the board and an ise 14.4 workflow.
         * The visitor has to be capable of creating an .mhs file for ise 14.4
         * from which the .bit file can be synthesised.
         * @param errors ErrorCollection to use by the visitor to store errors occurring
         *      during generation.
         * @return The MHS file for the board and ise 14.4.
         */
        public MHS getMHS_14_4(ErrorCollection errors);

        public SDKGenerator getSDK_14_4(Configuration config, ErrorCollection errors);
    }

    /**
     * Interface for boards that can be synthesised with ise version 14.6
     * @author Thomas Fischer
     */
    public interface ISEBoard_14_6 extends ISEBoard {
        /**
         * Generate an MHS visitor for the board and an ise 14.6 workflow.
         * The visitor has to be capable of creating an .mhs file for ise 14.6
         * from which the .bit file can be synthesised.
         * @param errors ErrorCollection to use by the visitor to store errors occurring
         *      during generation.
         * @return The MHS file for the board and ise 14.6.
         */
        public MHS getMHS_14_6(ErrorCollection errors);

        public SDKGenerator getSDK_14_6(Configuration config, ErrorCollection errors);
    }
}
