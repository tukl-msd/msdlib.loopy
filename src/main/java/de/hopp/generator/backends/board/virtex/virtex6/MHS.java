package de.hopp.generator.backends.board.virtex.virtex6;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.parser.MHS.*;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.backends.workflow.ise.xps.MHSGenerator;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.ETHERNETPos;
import de.hopp.generator.frontend.PCIEPos;
import de.hopp.generator.frontend.UARTPos;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.Blocks;
import de.hopp.generator.parser.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public class MHS extends MHSGenerator {

    public MHS(ISEBoard board, IPCoreVersions versions, ErrorCollection errors) {
        super(board, versions, errors);

        frequencies.add(100);
        frequencies.add(200);
        frequencies.add(400);
    }

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
            Attribute(PARAMETER(), Assignment("HW_VER",       Ident(versions.axi_ethernetlite))),
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
        errors.addError(new ParserError("USB/UART is not supported as communication interface yet", term.pos().term()));
    }

    public void visit(PCIEPos term) {
        errors.addError(new ParserError("PCIE is not supported as communication interface yet", term.pos().term()));
    }

    @Override
    protected MHSFile getDefaultParts() {
        return MHSFile(defaultAttributes(), defaultBlocks());
    }

    protected Attributes defaultAttributes() {
        return Attributes(
            Attribute(PARAMETER(), Assignment("VERSION", Ident(versions.mhs))
            ), Attribute(PORT(),
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
                Assignment("RS232_Uart_1_sout", Ident("RS232_Uart_1_sout")),
                Assignment("DIR", Ident("O"))
            ), Attribute(PORT(),
                Assignment("RS232_Uart_1_sin", Ident("RS232_Uart_1_sin")),
                Assignment("DIR", Ident("I"))
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
    }

    /**
     * Adds all basic components to the design, that are independent from the board.
     * @return all default blocks for a Virtex 6 board.
     */
    protected Blocks defaultBlocks() {
        intrCntrlPorts = intrCntrlPorts.add(Ident("RS232_Uart_1_Interrupt"));

       return Blocks(
            Block("proc_sys_reset",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("proc_sys_reset_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.proc_sys_reset))),
                Attribute(PARAMETER(), Assignment("C_EXT_RESET_HIGH", Number(1))),
                Attribute(PORT(), Assignment("MB_Debug_Sys_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))),
                Attribute(PORT(), Assignment("Dcm_locked", Ident("proc_sys_reset_0_Dcm_locked"))),
                Attribute(PORT(), Assignment("MB_Reset", Ident("proc_sys_reset_0_MB_Reset"))),
                Attribute(PORT(), Assignment("Slowest_sync_clk", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("Interconnect_aresetn", Ident("proc_sys_reset_0_Interconnect_aresetn"))),
                Attribute(PORT(), Assignment("Ext_Reset_In", Ident("RESET"))),
                Attribute(PORT(), Assignment("BUS_STRUCT_RESET", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
                Attribute(PORT(), Assignment("Peripheral_reset", Ident("proc_sys_reset_0_Peripheral_reset"))),
                Attribute(PORT(), Assignment("Peripheral_aresetn", Ident("proc_sys_reset_0_Peripheral_aresetn")))
            ), Block("axi_intc",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_intc"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_intc))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41200000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4120ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(BUS_IF(), Assignment("INTERRUPT", Ident("microblaze_0_interrupt"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("INTR", intrCntrlPorts.add(Ident("axi_timer_0_Interrupt"))))
            ), Block("lmb_v10",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_ilmb"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.lmb_v10))),
                Attribute(PORT(), Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
                Attribute(PORT(), Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0")))
            ), Block("lmb_bram_if_cntlr",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_i_bram_ctrl"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.lmb_bram_if_cntlr))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x00000000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
                Attribute(BUS_IF(), Assignment("SLMB", Ident("microblaze_0_ilmb"))),
                Attribute(BUS_IF(), Assignment("BRAM_PORT", Ident("microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block")))
            ), Block("lmb_v10",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_dlmb"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.lmb_v10))),
                Attribute(PORT(), Assignment("SYS_RST", Ident("proc_sys_reset_0_BUS_STRUCT_RESET"))),
                Attribute(PORT(), Assignment("LMB_CLK", Ident("clk_100_0000MHzMMCM0")))
            ), Block("lmb_bram_if_cntlr",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_d_bram_ctrl"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.lmb_bram_if_cntlr))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x00000000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x0000ffff"))),
                Attribute(BUS_IF(), Assignment("SLMB", Ident("microblaze_0_dlmb"))),
                Attribute(BUS_IF(), Assignment("BRAM_PORT", Ident("microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")))
            ), Block("bram_block",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0_bram_block"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.bram_block))),
                Attribute(BUS_IF(), Assignment("PORTA", Ident("microblaze_0_i_bram_ctrl_2_microblaze_0_bram_block"))),
                Attribute(BUS_IF(), Assignment("PORTB", Ident("microblaze_0_d_bram_ctrl_2_microblaze_0_bram_block")))
            ), Block("mdm",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("debug_module"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.mdm))),
                Attribute(PARAMETER(), Assignment("C_INTERCONNECT", Number(2))),
                Attribute(PARAMETER(), Assignment("C_USE_UART", Number(1))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41400000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x4140ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(BUS_IF(), Assignment("MBDEBUG_0", Ident("microblaze_0_debug"))),
                Attribute(PORT(), Assignment("Debug_SYS_Rst", Ident("proc_sys_reset_0_MB_Debug_Sys_Rst"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0")))
            ), Block("axi_timer",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi_timer_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_timer))),
                Attribute(PARAMETER(), Assignment("C_COUNT_WIDTH", Number(32))),
                Attribute(PARAMETER(), Assignment("C_ONE_TIMER_ONLY", Number(0))),
                Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr("0x41c00000"))),
                Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr("0x41c0ffff"))),
                Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi4lite_0"))),
                Attribute(PORT(), Assignment("S_AXI_ACLK", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("Interrupt", Ident("axi_timer_0_Interrupt")))
            ), Block("axi_interconnect",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4lite_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_interconnect))),
                Attribute(PARAMETER(), Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(0))),
                Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn"))),
                Attribute(PORT(), Assignment("INTERCONNECT_ACLK", Ident("clk_100_0000MHzMMCM0")))
            ), Block("axi_interconnect",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_interconnect))),
                Attribute(PORT(), Assignment("interconnect_aclk", Ident("clk_100_0000MHzMMCM0"))),
                Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("proc_sys_reset_0_Interconnect_aresetn")))
            ), Block("axi_v6_ddrx",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("DDR3_SDRAM"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_v6_ddrx))),
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
            ), Block("axi_uartlite",
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("RS232_Uart_1"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_uartlite))),
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
            )
        );
    }

    /** Adds the microblaze to the design. */
    @Override
    protected MHSFile getProcessorConnection() {
        Block microblaze = Block("microblaze",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("microblaze_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.microblaze))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT", Number(2))),
            Attribute(PARAMETER(), Assignment("C_USE_BARREL", Number(1))),
            Attribute(PARAMETER(), Assignment("C_USE_FPU", Number(0))), // default: 0 speed 2
            Attribute(PARAMETER(), Assignment("C_DEBUG_ENABLED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_ICACHE_BASEADDR", MemAddr("0xa4000000"))),
            Attribute(PARAMETER(), Assignment("C_ICACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            Attribute(PARAMETER(), Assignment("C_USE_ICACHE", Number(1))),
            Attribute(PARAMETER(), Assignment("C_CACHE_BYTE_SIZE", Number(65536))), // default: 65536 speed: 32768
            Attribute(PARAMETER(), Assignment("C_ICACHE_ALWAYS_USED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_BASEADDR", MemAddr("0xa4000000"))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_HIGHADDR", MemAddr("0xa7ffffff"))),
            Attribute(PARAMETER(), Assignment("C_USE_DCACHE", Number(1))),
            Attribute(PARAMETER(), Assignment("C_DCACHE_BYTE_SIZE", Number(65536))), // default: 65536 speed: 32768
            Attribute(PARAMETER(), Assignment("C_DCACHE_ALWAYS_USED", Number(1))),
            Attribute(PARAMETER(), Assignment("C_STREAM_INTERCONNECT", Number(1))),
            // new stuff
            //Attribute(PARAMETER(), Assignment("C_ICACHE_LINE_LEN", Number(8))),
            //Attribute(PARAMETER(), Assignment("C_ICACHE_STREAMS", Number(1))),
            //Attribute(PARAMETER(), Assignment("C_ICACHE_VICTIMS", Number(8))),
            //Attribute(PARAMETER(), Assignment("C_DCACHE_LINE_LEN", Number(8))),
            //Attribute(PARAMETER(), Assignment("C_DCACHE_USE_WRITEBACK", Number(1))),
            //Attribute(PARAMETER(), Assignment("C_DCACHE_VICTIMS", Number(8))),
            //Attribute(PARAMETER(), Assignment("C_USE_HW_MUL", Number(2))),
            //Attribute(PARAMETER(), Assignment("C_USE_DIV", Number(1))),
            //Attribute(PARAMETER(), Assignment("C_USE_BRANCH_TARGET_CACHE", Number(1))),
            Attribute(PARAMETER(), Assignment("C_USE_EXTENDED_FSL_INSTR", Number(1))),
            Attribute(BUS_IF(), Assignment("M_AXI_DP", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("M_AXI_DC", Ident("axi4_0"))),
            Attribute(BUS_IF(), Assignment("M_AXI_IC", Ident("axi4_0"))),
            Attribute(BUS_IF(), Assignment("DEBUG", Ident("microblaze_0_debug"))),
            Attribute(BUS_IF(), Assignment("INTERRUPT", Ident("microblaze_0_interrupt"))),
            Attribute(BUS_IF(), Assignment("DLMB", Ident("microblaze_0_dlmb"))),
            Attribute(BUS_IF(), Assignment("ILMB", Ident("microblaze_0_ilmb")))
            );

        // add master and slave interfaces for user-attached cores
        microblaze = add(microblaze, Attribute(PARAMETER(),
            Assignment("C_FSL_LINKS", Number(Math.max(axiStreamIdMaster, axiStreamIdSlave)))
            ));

        for(int i = 0; i < axiStreamIdMaster; i++)
            microblaze = add(microblaze, Attribute(BUS_IF(),
                Assignment("M" + i + "_AXIS", Ident("M" + i + "_AXIS"))));
        for(int i = 0; i < axiStreamIdSlave; i++)
            microblaze = add(microblaze, Attribute(BUS_IF(),
                Assignment("S" + i + "_AXIS", Ident("S" + i + "_AXIS"))));

        // add reset and clock ports
        microblaze = add(microblaze, Attribute(PORT(), Assignment("MB_RESET", Ident("proc_sys_reset_0_MB_Reset"))));
        microblaze = add(microblaze, Attribute(PORT(), Assignment("CLK", Ident("clk_100_0000MHzMMCM0"))));

        return MHSFile(Attributes(), microblaze);
    }

    @Override
    protected MHSFile getClk() {
        // TODO add information about this buf and varphase... find out what it means...
        Block timer = Block("clock_generator",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("clock_generator_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.clock_generator))),
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
            Attribute(PARAMETER(), Assignment("C_CLKOUT3_VARIABLE_PHASE", Ident("TRUE")))
            );

        Attributes ports = Attributes(
            Attribute(PORT(), Assignment("LOCKED", Ident("proc_sys_reset_0_Dcm_locked"))),
            Attribute(PORT(), Assignment("CLKIN", Ident("CLK"))),
            Attribute(PORT(), Assignment("PSCLK", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("PSEN", Ident("psen"))),
            Attribute(PORT(), Assignment("PSINCDEC", Ident("psincdec"))),
            Attribute(PORT(), Assignment("PSDONE", Ident("psdone"))),
            Attribute(PORT(), Assignment("RST", Ident("RESET"))),
            Attribute(PORT(), Assignment("CLKOUT0", Ident("clk_100_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("CLKOUT1", Ident("clk_200_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("CLKOUT2", Ident("clk_400_0000MHzMMCM0"))),
            Attribute(PORT(), Assignment("CLKOUT3", Ident("clk_400_0000MHzMMCM0_nobuf_varphase")))
            );

        int freqCounter = 4;

        frequencies.remove(100);
        frequencies.remove(200);
        frequencies.remove(400);

        for(Integer frequency : frequencies) {
            timer = add(timer, Attribute(PARAMETER(),
                Assignment("C_CLKOUT" + freqCounter + "_FREQ", Number(frequency * 1000000))));
            timer = add(timer, Attribute(PARAMETER(),
                Assignment("C_CLKOUT" + freqCounter + "_GROUP", Ident("MMCMC0"))));
            ports.add(Attribute(PORT(),
                Assignment("CLKOUT" + freqCounter, Ident("clk_" + frequency + "_0000MHzMMCM0"))));
            freqCounter++;
        }

        timer = add(timer, ports);
        return MHSFile(Attributes(), timer);
    }

}
