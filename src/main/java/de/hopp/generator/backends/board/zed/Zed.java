package de.hopp.generator.backends.board.zed;

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
import de.hopp.generator.backends.board.zed.gpio.Gpio;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_4;
import de.hopp.generator.backends.workflow.ise.ISEBoard.ISEBoard_14_6;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.sdk.DriverVersions;
import de.hopp.generator.backends.workflow.ise.xps.Clock;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.backends.workflow.vivado.VivadoBoard;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.model.BDLFile;
import de.hopp.generator.model.GPIO;

/**
 *
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public class Zed implements ISEBoard_14_4, ISEBoard_14_6, VivadoBoard {

    protected String folder = "deploy" +
             File.separator + "board"  +
             File.separator + "zed";

    // board memory and clock models
    private Memory memory = new Memory(0x40000000, 0x4fffffff);

    public String getName()  { return "zed"; }

    public void printUsage(IOHandler IO) {
        IO.println(" no usage help provided");
    }

    public String getArch()  { return "zynq"; }
    public String getDev()   { return "xc7z020"; }
    public String getPack()  { return "clg484"; }
    public String getSpeed() { return "-1"; }

    public Memory getMemory() { return memory; }
    // FIXME need a clock here ;)
    public Clock  getClock()  { return null; }

    public File xpsSources() { return new File(folder + File.separator + "xps"); }
    public File sdkSources() { return new File(folder + File.separator + "sdk"); }

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

    @Override
    public GpioComponent getGpio(String name) {
        return Gpio.fromString(name).getInstance();
    }

    @Override
    public Map<String, String> getData(BDLFile bdlFile) throws ParserError {
        Map<String, String> data = new HashMap<String, String>();

        data.put("ps7_constraints.ucf", getPS7UCF());
//        data.put("ps7_constraints.xdc", getPS7XDC()); // I'm guessing, xdc files are for vivado?
        data.put("ps7_system_prj.xml", getPS7XML());
        data.put("system.ucf", getUCF(bdlFile));
//        data.put("system.xdc", getXDC(bdlFile));

        return data;
    }

    @Override
    public Set<File> boardFiles(Configuration config) {
        Set<File> files = new HashSet<File>();

        files.add(new File(edkDir(config), "implementation/system.bit"));
        files.add(new File(sdkDir(config), "app/Debug/app.elf"));
        files.add(new File(sdkDir(config), "hw/ps7_init.tcl"));

        return files;
    }

    private String getUCF(BDLFile bdlFile) {
        String ucf = "";

        for(GPIO gpio : bdlFile.gpios())
            ucf += Gpio.fromString(gpio.name()).getInstance().getUCFConstraints();

        return ucf;
    }

    private String getPS7UCF() throws ParserError {
        return
            "NET \"MIO[53]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"C12\"; # Enet 0 / mdio / MIO[53]\n" +
            "NET \"MIO[52]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"D10\"; # Enet 0 / mdc / MIO[52]\n" +
            "NET \"MIO[51]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"C10\"; # GPIO / gpio[51] / MIO[51]\n" +
            "NET \"MIO[50]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"D13\"; # GPIO / gpio[50] / MIO[50]\n" +
            "NET \"MIO[49]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"C14\"; # UART 1 / rx / MIO[49]\n" +
            "NET \"MIO[48]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"D11\"; # UART 1 / tx / MIO[48]\n" +
            "NET \"MIO[47]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"B10\"; # SD 0 / cd / MIO[47]\n" +
            "NET \"MIO[46]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"D12\"; # SD 0 / wp / MIO[46]\n" +
            "NET \"MIO[45]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"B9\"; # SD 0 / data[3] / MIO[45]\n" +
            "NET \"MIO[44]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"E13\"; # SD 0 / data[2] / MIO[44]\n" +
            "NET \"MIO[43]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"B11\"; # SD 0 / data[1] / MIO[43]\n" +
            "NET \"MIO[42]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"D8\"; # SD 0 / data[0] / MIO[42]\n" +
            "NET \"MIO[41]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"C8\"; # SD 0 / cmd / MIO[41]\n" +
            "NET \"MIO[40]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"E14\"; # SD 0 / clk / MIO[40]\n" +
            "NET \"MIO[39]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"C13\"; # USB 0 / data[7] / MIO[39]\n" +
            "NET \"MIO[38]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"F13\"; # USB 0 / data[6] / MIO[38]\n" +
            "NET \"MIO[37]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"B14\"; # USB 0 / data[5] / MIO[37]\n" +
            "NET \"MIO[36]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A9\"; # USB 0 / clk / MIO[36]\n" +
            "NET \"MIO[35]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"F14\"; # USB 0 / data[3] / MIO[35]\n" +
            "NET \"MIO[34]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"B12\"; # USB 0 / data[2] / MIO[34]\n" +
            "NET \"MIO[33]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"G13\"; # USB 0 / data[1] / MIO[33]\n" +
            "NET \"MIO[32]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"C7\"; # USB 0 / data[0] / MIO[32]\n" +
            "NET \"MIO[31]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"F9\"; # USB 0 / nxt / MIO[31]\n" +
            "NET \"MIO[30]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"A11\"; # USB 0 / stp / MIO[30]\n" +
            "NET \"MIO[29]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"E8\"; # USB 0 / dir / MIO[29]\n" +
            "NET \"MIO[28]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"A12\"; # USB 0 / data[4] / MIO[28]\n" +
            "NET \"MIO[27]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"D7\"; # Enet 0 / rx_ctl / MIO[27]\n" +
            "NET \"MIO[26]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"A13\"; # Enet 0 / rxd[3] / MIO[26]\n" +
            "NET \"MIO[25]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"F12\"; # Enet 0 / rxd[2] / MIO[25]\n" +
            "NET \"MIO[24]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"B7\"; # Enet 0 / rxd[1] / MIO[24]\n" +
            "NET \"MIO[23]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"E11\"; # Enet 0 / rxd[0] / MIO[23]\n" +
            "NET \"MIO[22]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"A14\"; # Enet 0 / rx_clk / MIO[22]\n" +
            "NET \"MIO[21]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"F11\"; # Enet 0 / tx_ctl / MIO[21]\n" +
            "NET \"MIO[20]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A8\"; # Enet 0 / txd[3] / MIO[20]\n" +
            "NET \"MIO[19]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC = \"E10\"; # Enet 0 / txd[2] / MIO[19]\n" +
            "NET \"MIO[18]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A7\"; # Enet 0 / txd[1] / MIO[18]\n" +
            "NET \"MIO[17]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"E9\"; # Enet 0 / txd[0] / MIO[17]\n" +
            "NET \"MIO[16]\" IOSTANDARD = LVCMOS18 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"D6\"; # Enet 0 / tx_clk / MIO[16]\n" +
            "NET \"MIO[15]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"E6\"; # GPIO / gpio[15] / MIO[15]\n" +
            "NET \"MIO[14]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"B6\"; # GPIO / gpio[14] / MIO[14]\n" +
            "NET \"MIO[13]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"A6\"; # GPIO / gpio[13] / MIO[13]\n" +
            "NET \"MIO[12]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"C5\"; # GPIO / gpio[12] / MIO[12]\n" +
            "NET \"MIO[11]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"B4\"; # GPIO / gpio[11] / MIO[11]\n" +
            "NET \"MIO[10]\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"G7\"; # GPIO / gpio[10] / MIO[10]\n" +
            "NET \"MIO[9]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"C4\"; # GPIO / gpio[9] / MIO[9]\n" +
            "NET \"MIO[8]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"E5\"; # Quad SPI Flash / qspi_fbclk / MIO[8]\n" +
            "NET \"MIO[7]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"D5\"; # GPIO / gpio[7] / MIO[7]\n" +
            "NET \"MIO[6]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A4\"; # Quad SPI Flash / qspi0_sclk / MIO[6]\n" +
            "NET \"MIO[5]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A3\"; # Quad SPI Flash / qspi0_io[3] / MIO[5]\n" +
            "NET \"MIO[4]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"E4\"; # Quad SPI Flash / qspi0_io[2] / MIO[4]\n" +
            "NET \"MIO[3]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"F6\"; # Quad SPI Flash / qspi0_io[1] / MIO[3]\n" +
            "NET \"MIO[2]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A2\"; # Quad SPI Flash / qspi0_io[0] / MIO[2]\n" +
            "NET \"MIO[1]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"fast\" | LOC =  \"A1\"; # Quad SPI Flash / qspi0_ss_b / MIO[1]\n" +
            "NET \"MIO[0]\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC =  \"G6\"; # GPIO / gpio[0] / MIO[0]\n" +
            "\n" +
            "NET \"DDR_VRP\"   IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"N7\";\n" +
            "NET \"DDR_VRN\"   IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"M7\";\n" +
            "NET \"DDR_WEB\"   IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"R4\";\n" + // moved this down two lines
            "NET \"DDR_RAS_n\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"R5\";\n" +
            "NET \"DDR_ODT\"   IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"P5\";\n" +
            "NET \"DDR_DRSTB\" IOSTANDARD = SSTL15 | SLEW = \"FAST\" | LOC = \"F3\";\n" +
            "\n" +
            "NET \"DDR_DQS[3]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"V2\";\n" +
            "NET \"DDR_DQS[2]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"N2\";\n" +
            "NET \"DDR_DQS[1]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"H2\";\n" +
            "NET \"DDR_DQS[0]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"C2\";\n" +
            "\n" +
            "NET \"DDR_DQS_n[3]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"W2\";\n" +
            "NET \"DDR_DQS_n[2]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"P2\";\n" +
            "NET \"DDR_DQS_n[1]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"J2\";\n" +
            "NET \"DDR_DQS_n[0]\" IOSTANDARD = DIFF_SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"D2\";\n" +
            "\n" +
            "NET \"DDR_DQ[31]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"Y1\";\n" +
            "NET \"DDR_DQ[30]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"W3\";\n" +
            "NET \"DDR_DQ[29]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"Y3\";\n" +
            "NET \"DDR_DQ[28]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"W1\";\n" +
            "NET \"DDR_DQ[27]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"U2\";\n" +
            "NET \"DDR_DQ[26]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"AA1\";\n" +
            "NET \"DDR_DQ[25]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"U1\";\n" +
            "NET \"DDR_DQ[24]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"AA3\";\n" +
            "NET \"DDR_DQ[23]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"R1\";\n" +
            "NET \"DDR_DQ[22]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"M2\";\n" +
            "NET \"DDR_DQ[21]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"T2\";\n" +
            "NET \"DDR_DQ[20]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"R3\";\n" +
            "NET \"DDR_DQ[19]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"T1\";\n" +
            "NET \"DDR_DQ[18]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"N3\";\n" +
            "NET \"DDR_DQ[17]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"T3\";\n" +
            "NET \"DDR_DQ[16]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"M1\";\n" +
            "NET \"DDR_DQ[15]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"K3\";\n" +
            "NET \"DDR_DQ[14]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"J1\";\n" +
            "NET \"DDR_DQ[13]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"K1\";\n" +
            "NET \"DDR_DQ[12]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"L3\";\n" +
            "NET \"DDR_DQ[11]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"L2\";\n" +
            "NET \"DDR_DQ[10]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"L1\";\n" +
            "NET \"DDR_DQ[9]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"G1\";\n" +
            "NET \"DDR_DQ[8]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"G2\";\n" +
            "NET \"DDR_DQ[7]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"F1\";\n" +
            "NET \"DDR_DQ[6]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"F2\";\n" +
            "NET \"DDR_DQ[5]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"E1\";\n" +
            "NET \"DDR_DQ[4]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"E3\";\n" +
            "NET \"DDR_DQ[3]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"D3\";\n" +
            "NET \"DDR_DQ[2]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"B2\";\n" +
            "NET \"DDR_DQ[1]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"C3\";\n" +
            "NET \"DDR_DQ[0]\"  IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"D1\";\n" +
            "\n" +
            "NET \"DDR_DM[3]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC = \"AA2\";\n" +
            "NET \"DDR_DM[2]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"P1\";\n" +
            "NET \"DDR_DM[1]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"H3\";\n" +
            "NET \"DDR_DM[0]\" IOSTANDARD = SSTL15_T_DCI | SLEW = \"FAST\" | LOC =  \"B1\";\n" +
            "\n" +
            "NET \"DDR_CS_n\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"P6\";\n" +
            "NET \"DDR_CKE\"   IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"V3\";\n" +
            "NET \"DDR_CAS_n\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"P3\";\n" + //moved this up two lines
            "NET \"DDR_Clk\"   IOSTANDARD = DIFF_SSTL15 | SLEW = \"FAST\" | LOC = \"N4\";\n" +
            "NET \"DDR_Clk_n\" IOSTANDARD = DIFF_SSTL15 | SLEW = \"FAST\" | LOC = \"N5\";\n" +
            "\n" +
            "NET \"DDR_BankAddr[2]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"M6\";\n" +
            "NET \"DDR_BankAddr[1]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"L6\";\n" +
            "NET \"DDR_BankAddr[0]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"L7\";\n" +
            "\n " +
            "NET \"DDR_Addr[14]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"G4\";\n" +
            "NET \"DDR_Addr[13]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"F4\";\n" +
            "NET \"DDR_Addr[12]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"H4\";\n" +
            "NET \"DDR_Addr[11]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"G5\";\n" +
            "NET \"DDR_Addr[10]\" IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"J3\";\n" +
            "NET \"DDR_Addr[9]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"H5\";\n" +
            "NET \"DDR_Addr[8]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"J5\";\n" +
            "NET \"DDR_Addr[7]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"J6\";\n" +
            "NET \"DDR_Addr[6]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"J7\";\n" +
            "NET \"DDR_Addr[5]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"K5\";\n" +
            "NET \"DDR_Addr[4]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"K6\";\n" +
            "NET \"DDR_Addr[3]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"L4\";\n" +
            "NET \"DDR_Addr[2]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"K4\";\n" +
            "NET \"DDR_Addr[1]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"M5\";\n" +
            "NET \"DDR_Addr[0]\"  IOSTANDARD = SSTL15 | SLEW = \"SLOW\" | LOC = \"M4\";\n" +
            "\n" +
            "NET \"PS_PORB\"  IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"B5\";\n" +
            "NET \"PS_SRSTB\" IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"C9\";\n" +
            "NET \"PS_CLK\"   IOSTANDARD = LVCMOS33 | DRIVE = \"8\" | SLEW = \"slow\" | LOC = \"F7\";";
    }

    private String getPS7XML() {
        return
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<!DOCTYPE project PUBLIC \"project\" \"project.dtd\" >\n" +
            "<project version=\"1.0\" >\n" +
            "  <set param=\"PCW::SD0::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::UART1::UART1::IO\" value=\"MIO 48 .. 49\" />\n" +
            "  <set param=\"PCW::UART1::GRP_FULL::ENABLE\" value=\"0\" />\n" +
            "  <set param=\"PCW::GPIO::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::ENET0::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::QSPI::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::UART1::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::I2C0::PERIPHERAL::ENABLE\" value=\"0\" />\n" +
            "  <set param=\"PCW::PRESET::GLOBAL::CONFIG\" value=\"Default\" />\n" +
            "  <set param=\"PCW::TTC0::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::TTC0::TTC0::IO\" value=\"EMIO\" />\n" +
            "  <set param=\"PCW::ENET0::GRP_MDIO::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::ENET0::ENET0::IO\" value=\"MIO 16 .. 27\" />\n" +
            "  <set param=\"PCW::ENET0::GRP_MDIO::IO\" value=\"MIO 52 .. 53\" />\n" +
            "  <set param=\"PCW::USB0::PERIPHERAL::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::USB0::USB0::IO\" value=\"MIO 28 .. 39\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DRAM_WIDTH\" value=\"16 Bits\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::MEMORY_TYPE\" value=\"DDR 3\" />\n" +
            "  <set param=\"PCW::SD0::GRP_CD::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::SD0::GRP_WP::ENABLE\" value=\"1\" />\n" +
            "  <set param=\"PCW::SD0::GRP_POW::ENABLE\" value=\"0\" />\n" +
            "  <set param=\"PCW::SD0::GRP_CD::IO\" value=\"MIO 47\" />\n" +
            "  <set param=\"PCW::SD0::GRP_WP::IO\" value=\"MIO 46\" />\n" +
            "  <set param=\"PCW::QSPI::QSPI::IO\" value=\"MIO 1 .. 6\" />\n" +
            "  <set param=\"PCW::MIO::MIO[2]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[2]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[2]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[3]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[3]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[3]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[4]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[4]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[4]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[5]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[5]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[5]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[6]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[6]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[6]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[7]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[7]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[7]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[8]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[8]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[8]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[9]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[9]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[9]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[10]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[10]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[10]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[11]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[11]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[11]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[12]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[12]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[12]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[13]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[13]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[13]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[14]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[14]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[14]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[15]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[15]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[15]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[16]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[16]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[16]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[17]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[17]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[17]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[18]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[18]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[18]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[19]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[19]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[19]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[20]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[20]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[20]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::PJTAG::PERIPHERAL::ENABLE\" value=\"0\" />\n" +
            "  <set param=\"PCW::MIO::MIO[0]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[1]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[1]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[1]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[0]::SLEW\" value=\"slow\" />\n" +
            "  <set param=\"PCW::MIO::MIO[0]::IOTYPE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::PARTNO\" value=\"MT41J128M16 HA-15E\" />\n" +
            "  <set param=\"PCW::APU::PERIPHERAL::FREQMHZ\" value=\"666.666667\" />\n" +
            "  <set param=\"PCW::FPGA0::PERIPHERAL::FREQMHZ\" value=\"100.000000\" />\n" +
            "  <set param=\"PCW::FPGA1::PERIPHERAL::FREQMHZ\" value=\"150.000000\" />\n" +
            "  <set param=\"PCW::FPGA2::PERIPHERAL::FREQMHZ\" value=\"50.000000\" />\n" +
            "  <set param=\"PCW::FPGA3::PERIPHERAL::FREQMHZ\" value=\"50.000000\" />\n" +
            "  <set param=\"PCW::QSPI::PERIPHERAL::FREQMHZ\" value=\"200.000000\" />\n" +
            "  <set param=\"PCW::ENET0::PERIPHERAL::FREQMHZ\" value=\"1000 Mbps\" />\n" +
            "  <set param=\"PCW::SDIO::PERIPHERAL::FREQMHZ\" value=\"50\" />\n" +
            "  <set param=\"PCW::UART::PERIPHERAL::FREQMHZ\" value=\"50\" />\n" +
            "  <set param=\"PCW::CAN::PERIPHERAL::FREQMHZ\" value=\"100\" />\n" +
            "  <set param=\"PCW::PRESET::FPGA::PARTNUMBER\" value=\"xc7z020clg484-1\" />\n" +
            "  <set param=\"PCW::PRESET::FPGA::SPEED\" value=\"-1\" />\n" +
            "  <set param=\"PCW::PRESET::BANK0::VOLTAGE\" value=\"LVCMOS 3.3V\" />\n" +
            "  <set param=\"PCW::PRESET::BANK1::VOLTAGE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::PRESET::GLOBAL::DEFAULT\" value=\"powerup\" />\n" +
            "  <set param=\"PCW::GPIO::GPIO::IO\" value=\"MIO\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::BL\" value=\"8\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DEVICE_CAPACITY\" value=\"2048 MBits\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::SPEED_BIN\" value=\"DDR3_1066F\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::FREQ_MHZ\" value=\"533.333313\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::ROW_ADDR_COUNT\" value=\"14\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::CL\" value=\"7\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::CWL\" value=\"6\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::T_RCD\" value=\"7\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::T_RP\" value=\"7\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::T_RC\" value=\"49.5\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::T_RAS_MIN\" value=\"36.0\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::T_FAW\" value=\"45.0\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DQS_TO_CLK_DELAY_0\" value=\"0.025\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DQS_TO_CLK_DELAY_1\" value=\"0.028\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DQS_TO_CLK_DELAY_2\" value=\"-0.009\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::DQS_TO_CLK_DELAY_3\" value=\"-0.061\" />\n" +
            "  <set param=\"PCW::MIO::MIO[21]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[21]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[21]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[22]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[22]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[22]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[23]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[23]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[23]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[24]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[24]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[24]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[25]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[25]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[25]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[26]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[26]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[26]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[27]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[27]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[27]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[28]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[28]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[28]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[29]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[29]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[29]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[30]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[30]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[30]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[31]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[31]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[31]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[32]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[32]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[32]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[33]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[33]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[33]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[34]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[34]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[34]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[35]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[35]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[35]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[36]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[36]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[36]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[37]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[37]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[37]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[38]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[38]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[38]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[39]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[39]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[39]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[40]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[40]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[40]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[41]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[41]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[41]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[42]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[42]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[42]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[43]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[43]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[43]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[44]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[44]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[44]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[45]::SLEW\" value=\"fast\" />\n" +
            "  <set param=\"PCW::MIO::MIO[45]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[45]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[46]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[46]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[47]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[47]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[48]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[48]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[49]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[49]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[50]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[50]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[50]::DIRECTION\" value=\"in\" />\n" +
            "  <set param=\"PCW::MIO::MIO[51]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[51]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[51]::DIRECTION\" value=\"in\" />\n" +
            "  <set param=\"PCW::MIO::MIO[52]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[52]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::MIO::MIO[53]::IOTYPE\" value=\"LVCMOS 1.8V\" />\n" +
            "  <set param=\"PCW::MIO::MIO[53]::PULLUP\" value=\"disabled\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::TRAIN_WRITE_LEVEL\" value=\"1\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::TRAIN_READ_GATE\" value=\"1\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::TRAIN_DATA_EYE\" value=\"1\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::USE_INTERNAL_VREF\" value=\"1\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::BOARD_DELAY0\" value=\"0.41\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::BOARD_DELAY1\" value=\"0.411\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::BOARD_DELAY2\" value=\"0.341\" />\n" +
            "  <set param=\"PCW::UIPARAM::DDR::BOARD_DELAY3\" value=\"0.358\" />\n" +
            "</project>\n";
    }
}
