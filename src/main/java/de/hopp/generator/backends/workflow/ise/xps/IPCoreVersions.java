package de.hopp.generator.backends.workflow.ise.xps;

public class IPCoreVersions {

    public static IPCoreVersions ISE14_1 = create14_1();
    public static IPCoreVersions ISE14_2 = create14_2();
    public static IPCoreVersions ISE14_3 = create14_3();
    public static IPCoreVersions ISE14_4 = create14_4();
    public static IPCoreVersions ISE14_5 = create14_5();
    public static IPCoreVersions ISE14_6 = create14_6();


    public String ise;

    // XPS (MHS)
    public String mhs;

    public String microblaze;
    public String ps7;

    public String axi_intc;
    public String axi_interconnect;
    public String axi_timer;
    public String axi_v6_ddrx;
    public String bram_block;
    public String clock_generator;
    public String lmb_bram_if_cntlr;
    public String lmb_v10;
    public String mdm;
    public String proc_sys_reset;

    public String axi_uartlite;
    public String axi_ethernetlite;

    public String gpio_leds;
    public String gpio_buttons;
    public String gpio_switches;

    // SDK (MSS)
    public String mss;

    public String mss_os;
    public String mss_cpu_mb;
    public String mss_cpu_ca9;

    public String mss_intc;
    public String mss_v6_ddrx;
    public String mss_bram_block;
    public String mss_timer_controller;

    public String mss_uartlite;
    public String mss_ethernetlite;
    public String mss_lwip_lib_name;
    public String mss_lwip_lib;

    public String mss_gpio_leds;
    public String mss_gpio_buttons;
    public String mss_gpio_switches;

    public String mss_queue;
    public String mss_resizer;

    private IPCoreVersions() { }

    private static IPCoreVersions create14_1() {
        IPCoreVersions versions =  new IPCoreVersions();

        versions.ise               = "14.1";
        versions.mhs               = "2.1.0";

        versions.microblaze        = "8.30.a";

        versions.axi_intc          = "1.02.a";
        versions.axi_interconnect  = "1.06.a";
        versions.axi_timer         = "1.03.a";
        versions.axi_v6_ddrx       = "1.05.a";
        versions.bram_block        = "1.00.a";
        versions.clock_generator   = "4.03.a";
        versions.lmb_bram_if_cntlr = "3.00.b";
        versions.lmb_v10           = "2.00.b";
        versions.mdm               = "2.00.b";
        versions.proc_sys_reset    = "3.00.a";

        versions.axi_uartlite      = "1.02.a";
        versions.axi_ethernetlite  = "1.01.b";

        versions.gpio_leds         = "1.01.b";
        versions.gpio_buttons      = "1.01.b";
        versions.gpio_switches     = "1.01.b";

        // SDK (MHS)
        versions.mss                   = "2.2.0";

        versions.mss_os                = "3.08.a";
        versions.mss_cpu_mb            = "1.14.a";

        versions.mss_bram_block        = "3.01.a";
        versions.mss_intc              = "2.05.a";
        versions.mss_timer_controller  = "2.04.a";
        versions.mss_v6_ddrx           = "2.00.a";

        versions.mss_uartlite          = "2.00.a";
        versions.mss_ethernetlite      = "3.03.a";

        versions.mss_lwip_lib_name     = "lwip140";
        versions.mss_lwip_lib          = "1.03.a";

        versions.mss_gpio_leds         = "3.00.a";
        versions.mss_gpio_buttons      = "3.00.a";
        versions.mss_gpio_switches     = "3.00.a";

        versions.mss_queue             = "1.00.a";
        versions.mss_resizer           = "1.00.a";

        return versions;
    }

    private static IPCoreVersions create14_2() {
        IPCoreVersions versions = create14_1();
        versions.ise            = "14.2";
        return versions;
    }

    private static IPCoreVersions create14_3() {
        IPCoreVersions versions = create14_2();
        versions.ise            = "14.3";
        return versions;
    }

    private static IPCoreVersions create14_4() {
        IPCoreVersions versions = create14_3();

        versions.ise               = "14.4";

        versions.microblaze        = "8.40.a";
        versions.ps7               = "4.02.a";

        versions.axi_intc          = "1.03.a";
        versions.axi_v6_ddrx       = "1.06.a";
        versions.lmb_bram_if_cntlr = "3.10.c";
        versions.mdm               = "2.10.a";

        return versions;
    }

    private static IPCoreVersions create14_5() {
        IPCoreVersions versions = create14_4();
        versions.ise            = "14.5";
        return versions;
    }

    private static IPCoreVersions create14_6() {
        IPCoreVersions versions = create14_5();

        versions.ise               = "14.6";

        versions.microblaze        = "8.50.b";

        versions.axi_intc          = "1.04.a";

        versions.mss_cpu_ca9       = "1.01.a";

        // FIXME It obviously is a good idea to move board-specific stuff to the board-backend
        // This version list should only contain independent versions
        // This probably means only xsdk and maybe the mss version
        // Other things should be in the board, though they still depend on the ise version
        // So far I'm not sure how to do this with the current interface wrapping
//        versions.ps7_afi           = "1.00.a";
//        versions.ps7_ddr           = "1.00.a";
//        versions.ps7_dev_cfg       = "2.01.a";
//        versions.ps7_dma           = "1.04.a";
//        versions.ps7_eth           = "1.02.a";
//        versions.ps7_gpio          = "1.01.a";
//        versions.ps7_iop_bus_cfg   = "1.00.a";
//        versions.ps7_qspi          = "2.00.a";
//        versions.ps7_qspi_linear   = "1.00.a";
//        versions.ps7_ram           = "1.00.a";
//        versions.ps7_scu           = "1.02.a";
//        versions.ps7_sd            = "1.00.a";
//        versions.ps7_slcr          = "1.00.a";
//        versions.ps7_ttc           = "1.01.a";
//        versions.ps7_uart          = "1.03.a";
//        versions.ps7_usb           = "1.04.a";

        return versions;
    }
}

