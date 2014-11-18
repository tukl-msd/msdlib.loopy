package de.hopp.generator.backends.workflow.ise;

import java.io.File;

import de.hopp.generator.Configuration;

/**
 * Utility methods used by all ISE backends.
 *
 * Provides directory locations to be used for deployment of sourcefiles
 * for the Xilinx ISE workflow.
 * @author Thomas Fischer
 */
public class ISEUtils {

    /**
     * Returns the directory used for Xilinx SDK files.
     * @param config Configuration of this run.
     * @return The file representing the target directory.
     */
    public static File sdkDir(Configuration config) {
        return new File(config.tempDir(), "sdk");
    }

    /**
     * Returns the directory used for the Xilinx SDK application project.
     * @param config Configuration of this run.
     * @return The file representing the target directory.
     */
    public static File sdkAppDir(Configuration config) {
        return new File(sdkDir(config), "app");
    }

    /**
     * Returns the directory used for the Xilinx SDK board support package.
     * @param config Configuration of this run.
     * @return The file representing the target directory.
     */
    public static File sdkBSPDir(Configuration config) {
        return new File(sdkDir(config), "app_bsp");
    }

    /**
     * Returns the directory used for the Xilinx XPS files.
     * @param config Configuration of this run.
     * @return The file representing the target directory.
     */
    public static File edkDir(Configuration config) {
        return new File(config.tempDir(), "edk");
    }
}
