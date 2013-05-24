package de.hopp.generator.backends.server.virtex6.ise.xps;

import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.edkDir;
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;

import de.hopp.generator.Configuration;
import de.hopp.generator.frontend.BDLFile;
import de.hopp.generator.frontend.ETHERNET;
import de.hopp.generator.frontend.GPIO;

/**
 * Handles generation and deployment of user constraint files.
 *
 * @author Thomas Fischer
 * @since 24.5.13
 */
public class XPSUtils {

    /**
     * Generates and deploys a .ucf constraint file for a board description.
     * @param bdlFile BDL description
     * @param config Configuration for this run containing required directories.
     * @throws IOException if an Exception occurred with the underlying file operations.
     */
    public static void deployUCF(BDLFile bdlFile, Configuration config) throws IOException {
        File dataDir = new File(edkDir(config), "data");
        write(dataDir, generateUCF(bdlFile));
    }

    private static String generateUCF(BDLFile bdlFile) {
        String ucf = "# generic pin constraints\n" +
            "NET CLK_N LOC = \"H9\"   |  IOSTANDARD = \"LVDS_25\"  |  DIFF_TERM = \"TRUE\";\n" +
            "NET CLK_P LOC = \"J9\"   |  IOSTANDARD = \"LVDS_25\"  |  DIFF_TERM = \"TRUE\";\n" +
            "NET RESET LOC = \"H10\"  |  IOSTANDARD = \"SSTL15\"   |  TIG;\n" +
            "\n" +
            "NET RS232_Uart_1_sin LOC  = \"J24\"  |  IOSTANDARD = \"LVCMOS25\";\n" +
            "NET RS232_Uart_1_sout LOC = \"J25\"  |  IOSTANDARD = \"LVCMOS25\";\n" +
            "\n" +
            "# generic additional constraints\n" +
            "NET \"CLK\" TNM_NET = sys_clk_pin;\n" +
            "TIMESPEC TS_sys_clk_pin = PERIOD sys_clk_pin 200000 kHz;\n" +
            "\n" +
            "# non-generic pin constraints\n";

        if(bdlFile.medium() instanceof ETHERNET) {
            ucf += "\nNET Ethernet_Lite_COL LOC       = \"AK13\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_CRS LOC       = \"AL13\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_MDC LOC       = \"AP14\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_MDIO LOC      = \"AN14\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_PHY_RST_N LOC = \"AH13\"  |  IOSTANDARD = \"LVCMOS25\"  |  TIG;" +
                   "\nNET Ethernet_Lite_RXD[0] LOC    = \"AN13\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[1] LOC    = \"AF14\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[2] LOC    = \"AE14\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[3] LOC    = \"AN12\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_CLK LOC    = \"AP11\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_DV LOC     = \"AM13\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_ER LOC     = \"AG12\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[0] LOC    = \"AM11\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[1] LOC    = \"AL11\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[2] LOC    = \"AG10\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[3] LOC    = \"AG11\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TX_CLK LOC    = \"AD12\"  |  IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TX_EN LOC     = \"AJ10\"  |  IOSTANDARD = \"LVCMOS25\";\n";
        }

        for(GPIO gpio : bdlFile.gpios()) {
            String name = gpio.name();
            if(name.equals("leds")) {
                ucf += "\nNET LEDs_8Bits_TRI_O[0] LOC = \"AC22\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[1] LOC = \"AC24\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[2] LOC = \"AE22\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[3] LOC = \"AE23\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[4] LOC = \"AB23\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[5] LOC = \"AG23\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[6] LOC = \"AE24\"  |  IOSTANDARD = \"LVCMOS25\";" +
                       "\nNET LEDs_8Bits_TRI_O[7] LOC = \"AD24\"  |  IOSTANDARD = \"LVCMOS25\";\n";

            } else if(name.equals("buttons")) {
                ucf += "\nNET Push_Buttons_5Bits_TRI_I[0] LOC = \"G26\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET Push_Buttons_5Bits_TRI_I[1] LOC = \"A19\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET Push_Buttons_5Bits_TRI_I[2] LOC = \"G17\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET Push_Buttons_5Bits_TRI_I[3] LOC = \"A18\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET Push_Buttons_5Bits_TRI_I[4] LOC = \"H17\"  |  IOSTANDARD = \"LVCMOS15\";\n";

            } else if(name.equals("switches")) {
                ucf += "\nNET DIP_Switches_8Bits_TRI_I[0] LOC = \"D22\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[1] LOC = \"C22\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[2] LOC = \"L21\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[3] LOC = \"L20\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[4] LOC = \"C18\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[5] LOC = \"B18\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[6] LOC = \"K22\"  |  IOSTANDARD = \"LVCMOS15\";" +
                       "\nNET DIP_Switches_8Bits_TRI_I[7] LOC = \"K21\"  |  IOSTANDARD = \"LVCMOS15\";\n";
            }
        }

        return ucf;
    }

}
