package de.hopp.generator.utils;

public class Ethernet {

    public static String unparseIP(int[] ip) {
        return java.util.Arrays.toString(ip).replace(", ", ".").replace("[", "").replace("]", "");
    }
    public static String unparseMAC(String[] ip) {
        return java.util.Arrays.toString(ip).replace(", ", ":").replace("[", "").replace("]", "");
    }
}
