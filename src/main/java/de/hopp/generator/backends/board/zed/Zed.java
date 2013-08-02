package de.hopp.generator.backends.board.zed;

import java.io.File;

import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.vivado.VivadoBoard;

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
}
