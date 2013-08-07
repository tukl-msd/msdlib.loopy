package de.hopp.generator.backends.workflow.ise.xps;

import de.hopp.generator.frontend.BDLFilePos;
import de.hopp.generator.parser.MHSFile;

public interface MHS {
    public MHSFile generateMHSFile(BDLFilePos file);
}
