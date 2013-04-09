package de.hopp.generator.backends.server.virtex6;

import static de.hopp.generator.parser.MHS.*;

import java.io.File;
import java.io.IOException;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.frontend.*;
import de.hopp.generator.frontend.BDLFilePos.Visitor;
import de.hopp.generator.parser.AndExp;
import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.Blocks;
import de.hopp.generator.parser.MHSFile;

import static de.hopp.generator.backends.BackendUtils.*;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class XPS_14_1 extends Visitor<NE> implements ProjectBackend {

    MHSFile mhs;
    
    AndExp intrCntrlPorts;
    Block curBlock;
    
    ErrorCollection errors;
    Configuration config;
    
    private int axiStreamIdMaster;
    private int axiStreamIdSlave;
    
    public String getName() {
        return "xps14.1";
    }
    
    @Override
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        this.config = config;
        this.errors = errors;
        
        axiStreamIdMaster = 0;
        axiStreamIdSlave  = 0;
        
        MHSFile mhs = MHSFile(Attributes());
        
        visit(board);

        StringBuffer buffer = new StringBuffer();
        MHSUnparser unparser = new MHSUnparser(buffer);
        unparser.visit(mhs);
        
        try {
            File file = new File(new File(config.serverDir(), "xps"), "system.mhs");
            file.mkdirs();
            printBuffer(buffer, file);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
    }

    @Override
    public void visit(BDLFilePos term) {
        // visit boards components
        visit(term.insts());
        visit(term.gpios());
        visit(term.medium());
        
        // add the default stuff...
        addDefault();
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

    @Override
    public void visit(ETHERNETPos term) {
        Attributes attr = Attributes(
          PORT(
            Assignment("Ethernet_Lite_TX_EN", Ident("Ethernet_Lite_TX_EN")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("Ethernet_Lite_TX_CLK", Ident("Ethernet_Lite_TX_CLK")),
            Assignment("DIR", Ident("I"))
          ), PORT(
            Assignment("Ethernet_Lite_TXD", Ident("Ethernet_Lite_TXD")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(3, 0))
          ), PORT(
            Assignment("Ethernet_Lite_RX_ER", Ident("Ethernet_Lite_RX_ER")),
            Assignment("DIR", Ident("I"))
          ), PORT(
            Assignment("Ethernet_Lite_RX_DV", Ident("Ethernet_Lite_RX_DV")),
            Assignment("DIR", Ident("I"))
          ), PORT(
            Assignment("Ethernet_Lite_RX_CLK", Ident("Ethernet_Lite_RX_CLK")),
            Assignment("DIR", Ident("I"))
          ), PORT(
            Assignment("Ethernet_Lite_RXD", Ident("Ethernet_Lite_RXD")),
            Assignment("DIR", Ident("I")),
            Assignment("VEC", Range(3, 0))
          ), PORT(
            Assignment("Ethernet_Lite_PHY_RST_N", Ident("Ethernet_Lite_PHY_RST_N")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("Ethernet_Lite_MDIO", Ident("Ethernet_Lite_MDIO")),
            Assignment("DIR", Ident("IO"))
          ), PORT(
            Assignment("Ethernet_Lite_MDC", Ident("Ethernet_Lite_MDC")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("Ethernet_Lite_CRS", Ident("Ethernet_Lite_CRS")),
            Assignment("DIR", Ident("I"))
          ), PORT(
            Assignment("Ethernet_Lite_COL", Ident("Ethernet_Lite_COL")),
            Assignment("DIR", Ident("I"))
          )
        );
        
        mhs = add(mhs, attr);
        
        mhs = add(mhs, Block("axi_ethernetlite",
            PARAMETER(Assignment("INSTANCE", Ident("Ethernet_Lite"))),
            PARAMETER(Assignment("HW_VER", Ident("1.01.b"))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x40e00000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x40e0ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("PHY_tx_en", Ident("Ethernet_Lite_TX_EN"))),
            PORT(Assignment("PHY_tx_clk", Ident("Ethernet_Lite_TX_CLK"))),
            PORT(Assignment("PHY_tx_data", Ident("Ethernet_Lite_TXD"))),
            PORT(Assignment("PHY_rx_er", Ident("Ethernet_Lite_RX_ER"))),
            PORT(Assignment("PHY_dv", Ident("Ethernet_Lite_RX_DV"))),
            PORT(Assignment("PHY_rx_clk", Ident("Ethernet_Lite_RX_CLK"))),
            PORT(Assignment("PHY_rx_data", Ident("Ethernet_Lite_RXD"))),
            PORT(Assignment("PHY_rst_n", Ident("Ethernet_Lite_PHY_RST_N"))),
            PORT(Assignment("PHY_MDIO", Ident("Ethernet_Lite_MDIO"))),
            PORT(Assignment("PHY_MDC", Ident("Ethernet_Lite_MDC"))),
            PORT(Assignment("PHY_crs", Ident("Ethernet_Lite_CRS"))),
            PORT(Assignment("PHY_col", Ident("Ethernet_Lite_COL"))),
            PORT(Assignment("IP2INTC_Irpt", Ident("Ethernet_Lite_IP2INTC_Irpt")))
        ));
        
        intrCntrlPorts = intrCntrlPorts.add(Ident("Ethernet_Lite_IP2INTC_Irpt"));
    }

    public void visit(UARTPos term) {
        mhs = add(mhs, PORT(Assignment("RS232_Uart_1_sout", Ident("RS232_Uart_1_sout")),
                            Assignment("DIR", Ident("O"))));
        mhs = add(mhs, PORT(Assignment("RS232_Uart_1_sin", Ident("RS232_Uart_1_sin")),
                            Assignment("DIR", Ident("I"))));
        
        mhs = add(mhs, Block("axi_uartlite",
            PARAMETER(Assignment("INSTANCE", Ident("RS232_Uart_1"))),
            PARAMETER(Assignment("HW_VER", Ident("1.02.a"))),
            PARAMETER(Assignment("C_BAUDRATE", Number(9600))),
            PARAMETER(Assignment("C_DATA_BITS", Number(8))),
            PARAMETER(Assignment("C_USE_PARITY", Number(0))),
            PARAMETER(Assignment("C_ODD_PARITY", Number(1))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x40600000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x4060ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("TX", Ident("RS232_Uart_1_sout"))),
            PORT(Assignment("RX", Ident("RS232_Uart_1_sin"))),
            PORT(Assignment("Interrupt", Ident("RS232_Uart_1_Interrupt"))) 
        ));
        
        intrCntrlPorts = intrCntrlPorts.add(Ident("RS232_Uart_1_Interrupt"));
    }

    @Override
    public void visit(PCIEPos term) {
        // TODO Auto-generated method stub
    }

    public void visit(GPIOPos term) {
        switch(term.name().term()){
        case "led":
            mhs = add(mhs, PORT(Assignment("LEDs_8Bits_TRI_O", Ident("LEDs_8Bits_TRI_O")),
                                Assignment("DIR", Ident("O")),
                                Assignment("VEC", Range(7,0))));
            addPortToInterruptController("LEDs_8Bits_IP2INTC_Irpt");
            addLEDs();
            break;
        case "button":
            mhs = add(mhs, PORT(Assignment("Push_Buttons_5Bits_TRI_I", Ident("Push_Buttons_5Bits_TRI_I")),
                    Assignment("DIR", Ident("I")),
                    Assignment("VEC", Range(4,0))));
            addPortToInterruptController("Push_Buttons_5Bits_IP2INTC_Irpt");
            addButtons();
            break;
        case "switch":
            mhs = add(mhs, PORT(Assignment("DIP_Switches_8Bits_TRI_I", Ident("DIP_Switches_8Bits_TRI_I")),
                    Assignment("DIR", Ident("O")),
                    Assignment("VEC", Range(7,0))));
            addPortToInterruptController("DIP_Switches_8Bits_IP2INTC_Irpt");
            addSwitches();
            break;
        }
    }

    public void visit(InstancePos term) {
        // begin a new instance using the instances name
        curBlock = Block(term.name().term());
        
        // reference core and version
        curBlock = add(curBlock, PARAMETER(Assignment("INSTANCE", Ident(term.core().term()))));
//        instance = add(instance, Attribute(PARAMETER(), Assignment("HW_VER", Ident(term.core().version()))));
        
        // define bus interfaces
        visit(term.bind());
        
        // append default clock and reset ports
        curBlock = add(curBlock, PORT(Assignment("ACLK", Ident("clk_100_0000MHzMMCM0"))));
        curBlock = add(curBlock, PORT(Assignment("ARESETN", Ident("proc_sys_reset_0_Peripheral_aresetn"))));
        
        // add the block to the file
        mhs = add(mhs, curBlock);
    }
    
    public void visit(BindingsPos term) { for(BindingPos b : term) visit(b); }
    
    public void visit(AxisPos term) {
        curBlock = add(curBlock, BUS_INTERFACE(term.port().term(), term.axis().term()));
    }

    public void visit(final CPUAxisPos axis) {
        // get the direction of the corresponding port
        Direction direct = getPort(axis).direction().termDirection();
        
        // depending on the direction, add a different interface
        curBlock = add(curBlock, BUS_INTERFACE(axis.port().term(),
            direct.Switch(new Direction.Switch<String, NE>() {
                public String CaseIN(IN term) {
                    return "M" + axiStreamIdMaster++ + "_AXIS";
                }
                public String CaseOUT(OUT term) {
                    return "S" + axiStreamIdSlave++  + "_AXIS";
                }
                public String CaseDUAL(DUAL term) {
                    // TODO throw error, try, catch, add to errors...
                    throw new IllegalStateException();
                }
            })
        ));
    }
    
    // imports and backends should be handled before this visitor
    public void visit(ImportPos term)  { }
    public void visit(BackendPos term) { }

    // TODO create core repository!!!
    public void visit(CorePos term)  { }
    public void visit(PortsPos term) { }

    // options are irrelevant in this visitor (so far)
    // TODO inject queues and multiplexer into mhs design
    public void visit(HWQUEUEPos arg0)  { }
    public void visit(SWQUEUEPos arg0)  { }
    public void visit(BITWIDTHPos term) { }
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
    public void visit(PortPos term)     { }

    // literals
    public void visit(IntegerPos term) { }
    public void visit(StringsPos term) { }
    public void visit(StringPos term)  { }
    
    // static helper methods modifying an mhs term by adding child terms
    private static MHSFile add(MHSFile file, Attributes attr) {
        return file.replaceAttributes(file.attributes().addAll(attr));
    }
    private static MHSFile add(MHSFile file, Blocks blocks) {
        return file.replaceBlocks(file.blocks().addAll(blocks));
    }
    private static MHSFile add(MHSFile file, Attribute attr) {
        return file.replaceAttributes(file.attributes().add(attr));
    }
    private static MHSFile add(MHSFile file, Block block) {
        return file.replaceBlocks(file.blocks().add(block));
    }
    private static Block add(Block block, Attribute attr) {
        return block.replaceAttributes(block.attributes().add(attr));
    }
    
    // helper methods modifying the mhs file of this visitor
    private void addPortToInterruptController(String port) {
        intrCntrlPorts = intrCntrlPorts.add(Ident(port));
    }
    
    private void addLEDs() {
        mhs = add(mhs, Block("axi_gpio",
            PARAMETER(Assignment("INSTANCE", Ident("LEDs_8Bits"))),
            PARAMETER(Assignment("HW_VER", Ident("1.01.b"))),
            PARAMETER(Assignment("C_GPIO_WIDTH", Number(8))),
            PARAMETER(Assignment("C_ALL_INPUTS", Number(0))),
            PARAMETER(Assignment("C_INTERRUPT_PRESENT", Number(1))),
            PARAMETER(Assignment("C_IS_DUAL", Number(0))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("40020000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("4002ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("GPIO_IO_O", Ident("LEDs_8Bits_TRI_O"))),
            PORT(Assignment("IP2INTC_Irpt", Ident("LEDs_8Bits_IP2INTC_Irpt")))
        ));
    }
    
    private void addButtons() {
        mhs = add(mhs, Block("axi_gpio",
            PARAMETER(Assignment("INSTANCE", Ident("Push_Buttons_5Bits"))),
            PARAMETER(Assignment("HW_VER", Ident("1.01.b"))),
            PARAMETER(Assignment("C_GPIO_WIDTH", Number(5))),
            PARAMETER(Assignment("C_ALL_INPUTS", Number(1))),
            PARAMETER(Assignment("C_INTERRUPT_PRESENT", Number(1))),
            PARAMETER(Assignment("C_IS_DUAL", Number(0))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("40000000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("4000ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("GPIO_IO_O", Ident("Push_Buttons_5Bits_TRI_I"))),
            PORT(Assignment("IP2INTC_Irpt", Ident("Push_Buttons_5Bits_IP2INTC_Irpt")))
        ));
    }
    
    private void addSwitches() {
        mhs = add(mhs, Block("axi_gpio",
            PARAMETER(Assignment("INSTANCE", Ident("DIP_Switches_8Bits"))),
            PARAMETER(Assignment("HW_VER", Ident("1.01.b"))),
            PARAMETER(Assignment("C_GPIO_WIDTH", Number(8))),
            PARAMETER(Assignment("C_ALL_INPUTS", Number(1))),
            PARAMETER(Assignment("C_INTERRUPT_PRESENT", Number(1))),
            PARAMETER(Assignment("C_IS_DUAL", Number(0))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("40040000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("4004ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("GPIO_IO_O", Ident("DIP_Switches_8Bits_TRI_I"))),
            PORT(Assignment("IP2INTC_Irpt", Ident("DIP_Switches_8Bits_IP2INTC_Irpt")))
        ));
    }
    
    private void addDefault() {
        Attributes attr = Attributes(
          PARAMETER(Assignment("VERSION", Ident("2.1.0"))),
          PORT(
            Assignment("ddr_memory_we_n", Ident("ddr_memory_we_n")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_ras_n", Ident("ddr_memory_ras_n")), 
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_odt", Ident("ddr_memory_odt")), 
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_dqs_n", Ident("ddr_memory_dqs_n")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(0, 0))
          ), PORT(
            Assignment("ddr_memory_dqs", Ident("ddr_memory_dqs")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(0, 0))
          ), PORT(
            Assignment("ddr_memory_dq", Ident("ddr_memory_dq")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(7, 0))
          ), PORT(
            Assignment("ddr_memory_dm", Ident("ddr_memory_dm")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(0, 0))
          ), PORT(
            Assignment("ddr_memory_ddr3_rst", Ident("ddr_memory_ddr3_rst")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_cs_n", Ident("ddr_memory_cs_n")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_clk_n", Ident("ddr_memory_clk_n")),
            Assignment("DIR", Ident("O")),
            Assignment("SIGIS", Ident("CLK"))
          ), PORT(
            Assignment("ddr_memory_clk", Ident("ddr_memory_clk")),
            Assignment("DIR", Ident("O")),
            Assignment("SIGIS", Ident("CLK"))
          ), PORT(
            Assignment("ddr_memory_cke", Ident("ddr_memory_cke")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_cas_n", Ident("ddr_memory_cas_n")),
            Assignment("DIR", Ident("O"))
          ), PORT(
            Assignment("ddr_memory_ba", Ident("ddr_memory_ba")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(2, 0))
          ), PORT(
            Assignment("ddr_memory_addr", Ident("ddr_memory_addr")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(12, 0))
          ), PORT(
            Assignment("RESET", Ident("RESET")),
            Assignment("DIR", Ident("I")),
            Assignment("SIGIS", Ident("RST")),
            Assignment("RST_POLARITY", Number(1))
          ), PORT(
            Assignment("CLK_P", Ident("CLK")),
            Assignment("DIR", Ident("I")),
            Assignment("DIFFERENTIAL_POLARITY", Ident("P")),
            Assignment("SIGIS", Ident("CLK")),
            Assignment("CLK_FREQ", Number(200000000))
          ), PORT(
            Assignment("CLK_N", Ident("CLK")),
            Assignment("DIR", Ident("I")),
            Assignment("DIFFERENTIAL_POLARITY", Ident("N")),
            Assignment("SIGIS", Ident("CLK")),
            Assignment("CLK_FREQ", Number(200000000))
          )
        );
        
        Blocks blocks = Blocks(
          Block("proc_sys_reset",
            PARAMETER(Assignment("INSTANCE", Ident("proc_sys_reset_0"))), 
            PARAMETER(Assignment("HW_VER", Ident("3.00.a"))),
            PARAMETER(Assignment("C_EXT_RESET_HIGH", Number(1))),
            PORT(Assignment("MB_Debug_Sys_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))), 
            PORT(Assignment("Dcm_locked", Ident("proc_sys_reset_0_Dcm_locked"))),
            PORT(Assignment("MB_Reset", Ident("proc_sys_reset_0_MB_Reset"))),
            PORT(Assignment("Slowest_sync_clk", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("Interconnect_aresetn", Ident("proc_sys_reset_0_Interconnect_aresetn"))), 
            PORT(Assignment("Ext_Reset_In", Ident("RESET"))),
            PORT(Assignment("BUS_STRUCT_RESET", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            PORT(Assignment("Peripheral_aresetn", Ident("proc_sys_reset_0_Peripheral_aresetn")))
          ), Block("axi_intc",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_intc"))),
            PARAMETER(Assignment("HW_VER", Ident("1.02.a"))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x41200000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x4120ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            BUS_INTERFACE("INTERRUPT", "microblaze_0_interrupt"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("INTR", intrCntrlPorts.add(Ident("axi_timer_0_Interrupt"))))
          ), Block("lmb_v10",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_ilmb"))),
            PARAMETER(Assignment("HW_VER", Ident("2.00.b"))),
            PORT(Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            PORT(Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("lmb_bram_if_cntlr",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_i_bram_ctrl"))),
            PARAMETER(Assignment("HW_VER", Ident("3.00.b"))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x00000000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
            BUS_INTERFACE("SLMB", "microblaze_0_ilmb"),
            BUS_INTERFACE("BRAM_PORT", "microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block")
          ), Block("lmb_v10",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_dlmb"))),
            PARAMETER(Assignment("HW_VER", Ident("2.00.b"))),
            PORT(Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            PORT(Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0"))) 
          ), Block("lmb_bram_if_cntlr",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_d_bram_ctrl"))),
            PARAMETER(Assignment("HW_VER", Ident("3.00.b"))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x00000000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
            BUS_INTERFACE("SLMB", "microblaze_0_dlmb"),
            BUS_INTERFACE("BRAM_PORT", "microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")
          ), Block("bram_block",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0_bram_block"))),
            PARAMETER(Assignment("HW_VER", Ident("1.00.a"))),
            BUS_INTERFACE("PORTA", "microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block"),
            BUS_INTERFACE("PORTB", "microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")
          ), Block("mdm",
            PARAMETER(Assignment("INSTANCE", Ident("debug_module"))),
            PARAMETER(Assignment("HW_VER", Ident("2.00.b"))),
            PARAMETER(Assignment("C_INTERCONNECT", Number(2))),
            PARAMETER(Assignment("C_USE_UART", Number(1))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x41400000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x4140ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            BUS_INTERFACE("MBDEBUG_0", "microblaze_0_debug"),
            PORT(Assignment("Debug_SYS_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("clock_generator",
            PARAMETER(Assignment("INSTANCE", Ident("clock_generator_0"))),
            PARAMETER(Assignment("HW_VER", Ident("4.03.a"))),
            PARAMETER(Assignment("C_CLKIN_FREQ", Number(200000000))),
            PARAMETER(Assignment("C_CLKOUT0_FREQ", Number(100000000))),
            PARAMETER(Assignment("C_CLKOUT0_GROUP", Ident("MMCM0"))),
            PARAMETER(Assignment("C_CLKOUT1_FREQ", Number(200000000))),
            PARAMETER(Assignment("C_CLKOUT1_GROUP", Ident("MMCM0"))),
            PARAMETER(Assignment("C_CLKOUT2_FREQ", Number(400000000))),
            PARAMETER(Assignment("C_CLKOUT2_GROUP", Ident("MMCM0"))),
            PARAMETER(Assignment("C_CLKOUT3_FREQ", Number(400000000))),
            PARAMETER(Assignment("C_CLKOUT3_GROUP", Ident("MMCM0"))),
            PARAMETER(Assignment("C_CLKOUT3_BUF", Ident("FALSE"))),
            PARAMETER(Assignment("C_CLKOUT3_VARIABLE_PHASE", Ident("TRUE"))),
            PORT(Assignment("LOCKED", Ident("proc_sys_reset_0_Dcm_locked"))),
            PORT(Assignment("CLKOUT0", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("RST", Ident("RESET"))),
            PORT(Assignment("CLKOUT3", Ident("clk_400_0000MHzMMCM0_nobuf_varphase"))),
            PORT(Assignment("CLKOUT2", Ident("clk_400_0000MHzMMCM0"))),
            PORT(Assignment("CLKOUT1", Ident("clk_200_0000MHzMMCM0"))),
            PORT(Assignment("CLKIN", Ident("CLK"))),
            PORT(Assignment("PSCLK", Ident("clk_200_0000MHzMMCM0"))),
            PORT(Assignment("PSEN", Ident("psen"))),
            PORT(Assignment("PSINCDEC", Ident("psincdec"))),
            PORT(Assignment("PSDONE", Ident("psdone")))
          ), Block("axi_timer",
            PARAMETER(Assignment("INSTANCE", Ident("axi_timer_0"))),
            PARAMETER(Assignment("HW_VER", Ident("1.03.a"))),
            PARAMETER(Assignment("C_COUNT_WIDTH", Number(32))),
            PARAMETER(Assignment("C_ONE_TIMER_ONLY", Number(0))),
            PARAMETER(Assignment("C_BASEADDR", MemAddr("0x41c00000"))),
            PARAMETER(Assignment("C_HIGHADDR", MemAddr("0x41c0ffff"))),
            BUS_INTERFACE("S_AXI", "axi4lite_0"),
            PORT(Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("Interrupt", Ident("axi_timer_0_Interrupt")))
          ), Block("axi_interconnect",
            PARAMETER(Assignment("INSTANCE", Ident("axi4lite_0"))),
            PARAMETER(Assignment("HW_VER", Ident("1.06.a"))),
            PARAMETER(Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(0))),
            PORT(Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn"))),
            PORT(Assignment("INTERCONNECT_ACLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("axi_interconnect",
            PARAMETER(Assignment("INSTANCE", Ident("axi4_0"))),
            PARAMETER(Assignment("HW_VER", Ident("1.06.a"))),
            PORT(Assignment("interconnect_aclk", Ident("clk_100_0000MHzMMCM0"))),
            PORT(Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn")))
          ), Block("axi_v6_ddrx",
            PARAMETER(Assignment("INSTANCE", Ident("DDR3_SDRAM"))),
            PARAMETER(Assignment("HW_VER", Ident("1.05.a"))),
            PARAMETER(Assignment("C_MEM_PARTNO", Ident("MT41J64M16XX-15E"))),
            PARAMETER(Assignment("C_DM_WIDTH", Number(1))),
            PARAMETER(Assignment("C_DQS_WIDTH", Number(1))),
            PARAMETER(Assignment("C_DQ_WIDTH", Number(8))),
            PARAMETER(Assignment("C_INTERCONNECT_S_AXI_MASTERS", 
              AndExp(
                Ident("microblaze_0.M_AXI_DC"),
                Ident("microblaze_0.M_AXI_IC")
              )
            )),
            PARAMETER(Assignment("C_MMCM_EXT_LOC", Ident("MMCM_ADV_X0Y8"))),
            PARAMETER(Assignment("C_NDQS_COL0", Number(1))),
            PARAMETER(Assignment("C_NDQS_COL1", Number(0))),
            PARAMETER(Assignment("C_S_AXI_BASEADDR", MemAddr("0xa4000000"))),
            PARAMETER(Assignment("C_S_AXI_HIGHADDR", MemAddr("0xa7ffffff"))),
            BUS_INTERFACE("S_AXI", "axi4_0"),
            PORT(Assignment("ddr_we_n", Ident("ddr_memory_we_n"))),
            PORT(Assignment("ddr_ras_n", Ident("ddr_memory_ras_n"))),
            PORT(Assignment("ddr_odt", Ident("ddr_memory_odt"))),
            PORT(Assignment("ddr_dqs_n", Ident("ddr_memory_dqs_n"))),
            PORT(Assignment("ddr_dqs_p", Ident("ddr_memory_dqs"))),
            PORT(Assignment("ddr_dq", Ident("ddr_memory_dq"))),
            PORT(Assignment("ddr_dm", Ident( "ddr_memory_dm"))),
            PORT(Assignment("ddr_reset_n", Ident("ddr_memory_ddr3_rst"))),
            PORT(Assignment("ddr_cs_n", Ident("ddr_memory_cs_n"))),
            PORT(Assignment("ddr_ck_n", Ident("ddr_memory_clk_n"))),
            PORT(Assignment("ddr_ck_p", Ident("ddr_memory_clk"))),
            PORT(Assignment("ddr_cke", Ident("ddr_memory_cke"))),
            PORT(Assignment("ddr_cas_n", Ident("ddr_memory_cas_n"))),
            PORT(Assignment("ddr_ba", Ident("ddr_memory_ba"))),
            PORT(Assignment("ddr_addr", Ident("ddr_memory_addr"))),
            PORT(Assignment("clk_rd_base", Ident("clk_400_0000MHzMMCM0_nobuf_varphase"))),
            PORT(Assignment("clk_mem", Ident("clk_400_0000MHzMMCM0"))),
            PORT(Assignment("clk", Ident("clk_200_0000MHzMMCM0"))),
            PORT(Assignment("clk_ref", Ident("clk_200_0000MHzMMCM0"))),
            PORT(Assignment("PD_PSEN", Ident("psen"))),
            PORT(Assignment("PD_PSINCDEC", Ident("psincdec"))),
            PORT(Assignment("PD_PSDONE", Ident("psdone")))
          )
        );
        
        mhs = add(mhs, attr);
        mhs = add(mhs, blocks);
    }
    
    private void addMicroblaze() {
        Block microblaze = Block("microblaze",
            PARAMETER(Assignment("INSTANCE", Ident("microblaze_0"))),
            PARAMETER(Assignment("HW_VER", Ident("8.30.a"))),
            PARAMETER(Assignment("C_INTERCONNECT", Number(2))),
            PARAMETER(Assignment("C_USE_BARREL", Number(1))),
            PARAMETER(Assignment("C_USE_FPU", Number(0))),
            PARAMETER(Assignment("C_DEBUG_ENABLED", Number(1))),
            PARAMETER(Assignment("C_ICACHE_BASEADDR", MemAddr("0xa4000000"))),
            PARAMETER(Assignment("C_ICACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            PARAMETER(Assignment("C_USE_ICACHE", Number(1))),
            PARAMETER(Assignment("C_CACHE_BYTE_SIZE", Number(65536))),
            PARAMETER(Assignment("C_ICACHE_ALWAYS_USED", Number(1))),
            PARAMETER(Assignment("C_DCACHE_BASEADDR", MemAddr("0xa4000000"))),
            PARAMETER(Assignment("C_DCACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            PARAMETER(Assignment("C_USE_DCACHE", Number(1))),
            PARAMETER(Assignment("C_DCACHE_BYTE_SIZE", Number(65536))),
            PARAMETER(Assignment("C_DCACHE_ALWAYS_USED", Number(1))),
            PARAMETER(Assignment("C_FSL_LINKS", Number(1))),
            PARAMETER(Assignment("C_STREAM_INTERCONNECT", Number(1))),
            BUS_INTERFACE("M_AXI_DP", "axi4lite_0"),
            BUS_INTERFACE("M_AXI_DC", "axi4_0"),
            BUS_INTERFACE("M_AXI_IC", "axi4_0"),
            BUS_INTERFACE("DEBUG", "microblaze_0_debug"),
            BUS_INTERFACE("INTERRUPT", "microblaze_0_interrupt"),
            BUS_INTERFACE("DLMB", "microblaze_0_dlmb"),
            BUS_INTERFACE("ILMB", "microblaze_0_ilmb")
        );
        
        // add master and slave interfaces for user-attached cores
        for(int i = 0; i < axiStreamIdMaster; i++)
            microblaze = add(microblaze, BUS_INTERFACE("M" + i + "_AXIS", "microblaze_0_M" + i + "_AXIS"));
        for(int i = 0; i < axiStreamIdSlave; i++)
            microblaze = add(microblaze, BUS_INTERFACE("S" + i + "_AXIS", "microblaze_0_S" + i + "_AXIS"));
        
        // add reset and clock ports
        microblaze = add(microblaze, PORT(Assignment("MB_RESET", Ident("proc_sys_reset_0_MB_Reset"))));
        microblaze = add(microblaze, PORT(Assignment("CLK", Ident("clk_100_0000MHzMMCM0"))));
        
        mhs = add(mhs, microblaze);
    }
}
