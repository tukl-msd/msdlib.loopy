package de.hopp.generator.backends.workflow.ise.xps;

import de.hopp.generator.model.BDLFilePos;
import de.hopp.generator.model.mhs.MHSFile;

public interface MHS {
    public MHSFile generateMHSFile(BDLFilePos file);
}
