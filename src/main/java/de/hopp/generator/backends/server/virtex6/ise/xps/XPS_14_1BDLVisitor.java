package de.hopp.generator.backends.server.virtex6.ise.xps;

import static de.hopp.generator.backends.BackendUtils.getPort;
import static de.hopp.generator.parser.MHS.*;

import java.io.File;

import katja.common.NE;

import org.apache.commons.io.FilenameUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.backends.server.virtex6.ise.ISEUtils;
import de.hopp.generator.frontend.*;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;

/**
 * Generation backend for a project for Xilinx XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class XPS_14_1BDLVisitor extends XPSBDLVisitor {

    private File coresDir;
    
    // temporary variables used to build up the mhs file
    private Block curBlock;
    
    public XPS_14_1BDLVisitor(Configuration config, ErrorCollection errors) {
        this.errors = errors;
        
        coresDir = new File(ISEUtils.edkDir(config), "pcores");
        
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
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("Ethernet_Lite"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.01.b"))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x40e00000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x40e0ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("PHY_tx_en", Ident("Ethernet_Lite_TX_EN"))),
            Attribute(PORT(), Assignment("PHY_tx_clk", Ident("Ethernet_Lite_TX_CLK"))),
            Attribute(PORT(), Assignment("PHY_tx_data", Ident("Ethernet_Lite_TXD"))),
            Attribute(PORT(), Assignment("PHY_rx_er", Ident("Ethernet_Lite_RX_ER"))),
            Attribute(PORT(), Assignment("PHY_dv", Ident("Ethernet_Lite_RX_DV"))),
            Attribute(PORT(), Assignment("PHY_rx_clk", Ident("Ethernet_Lite_RX_CLK"))),
            Attribute(PORT(), Assignment("PHY_rx_data", Ident("Ethernet_Lite_RXD"))),
            Attribute(PORT(), Assignment("PHY_rst_n", Ident("Ethernet_Lite_PHY_RST_N"))),
            Attribute(PORT(), Assignment("PHY_MDIO", Ident("Ethernet_Lite_MDIO"))),
            Attribute(PORT(), Assignment("PHY_MDC", Ident("Ethernet_Lite_MDC"))),
            Attribute(PORT(), Assignment("PHY_crs", Ident("Ethernet_Lite_CRS"))),
            Attribute(PORT(), Assignment("PHY_col", Ident("Ethernet_Lite_COL"))),
            Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident("Ethernet_Lite_IP2INTC_Irpt")))
        ));
        
        intrCntrlPorts = intrCntrlPorts.add(Ident("Ethernet_Lite_IP2INTC_Irpt"));
    }

    public void visit(UARTPos term) {
        mhs = add(mhs, Attribute(PORT(), Assignment("RS232_Uart_1_sout", Ident("RS232_Uart_1_sout")),
                            Assignment("DIR", Ident("O"))));
        mhs = add(mhs, Attribute(PORT(), Assignment("RS232_Uart_1_sin", Ident("RS232_Uart_1_sin")),
                            Assignment("DIR", Ident("I"))));
        
        mhs = add(mhs, Block("axi_uartlite",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("RS232_Uart_1"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.02.a"))),
            Attribute(PARAMETER(), Assignment("C_BAUDRATE", Number(9600))),
            Attribute(PARAMETER(), Assignment("C_DATA_BITS", Number(8))),
            Attribute(PARAMETER(), Assignment("C_USE_PARITY", Number(0))),
            Attribute(PARAMETER(), Assignment("C_ODD_PARITY", Number(1))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x40600000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4060ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("TX", Ident("RS232_Uart_1_sout"))),
            Attribute(PORT(), Assignment("RX", Ident("RS232_Uart_1_sin"))),
            Attribute(PORT(), Assignment("Interrupt", Ident("RS232_Uart_1_Interrupt"))) 
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
            mhs = add(mhs, Attribute(PORT(), Assignment("LEDs_8Bits_TRI_O", Ident("LEDs_8Bits_TRI_O")),
                                Assignment("DIR", Ident("O")),
                                Assignment("VEC", Range(7,0))));
            addPortToInterruptController("LEDs_8Bits_IP2INTC_Irpt");
            mhs = add(mhs, createLEDs());
            break;
        case "button":
            mhs = add(mhs, Attribute(PORT(), Assignment("Push_Buttons_5Bits_TRI_I", Ident("Push_Buttons_5Bits_TRI_I")),
                    Assignment("DIR", Ident("I")),
                    Assignment("VEC", Range(4,0))));
            addPortToInterruptController("Push_Buttons_5Bits_IP2INTC_Irpt");
            mhs = add(mhs, createButtons());
            break;
        case "switch":
            mhs = add(mhs, Attribute(PORT(), Assignment("DIP_Switches_8Bits_TRI_I", Ident("DIP_Switches_8Bits_TRI_I")),
                    Assignment("DIR", Ident("O")),
                    Assignment("VEC", Range(7,0))));
            addPortToInterruptController("DIP_Switches_8Bits_IP2INTC_Irpt");
            mhs = add(mhs, createSwitches());
            break;
        }
    }

    public void visit(InstancePos term) {
        // begin a new instance using the instances name
        curBlock = Block(term.name().term());
        
        // reference core and version
        curBlock = add(curBlock, Attribute(PARAMETER(), Assignment("INSTANCE", Ident(term.core().term()))));
//        instance = add(instance, Attribute(Attribute(PARAMETER(), ), Assignment("HW_VER", Ident(term.core().version()))));
        
        // define bus interfaces
        visit(term.bind());
        
        // append default clock and reset ports
        curBlock = add(curBlock, Attribute(PORT(), Assignment("ACLK", Ident("clk_100_0000MHzMMCM0"))));
        curBlock = add(curBlock, Attribute(PORT(), Assignment("ARESETN", Ident("proc_sys_reset_0_Peripheral_aresetn"))));
        
        // add the block to the file
        mhs = add(mhs, curBlock);
    }
    
    public void visit(BindingsPos term) { for(BindingPos b : term) visit(b); }
    
    public void visit(AxisPos term) {
        curBlock = add(curBlock, Attribute(BUS_IF(), Assignment(term.port().term(), Ident(term.axis().term()))));
    }

    public void visit(final CPUAxisPos axis) {
        // get the direction of the corresponding port
        Direction direct = getPort(axis).direction().termDirection();
        
        // depending on the direction, add a different interface
        curBlock = add(curBlock, Attribute(BUS_IF(), Assignment(axis.port().term(),
            Ident(direct.Switch(new Direction.Switch<String, NE>() {
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
            }))
        )));
    }
    
    // imports and backends should be handled before this visitor
    public void visit(ImportPos term)  { }
    public void visit(BackendPos term) { }

    public void visit(CorePos term)  {
        String pao = new String();
        
//      pao += "\n##############################################################################";
//      pao += "\n## Filename:          /net/user/r1/unix/fischer/rngled/xps/pcores/AxiTwoRNG_v1_00_a/data/AxiTwoRNG_v2_1_0.pao";
//      pao += "\n## Description:       Peripheral Analysis Order";
//      pao += "\n## Date:              Wed Nov 28 14:51:19 2012 (by Create and Import Peripheral Wizard)";
//      pao += "\n##############################################################################";
        
        // we need to get the name of the source, WITHOUT extension...
        
        // the name of the core, constructed by replacing . with _
        String name = term.name() + "_" + term.version().term().replace('.', '_');
        
        // create folders for the core
        File coreDir     = new File(coresDir, name);
        File coreSrcDir  = new File(new File(coreDir, "hdl"), "vhdl");
        File coreDataDir = new File(coreDir, "data");
        
        // put all sources in there
        for(Import imp : term.source().term()) {
            // strip the path of the filename
            String src = FilenameUtils.getName(imp.file());
            
            // add the file to the .pao
            pao += "\nlib " + name + " " + FilenameUtils.removeExtension(src) + " vhdl";
            
            // add the file to the file list
            srcFiles.put(new File(coreSrcDir, src), imp.file());
        }
        
        // add the .pao file to the pao list
        paoFiles.put(new File(coreDataDir, name + ".pao"), pao);
        
        // add the .mpd file to the mpd list
        try {
            mpdFiles.put(new File(coreDataDir, name + ".mpd"), createCoreMPD(term.term()));
        } catch(GenerationFailed e) {
            errors.addError(e);
        }
    }

    public void visit(PortsPos term) { }

    // options are irrelevant in this visitor (so far)
    // TODO inject queues and multiplexer into mhs design --> provide some default cores!
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
    public void visit(PortPos term)     { }

    // literals
    public void visit(IntegerPos term) { }
    public void visit(StringsPos term) { }
    public void visit(StringPos term)  { }
}
