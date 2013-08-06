package de.hopp.generator.backends.workflow.ise;

import java.io.File;
import java.util.Map;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.board.BoardIF;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.MHS_14_1;
import de.hopp.generator.backends.workflow.ise.xps.MHS_14_4;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.BDLFile;

/**
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public interface ISEBoard extends BoardIF {

    public String getArch();
    public String getDev();
    public String getPack();
    public String getSpeed();

    public File xpsSources();
    public File sdkSources();

    public MHS_14_1 getMHS_14_1(ErrorCollection errors);
    public MHS_14_4 getMHS_14_4(ErrorCollection errors);

    public Map<String, String> getData(BDLFile bdlFile) throws ParserError;

    public GpioComponent getGpio(String name);
}
