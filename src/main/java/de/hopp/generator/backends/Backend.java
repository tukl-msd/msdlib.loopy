package de.hopp.generator.backends;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.frontend.BDLFilePos;

public interface Backend {
    
    String getName();
    
    void printUsage(IOHandler IO);
    
    Configuration parseParameters(Configuration config, String[] args);
    
    void generate(BDLFilePos board, Configuration config, ErrorCollection errors);
}
