package de.hopp.generator;

import java.io.File;

import de.hopp.generator.backends.client.ClientBackend;
import de.hopp.generator.backends.server.ServerBackend;

/**
 * Configuration of the generator run itself, not of the board.
 * @author Thomas Fischer
 *
 */
public class Configuration {

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

    private IOHandler IO;
    
    // debug flag (for generated driver)
    private boolean debug   = false;
    
    // progress flags
    private boolean dryrun    = false;
    private boolean parseonly = false;
    
    
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
    
    public void enableDryrun() {
        dryrun = true;
    }
    public void enableParseonly() {
        parseonly = true;
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
    /** get the debug flag indicating additional console print outs of the generated driver */
    public boolean debug()   { return debug; }
    
    public boolean dryrun() { return dryrun; }
    public boolean parseonly() { return parseonly; }
    
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
        IO.println("- host backend    : " + client().getName());
        IO.println("- board backend   : " + server().getName());
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
