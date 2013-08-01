package de.hopp.generator.backends.board.zed;

import java.io.File;

import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.vivado.VivadoBoard;

public class Zed implements ISEBoard, VivadoBoard {

    protected String folder = "deploy" +
             File.separator + "board"  +
             File.separator + "zed";

    public String getName()  { return "zed"; }

    public String getArch()  { throw new UnsupportedOperationException("ZedBoard not yet supported"); }
    public String getDev()   { throw new UnsupportedOperationException("ZedBoard not yet supported"); }
    public String getPack()  { throw new UnsupportedOperationException("ZedBoard not yet supported"); }
    public String getSpeed() { throw new UnsupportedOperationException("ZedBoard not yet supported"); }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }
}
