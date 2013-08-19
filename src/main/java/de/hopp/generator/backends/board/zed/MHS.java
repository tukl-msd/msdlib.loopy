package de.hopp.generator.backends.board.zed;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.mhs.MHS.*;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.ISEBoard;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.backends.workflow.ise.xps.MHSGenerator;
import de.hopp.generator.model.ETHERNETPos;
import de.hopp.generator.model.PCIEPos;
import de.hopp.generator.model.UARTPos;
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
        // TODO Auto-generated method stub
    }

    @Override
    protected MHSFile getDefault() {
        MHSFile mhs = MHSFile(getAttributes(), getPS7());
        mhs = add(mhs, getAxiInterCon());
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
            // PORT processing_system7_0_PS_SRSTB = processing_system7_0_PS_SRSTB, DIR = I
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_SRSTB", Ident("processing_system7_0_PS_SRSTB")),
                Assignment("DIR", Ident("I"))
            ),
            // PORT processing_system7_0_PS_CLK = processing_system7_0_PS_CLK, DIR = I, SIGIS = CLK
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_CLK", Ident("processing_system7_0_PS_CLK")),
                Assignment("DIR", Ident("I")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            // PORT processing_system7_0_PS_PORB = processing_system7_0_PS_PORB, DIR = I
            Attribute(PORT(),
                Assignment("processing_system7_0_PS_PORB", Ident("processing_system7_0_PS_PORB")),
                Assignment("DIR", Ident("I"))
            ),
            // PORT processing_system7_0_DDR_Clk = processing_system7_0_DDR_Clk, DIR = IO, SIGIS = CLK
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Clk", Ident("processing_system7_0_DDR_Clk")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            // PORT processing_system7_0_DDR_Clk_n = processing_system7_0_DDR_Clk_n, DIR = IO, SIGIS = CLK
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Clk_n", Ident("processing_system7_0_DDR_Clk_n")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("CLK"))
            ),
            // PORT processing_system7_0_DDR_CKE = processing_system7_0_DDR_CKE, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CKE", Ident("processing_system7_0_DDR_CKE")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_CS_n = processing_system7_0_DDR_CS_n, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CS_n", Ident("processing_system7_0_DDR_CS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_RAS_n = processing_system7_0_DDR_RAS_n, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_RAS_n", Ident("processing_system7_0_DDR_RAS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_CAS_n = processing_system7_0_DDR_CAS_n, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_CAS_n", Ident("processing_system7_0_DDR_CAS_n")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_WEB_pin = processing_system7_0_DDR_WEB, DIR = O
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_WEB_pin", Ident("processing_system7_0_DDR_WEB")),
                Assignment("DIR", Ident("O"))
            ),
            // PORT processing_system7_0_DDR_BankAddr = processing_system7_0_DDR_BankAddr, DIR = IO, VEC = [2:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_BankAddr", Ident("processing_system7_0_DDR_BankAddr")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(2, 0))
            ),
            // PORT processing_system7_0_DDR_Addr = processing_system7_0_DDR_Addr, DIR = IO, VEC = [14:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_Addr", Ident("processing_system7_0_DDR_Addr")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(14, 0))
            ),
            // PORT processing_system7_0_DDR_ODT = processing_system7_0_DDR_ODT, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_ODT", Ident("processing_system7_0_DDR_ODT")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_DRSTB = processing_system7_0_DDR_DRSTB, DIR = IO, SIGIS = RST
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DRSTB", Ident("processing_system7_0_DDR_DRSTB")),
                Assignment("DIR", Ident("IO")),
                Assignment("SIGIS", Ident("RST"))
            ),
            // PORT processing_system7_0_DDR_DQ = processing_system7_0_DDR_DQ, DIR = IO, VEC = [31:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQ", Ident("processing_system7_0_DDR_DQ")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(31, 0))
            ),
            // PORT processing_system7_0_DDR_DM = processing_system7_0_DDR_DM, DIR = IO, VEC = [3:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DM", Ident("processing_system7_0_DDR_DM")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            // PORT processing_system7_0_DDR_DQS = processing_system7_0_DDR_DQS, DIR = IO, VEC = [3:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQS", Ident("processing_system7_0_DDR_DQS")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            // PORT processing_system7_0_DDR_DQS_n = processing_system7_0_DDR_DQS_n, DIR = IO, VEC = [3:0]
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_DQS_n", Ident("processing_system7_0_DDR_DQS_n")),
                Assignment("DIR", Ident("IO")),
                Assignment("VEC", Range(3, 0))
            ),
            // PORT processing_system7_0_DDR_VRN = processing_system7_0_DDR_VRN, DIR = IO
            Attribute(PORT(),
                Assignment("processing_system7_0_DDR_VRN", Ident("processing_system7_0_DDR_VRN")),
                Assignment("DIR", Ident("IO"))
            ),
            // PORT processing_system7_0_DDR_VRP = processing_system7_0_DDR_VRP, DIR = IO
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
            // PARAMETER INSTANCE = processing_system7_0
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("processing_system7_0"))),
            // PARAMETER HW_VER = 4.02.a
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.ps7))),
            // PARAMETER C_DDR_RAM_HIGHADDR = 0x1FFFFFFF
            Attribute(PARAMETER(), Assignment("C_DDR_RAM_HIGHADDR", MemAddr("0x1FFFFFFF"))),
            // PARAMETER C_USE_M_AXI_GP0 = 1
            Attribute(PARAMETER(), Assignment("C_USE_M_AXI_GP0", Number(1))),
            // PARAMETER C_EN_EMIO_CAN0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CAN0", Number(0))),
            // PARAMETER C_EN_EMIO_CAN1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CAN1", Number(0))),
            // PARAMETER C_EN_EMIO_ENET0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_ENET0", Number(0))),
            // PARAMETER C_EN_EMIO_ENET1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_ENET1", Number(0))),
            // PARAMETER C_EN_EMIO_I2C0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_I2C0", Number(0))),
            // PARAMETER C_EN_EMIO_I2C1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_I2C1", Number(0))),
            // PARAMETER C_EN_EMIO_PJTAG = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_PJTAG", Number(0))),
            // PARAMETER C_EN_EMIO_SDIO0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SDIO0", Number(0))),
            // PARAMETER C_EN_EMIO_CD_SDIO0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CD_SDIO0", Number(0))),
            // PARAMETER C_EN_EMIO_WP_SDIO0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WP_SDIO0", Number(0))),
            // PARAMETER C_EN_EMIO_SDIO1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SDIO1", Number(0))),
            // PARAMETER C_EN_EMIO_CD_SDIO1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_CD_SDIO1", Number(0))),
            // PARAMETER C_EN_EMIO_WP_SDIO1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WP_SDIO1", Number(0))),
            // PARAMETER C_EN_EMIO_SPI0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SPI0", Number(0))),
            // PARAMETER C_EN_EMIO_SPI1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SPI1", Number(0))),
            // PARAMETER C_EN_EMIO_SRAM_INT = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_SRAM_INT", Number(0))),
            // PARAMETER C_EN_EMIO_TRACE = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TRACE", Number(0))),
            // PARAMETER C_EN_EMIO_TTC0 = 1
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TTC0", Number(1))),
            // PARAMETER C_EN_EMIO_TTC1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_TTC1", Number(0))),
            // PARAMETER C_EN_EMIO_UART0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_UART0", Number(0))),
            // PARAMETER C_EN_EMIO_UART1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_UART1", Number(0))),
            // PARAMETER C_EN_EMIO_MODEM_UART0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_MODEM_UART0", Number(0))),
            // PARAMETER C_EN_EMIO_MODEM_UART1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_MODEM_UART1", Number(0))),
            // PARAMETER C_EN_EMIO_WDT = 0
            Attribute(PARAMETER(), Assignment("C_EN_EMIO_WDT", Number(0))),
            // PARAMETER C_EMIO_GPIO_WIDTH = 64
            Attribute(PARAMETER(), Assignment("C_EMIO_GPIO_WIDTH", Number(64))),
            // PARAMETER C_EN_QSPI = 1
            Attribute(PARAMETER(), Assignment("C_EN_QSPI", Number(1))),
            // PARAMETER C_EN_SMC = 0
            Attribute(PARAMETER(), Assignment("C_EN_SMC", Number(0))),
            // PARAMETER C_EN_CAN0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_CAN0", Number(0))),
            // PARAMETER C_EN_CAN1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_CAN1", Number(0))),
            // PARAMETER C_EN_ENET0 = 1
            Attribute(PARAMETER(), Assignment("C_EN_ENET0", Number(1))),
            // PARAMETER C_EN_ENET1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_ENET1", Number(0))),
            // PARAMETER C_EN_I2C0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_I2C0", Number(0))),
            // PARAMETER C_EN_I2C1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_I2C1", Number(0))),
            // PARAMETER C_EN_PJTAG = 0
            Attribute(PARAMETER(), Assignment("C_EN_PJTAG", Number(0))),
            // PARAMETER C_EN_SDIO0 = 1
            Attribute(PARAMETER(), Assignment("C_EN_SDIO0", Number(1))),
            // PARAMETER C_EN_SDIO1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_SDIO1", Number(0))),
            // PARAMETER C_EN_SPI0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_SPI0", Number(0))),
            // PARAMETER C_EN_SPI1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_SPI1", Number(0))),
            // PARAMETER C_EN_TRACE = 0
            Attribute(PARAMETER(), Assignment("C_EN_TRACE", Number(0))),
            // PARAMETER C_EN_TTC0 = 1
            Attribute(PARAMETER(), Assignment("C_EN_TTC0", Number(1))),
            // PARAMETER C_EN_TTC1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_TTC1", Number(0))),
            // PARAMETER C_EN_UART0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_UART0", Number(0))),
            // PARAMETER C_EN_UART1 = 1
            Attribute(PARAMETER(), Assignment("C_EN_UART1", Number(1))),
            // PARAMETER C_EN_MODEM_UART0 = 0
            Attribute(PARAMETER(), Assignment("C_EN_MODEM_UART0", Number(0))),
            // PARAMETER C_EN_MODEM_UART1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_MODEM_UART1", Number(0))),
            // PARAMETER C_EN_USB0 = 1
            Attribute(PARAMETER(), Assignment("C_EN_USB0", Number(1))),
            // PARAMETER C_EN_USB1 = 0
            Attribute(PARAMETER(), Assignment("C_EN_USB1", Number(0))),
            // PARAMETER C_EN_WDT = 0
            Attribute(PARAMETER(), Assignment("C_EN_WDT", Number(0))),
            // PARAMETER C_EN_DDR = 1
            Attribute(PARAMETER(), Assignment("C_EN_DDR", Number(1))),
            // PARAMETER C_EN_GPIO = 1
            Attribute(PARAMETER(), Assignment("C_EN_GPIO", Number(1))),
            // PARAMETER C_FCLK_CLK0_FREQ = 100000000
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK0_FREQ", Number(100000000))),
            // PARAMETER C_FCLK_CLK1_FREQ = 142857132
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK1_FREQ", Number(142857132))),
            // PARAMETER C_FCLK_CLK2_FREQ = 50000000
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK2_FREQ", Number(50000000))),
            // PARAMETER C_FCLK_CLK3_FREQ = 50000000
            Attribute(PARAMETER(), Assignment("C_FCLK_CLK3_FREQ", Number(50000000))),
            // BUS_INTERFACE M_AXI_GP0 = axi4lite_0
            Attribute(BUS_IF(), Assignment("M_AXI_GP0", Ident("axi4lite_0"))),
            // PORT MIO = processing_system7_0_MIO
            Attribute(PORT(), Assignment("MIO", Ident("processing_system7_0_MIO"))),
            // PORT PS_SRSTB = processing_system7_0_PS_SRSTB
            Attribute(PORT(), Assignment("PS_SRSTB", Ident("processing_system7_0_PS_SRSTB"))),
            // PORT PS_CLK = processing_system7_0_PS_CLK
            Attribute(PORT(), Assignment("PS_CLK", Ident("processing_system7_0_PS_CLK"))),
            // PORT PS_PORB = processing_system7_0_PS_PORB
            Attribute(PORT(), Assignment("PS_PORB", Ident("processing_system7_0_PS_PORB"))),
            // PORT DDR_Clk = processing_system7_0_DDR_Clk
            Attribute(PORT(), Assignment("DDR_Clk", Ident("processing_system7_0_DDR_Clk"))),
            // PORT DDR_Clk_n = processing_system7_0_DDR_Clk_n
            Attribute(PORT(), Assignment("DDR_Clk_n", Ident("processing_system7_0_DDR_Clk_n"))),
            // PORT DDR_CKE = processing_system7_0_DDR_CKE
            Attribute(PORT(), Assignment("DDR_CKE", Ident("processing_system7_0_DDR_CKE"))),
            // PORT DDR_CS_n = processing_system7_0_DDR_CS_n
            Attribute(PORT(), Assignment("DDR_CS_n", Ident("processing_system7_0_DDR_CS_n"))),
            // PORT DDR_RAS_n = processing_system7_0_DDR_RAS_n
            Attribute(PORT(), Assignment("DDR_RAS_n", Ident("processing_system7_0_DDR_RAS_n"))),
            // PORT DDR_CAS_n = processing_system7_0_DDR_CAS_n
            Attribute(PORT(), Assignment("DDR_CAS_n", Ident("processing_system7_0_DDR_CAS_n"))),
            // PORT DDR_WEB = processing_system7_0_DDR_WEB
            Attribute(PORT(), Assignment("DDR_WEB", Ident("processing_system7_0_DDR_WEB"))),
            // PORT DDR_BankAddr = processing_system7_0_DDR_BankAddr
            Attribute(PORT(), Assignment("DDR_BankAddr", Ident("processing_system7_0_DDR_BankAddr"))),
            // PORT DDR_Addr = processing_system7_0_DDR_Addr
            Attribute(PORT(), Assignment("DDR_Addr", Ident("processing_system7_0_DDR_Addr"))),
            // PORT DDR_ODT = processing_system7_0_DDR_ODT
            Attribute(PORT(), Assignment("DDR_ODT", Ident("processing_system7_0_DDR_ODT"))),
            // PORT DDR_DRSTB = processing_system7_0_DDR_DRSTB
            Attribute(PORT(), Assignment("DDR_DRSTB", Ident("processing_system7_0_DDR_DRSTB"))),
            // PORT DDR_DQ = processing_system7_0_DDR_DQ
            Attribute(PORT(), Assignment("DDR_DQ", Ident("processing_system7_0_DDR_DQ"))),
            // PORT DDR_DM = processing_system7_0_DDR_DM
            Attribute(PORT(), Assignment("DDR_DM", Ident("processing_system7_0_DDR_DM"))),
            // PORT DDR_DQS = processing_system7_0_DDR_DQS
            Attribute(PORT(), Assignment("DDR_DQS", Ident("processing_system7_0_DDR_DQS"))),
            // PORT DDR_DQS_n = processing_system7_0_DDR_DQS_n
            Attribute(PORT(), Assignment("DDR_DQS_n", Ident("processing_system7_0_DDR_DQS_n"))),
            // PORT DDR_VRN = processing_system7_0_DDR_VRN
            Attribute(PORT(), Assignment("DDR_VRN", Ident("processing_system7_0_DDR_VRN"))),
            // PORT DDR_VRP = processing_system7_0_DDR_VRP
            Attribute(PORT(), Assignment("DDR_VRP", Ident("processing_system7_0_DDR_VRP"))),
            // PORT FCLK_CLK0 = processing_system7_0_FCLK_CLK0
            Attribute(PORT(), Assignment("FCLK_CLK0", Ident("processing_system7_0_FCLK_CLK0"))),
            // PORT FCLK_RESET0_N = processing_system7_0_FCLK_RESET0_N_0
            Attribute(PORT(), Assignment("FCLK_RESET0_N", Ident("processing_system7_0_FCLK_RESET0_N_0"))),
            // PORT M_AXI_GP0_ACLK = processing_system7_0_FCLK_CLK0
            Attribute(PORT(), Assignment("M_AXI_GP0_ACLK", Ident("processing_system7_0_FCLK_CLK0"))),
            // PORT IRQ_F2P = BTNs_5Bits_IP2INTC_Irpt & SWs_8Bits_IP2INTC_Irpt & LEDs_8Bits_IP2INTC_Irpt & axi_timer_0_Interrupt
            Attribute(PORT(), Assignment("IRQ_F2P", intrCntrlPorts))
        );
    }

    private MHSFile getAxiInterCon() {
        return MHSFile(Attributes(), Block("axi_interconnect",Attributes(
            Attribute(PARAMETER(), Assignment("INSTANCE", Ident("axi4lite_0"))),
            Attribute(PARAMETER(), Assignment("HW_VER", Ident(versions.axi_interconnect))),
            Attribute(PARAMETER(), Assignment("C_INTERCONNECT_CONNECTIVITY_MODE", Number(0))),
            Attribute(PORT(), Assignment("interconnect_aclk", Ident("processing_system7_0_FCLK_CLK0"))),
            Attribute(PORT(), Assignment("INTERCONNECT_ARESETN", Ident("processing_system7_0_FCLK_RESET0_N_0")))
        )));
    }

}
