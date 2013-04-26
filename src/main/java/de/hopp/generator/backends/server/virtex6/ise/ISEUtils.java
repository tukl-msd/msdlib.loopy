package de.hopp.generator.backends.server.virtex6.ise;

import java.io.File;

import de.hopp.generator.Configuration;

public class ISEUtils {

    public static File sdkDir(Configuration config) {
        return new File(config.tempDir(), "sdk");
    }
    public static File sdkAppDir(Configuration config) {
        return new File(sdkDir(config), "app");
    }
    public static File sdkBSPDir(Configuration config) {
        return new File(sdkDir(config), "app_bsp");
    }
    public static File edkDir(Configuration config) {
        return new File(config.tempDir(), "edk");
    }
    
}
