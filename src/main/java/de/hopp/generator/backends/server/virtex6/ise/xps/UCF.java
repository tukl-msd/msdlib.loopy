package de.hopp.generator.backends.server.virtex6.ise.xps;

import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.edkDir;
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;

import de.hopp.generator.Configuration;
import de.hopp.generator.backends.server.virtex6.ise.gpio.GpioEnum;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.BDLFile;
import de.hopp.generator.frontend.ETHERNET;
import de.hopp.generator.frontend.GPIO;

/**
 * Handles generation and deployment of user constraint files.
 *
 * @author Thomas Fischer
 * @since 24.5.13
 */
public class UCF {

    /**
     * Generates and deploys a .ucf constraint file for a board description.
     * @param bdlFile BDL description
     * @param config Configuration for this run containing required directories.
     * @throws IOException if an Exception occurred with the underlying file operations.
     * @throws ParserError If errors occurred during generation of the .ucf file
     */
    public static void deployUCF(BDLFile bdlFile, Configuration config) throws IOException, ParserError {
        File target = new File(new File(edkDir(config), "data"), "system.ucf");
        write(target, generateUCF(bdlFile));
    }

    private static String generateUCF(BDLFile bdlFile) throws ParserError {
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

        for(GPIO term : bdlFile.gpios()) {
            GpioEnum gpio;

            try {
                gpio = GpioEnum.fromString(term.name());
            } catch (IllegalArgumentException e) {
                throw new ParserError(e.getMessage(), term.pos());
            }

            ucf += gpio.getUCFConstraints();
        }

        return ucf;
    }

}
