package de.hopp.generator.backends.server.virtex6.ise.xps;

import static de.hopp.generator.parser.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getClockPort;
import static de.hopp.generator.utils.BoardUtils.getCore;
import static de.hopp.generator.utils.BoardUtils.getResetPort;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.server.virtex6.ise.ISEUtils;
import de.hopp.generator.backends.server.virtex6.ise.gpio.GpioEnum;
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
public class XPS_14_1 extends XPS {

    private final File coresDir;

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
    public XPS_14_1(Configuration config, ErrorCollection errors) {
        coresDir = new File(ISEUtils.edkDir(config), "pcores");

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

    @Override
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

    @Override
    public void visit(ImportsPos term)  { }
    @Override
    public void visit(BackendsPos term) { }
    @Override
    public void visit(OptionsPos term)  { }

    @Override
    public void visit(CoresPos term)     { for(CorePos core : term) visit(core); }
    @Override
    public void visit(GPIOsPos term)     { for(GPIOPos gpio : term) visit(gpio); }
    @Override
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }

    @Override
    public void visit(SchedulerPos term) { }

    @Override
    public void visit(NONEPos term) { }

    @Override
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

    @Override
    public void visit(UARTPos term) {

    }

    @Override
    public void visit(PCIEPos term) {
        // TODO Auto-generated method stub
    }

    @Override
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
        ucf += gpio.getUCFConstraints();
    }

    @Override
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

    @Override
    public void visit(BindingsPos term) { for(BindingPos b : term) visit(b); }

    @Override
    public void visit(AxisPos term) {
        curBlock = add(curBlock, Attribute(BUS_IF(), Assignment(term.port().term(), Ident(term.axis().term()))));
    }

    @Override
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
    @Override
    public void visit(ImportPos term)  { }
    @Override
    public void visit(BackendPos term) { }

    @Override
    public void visit(CorePos term)  {
        String pao = new String();

//      pao += "\n##############################################################################";
//      pao += "\n## Filename:          /net/user/r1/unix/fischer/rngled/xps/pcores/AxiTwoRNG_v1_00_a/data/AxiTwoRNG_v2_1_0.pao";
//      pao += "\n## Description:       Peripheral Analysis Order";
//      pao += "\n## Date:              Wed Nov 28 14:51:19 2012 (by Create and Import Peripheral Wizard)";
//      pao += "\n##############################################################################";

        // we need to get the name of the source, WITHOUT extension...

        // the name of the core, constructed by replacing . with _
        String name = term.name().term();
        String fullName = name + "_v" + term.version().term().replace('.', '_');

        // create folders for the core
        File coreDir     = new File(coresDir, fullName);
        File coreSrcDir  = new File(new File(coreDir, "hdl"), "vhdl");
        File coreDataDir = new File(coreDir, "data");

        // put all sources in there
        for(Import imp : term.source().term()) {
            // strip the path of the filename
            String src = FilenameUtils.getName(imp.file());

            // add the file to the .pao
            pao += "\nlib " + fullName + " " + FilenameUtils.removeExtension(src) + " vhdl";

            // add the file to the file list
            srcFiles.put(new File(coreSrcDir, src), imp.file());
        }

        // add the .pao file to the pao list
        paoFiles.put(new File(coreDataDir, name + "_v2_1_0" + ".pao"), pao);

        // add the .mpd file to the mpd list
        try {
            mpdFiles.put(new File(coreDataDir, name + "_v2_1_0" + ".mpd"), createCoreMPD(term.term()));
        } catch(UsageError e) {
            errors.addError(e);
        }
    }

    @Override
    public void visit(PortsPos term) { }

    // options are irrelevant in this visitor (so far)
    @Override
    public void visit(HWQUEUEPos arg0)  { }
    @Override
    public void visit(SWQUEUEPos arg0)  { }
    @Override
    public void visit(BITWIDTHPos term) { }
    @Override
    public void visit(DEBUGPos term)    { }
    @Override
    public void visit(POLLPos term)     { }

    // code blocks are irrelevant in this visitor
    @Override
    public void visit(DEFAULTPos term)      { }
    @Override
    public void visit(USER_DEFINEDPos term) { }

    // positions are handled directly when they occur
    @Override
    public void visit(PositionPos term) { }
    @Override
    public void visit(INPos term)       { }
    @Override
    public void visit(OUTPos term)      { }
    @Override
    public void visit(DUALPos term)     { }

    // Ethernet options are irrelevant in this visitor
    @Override
    public void visit(MOptionsPos term) { }
    @Override
    public void visit(MACPos term)      { }
    @Override
    public void visit(IPPos term)       { }
    @Override
    public void visit(MASKPos term)     { }
    @Override
    public void visit(GATEPos term)     { }
    @Override
    public void visit(PORTIDPos term)   { }
    @Override
    public void visit(AXIPos term)      { }
    @Override
    public void visit(CLKPos term)      { }
    @Override
    public void visit(RSTPos term)      { }

    // literals
    @Override
    public void visit(IntegerPos term) { }
    @Override
    public void visit(BooleanPos term) { }
    @Override
    public void visit(StringsPos term) { }
    @Override
    public void visit(StringPos term)  { }
}
