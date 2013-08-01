package de.hopp.generator.backends.board.virtex.virtex6;

import java.io.File;

import de.hopp.generator.backends.workflow.ise.ISEBoard;

public class Virtex6 implements ISEBoard {

    protected String folder = "deploy" +
             File.separator + "board"  +
             File.separator + "virtex" +
             File.separator + "virtex6";

    public String getName()  { return "virtex6"; }

    public String getArch()  { return "virtex6"; }
    public String getDev()   { return "xc6vlx240t"; }
    public String getPack()  { return "ff1156"; }
    public String getSpeed() { return "-1"; }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }
}
