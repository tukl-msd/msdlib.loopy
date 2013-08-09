package de.hopp.generator.backends.board.zed.gpio;

import static de.hopp.generator.parser.MHS.*;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.parser.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioSwitches implements GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "switches"; }
    public int width()     { return 8; }
    public boolean isGPI() { return true; }
    public boolean isGPO() { return false; }

    // ISE
    public String hwInstance() {
        return "SWs_8Bits";
    }

    public String getINTCPort() {
        return hwInstance() + "_IP2INTC_Irpt";
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        return MHSFile(Attributes(
            Attribute(PORT(),
                Assignment("SWs_8Bits_TRI_IO", Ident("SWs_8Bits_TRI_IO")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(width()-1,0))
            )), Block("axi_gpio",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(hwInstance()))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio_switches))),
                Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(width()))),
                Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
                Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
                Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41200000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4120ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("processing_system7_0_FCLK_CLK0"))),
                Attribute(PORT(), Assignment("GPIO_IO", Ident("SWs_8Bits_TRI_IO"))),
                Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(getINTCPort())))
            ));
    }

    public String getUCFConstraints() {
        return "\nNET SWs_8Bits_TRI_IO[0] LOC = \"F22\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[1] LOC = \"G22\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[2] LOC = \"H22\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[3] LOC = \"F21\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[4] LOC = \"H19\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[5] LOC = \"H18\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[6] LOC = \"H17\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET SWs_8Bits_TRI_IO[7] LOC = \"M15\"  |  IOSTANDARD = \"LVCMOS25\";\n";
    }

    // SDK
    public String deviceID() {
        return "XPAR_" + hwInstance().toUpperCase() + "_DEVICE_ID";
    }

    public String deviceIntrChannel() {
        return "XPAR_FABRIC_" + getINTCPort().toUpperCase() + "_INTR";
    }

}
