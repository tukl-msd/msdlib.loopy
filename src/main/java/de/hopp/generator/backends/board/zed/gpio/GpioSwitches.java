package de.hopp.generator.backends.board.zed.gpio;

import static de.hopp.generator.parser.MHS.*;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Block;

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

    private final String unsupportedError = "LEDs for Zed Board not fully supported yet";

    // ISE
    public Attribute getMHSAttribute() {
        return Attribute(PORT(),
            Assignment("SWs_8Bits_TRI_IO", Ident("SWs_8Bits_TRI_IO")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(width()-1,0))
        );
    }

    public String hwInstance() {
        return "SWs_8Bits";
    }

    public String getINTCPort() {
        return "SWs_8Bits_IP2INTC_Irpt";
    }

    public Block getMHSBlock(IPCoreVersions versions) {
        return Block("axi_gpio",
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
        );
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

//        return "\nNET DIP_Switches_8Bits_TRI_I[0] LOC = \"D22\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[1] LOC = \"C22\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[2] LOC = \"L21\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[3] LOC = \"L20\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[4] LOC = \"C18\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[5] LOC = \"B18\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[6] LOC = \"K22\"  |  IOSTANDARD = \"LVCMOS15\";" +
//               "\nNET DIP_Switches_8Bits_TRI_I[7] LOC = \"K21\"  |  IOSTANDARD = \"LVCMOS15\";\n";
    }

    // SDK
    public String deviceID() {
        throw new UnsupportedOperationException(unsupportedError);
//        return "XPAR_DIP_SWITCHES_8BITS_DEVICE_ID";
    }

    public String deviceIntrChannel() {
        throw new UnsupportedOperationException(unsupportedError);
//        return "XPAR_MICROBLAZE_0_INTC_DIP_SWITCHES_8BITS_IP2INTC_IRPT_INTR";
    }

}
