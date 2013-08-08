package de.hopp.generator.backends.board.zed;

import java.io.File;
import java.util.Map;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.board.virtex.virtex6.MHS;
import de.hopp.generator.backends.board.virtex.virtex6.gpio.Gpio;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_4;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_6;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.backends.workflow.vivado.VivadoBoard;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.BDLFile;

/**
 *
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public class Zed implements ISEBoard_14_4, ISEBoard_14_6, VivadoBoard {

    protected String folder = "deploy" +
             File.separator + "board"  +
             File.separator + "zed";

    public String getName()  { return "zed"; }

    public void printUsage(IOHandler IO) {
        // TODO Auto-generated method stub
        IO.println(" no usage help provided");
    }

    public String getArch()  { return "zynq"; }
    public String getDev()   { return "xc7z020"; }
    public String getPack()  { return "clg484"; }
    public String getSpeed() { return "-1"; }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }

    @Override
    public MHS getMHS_14_4(ErrorCollection errors) {
        return new MHS(this, IPCoreVersions.ISE14_4, errors);
    }

    @Override
    public MHS getMHS_14_6(ErrorCollection errors) {
        return new MHS(this, IPCoreVersions.ISE14_6, errors);
    }

    public Map<String, String> getData(BDLFile bdlFile) throws ParserError {
        // FIXME constraint files? probably need something different from a single UCF file as well!
        // (as in... getDataFiles or similar...)
        throw new UnsupportedOperationException("Zed Board not fully supported yet");
    }

    public GpioComponent getGpio(String name) {
        return Gpio.fromString(name).getInstance();
    }
}
