package de.hopp;

import java.io.File;

/**
 * Configuration of the generator run itself, not of the board
 * (but the board layout is stored here as well).
 * @author Thomas Fischer
 *
 */
public class Configuration {

    private File  sourceFile;
    private File  destDir;
    
    /** setup an empty driver generator configuration */
    public Configuration() { 
    }
    
    /** set the source file, for which the driver should be generated */
    public void setSourceFile(File file) {
        this.sourceFile = file;
    }
    
    /** set the directory, into which the driver should be generated */
    public void setDestDir(File dir) {
        this.destDir = dir;
    }
    
    /** get the source file, for which the driver should be generated */
    public File getSourceFile() {
        return sourceFile;
    }
    
    /** get the directory, into which the driver should be generated */
    public File getDest() {
        return destDir;
    }
}
