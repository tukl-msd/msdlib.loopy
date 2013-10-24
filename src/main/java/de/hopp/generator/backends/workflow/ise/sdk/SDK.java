package de.hopp.generator.backends.workflow.ise.sdk;

import java.io.File;
import java.util.Map;

import de.hopp.generator.model.BDLFilePos;
import de.hopp.generator.model.cpp.MFile;
import de.hopp.generator.model.mhs.MHSFile;

public interface SDK {
    public void generate(BDLFilePos file);

    public MFile getComponents();

    public MFile getConstants();

    public MFile getScheduler();

    public MHSFile getMSS();

    public String getLScript();

    public Map<File, File> getFiles();
}
