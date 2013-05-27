package de.hopp.generator;

import java.io.File;

import de.hopp.generator.backends.client.ClientBackend;
import de.hopp.generator.backends.server.ServerBackend;

/**
 * Configuration of the generator run itself.
 * This includes used backends, target directories and several debugging flags.
 * @author Thomas Fischer
 */
public class Configuration {

    private final IOHandler IO;
    private String[] args;

    // backends
    private ClientBackend client;
    private ServerBackend server;

    // destination folders
    public final static String defaultServerDir = "server";
    public final static String defaultClientDir = "client";
    public final static String defaultTempDir   = "temp";

    private File serverDir = new File(defaultServerDir);
    private File clientDir = new File(defaultClientDir);
    private File tempDir   = new File(defaultTempDir);

    // logging related properties
    public static final int LOG_QUIET   = 0;
    public static final int LOG_INFO    = 100;
    public static final int LOG_VERBOSE = 200;
    public static final int LOG_DEBUG   = 300;

    private int loglevel = LOG_INFO;

    // progress flags
    private boolean parseonly = false;
    private boolean dryrun    = false;
    private boolean noBitGen  = false;

    private boolean startGUI  = false;

//    /** setup an empty driver generator configuration */
    public Configuration() {
        IO = new IOHandler(this);
    }

    public void setServer(ServerBackend server) { this.server = server; }
    public void setClient(ClientBackend client) { this.client = client; }

    /** set the directory, into which the client-side files of the driver should be generated */
    public void setClientDir(File dir) {
        this.clientDir = dir;
    }

    /** set the directory, into which the server-side files of the driver should be generated */
    public void setServerDir(File dir) {
        this.serverDir = dir;
    }

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
    /** disable bitfile generation */
    public void enableNoBitGen() {
        noBitGen = true;
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

    public ServerBackend server() { return server; }
    public ClientBackend client() { return client; }

    /** get the directory, into which the server files should be generated */
    public File serverDir()  { return serverDir; }
    /** get the directory, into which the client files should be generated */
    public File clientDir()  { return clientDir; }
    /** get the directory, into which temporary files should be generated */
    public File tempDir()    { return tempDir; }

    /** check if the parseonly flag is set, meaning that only the frontend should be executed */
    public boolean parseonly() { return parseonly; }
    /** check if this is only a dryrun, meaning that code deployment is skipped */
    public boolean dryrun()    { return dryrun; }
    /** check if bitfile generation should be skipped */
    public boolean noBitGen()  { return noBitGen; }

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
        IO.println("- host backend    : " + (client == null ? "none" : client().getName()));
        IO.println("- board backend   : " + (server == null ? "none" : server().getName()));
        IO.println("- host folder     : " + clientDir().getAbsolutePath());
        IO.println("- board folder    : " + serverDir().getAbsolutePath());
        IO.println("- temp folder     : " + tempDir().getAbsolutePath());

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
