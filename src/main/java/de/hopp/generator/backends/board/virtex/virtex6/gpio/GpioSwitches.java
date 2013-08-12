package de.hopp.generator.backends.board.virtex.virtex6.gpio;

import static de.hopp.generator.parser.MHS.*;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.parser.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioSwitches extends GpioComponent {

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
    @Override
    public String hwInstance() {
        return "DIP_Switches_8Bits";
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        return MHSFile(Attributes(
            Attribute(PORT(),
                Assignment("DIP_Switches_8Bits_TRI_I", Ident("DIP_Switches_8Bits_TRI_I")),
                Assignment("DIR", Ident("I")),
                Assignment("VEC", Range(width()-1,0))
            )), Block("axi_gpio",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(hwInstance().toLowerCase()))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio_switches))),
                Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(width()))),
                Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
                Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
                Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x40040000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4004ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("GPIO_IO_I", Ident("DIP_Switches_8Bits_TRI_I"))),
                Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(getINTCPort())))
            ));
    }

    @Override
    public String getUCFConstraints() {
        return "\nNET DIP_Switches_8Bits_TRI_I[0] LOC = \"D22\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[1] LOC = \"C22\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[2] LOC = \"L21\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[3] LOC = \"L20\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[4] LOC = \"C18\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[5] LOC = \"B18\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[6] LOC = \"K22\"  |  IOSTANDARD = \"LVCMOS15\";" +
               "\nNET DIP_Switches_8Bits_TRI_I[7] LOC = \"K21\"  |  IOSTANDARD = \"LVCMOS15\";\n";
    }

    @Override
    public String deviceIntrChannel() {
        return "XPAR_MICROBLAZE_0_INTC_DIP_SWITCHES_8BITS_IP2INTC_IRPT_INTR";
    }

}
