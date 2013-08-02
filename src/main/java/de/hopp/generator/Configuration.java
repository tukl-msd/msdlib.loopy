package de.hopp.generator;

import java.io.File;

import de.hopp.generator.backends.board.BoardIF;
import de.hopp.generator.backends.host.HostBackend;
import de.hopp.generator.backends.workflow.WorkflowIF;

/**
 * Configuration of the generator run itself.
 * This includes used backends, target directories and several debugging flags.
 * @author Thomas Fischer
 */
public class Configuration {

    private final IOHandler IO;
    private String[] args;

    // backends
    private HostBackend client;
    private BoardIF board;
    private WorkflowIF flow;

    // destination folders
    public final static String defaultBoardDir = "board";
    public final static String defaultHostDir  = "host";
    public final static String defaultTempDir  = "temp";

    private File boardDir = new File(defaultBoardDir);
    private File hostDir  = new File(defaultHostDir);
    private File tempDir  = new File(defaultTempDir);

    // logging related properties
    public static final int LOG_QUIET   = 0;
    public static final int LOG_INFO    = 100;
    public static final int LOG_VERBOSE = 200;
    public static final int LOG_DEBUG   = 300;

    private int loglevel = LOG_INFO;

    // progress flags
    private boolean parseonly = false;
    private boolean dryrun    = false;
    private boolean noGen     = false;
    private boolean sdkOnly   = false;

    private boolean startGUI  = false;

//    /** setup an empty driver generator configuration */
    public Configuration() {
        IO = new IOHandler(this);
    }

    public void setClient(HostBackend client) { this.client = client; }
    public void setBoard(BoardIF board)         { this.board  = board; }
    public void setFlow(WorkflowIF flow)        { this.flow   = flow; }

    /** set the directory, into which the host-side files of the driver should be generated */
    public void setHostDir(File dir) {
        this.hostDir = dir;
    }

    /** set the directory, into which the board-side files of the driver should be generated */
    public void setBoardDir(File dir) {
        this.boardDir = dir;
    }

    /** set the directory, into which the temporary files of the driver should be generated */
    public void setTempDir(File dir) {
        this.tempDir = dir;
    }

    /** sets the log level to debug, which will result in additional console prints of the generator */
    public void enableDebug() {
        loglevel = LOG_DEBUG;
    }
    /** sets the log level to verbose, which will result in additional console prints of the generator */
    public void enableVerbose() {
        loglevel = LOG_VERBOSE;
    }
    /** sets the log level to quiet, which will disable console prints of the generator */
    public void enableQuiet() {
        loglevel = LOG_QUIET;
    }

    /** enable parse only mode, in which only lexer and parser are executed */
    public void enableParseonly() {
        parseonly = true;
    }
    /** enable dryrun, meaning that generation phases are skipped */
    public void enableDryrun() {
        dryrun = true;
    }
    /** disable generation */
    public void enableNoGen() {
        noGen = true;
    }
    /** disable xps */
    public void enableSDKOnly() {
        sdkOnly = true;
    }

    public void enableGUI() {
        startGUI = true;
    }

    public void setUnusued(String[] args) {
        this.args = args;
    }

    /**
     * Set the log level of this generator run.
     * @param printLevel
     */
    public void setPrintLevel(int printLevel) {
             if (printLevel < LOG_QUIET) loglevel = LOG_QUIET;
        else if (printLevel > LOG_DEBUG) loglevel = LOG_DEBUG;
        else loglevel = printLevel;
    }

    public String[] UNUSED() { return args; }

    public HostBackend client() { return client; }
    public BoardIF board() { return board; }
    public WorkflowIF flow() { return flow; }


    /** get the directory, into which the board-side files should be generated */
    public File boardDir()  { return boardDir; }
    /** get the directory, into which the host-side files should be generated */
    public File hostDir()  { return hostDir; }
    /** get the directory, into which temporary files should be generated */
    public File tempDir()    { return tempDir; }

    /** check if the parseonly flag is set, meaning that only the frontend should be executed */
    public boolean parseonly() { return parseonly; }
    /** check if this is only a dryrun, meaning that code deployment is skipped */
    public boolean dryrun()    { return dryrun; }
    /** check if generation should be skipped */
    public boolean noGen()     { return noGen; }
    /** check if xps should be skipped */
    public boolean sdkOnly()   { return sdkOnly; }

    public boolean startGUI()  { return startGUI; }

    /** check if the generator is set to produce no console output */
    public boolean QUIET()   { return loglevel == LOG_QUIET; }
    /** check if the generator is set to produce more console output */
    public boolean VERBOSE() { return loglevel >= LOG_VERBOSE; }
    /** check if the generator is set to produce debug console output */
    public boolean DEBUG()   { return loglevel >= LOG_DEBUG; }
    /** get the io handler associated with this run of the generator */
    public IOHandler IOHANDLER() { return IO; }

    /** print this config on console */
    public void printConfig() {
        IO.println("- host backend    : " + (client == null ? "none" : client.getName()));
        IO.println("- board backend   : " + (board  == null ? "none" : board.getName()));
        IO.println("- workflow backend: " + (flow   == null ? "none" : flow.getName()));
        IO.println("- host folder     : " + hostDir.getAbsolutePath());
        IO.println("- board folder    : " + boardDir.getAbsolutePath());
        IO.println("- temp folder     : " + tempDir.getAbsolutePath());

        IO.print  ("- log level       : ");
        switch(loglevel) {
        case LOG_QUIET   : IO.println("quiet");   break; // (;
        case LOG_INFO    : IO.println("info");    break;
        case LOG_VERBOSE : IO.println("verbose"); break;
        case LOG_DEBUG   : IO.println("debug");   break;
        default: // should never happen
        }

        if(parseonly)   IO.println("- parse only");
        else if(dryrun) IO.println("- dryrun");
    }
}
