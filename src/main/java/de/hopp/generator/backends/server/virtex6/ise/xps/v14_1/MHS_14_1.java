package de.hopp.generator.backends.server.virtex6.ise.xps.v14_1;

import static de.hopp.generator.backends.server.virtex6.ise.xps.MHSUtils.add;
import static de.hopp.generator.parser.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getClockPort;
import static de.hopp.generator.utils.BoardUtils.getCore;
import static de.hopp.generator.utils.BoardUtils.getResetPort;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.server.virtex6.ise.gpio.GpioEnum;
import de.hopp.generator.backends.server.virtex6.ise.xps.MHSGenerator;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.frontend.*;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class MHS_14_1 extends MHSGenerator {

    // temporary variables used to build up the mhs file
    protected Block curBlock;
    protected int globalHWQueueSize;
    protected int globalSWQueueSize;

    /**
     * Creates an XPS 14.1 project backend for a Virtex 6 board.
     *
     * Initialises version strings of components from the Xilinx catalogue.
     * @param config Configuration for this backend and the related generator run.
     * @param errors ErrorCollection for this backend and the related generator run.
     */
    public MHS_14_1(Configuration config, ErrorCollection errors) {
        this.errors = errors;

        // version strings
        version                   = "2.1.0";

        version_microblaze        = "8.30.a";

        version_proc_sys_reset    = "3.00.a";
        version_axi_intc          = "1.02.a";
        version_lmb_v10           = "2.00.b";
        version_lmb_bram_if_cntlr = "3.00.b";
        version_bram_block        = "1.00.a";
        version_mdm               = "2.00.b";
        version_clock_generator   = "4.03.a";
        version_axi_timer         = "1.03.a";
        version_axi_interconnect  = "1.06.a";
        version_axi_v6_ddrx       = "1.05.a";

        version_axi_uartlite      = "1.02.a";
        version_axi_ethernetlite  = "1.01.b";

        version_gpio_leds         = "1.01.b";
        version_gpio_buttons      = "1.01.b";
        version_gpio_switches     = "1.01.b";
    }

    public void visit(BDLFilePos term) {

        for(OptionPos opt : term.opts())
            if(opt instanceof HWQUEUEPos)
                globalHWQueueSize = ((HWQUEUEPos)opt).qsize().term();
            else if(opt instanceof SWQUEUEPos)
                globalSWQueueSize = ((SWQUEUEPos)opt).qsize().term();

        addDefaultParameters();

        // visit boards components
        visit(term.cores());
        visit(term.insts());
        visit(term.gpios());
        visit(term.medium());

        // TODO independent interrupt controller?
        // add the default stuff...
        addDefaultBlocks();
        // addINTC();
        addTimer(term);
        addMicroblaze();
    }

    public void visit(ImportsPos term)  { }
    public void visit(BackendsPos term) { }
    public void visit(OptionsPos term)  { }

    public void visit(CoresPos term)     { for(CorePos core : term) visit(core); }
    public void visit(GPIOsPos term)     { for(GPIOPos gpio : term) visit(gpio); }
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }

    public void visit(SchedulerPos term) { }

    public void visit(NONEPos term) { }

    public void visit(ETHERNETPos term) {
        Attributes attr = Attributes(
          Attribute(PORT(),
            Assignment("Ethernet_Lite_TX_EN", Ident("Ethernet_Lite_TX_EN")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_TX_CLK", Ident("Ethernet_Lite_TX_CLK")),
            Assignment("DIR", Ident("I"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_TXD", Ident("Ethernet_Lite_TXD")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(3, 0))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_RX_ER", Ident("Ethernet_Lite_RX_ER")),
            Assignment("DIR", Ident("I"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_RX_DV", Ident("Ethernet_Lite_RX_DV")),
            Assignment("DIR", Ident("I"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_RX_CLK", Ident("Ethernet_Lite_RX_CLK")),
            Assignment("DIR", Ident("I"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_RXD", Ident("Ethernet_Lite_RXD")),
            Assignment("DIR", Ident("I")),
            Assignment("VEC", Range(3, 0))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_PHY_RST_N", Ident("Ethernet_Lite_PHY_RST_N")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_MDIO", Ident("Ethernet_Lite_MDIO")),
            Assignment("DIR", Ident("IO"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_MDC", Ident("Ethernet_Lite_MDC")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_CRS", Ident("Ethernet_Lite_CRS")),
            Assignment("DIR", Ident("I"))
          ), Attribute(PORT(),
            Assignment("Ethernet_Lite_COL", Ident("Ethernet_Lite_COL")),
            Assignment("DIR", Ident("I"))
          )
        );

        mhs = add(mhs, attr);

        mhs = add(mhs, Block("axi_ethernetlite",
            Attribute(PARAMETER(), Assignment("INSTANCE",     Ident("Ethernet_Lite"))),
            Attribute(PARAMETER(), Assignment("HW_VER",       Ident(version_axi_ethernetlite))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR",   MemAddr("0x40e00000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR",   MemAddr("0x40e0ffff"))),
            Attribute(BUS_IF(),    Assignment("S_AXI",        Ident("axi4lite_0"))),
            Attribute(PORT(),      Assignment("S_AXI_ACLK",   Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(),      Assignment("PHY_tx_en",    Ident("Ethernet_Lite_TX_EN"))),
            Attribute(PORT(),      Assignment("PHY_tx_clk",   Ident("Ethernet_Lite_TX_CLK"))),
            Attribute(PORT(),      Assignment("PHY_tx_data",  Ident("Ethernet_Lite_TXD"))),
            Attribute(PORT(),      Assignment("PHY_rx_er",    Ident("Ethernet_Lite_RX_ER"))),
            Attribute(PORT(),      Assignment("PHY_dv",       Ident("Ethernet_Lite_RX_DV"))),
            Attribute(PORT(),      Assignment("PHY_rx_clk",   Ident("Ethernet_Lite_RX_CLK"))),
            Attribute(PORT(),      Assignment("PHY_rx_data",  Ident("Ethernet_Lite_RXD"))),
            Attribute(PORT(),      Assignment("PHY_rst_n",    Ident("Ethernet_Lite_PHY_RST_N"))),
            Attribute(PORT(),      Assignment("PHY_MDIO",     Ident("Ethernet_Lite_MDIO"))),
            Attribute(PORT(),      Assignment("PHY_MDC",      Ident("Ethernet_Lite_MDC"))),
            Attribute(PORT(),      Assignment("PHY_crs",      Ident("Ethernet_Lite_CRS"))),
            Attribute(PORT(),      Assignment("PHY_col",      Ident("Ethernet_Lite_COL"))),
            Attribute(PORT(),      Assignment("IP2INTC_Irpt", Ident("Ethernet_Lite_IP2INTC_Irpt")))
        ));

        intrCntrlPorts = intrCntrlPorts.add(Ident("Ethernet_Lite_IP2INTC_Irpt"));

        // add constraints to .ucf file
    }

    public void visit(UARTPos term) {

    }

    public void visit(PCIEPos term) {
        // TODO Auto-generated method stub
    }

    public void visit(GPIOPos term) {
        GpioEnum gpio;

        try {
            gpio = GpioEnum.fromString(term.name().term());
        } catch (IllegalArgumentException e) {
            errors.addError(new ParserError(e.getMessage(), term.pos().term()));
            return;
        }

        mhs = add(mhs, gpio.getMHSAttribute());
        mhs = add(mhs, gpio.getMHSBlock(version_gpio_leds));

        addPortToInterruptController(gpio.getINTCPort());
    }

    public void visit(InstancePos term) {
        // begin a new instance using the instances name
        curBlock = Block(term.core().term());

        // reference core and version
        curBlock = add(curBlock, Attribute(PARAMETER(), Assignment("INSTANCE", Ident(term.name().term()))));
        curBlock = add(curBlock, Attribute(PARAMETER(), Assignment("HW_VER",   Ident(term.version().term()))));

        // define bus interfaces
        visit(term.bind());

        // append clock and reset ports
        CLK clk = getClockPort(getCore(term));
        curBlock = add(curBlock, Attribute(PORT(), Assignment(
                clk.name(), Ident("clk_" + clk.frequency() + "_0000MHzMMCM0"))));
        RST rst = getResetPort(getCore(term));
        curBlock = add(curBlock, Attribute(PORT(), Assignment(
                rst.name(), Ident("proc_sys_reset_0_Peripheral_" + (rst.polarity() ? "reset": "aresetn")))));

        // add the block to the file
        mhs = add(mhs, curBlock);
    }

    public void visit(BindingsPos term) { for(BindingPos b : term) visit(b); }

    public void visit(AxisPos term) {
        curBlock = add(curBlock, Attribute(BUS_IF(), Assignment(term.port().term(), Ident(term.axis().term()))));
    }

    public void visit(CPUAxisPos axis) {
        try {
            curBlock = add(curBlock, createCPUAxisBinding(axis));
        } catch (UsageError e) {
            errors.addError(e);
        } catch (GenerationFailed e) {
            errors.addError(e);
        }
    }

    // imports and backends should be handled before this visitor
    public void visit(ImportPos term)  { }
    public void visit(BackendPos term) { }

    // cores themselves are not represented in the mhs
    public void visit(CorePos term)  { }
    public void visit(PortsPos term) { }

    // options are irrelevant in this visitor (so far)
    public void visit(HWQUEUEPos arg0)  { }
    public void visit(SWQUEUEPos arg0)  { }
    public void visit(BITWIDTHPos term) { }
    public void visit(DEBUGPos term)    { }
    public void visit(POLLPos term)     { }

    // code blocks are irrelevant in this visitor
    public void visit(DEFAULTPos term)      { }
    public void visit(USER_DEFINEDPos term) { }

    // positions are handled directly when they occur
    public void visit(PositionPos term) { }
    public void visit(INPos term)       { }
    public void visit(OUTPos term)      { }
    public void visit(DUALPos term)     { }

    // Ethernet options are irrelevant in this visitor
    public void visit(MOptionsPos term) { }
    public void visit(MACPos term)      { }
    public void visit(IPPos term)       { }
    public void visit(MASKPos term)     { }
    public void visit(GATEPos term)     { }
    public void visit(PORTIDPos term)   { }
    public void visit(TOUTPos term)     { }
    public void visit(AXIPos term)      { }
    public void visit(CLKPos term)      { }
    public void visit(RSTPos term)      { }

    // literals
    public void visit(IntegerPos term) { }
    public void visit(BooleanPos term) { }
    public void visit(StringsPos term) { }
    public void visit(StringPos term)  { }
}
