package de.hopp.generator.backends.board.virtex.virtex6.gpio;

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
public class GpioButtons implements GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "buttons"; }
    public int width()     { return 5; }
    public boolean isGPI() { return true; }
    public boolean isGPO() { return false; }

    // ISE stuff
    public Attribute getMHSAttribute() {
        return Attribute(PORT(),
            Assignment("Push_Buttons_5Bits_TRI_I", Ident("Push_Buttons_5Bits_TRI_I")),
            Assignment("DIR", Ident("I")),
            Assignment("VEC", Range(width()-1,0))
        );
    }

    public String hwInstance() {
        return "push_buttons_5bits";
    }

    public String getINTCPort() {
        return "Push_Buttons_5Bits_IP2INTC_Irpt";
    }

    public Block getMHSBlock(IPCoreVersions versions) {
        return Block("axi_gpio",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident(hwInstance()))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio_buttons))),
            Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(width()))),
            Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
            Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
            Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x40000000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4000ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("GPIO_IO_I", Ident("Push_Buttons_5Bits_TRI_I"))),
            Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(getINTCPort())))
        );
    }

    public String getUCFConstraints() {
        return "\nNET Push_Buttons_5Bits_TRI_I[0] LOC = \"G26\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET Push_Buttons_5Bits_TRI_I[1] LOC = \"A19\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET Push_Buttons_5Bits_TRI_I[2] LOC = \"G17\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET Push_Buttons_5Bits_TRI_I[3] LOC = \"A18\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET Push_Buttons_5Bits_TRI_I[4] LOC = \"H17\"  |  IOSTANDARD = \"LVCMOS15\";\n";
    }

    public String deviceID() {
        return "XPAR_PUSH_BUTTONS_5BITS_DEVICE_ID";
    }

    public String deviceIntrChannel() {
        return "XPAR_MICROBLAZE_0_INTC_PUSH_BUTTONS_5BITS_IP2INTC_IRPT_INTR";
    }
}
