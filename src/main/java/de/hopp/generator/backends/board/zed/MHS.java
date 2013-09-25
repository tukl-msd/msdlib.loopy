package de.hopp.generator.backends.board.zed;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.mhs.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getDirection;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.Memory;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.backends.workflow.ise.xps.MHSGenerator;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.model.CPUAxisPos;
import de.hopp.generator.model.ETHERNETPos;
import de.hopp.generator.model.PCIEPos;
import de.hopp.generator.model.UARTPos;
import de.hopp.generator.model.mhs.Attribute;
import de.hopp.generator.model.mhs.Attributes;
import de.hopp.generator.model.mhs.Block;
import de.hopp.generator.model.mhs.MHSFile;

public class MHS extends MHSGenerator {

    public MHS(ISEBoard board, IPCoreVersions versions, ErrorCollection errors) {
        super(board, versions, errors);
    }

    public void visit(ETHERNETPos term) {
        // TODO Auto-generated method stub
    }

    public void visit(UARTPos term) {
        // TODO Auto-generated method stub
    }

    public void visit(PCIEPos term) {
        errors.addError(new UsageError("ZedBoard does not support PCIe as communication medium"));
    }

    // GENERAL IDEA:
    // - count number of axi fifo components
    // - we know, each component comes with a master and a slave port
    // - if the count corresponding to the current direction is lower, then the fifo count, use existing fifo
    // - otherwise, add a fifo

    protected int fifoCount = 0;

    protected Block createFIFO() {
        String axisMaster = "M" + fifoCount + "_AXIS";
        String axisSlave  = "S" + fifoCount + "_AXIS";
        String ident      = "axi_fifo_mm_s_" + fifoCount++;

        Memory.Range axi4memRange = board.getMemory().allocateMemory(0xfff);
        Memory.Range memRange = board.getMemory().allocateMemory(0xfff);

        return de.hopp.generator.model.mhs.MHS.Block("axi_fifo_mm_s",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident(ident))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_fifo))),
            Attribute(PARAMETER(), Assignment("C_DATA_INTERFACE_TYPE", Number(0))),
            Attribute(PARAMETER(), Assignment("C_AXI4_BASEADDR", MemAddr(axi4memRange.getBaseAddress()))),
            Attribute(PARAMETER(), Assignment("C_AXI4_HIGHADDR", MemAddr(axi4memRange.getHighAddress()))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr(memRange.getBaseAddress()))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr(memRange.getHighAddress()))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT_S_AXI_MASTERS", Ident("axi_cdma_0.M_AXI"))),
            Attribute(BUS_IF(), Assignment("S_AXI", Ident("axi_interconnect_0"))),
            Attribute(BUS_IF(), Assignment("AXI_STR_TXD", Ident(axisMaster))),
            Attribute(BUS_IF(), Assignment("AXI_STR_RXD", Ident(axisSlave))),
            Attribute(PORT(), Assignment("S_AXI_ACLK", Ident(board.getClock().getClockPort(100))))
        );
    }

    @Override
    protected Attribute createCPUAxisBinding(final CPUAxisPos axis) throws UsageError {
        // if required, add a FIFO to the design (translating from full AXI4 to AXI4 stream)
        boolean direct = direction(getDirection(axis).termDirection());
        System.out.println("direction : " + direct);
        System.out.println("fifo count: " + fifoCount);
        System.out.println("master count: " + axiStreamIdMaster);
        System.out.println("slave count : " + axiStreamIdSlave);
        if((direct && fifoCount <= axiStreamIdMaster) || (!direct && fifoCount <= axiStreamIdSlave))
            mhs = add(mhs, createFIFO());

        return super.createCPUAxisBinding(axis);
    }

    @Override
    protected MHSFile getDefault() {
        MHSFile mhs = MHSFile(getAttributes(), getPS7());
        mhs = add(mhs, getAxiInterconnects());
        mhs = add(mhs, getCDMA());
        return mhs;
    }

    private Attributes getAttributes() {
        // TODO Auto-generated method stub
        return Attributes(
            Attribute(PORT(),
                Assignment("processing_system7_0_MIO", Ident("processing_system7_0_MIO")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(53, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_SRSTB", Ident("processing_system7_0_PS_SRSTB")),
                Assignment("DIR", Ident("I"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_CLK", Ident("processing_system7_0_PS_CLK")),
                Assignment("DIR", Ident("I")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_PORB", Ident("processing_system7_0_PS_PORB")),
                Assignment("DIR", Ident("I"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Clk", Ident("processing_system7_0_DDR_Clk")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Clk_n", Ident("processing_system7_0_DDR_Clk_n")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CKE", Ident("processing_system7_0_DDR_CKE")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CS_n", Ident("processing_system7_0_DDR_CS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_RAS_n", Ident("processing_system7_0_DDR_RAS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CAS_n", Ident("processing_system7_0_DDR_CAS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_WEB_pin", Ident("processing_system7_0_DDR_WEB")),
                Assignment("DIR", Ident("O"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_BankAddr", Ident("processing_system7_0_DDR_BankAddr")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(2, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Addr", Ident("processing_system7_0_DDR_Addr")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(14, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_ODT", Ident("processing_system7_0_DDR_ODT")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DRSTB", Ident("processing_system7_0_DDR_DRSTB")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("RST"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQ", Ident("processing_system7_0_DDR_DQ")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(31, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DM", Ident("processing_system7_0_DDR_DM")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQS", Ident("processing_system7_0_DDR_DQS")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQS_n", Ident("processing_system7_0_DDR_DQS_n")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_VRN", Ident("processing_system7_0_DDR_VRN")),
                Assignment("DIR", Ident("IO"))
            ),
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_VRP", Ident("processing_system7_0_DDR_VRP")),
                Assignment("DIR", Ident("IO"))
            )
        );
    }

    private Block getPS7() {
        // FIXME clocks!!
        // BEGIN processing_system7
        return Block("processing_system7",
            // General setup
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("processing_system7_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.ps7))),
            Attribute(PARAMETER(), Assignment("C_DDR_RAM_HIGHADDR", MemAddr(0x1FFFFFFF))),
            // Used AXI Ports
            Attribute(PARAMETER(), Assignment("C_USE_M_AXI_GP0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_USE_S_AXI_HP0", Number(1))),
            // I/O Peripherals
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CAN0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CAN1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_ENET0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_ENET1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_I2C0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_I2C1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_PJTAG", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SDIO0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CD_SDIO0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WP_SDIO0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SDIO1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CD_SDIO1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WP_SDIO1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SPI0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SPI1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SRAM_INT", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TRACE", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TTC0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TTC1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_UART0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_UART1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_MODEM_UART0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_MODEM_UART1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WDT", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EMIO_GPIO_WIDTH", Number(64))),
            Attribute(PARAMETER(), Assignment("C_EN_QSPI", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_SMC", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_CAN0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_CAN1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_ENET0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_ENET1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_I2C0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_I2C1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_PJTAG", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_SDIO0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_SDIO1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_SPI0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_SPI1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_TRACE", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_TTC0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_TTC1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_UART0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_UART1", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_MODEM_UART0", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_MODEM_UART1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_USB0", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_USB1", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_WDT", Number(0))),
            Attribute(PARAMETER(), Assignment("C_EN_DDR", Number(1))),
            Attribute(PARAMETER(), Assignment("C_EN_GPIO", Number(1))),
            // clock generator frequencies
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK0_FREQ", Number(100000000))),
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK1_FREQ", Number(150000000))),
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK2_FREQ", Number(50000000))),
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK3_FREQ", Number(50000000))),
            // This seems to be necessary for clk and rst ports in general...
            Attribute(PARAMETER(), Assignment("C_USE_CR_FABRIC", Number(1))),
            // TODO No idea what this does and if we need it...
            Attribute(PARAMETER(), Assignment("C_NUM_F2P_INTR_INPUTS", Number(1))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT_S_AXI_HP0_MASTERS", Ident("axi_cdma_0.M_AXI"))),
            // AXI bus interfaces
            Attribute(BUS_IF(), Assignment("M_AXI_GP0", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("S_AXI_HP0", Ident("axi_interconnect_0"))),
            // port connections
            Attribute(PORT(), Assignment("MIO", Ident("processing_system7_0_MIO"))),
            Attribute(PORT(), Assignment("PS_SRSTB", Ident("processing_system7_0_PS_SRSTB"))),
            Attribute(PORT(), Assignment("PS_CLK", Ident("processing_system7_0_PS_CLK"))),
            Attribute(PORT(), Assignment("PS_PORB", Ident("processing_system7_0_PS_PORB"))),
            Attribute(PORT(), Assignment("DDR_Clk", Ident("processing_system7_0_DDR_Clk"))),
            Attribute(PORT(), Assignment("DDR_Clk_n", Ident("processing_system7_0_DDR_Clk_n"))),
            Attribute(PORT(), Assignment("DDR_CKE", Ident("processing_system7_0_DDR_CKE"))),
            Attribute(PORT(), Assignment("DDR_CS_n", Ident("processing_system7_0_DDR_CS_n"))),
            Attribute(PORT(), Assignment("DDR_RAS_n", Ident("processing_system7_0_DDR_RAS_n"))),
            Attribute(PORT(), Assignment("DDR_CAS_n", Ident("processing_system7_0_DDR_CAS_n"))),
            Attribute(PORT(), Assignment("DDR_WEB", Ident("processing_system7_0_DDR_WEB"))),
            Attribute(PORT(), Assignment("DDR_BankAddr", Ident("processing_system7_0_DDR_BankAddr"))),
            Attribute(PORT(), Assignment("DDR_Addr", Ident("processing_system7_0_DDR_Addr"))),
            Attribute(PORT(), Assignment("DDR_ODT", Ident("processing_system7_0_DDR_ODT"))),
            Attribute(PORT(), Assignment("DDR_DRSTB", Ident("processing_system7_0_DDR_DRSTB"))),
            Attribute(PORT(), Assignment("DDR_DQ", Ident("processing_system7_0_DDR_DQ"))),
            Attribute(PORT(), Assignment("DDR_DM", Ident("processing_system7_0_DDR_DM"))),
            Attribute(PORT(), Assignment("DDR_DQS", Ident("processing_system7_0_DDR_DQS"))),
            Attribute(PORT(), Assignment("DDR_DQS_n", Ident("processing_system7_0_DDR_DQS_n"))),
            Attribute(PORT(), Assignment("DDR_VRN", Ident("processing_system7_0_DDR_VRN"))),
            Attribute(PORT(), Assignment("DDR_VRP", Ident("processing_system7_0_DDR_VRP"))),
            Attribute(PORT(), Assignment("FCLK_CLK0", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("FCLK_RESET0_N", Ident(getResetPort()))),
            Attribute(PORT(), Assignment("M_AXI_GP0_ACLK", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("S_AXI_HP0_ACLK", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("IRQ_F2P", intrCntrlPorts))
        );
    }

    private MHSFile getAxiInterconnects() {
        return MHSFile(Attributes(),
            Block("axi_interconnect",Attributes(
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4lite_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_interconnect))),
                Attribute(PARAMETER(), Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(0))),
                Attribute(PORT(), Assignment("INTERCONNECT_ACLK", Ident(board.getClock().getClockPort(100)))),
                Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("M_AXI_GP0_ARESETN")))
            )), Block("axi_interconnect", Attributes(
                Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi_interconnect_0"))),
                Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_interconnect))),
                Attribute(PARAMETER(), Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(1))),
                Attribute(PORT(), Assignment("INTERCONNECT_ACLK", Ident(board.getClock().getClockPort(100)))),
                Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("S_AXI_HP0_ARESETN")))
            ))
        );
    }

    private MHSFile getCDMA() {
        Memory.Range memRange = board.getMemory().allocateMemory(0xffff);
        return MHSFile(Attributes(), Block("axi_cdma",
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi_cdma_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_cdma))),
            Attribute(PARAMETER(), Assignment("C_INCLUDE_SG", Number(0))),
            Attribute(PARAMETER(), Assignment("C_ENABLE_KEYHOLE", Number(0))),
            Attribute(PARAMETER(), Assignment("C_BASEADDR", MemAddr(memRange.getBaseAddress()))),
            Attribute(PARAMETER(), Assignment("C_HIGHADDR", MemAddr(memRange.getHighAddress()))),
            Attribute(BUS_IF(), Assignment("S_AXI_LITE", Ident("axi4lite_0"))),
            Attribute(BUS_IF(), Assignment("M_AXI", Ident("axi_interconnect_0"))),
            Attribute(PORT(), Assignment("M_AXI_ACLK", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("S_AXI_LITE_ACLK", Ident(board.getClock().getClockPort(100)))),
            Attribute(PORT(), Assignment("cdma_introut", Ident("axi_cdma_0_cdma_introut")))
        ));
    }

    @Override
    protected String getResetPort() {
        return "processing_system7_0_FCLK_RESET0_N_0";
    }

    @Override
    protected String getAResetNPort() {
        // FIXME This looks like a horrible idea...
        return getResetPort();
    }

}
