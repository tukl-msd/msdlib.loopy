package de.hopp.generator.backends.server.virtex6.ise;

import java.io.File;

import de.hopp.generator.Configuration;

public class ISEUtils {

    public static File sdkDir(Configuration config) {
        return new File(config.tempDir(), "sdk");
    }
    public static File edkDir(Configuration config) {
        return new File(config.tempDir(), "edk");
    }
    
}
