package de.hopp.generator.backends.server.virtex6.ise.xps;

import static de.hopp.generator.parser.MHS.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import katja.common.NE;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.frontend.BDLFilePos.Visitor;
import de.hopp.generator.frontend.*;
import de.hopp.generator.parser.*;

/**
 * Abstract XPS generation backend, providing some utility methods.
 * Also defines basic constants, that have to be instantiated by all extending versions.
 * @author Thomas Fischer
 * 
 * 
 * The cores described here are assumed to be compatible with all versions of XPS.
 * If this proves not to be the case in some earlier or later, unsupported version,
 * introduce a new abstract visitor subclass with adjusted cores and
 * rename this file accordingly to the earliest version number,
 * the core description is compatible with (e.g. XPS14).
 */
public abstract class XPS extends Visitor<NE> {
    protected ErrorCollection errors;
    
    // the generated mhs file
    protected MHSFile mhs;
    // other files to deploy
    protected Map<File, String>  srcFiles;  // core sources
    protected Map<File, String>  paoFiles;  // pao descriptors
    protected Map<File, MHSFile> mpdFiles;  // mpd descriptors
    
    // temporary variables used to build up the mhs file
    protected AndExp intrCntrlPorts;
    
    // counter variables
    protected int axiStreamIdMaster;
    protected int axiStreamIdSlave;
    
    protected String version;
    
    protected String version_microblaze;
    
    protected String version_proc_sys_reset;
    protected String version_axi_intc;
    protected String version_lmb_v10;
    protected String version_lmb_bram_if_cntlr;
    protected String version_bram_block;
    protected String version_mdm;
    protected String version_clock_generator;
    protected String version_axi_timer;
    protected String version_axi_interconnect;
    protected String version_axi_v6_ddrx;
    
    protected String version_axi_uartlite;
    protected String version_axi_ethernetlite;
    
    protected String version_gpio_leds;
    protected String version_gpio_buttons;
    protected String version_gpio_switches;
    
    public XPS() {
        // initiate variables  
        mhs = MHSFile(Attributes());
        
        srcFiles = new HashMap<File, String>();
        paoFiles = new HashMap<File, String>();
        mpdFiles = new HashMap<File, MHSFile>();
        
        intrCntrlPorts = AndExp();

        axiStreamIdMaster = 0;
        axiStreamIdSlave  = 0;
    }
    
    public MHSFile getMHSFile() {
        return mhs;
    }

    public Map<File, String> getCoreSources() {
        return srcFiles;
    }

    public Map<File, String> getPAOFiles() {
        return paoFiles;
    }

    public Map<File, MHSFile> getMPDFiles() {
        return mpdFiles;
    }
    
    // static helper methods modifying an mhs term by adding child terms
    protected static MHSFile add(MHSFile file, Attributes attr) {
        return file.replaceAttributes(file.attributes().addAll(attr));
    }
    protected static MHSFile add(MHSFile file, Blocks blocks) {
        return file.replaceBlocks(file.blocks().addAll(blocks));
    }
    protected static MHSFile add(MHSFile file, Attribute attr) {
        return file.replaceAttributes(file.attributes().add(attr));
    }
    protected static MHSFile add(MHSFile file, Block block) {
        return file.replaceBlocks(file.blocks().add(block));
    }
    protected static Block add(Block block, Attribute attr) {
        return block.replaceAttributes(block.attributes().add(attr));
    }
    
    // (static) helpers creating parts of the file
    
    /**
     * Creates the mpd file of a core. The file has still to be added to the mpd list.
     * @param core The core, for which an mpd file should be created
     * @return The generated mpd file
     * @throws GenerationFailed Something went wrong during generation. This can be the case,
     *                          if unexpected attributes occurred in the core description.
     */
    protected static MHSFile createCoreMPD(Core core) throws GenerationFailed {
        
        Block block = Block(core.name());
        
        block = add(block, Attribute(OPTION(), Assignment("IPTYPE", Ident("PERIPHERAL"))));
        block = add(block, Attribute(OPTION(), Assignment("IMP_NETLIST", Ident("TRUE"))));
        block = add(block, Attribute(OPTION(), Assignment("IP_GROUP", Ident("USER"))));
        block = add(block, Attribute(OPTION(), Assignment("HDL", Ident("VHDL"))));
        block = add(block, Attribute(OPTION(), Assignment("STYLE", Ident("HDL"))));

        block = add(block, Attribute(OPTION(), Assignment("DESC", STR("AXIS_FIFO"))));
        block = add(block, Attribute(OPTION(), Assignment("LONG_DESC", STR(""))));

        for(final Port port : core.ports()) {
            int bitwidth = 32;
            for(Option opt : port.opts()) {
                if(opt instanceof BITWIDTH) bitwidth = ((BITWIDTH)opt).bit();
            }
            
            block = add(block, Attribute(BUS_IF(),
                Assignment("BUS", Ident(port.name())),
                Assignment("BUS_STD", Ident("AXIS")),
                port.direction().Switch(new Direction.Switch<Assignment, GenerationFailed>() {
                    public Assignment CaseIN(IN term)   {
                        return Assignment("BUS_TYPE", Ident("TARGET"));
                    }
                    public Assignment CaseOUT(OUT term) {
                        return Assignment("BUS_TYPE", Ident("INITIATOR"));
                    }
                    public Assignment CaseDUAL(DUAL term) throws GenerationFailed {
                        throw new GenerationFailed("invalid direction for virtex 6");
                    }
                })
            ));
            
            block = add(block, Attribute(PARAMETER(),
                    Assignment("C_S_AXIS_PROTOCOL", Ident("GENERIC")),
                    Assignment("DT", Ident("string")),
                    Assignment("TYPE", Ident("NON_HDL")),
                    Assignment("ASSIGNMENT", Ident("CONSTANT")),
                    Assignment("BUS", Ident(port.name()))
            ));
            
            block = add(block, Attribute(PARAMETER(),
                    Assignment("C_S_AXIS_TDATA_WIDTH", Number(bitwidth)),
                    Assignment("DT", Ident("integer")),
                    Assignment("TYPE", Ident("NON_HDL")),
                    Assignment("ASSIGNMENT", Ident("CONSTANT")),
                    Assignment("BUS", Ident(port.name()))
            ));
            
            Direction.Switch<String, GenerationFailed> dSwitch =
                new Direction.Switch<String, GenerationFailed>() {
                    public String CaseIN(IN term) throws GenerationFailed {
                        return "I";
                    }
                    public String CaseOUT(OUT term) throws GenerationFailed {
                        return "O";
                    }
                    public String CaseDUAL(DUAL term) throws GenerationFailed {
                        throw new GenerationFailed("invalid direction for virtex6");
                    }
                };
            
            block = add(block, Attribute(PORT(),
                    Assignment(port.name() + "_tvalid", Ident("TVALID")),
                    Assignment("DIR", Ident(port.direction().Switch(dSwitch))),
                    Assignment("BUS", Ident(port.name()))
            ));
            
            block = add(block, Attribute(PORT(),
                    Assignment(port.name() + "_tdata", Ident("TDATA")),
                    Assignment("DIR", Ident(port.direction().Switch(dSwitch))),
                    Assignment("VEC", Range(bitwidth-1, 0)),
                    Assignment("BUS", Ident(port.name()))
            ));
            
            block = add(block, Attribute(PORT(),
                    Assignment(port.name() + "_tlast", Ident("TLAST")),
                    Assignment("DIR", Ident(port.direction().Switch(dSwitch))),
                    Assignment("BUS", Ident(port.name()))
            ));
            
            block = add(block, Attribute(PORT(),
                    Assignment(port.name() + "_tready", Ident("TREADY")),
                    Assignment("DIR", Ident(port.direction().Switch(dSwitch))),
                    Assignment("BUS", Ident(port.name()))
            ));
        }
        
        block = add(block, Attribute(PORT(),
                Assignment("aclk", STR("ACLK")),
                Assignment("DIR", Ident("I")),
                Assignment("SIGIS", Ident("CLK")),
                Assignment("ASSIGNMENT", Ident("REQUIRE")),
                Assignment("BUS", Ident("I")) // TODO use correct bus identifier
        ));
        
        block = add(block, Attribute(PORT(),
                Assignment("aresetn", STR("ARESETN")),
                Assignment("DIR", Ident("I")),
                Assignment("SIGIS", Ident("RST")),
                Assignment("ASSIGNMENT", Ident("REQUIRE")),
                Assignment("BUS", Ident("I")) // TODO use correct bus identifier
        ));
        
        return MHSFile(Attributes(), block);
    }
    
    /** 
     * Generates an LED core.
     * @return A Block representing the generated core.
     */
    protected Block createLEDs() {
        return Block("axi_gpio",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("LEDs_8Bits"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_gpio_leds))),
            Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(8))),
            Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(0))),
            Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
            Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("40020000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("4002ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("GPIO_IO_O", Ident("LEDs_8Bits_TRI_O"))),
            Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident("LEDs_8Bits_IP2INTC_Irpt")))
        );
    }
    
    /** 
     * Generates a Pushbutton core.
     * @return A Block representing the generated core.
     */
    protected Block createButtons() {
        return Block("axi_gpio",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("Push_Buttons_5Bits"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_gpio_buttons))),
            Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(5))),
            Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
            Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
            Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("40000000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("4000ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("GPIO_IO_O", Ident("Push_Buttons_5Bits_TRI_I"))),
            Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident("Push_Buttons_5Bits_IP2INTC_Irpt")))
        );
    }
    
    /** 
     * Generates a DIP Switch core.
     * @return A Block representing the generated core.
     */
    protected Block createSwitches() {
       return Block("axi_gpio",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("DIP_Switches_8Bits"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_gpio_switches))),
            Attribute(PARAMETER(), Assignment("C_GPIO_WIDTH", Number(8))),
            Attribute(PARAMETER(), Assignment("C_ALL_INPUTS", Number(1))),
            Attribute(PARAMETER(), Assignment("C_INTERRUPT_PRESENT", Number(1))),
            Attribute(PARAMETER(), Assignment("C_IS_DUAL", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("40040000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("4004ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("GPIO_IO_O", Ident("DIP_Switches_8Bits_TRI_I"))),
            Attribute(PORT(), Assignment("IP2INTC_Irpt", Ident("DIP_Switches_8Bits_IP2INTC_Irpt")))
        );
    }
    
    // helper methods modifying the mhs file of this visitor
    
    protected Attribute createCPUAxisBinding(final CPUAxis axis, boolean d, int width, int queueSize) throws UsageError {
        
        String axisGroup   = d ? "M" + axiStreamIdMaster++ : "S" + axiStreamIdSlave++;
        String currentAxis = axisGroup + "_AXIS";
        
        // add multiplexing, if required
        currentAxis = addMux(axisGroup, d, width, currentAxis);
        
        // add queueing, if required
        currentAxis = addQueue(axisGroup, d, width, queueSize, currentAxis);
        
        // connect the component to the last axis
        return Attribute(BUS_IF(), Assignment(axis.port(), Ident(currentAxis)));
    }
    
    private String addQueue(String axisGroup, boolean d, int width, int depth, String currentAxis) throws UsageError {
        
        if(depth == 0) return currentAxis;
        if(depth < 0) throw new UsageError("negative queue size");
        
        String queueAxis = axisGroup + "_QUEUE_AXIS";
        
        // add a queue component in between the component and the microblaze
        mhs = add(mhs, Block(axisGroup + "_QUEUE",
               Attribute(PARAMETER(), Assignment("INSTANCE", Ident("Queue"))),
               Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.00.a"))),
               Attribute(PARAMETER(), Assignment("DEPTH", Number(depth))),
               Attribute(PARAMETER(), Assignment("WIDTH", Number(width))),
               Attribute(BUS_IF(), Assignment("IN", Ident(d ? currentAxis : queueAxis))),
               Attribute(BUS_IF(), Assignment("OUT", Ident(d ? queueAxis : currentAxis))),
               Attribute(PORT(), Assignment("ACLK", Ident("clk_100_0000MHzMMCM0"))),
               Attribute(PORT(), Assignment("ARESETN", Ident("proc_sys_reset_0_Peripheral_aresetn")))
        ));
        
        // return the axis identifier of the queues port, that should be attached to the components port
        return queueAxis;
    }

    private String addMux(String axisGroup, boolean d, int width, String currentAxis) throws UsageError {
        
        if(width == 32) return currentAxis;
        if(width <= 0) throw new UsageError("encountered port with bitwidth of " + width);
        
        // add a mux component
        if (width > 32) {
            int mult = new Double(Math.floor(width / 32)).intValue();
            
            String muxAxis  = axisGroup + "_MUX_AXIS";
            
            // depending on the direction, we need an upsizer or downsizer
            mhs = add(mhs, Block(axisGroup + "_MUX",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(d ? "Upsizer" : "Downsizer"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.00.a"))),
                Attribute(PARAMETER(), Assignment("WIDTH", Number(32))),
                Attribute(PARAMETER(), Assignment("NUM_REG", Number(mult))),
                Attribute(BUS_IF(), Assignment("TDATA", Ident(d ? currentAxis : muxAxis))),
                Attribute(BUS_IF(), Assignment("TARRAY", Ident(d ? muxAxis : currentAxis))),
                Attribute(PORT(), Assignment("ACLK", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("ARESETN", Ident("proc_sys_reset_0_Peripheral_aresetn")))
            ));
                    
            currentAxis = muxAxis;
        }
        
        
        // add a drop component, if the width is not a multiple of 32 bit
        if (width % 32 > 0) {
            int pad = width % 32;
            
            String dropAxis = axisGroup + "_DROP_AXIS";

            // TODO support it ;)
            throw new UsageError("conversion to non-multiples of 32-bit is currently unsupported");
            
            // depending on the direction, drop or add
//            mhs = add(mhs, Block(axisGroup + "_BITSTUFFER",
//                Attribute(PARAMETER(), Assignment("INSTANCE", Ident(d ? "BitRemover" : "BitStuffer"))),
//                Attribute(PARAMETER(), Assignment("HW_VER", Ident("1.00.a"))),
//                Attribute(PARAMETER(), Assignment("WIDTH", Number(width))),
//                Attribute(PARAMETER(), Assignment("PADDING", Number(32 - (width % 32)))),
//                Attribute(BUS_IF(), Assignment("TDATA", Ident(d ? currentAxis : dropAxis))),
//                Attribute(BUS_IF(), Assignment("TARRAY", Ident(d ? dropAxis : currentAxis))),
//                Attribute(PORT(), Assignment("ACLK", Ident("clk_100_0000MHzMMCM0"))),
//                Attribute(PORT(), Assignment("ARESETN", Ident("proc_sys_reset_0_Peripheral_aresetn")))
//            ));
//            
//            currentAxis = dropAxis;
        }
        
        return currentAxis;
    }
    
    /**
     * Adds a port descriptor to the interrupt controller port list.
     * This list is later used add the interrupt controller core.
     * @param port Port to be added
     */
    protected void addPortToInterruptController(String port) {
        intrCntrlPorts = intrCntrlPorts.add(Ident(port));
    }
    
    /** Adds all basic components to the design, that are independent from the board. */
    protected void addDefault() {
        Attributes attr = Attributes(
          Attribute(PARAMETER(), Assignment("VERSION", Ident(version))),
          Attribute(PORT(), 
            Assignment("ddr_memory_we_n", Ident("ddr_memory_we_n")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_ras_n", Ident("ddr_memory_ras_n")), 
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_odt", Ident("ddr_memory_odt")), 
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_dqs_n", Ident("ddr_memory_dqs_n")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(0, 0))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_dqs", Ident("ddr_memory_dqs")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(0, 0))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_dq", Ident("ddr_memory_dq")),
            Assignment("DIR", Ident("IO")),
            Assignment("VEC", Range(7, 0))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_dm", Ident("ddr_memory_dm")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(0, 0))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_ddr3_rst", Ident("ddr_memory_ddr3_rst")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_cs_n", Ident("ddr_memory_cs_n")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_clk_n", Ident("ddr_memory_clk_n")),
            Assignment("DIR", Ident("O")),
            Assignment("SIGIS", Ident("CLK"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_clk", Ident("ddr_memory_clk")),
            Assignment("DIR", Ident("O")),
            Assignment("SIGIS", Ident("CLK"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_cke", Ident("ddr_memory_cke")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_cas_n", Ident("ddr_memory_cas_n")),
            Assignment("DIR", Ident("O"))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_ba", Ident("ddr_memory_ba")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(2, 0))
          ), Attribute(PORT(), 
            Assignment("ddr_memory_addr", Ident("ddr_memory_addr")),
            Assignment("DIR", Ident("O")),
            Assignment("VEC", Range(12, 0))
          ), Attribute(PORT(), 
            Assignment("RESET", Ident("RESET")),
            Assignment("DIR", Ident("I")),
            Assignment("SIGIS", Ident("RST")),
            Assignment("RST_POLARITY", Number(1))
          ), Attribute(PORT(), 
            Assignment("CLK_P", Ident("CLK")),
            Assignment("DIR", Ident("I")),
            Assignment("DIFFERENTIAL_POLARITY", Ident("P")),
            Assignment("SIGIS", Ident("CLK")),
            Assignment("CLK_FREQ", Number(200000000))
          ), Attribute(PORT(), 
            Assignment("CLK_N", Ident("CLK")),
            Assignment("DIR", Ident("I")),
            Assignment("DIFFERENTIAL_POLARITY", Ident("N")),
            Assignment("SIGIS", Ident("CLK")),
            Assignment("CLK_FREQ", Number(200000000))
          )
        );
        
        Blocks blocks = Blocks(
          Block("proc_sys_reset",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("proc_sys_reset_0"))), 
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_proc_sys_reset))),
            Attribute(PARAMETER(), Assignment("C_EXT_RESET_HIGH", Number(1))),
            Attribute(PORT(), Assignment("MB_Debug_Sys_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))), 
            Attribute(PORT(), Assignment("Dcm_locked", Ident("proc_sys_reset_0_Dcm_locked"))),
            Attribute(PORT(), Assignment("MB_Reset", Ident("proc_sys_reset_0_MB_Reset"))),
            Attribute(PORT(), Assignment("Slowest_sync_clk", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("Interconnect_aresetn", Ident("proc_sys_reset_0_Interconnect_aresetn"))), 
            Attribute(PORT(), Assignment("Ext_Reset_In", Ident("RESET"))),
            Attribute(PORT(), Assignment("BUS_STRUCT_RESET", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            Attribute(PORT(), Assignment("Peripheral_aresetn", Ident("proc_sys_reset_0_Peripheral_aresetn")))
          ), Block("axi_intc",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_intc"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_axi_intc))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41200000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4120ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("INTERRUPT", Ident("microblaze_0_interrupt"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("INTR", intrCntrlPorts.add(Ident("axi_timer_0_Interrupt"))))
          ), Block("lmb_v10",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_ilmb"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_lmb_v10))),
            Attribute(PORT(), Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            Attribute(PORT(), Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("lmb_bram_if_cntlr",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_i_bram_ctrl"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_lmb_bram_if_cntlr))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x00000000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
            Attribute(BUS_IF(), Assignment("SLMB", Ident("microblaze_0_ilmb"))),
            Attribute(BUS_IF(), Assignment("BRAM_PORT", Ident("microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block")))
          ), Block("lmb_v10",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_dlmb"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_lmb_v10))),
            Attribute(PORT(), Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
            Attribute(PORT(), Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0"))) 
          ), Block("lmb_bram_if_cntlr",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_d_bram_ctrl"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_lmb_bram_if_cntlr))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x00000000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
            Attribute(BUS_IF(), Assignment("SLMB", Ident("microblaze_0_dlmb"))),
            Attribute(BUS_IF(), Assignment("BRAM_PORT", Ident("microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")))
          ), Block("bram_block",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_bram_block"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_bram_block))),
            Attribute(BUS_IF(), Assignment("PORTA", Ident("microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block"))),
            Attribute(BUS_IF(), Assignment("PORTB", Ident("microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")))
          ), Block("mdm",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("debug_module"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_mdm))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT", Number(2))),
            Attribute(PARAMETER(), Assignment("C_USE_UART", Number(1))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41400000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4140ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("MBDEBUG_0", Ident("microblaze_0_debug"))),
            Attribute(PORT(), Assignment("Debug_SYS_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("clock_generator",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("clock_generator_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_clock_generator))),
            Attribute(PARAMETER(), Assignment("C_CLKIN_FREQ", Number(200000000))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT0_FREQ", Number(100000000))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT0_GROUP", Ident("MMCM0"))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT1_FREQ", Number(200000000))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT1_GROUP", Ident("MMCM0"))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT2_FREQ", Number(400000000))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT2_GROUP", Ident("MMCM0"))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT3_FREQ", Number(400000000))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT3_GROUP", Ident("MMCM0"))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT3_BUF", Ident("FALSE"))),
            Attribute(PARAMETER(), Assignment("C_CLKOUT3_VARIABLE_PHASE", Ident("TRUE"))),
            Attribute(PORT(), Assignment("LOCKED", Ident("proc_sys_reset_0_Dcm_locked"))),
            Attribute(PORT(), Assignment("CLKOUT0", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("RST", Ident("RESET"))),
            Attribute(PORT(), Assignment("CLKOUT3", Ident("clk_400_0000MHzMMCM0_nobuf_varphase"))),
            Attribute(PORT(), Assignment("CLKOUT2", Ident("clk_400_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("CLKOUT1", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("CLKIN", Ident("CLK"))),
            Attribute(PORT(), Assignment("PSCLK", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("PSEN", Ident("psen"))),
            Attribute(PORT(), Assignment("PSINCDEC", Ident("psincdec"))),
            Attribute(PORT(), Assignment("PSDONE", Ident("psdone")))
          ), Block("axi_timer",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi_timer_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_axi_timer))),
            Attribute(PARAMETER(), Assignment("C_COUNT_WIDTH", Number(32))),
            Attribute(PARAMETER(), Assignment("C_ONE_TIMER_ONLY", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41c00000"))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x41c0ffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("Interrupt", Ident("axi_timer_0_Interrupt")))
          ), Block("axi_interconnect",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4lite_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_axi_interconnect))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(0))),
            Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn"))),
            Attribute(PORT(), Assignment("INTERCONNECT_ACLK", Ident("clk_100_0000MHzMMCM0")))
          ), Block("axi_interconnect",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_axi_interconnect))),
            Attribute(PORT(), Assignment("interconnect_aclk", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn")))
          ), Block("axi_v6_ddrx",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("DDR3_SDRAM"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_axi_v6_ddrx))),
            Attribute(PARAMETER(), Assignment("C_MEM_PARTNO", Ident("MT41J64M16XX-15E"))),
            Attribute(PARAMETER(), Assignment("C_DM_WIDTH", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DQS_WIDTH", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DQ_WIDTH", Number(8))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT_S_AXI_MASTERS", 
              AndExp(
                Ident("microblaze_0.M_AXI_DC"),
                Ident("microblaze_0.M_AXI_IC")
              )
            )),
            Attribute(PARAMETER(), Assignment("C_MMCM_EXT_LOC", Ident("MMCM_ADV_X0Y8"))),
            Attribute(PARAMETER(), Assignment("C_NDQS_COL0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_NDQS_COL1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_S_AXI_BASEADDR", MemAddr("0xa4000000"))),
            Attribute(PARAMETER(), Assignment("C_S_AXI_HIGHADDR", MemAddr("0xa7ffffff"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4_0"))),
            Attribute(PORT(), Assignment("ddr_we_n", Ident("ddr_memory_we_n"))),
            Attribute(PORT(), Assignment("ddr_ras_n", Ident("ddr_memory_ras_n"))),
            Attribute(PORT(), Assignment("ddr_odt", Ident("ddr_memory_odt"))),
            Attribute(PORT(), Assignment("ddr_dqs_n", Ident("ddr_memory_dqs_n"))),
            Attribute(PORT(), Assignment("ddr_dqs_p", Ident("ddr_memory_dqs"))),
            Attribute(PORT(), Assignment("ddr_dq", Ident("ddr_memory_dq"))),
            Attribute(PORT(), Assignment("ddr_dm", Ident( "ddr_memory_dm"))),
            Attribute(PORT(), Assignment("ddr_reset_n", Ident("ddr_memory_ddr3_rst"))),
            Attribute(PORT(), Assignment("ddr_cs_n", Ident("ddr_memory_cs_n"))),
            Attribute(PORT(), Assignment("ddr_ck_n", Ident("ddr_memory_clk_n"))),
            Attribute(PORT(), Assignment("ddr_ck_p", Ident("ddr_memory_clk"))),
            Attribute(PORT(), Assignment("ddr_cke", Ident("ddr_memory_cke"))),
            Attribute(PORT(), Assignment("ddr_cas_n", Ident("ddr_memory_cas_n"))),
            Attribute(PORT(), Assignment("ddr_ba", Ident("ddr_memory_ba"))),
            Attribute(PORT(), Assignment("ddr_addr", Ident("ddr_memory_addr"))),
            Attribute(PORT(), Assignment("clk_rd_base", Ident("clk_400_0000MHzMMCM0_nobuf_varphase"))),
            Attribute(PORT(), Assignment("clk_mem", Ident("clk_400_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("clk", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("clk_ref", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("PD_PSEN", Ident("psen"))),
            Attribute(PORT(), Assignment("PD_PSINCDEC", Ident("psincdec"))),
            Attribute(PORT(), Assignment("PD_PSDONE", Ident("psdone")))
          )
        );
        
        mhs = add(mhs, attr);
        mhs = add(mhs, blocks);
    }
    
    /** Adds the microblaze to the design. */
    protected void addMicroblaze() {
        Block microblaze = Block("microblaze",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(version_microblaze))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT", Number(2))),
            Attribute(PARAMETER(), Assignment("C_USE_BARREL", Number(1))),
            Attribute(PARAMETER(), Assignment("C_USE_FPU", Number(0))),
            Attribute(PARAMETER(), Assignment("C_DEBUG_ENABLED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_ICACHE_BASEADDR", MemAddr("0xa4000000"))),
            Attribute(PARAMETER(), Assignment("C_ICACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            Attribute(PARAMETER(), Assignment("C_USE_ICACHE", Number(1))),
            Attribute(PARAMETER(), Assignment("C_CACHE_BYTE_SIZE", Number(65536))),
            Attribute(PARAMETER(), Assignment("C_ICACHE_ALWAYS_USED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_BASEADDR", MemAddr("0xa4000000"))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            Attribute(PARAMETER(), Assignment("C_USE_DCACHE", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_BYTE_SIZE", Number(65536))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_ALWAYS_USED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_FSL_LINKS", Number(1))),
            Attribute(PARAMETER(), Assignment("C_STREAM_INTERCONNECT", Number(1))),
            Attribute(BUS_IF(), Assignment("M_AXI_DP", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("M_AXI_DC", Ident("axi4_0"))),
            Attribute(BUS_IF(), Assignment("M_AXI_IC", Ident("axi4_0"))),
            Attribute(BUS_IF(), Assignment("DEBUG", Ident("microblaze_0_debug"))),
            Attribute(BUS_IF(), Assignment("INTERRUPT", Ident("microblaze_0_interrupt"))),
            Attribute(BUS_IF(), Assignment("DLMB", Ident("microblaze_0_dlmb"))),
            Attribute(BUS_IF(), Assignment("ILMB", Ident("microblaze_0_ilmb")))
        );
        
        // add master and slave interfaces for user-attached cores
        for(int i = 0; i < axiStreamIdMaster; i++)
            microblaze = add(microblaze, Attribute(BUS_IF(),
                    Assignment("M" + i + "_AXIS", Ident("microblaze_0_M" + i + "_AXIS"))));
        for(int i = 0; i < axiStreamIdSlave; i++)
            microblaze = add(microblaze, Attribute(BUS_IF(),
                    Assignment("S" + i + "_AXIS", Ident("microblaze_0_S" + i + "_AXIS"))));
        
        // add reset and clock ports
        microblaze = add(microblaze, Attribute(PORT(), Assignment("MB_RESET", Ident("proc_sys_reset_0_MB_Reset"))));
        microblaze = add(microblaze, Attribute(PORT(), Assignment("CLK", Ident("clk_100_0000MHzMMCM0"))));
        
        mhs = add(mhs, microblaze);
    }
}