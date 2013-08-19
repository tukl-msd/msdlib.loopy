package de.hopp.generator.backends.board.zed.gpio;

import static de.hopp.generator.model.mhs.MHS.*;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.model.mhs.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioButtons extends GpioComponent {

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
    @Override
    public String hwInstance() {
        return "BTNs_5Bits";
    }

    @Override
    public MHSFile getMHS(IPCoreVersions versions) {
        return MHSFile(Attributes(
            Attribute(PORT(),
                Assignment("BTNs_5Bits_TRI_IO", Ident("BTNs_5Bits_TRI_IO")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(width()-1,0))
            )), Block("axi_gpio",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(hwInstance().toLowerCase()))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.gpio_buttons))),
                Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(width()))),
                Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
                Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
                Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41240000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4124ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("processing_system7_0_FCLK_CLK0"))),
                Attribute(PORT(), Assignment("GPIO_IO", Ident("BTNs_5Bits_TRI_IO"))),
                Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident(getINTCPort())))
            )
        );
    }

    @Override
    public String getUCFConstraints() {
        return  "\nNET BTNs_5Bits_TRI_IO[0] LOC = \"P16\"  |  IOSTANDARD = \"LVCMOS25\";" +
                "\nNET BTNs_5Bits_TRI_IO[1] LOC = \"R16\"  |  IOSTANDARD = \"LVCMOS25\";" +
                "\nNET BTNs_5Bits_TRI_IO[2] LOC = \"N15\"  |  IOSTANDARD = \"LVCMOS25\";" +
                "\nNET BTNs_5Bits_TRI_IO[3] LOC = \"R18\"  |  IOSTANDARD = \"LVCMOS25\";" +
                "\nNET BTNs_5Bits_TRI_IO[4] LOC = \"T18\"  |  IOSTANDARD = \"LVCMOS25\";\n";
    }

    // SDK stuff
    @Override
    public String deviceIntrChannel() {
        return "XPAR_FABRIC_" + getINTCPort().toUpperCase() + "_INTR";
    }
}
