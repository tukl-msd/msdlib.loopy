package de.hopp.generator.backends.workflow.ise;

import java.io.File;

import de.hopp.generator.backends.board.BoardIF;

public interface ISEBoard extends BoardIF {

    public String getArch();
    public String getDev();
    public String getPack();
    public String getSpeed();

    public File xpsSources();
    public File sdkSources();

}
