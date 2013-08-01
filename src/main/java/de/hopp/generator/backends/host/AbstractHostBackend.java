package de.hopp.generator.backends.host;

import de.hopp.generator.Configuration;
import de.hopp.generator.IOHandler;

public abstract class AbstractHostBackend implements HostBackend {

    public void printUsage(IOHandler IO) {
        IO.println(" no parameters");
    }
    
    public Configuration parseParameters(Configuration config, String[] args) {
        // parse backend specific parameters...
        // basically everything, that was not handled by the system...
        config.setUnusued(args);
        return config;
    }
    
}
