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
public class GpioLEDs extends GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "leds";  }
    public int width()     { return 8; }
    public boolean isGPI() { return false; }
    public boolean isGPO() { return true; }

    // ISE
    @Override
    public String hwInstance() {
        return "LEDs_8Bits";
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        return MHSFile(Attributes(
            Attribute(PORT(),
                Assignment("LEDs_8Bits_TRI_O", Ident("LEDs_8Bits_TRI_O")),
                Assignment("DIR", Ident("O")),
                Assignment("VEC", Range(width()-1,0))
            )), Block("axi_gpio",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(hwInstance().toLowerCase()))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio_leds))),
                Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(width()))),
                Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(0))),
                Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
                Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x40020000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4002ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("GPIO_IO_O", Ident("LEDs_8Bits_TRI_O"))),
                Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(getINTCPort())))
            ));
    }

    @Override
    public String getUCFConstraints() {
        return "\nNET LEDs_8Bits_TRI_O[0] LOC = \"AC22\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[1] LOC = \"AC24\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[2] LOC = \"AE22\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[3] LOC = \"AE23\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[4] LOC = \"AB23\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[5] LOC = \"AG23\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[6] LOC = \"AE24\"  |  IOSTANDARD = \"LVCMOS25\";" +
               "\nNET LEDs_8Bits_TRI_O[7] LOC = \"AD24\"  |  IOSTANDARD = \"LVCMOS25\";\n";
    }

    @Override
    public String deviceIntrChannel() {
        throw new UnsupportedOperationException("GPI component 'LEDS' does not support interrupts");
        // pretty sure there IS an interrupt channel for LEDs...
    }
}
