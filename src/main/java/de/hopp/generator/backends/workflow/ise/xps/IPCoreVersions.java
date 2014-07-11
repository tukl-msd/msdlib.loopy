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

    public String axi_cdma;
    public String axi_fifo;
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

    public String gpio;
    public String gpio_leds;
    public String gpio_buttons;
    public String gpio_switches;

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

        versions.gpio              = "1.01.b";
        versions.gpio_leds         = "1.01.b";
        versions.gpio_buttons      = "1.01.b";
        versions.gpio_switches     = "1.01.b";

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
        versions.axi_cdma          = "3.04.a";
        versions.axi_fifo          = "3.00.a";
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

        return versions;
    }
	

		private static IPCoreVersions create14_7() {
        IPCoreVersions versions = create14_6();

        versions.ise               = "14.7";

        versions.microblaze        = "8.50.c";

        return versions;
    }
}

