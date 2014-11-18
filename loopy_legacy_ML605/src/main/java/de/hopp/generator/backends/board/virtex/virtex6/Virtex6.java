package de.hopp.generator.backends.board.virtex.virtex6;

import static de.hopp.generator.backends.workflow.ise.ISEUtils.edkDir;
import static de.hopp.generator.backends.workflow.ise.ISEUtils.sdkDir;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.Memory;
import de.hopp.generator.backends.board.virtex.virtex6.gpio.Gpio;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_1;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_4;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_6;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.sdk.DriverVersions;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.model.BDLFile;
import de.hopp.generator.model.ETHERNET;
import de.hopp.generator.model.GPIO;

/**
 * Virtex 6 board backend.
 *
 * This backend provides workflows with information specific for a Xilinx Virtex 6 board.
 * Currently, different versions of the ISE workflow are supported.
 *
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public class Virtex6 implements ISEBoard_14_1, ISEBoard_14_4, ISEBoard_14_6 {

    protected static final String folder =
        "deploy" + File.separator +
        "board"  + File.separator +
        "virtex" + File.separator +
        "virtex6";

    public static final String mbIdent   = "microblaze_0";
    public static final String intcIdent = mbIdent + "_intc";

    // board memory and clock models
    private Memory memory = new Memory(0x40000000, 0x4fffffff);
    private Clock  clock  = new Clock();

    public String getName()  { return "virtex6"; }

    public void printUsage(IOHandler IO) {
        IO.println(" no usage help provided");
        IO.println(" I'm the usage help!");
    }

    public String getArch()  { return "virtex6"; }
    public String getDev()   { return "xc6vlx240t"; }
    public String getPack()  { return "ff1156"; }
    public String getSpeed() { return "-1"; }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }

    @Override
    public Map<String, String> getData(BDLFile bdlFile) throws ParserError {
        Map<String, String> constraints = new HashMap<String, String>();

        constraints.put("system.ucf", getUCF(bdlFile));

        return constraints;
    }

    public Memory getMemory() { return memory; }
    public Clock  getClock()  { return clock; }

    @Override
    public GpioComponent getGpio(String name) throws IllegalArgumentException {
        return Gpio.fromString(name).getInstance();
    }

    @Override
    public Set<File> boardFiles(Configuration config) {
        Set<File> files = new HashSet<File>();

        files.add(new File(edkDir(config), "implementation/system.bit"));
        files.add(new File(sdkDir(config), "app/Debug/app.elf"));

        return files;
    }

    @Override
    public MHS getMHS_14_1(ErrorCollection errors) {
        return new MHS(this, IPCoreVersions.ISE14_1, errors);
    }

    @Override
    public SDK getSDK_14_1(Configuration config, ErrorCollection errors) {
        return new SDK(config, DriverVersions.ISE14_1, errors);
    }

    @Override
    public MHS getMHS_14_4(ErrorCollection errors) {
        return new MHS(this, IPCoreVersions.ISE14_4, errors);
    }

    @Override
    public SDK getSDK_14_4(Configuration config, ErrorCollection errors) {
        return new SDK(config, DriverVersions.ISE14_4, errors);
    }

    @Override
    public MHS getMHS_14_6(ErrorCollection errors) {
        return new MHS(this, IPCoreVersions.ISE14_6, errors);
    }

    @Override
    public SDK getSDK_14_6(Configuration config, ErrorCollection errors) {
        return new SDK(config, DriverVersions.ISE14_6, errors);
    }

    /**
     * Generates and a .ucf constraint file for a board description.
     *
     * This .ucf file has to be complete, i.e. it contains all pin constraints for
     * this board and the provided design.
     *
     * @param bdlFile BDL description of the board design.
     * @return The generated user constraint file.
     * @throws ParserError If errors occurred during generation of the .ucf file
     */
    private String getUCF(BDLFile bdlFile) throws ParserError {
        // generic pin constraints
        String ucf = "# generic pin constraints\n" +
            "NET CLK_N LOC =  \"H9\" | IOSTANDARD = \"LVDS_25\" | DIFF_TERM = \"TRUE\";\n" +
            "NET CLK_P LOC =  \"J9\" | IOSTANDARD = \"LVDS_25\" | DIFF_TERM = \"TRUE\";\n" +
            "NET RESET LOC = \"H10\" | IOSTANDARD =  \"SSTL15\" | TIG;\n" +
            "\n" +
            "NET RS232_Uart_1_sin  LOC = \"J24\" | IOSTANDARD = \"LVCMOS25\";\n" +
            "NET RS232_Uart_1_sout LOC = \"J25\" | IOSTANDARD = \"LVCMOS25\";\n" +
            "\n" +
            "# generic additional constraints\n" +
            "NET \"CLK\" TNM_NET = sys_clk_pin;\n" +
            "TIMESPEC TS_sys_clk_pin = PERIOD sys_clk_pin 200000 kHz;\n" +
            "\n" +
            "# non-generic pin constraints\n";

        // additional constraints for an Ethernet medium
        if(bdlFile.medium() instanceof ETHERNET) {
            ucf += "\nNET Ethernet_Lite_COL       LOC = \"AK13\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_CRS       LOC = \"AL13\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_MDC       LOC = \"AP14\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_MDIO      LOC = \"AN14\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_PHY_RST_N LOC = \"AH13\" | IOSTANDARD = \"LVCMOS25\" | TIG;" +
                   "\nNET Ethernet_Lite_RXD[0]    LOC = \"AN13\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[1]    LOC = \"AF14\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[2]    LOC = \"AE14\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RXD[3]    LOC = \"AN12\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_CLK    LOC = \"AP11\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_DV     LOC = \"AM13\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_RX_ER     LOC = \"AG12\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[0]    LOC = \"AM11\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[1]    LOC = \"AL11\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[2]    LOC = \"AG10\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TXD[3]    LOC = \"AG11\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TX_CLK    LOC = \"AD12\" | IOSTANDARD = \"LVCMOS25\";" +
                   "\nNET Ethernet_Lite_TX_EN     LOC = \"AJ10\" | IOSTANDARD = \"LVCMOS25\";\n";
        }

        // constraints for gpio components
        for(GPIO term : bdlFile.gpios()) {
            GpioComponent gpio;

            try {
                gpio = Gpio.fromString(term.name()).getInstance();
            } catch (IllegalArgumentException e) {
                throw new ParserError(e.getMessage(), term.pos());
            }

            ucf += gpio.getUCFConstraints();
        }

        return ucf;
    }
}
