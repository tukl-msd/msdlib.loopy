package de.hopp.generator.backends.board.zed;

import java.io.File;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.MHS_14_1;
import de.hopp.generator.backends.workflow.ise.xps.MHS_14_4;
import de.hopp.generator.backends.workflow.vivado.VivadoBoard;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.BDLFile;

/**
 *
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public class Zed implements ISEBoard, VivadoBoard {

    protected String folder = "deploy" +
             File.separator + "board"  +
             File.separator + "zed";

    public String getName()  { return "zed"; }

    public String getArch()  { return "zynq"; }
    public String getDev()   { return "xc7z020"; }
    public String getPack()  { return "clg484"; }
    public String getSpeed() { return "-1"; }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }

    public MHS_14_1 getMHS_14_1(ErrorCollection errors) {
        throw new UnsupportedOperationException("Zed Board not fully supported yet");
    }

    public MHS_14_4 getMHS_14_4(ErrorCollection errors) {
        throw new UnsupportedOperationException("Zed Board not fully supported yet");
    }

    public String getUCF(BDLFile bdlFile) throws ParserError {
        throw new UnsupportedOperationException("Zed Board not fully supported yet");
    }

    public GpioComponent getGpio(String name) {
        throw new UnsupportedOperationException("Zed Board not fully supported yet");
    }
}
