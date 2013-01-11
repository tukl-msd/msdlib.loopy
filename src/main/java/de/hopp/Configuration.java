package de.hopp;

import java.io.File;

/**
 * Configuration of the generator run itself, not of the board
 * (but the board layout is stored here as well).
 * @author Thomas Fischer
 *
 */
public class Configuration {

    // destination folders
    public final static String defaultServerDir = "server";
    public final static String defaultClientDir = "client";

//  private File destDir = new File();
    private File serverDir = new File(defaultServerDir);
    private File clientDir = new File(defaultClientDir);
    
    // Ethernet related properties
    public final static String[] defaultMAC  = {"00","0a","35","00","01","02"};
    public final static int[]    defaultIP   = {192,169,  1, 10},
                                 defaultGW   = {192,169,  1, 23},
                                 defaultMask = {255,255,255,  0};
    public final static int      defaultPort = 8844;

    private String[] mac  = defaultMAC;
    private int []   ip   = defaultIP,
                     gw   = defaultGW,
                     mask = defaultMask;
    private int      port = defaultPort;
    
    // debug flags
    private boolean debug   = false;
    private boolean verbose = false;
    
//    /** setup an empty driver generator configuration */
//    public Configuration() { }
    
    /** set the directory, into which the client-side files of the driver should be generated */
    public void setClientDir(File dir) {
        this.clientDir = dir;
    }
    
    /** set the directory, into which the server-side files of the driver should be generated */
    public void setServerDir(File dir) {
        this.serverDir = dir;
    }
    
//    /** set the directory, into which the driver should be generated */
//    public void setDestDir(File dir) {
//        this.destDir = dir;
////        serverDir = new File(destDir, "server");
//        serverDir = destDir;
//        clientDir = new File(destDir, "client");
//    }
    
    /** enables the debug flag, which will result in additional console prints of the driver */
    public void enableDebug() {
        debug = true;
    }
    /** enables the verbose flag, which will result in additional console prints of the driver generator */
    public void enableVerbose() {
        verbose = true;
    }

    /** set mac address, which should be generated in driver. The mac
     * address is checked for validity.
     * @param mac target mac address represented as string array. The
     * array should contain exactly six hexadecimal strings of length two.
     */
    public void setMac(String[] mac) {
        if(mac.length != 6) throw new IllegalArgumentException("invalid mac address: must contain 6 components");
        for(String s : mac) if(s.length() != 2)
            throw new IllegalArgumentException("invalid mac address: each component has to be 2 characters long");
        for(String s : mac) if(!isHexString(s))
            throw new IllegalArgumentException("invalid mac address: only hexadecimal characters are allowed");
        this.mac = mac;
    }
    
    private static boolean isHexString(String s) {
        for(char c : s.toCharArray()) {
            if(!isHexCharacter(c)) return false;
        }
        return true;
    }
    
    private static boolean isHexCharacter(char c) {
        return c >= 0 || c <= 9 || c >= 'a' || c <= 'f' || c >= 'A' || c <= 'F'; 
    }
    
//    private boolean isHexStringRegEx(String s) {
//        return s.matches("[0123456789abcdefABCDEF]+");
//    }
    
    /** set the ip address, which should be generated in the driver.
     *  The ip address is checked for validity.
     * @param ip ip address, represented as string array. The array
     * should contain exactly four decimal numbers ranging from 0 to 255
     */
    public void setIP(String[] ip) {
        this.ip = convertIPAddress(ip);
    }
   
    /** set the subnet mask, which should be generated in the driver.
     *  The subnet mask is checked for validity.
     * @param mask subnet mask, represented as string array. The array
     * should contain exactly four decimal numbers ranging from 0 to 255
     */
    public void setMask(String[] mask) {
        this.mask = convertIPAddress(mask);
    }
   
    /** set the standard gateway, which should be generated in the driver.
     *  The standard gateway is checked for validity.
     * @param gw standard gateway, represented as string array. The array
     * should contain exactly four decimal numbers ranging from 0 to 255
     */
    public void setGW(String[] gw) {
        this.gw = convertIPAddress(gw);
    }
    
    public void setPort(int port) {
        this.port = port;
    }
   
    /** converts ip addresses represented as string arrays int integer arrays.
     * Also does validity checks for the given ip address.
     * @throws IllegalArgumentException The given string array does not fulfill
     * format requirements for ip addresses, i.e. wrong size or wrong contents.
     */
    private static int[] convertIPAddress(String[] ip) throws IllegalArgumentException {
        int[] targ = new int[4];
        if(ip.length != 4) throw new IllegalArgumentException("invalid ip address: must contain 4 components");
        for(String s : ip) if(s.length() < 1 || s.length() > 3)
            throw new IllegalArgumentException("invalid ip address: each component has to be 1-3 characters long");
        for(int i = 0; i<ip.length; i++) {
            try {
                int j = Integer.valueOf(ip[i]);
                if (j < 0 | j > 255) 
                    throw new IllegalArgumentException("invalid ip address: components can only range from 0 to 255");
                targ[i] = j;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid ip address: components have to be decimal numbers");
            }
        }
        return targ;
    }
    
//    /** get the directory, into which the driver should be generated */
//    public File getDest()    { return destDir; }
    /** get the directory, into which the server files should be generated */
    public File serverDir()  { return serverDir; }
    /** get the directory, into which the client files should be generated */
    public File clientDir()  { return clientDir; }
    /** get the debug flag indicating additional console print outs of the generated driver */
    public boolean debug()   { return debug; }
    /** get the verbose flag indicating additional console print outs of the driver generator */
    public boolean verbose() { return verbose; }
    /** get the MAC address for the board */
    public String[] getMAC() { return mac; }
    /** get the IP address for the board */
    public int[] getIP()     { return ip; }
    /** get the subnet mask for the board */
    public int[] getMask()   { return mask; }
    /** get the standard gateway for the board */
    public int[] getGW()     { return gw; }
    /** get the port over which Ethernet communication should be sent */
    public int getPort()     { return port; }
}
