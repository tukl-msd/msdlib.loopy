package de.hopp;

import static de.hopp.generator.board.BoardSpec.Board;

import java.io.File;

import de.hopp.generator.board.Board;

/**
 * Configuration of the generator run itself, not of the board
 * (but the board layout is stored here as well).
 * @author Thomas Fischer
 *
 */
public class Configuration {

    private Board board;    
    private File  sourceFile;
    private File  destDir;
    
    /** setup an empty driver generator configuration */
    public Configuration() { 
        board = Board();
    }
    
    /** set the source file, for which the driver should be generated */
    public void setSourceFile(File file) {
        this.sourceFile = file;
    }
    
    /** set the board, for which the driver should be generated */
    public void setBoard(Board board) {
        this.board = board;
    }
    
    /** set the directory, into which the driver should be generated */
    public void setDestDir(File dir) {
        this.destDir = dir;
    }
    
    /** get the board, for which the driver should be generated */
    public Board getBoard() {
        return board;
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
