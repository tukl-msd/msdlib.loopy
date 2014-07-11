package de.hopp.generator.backends.workflow.ise.sdk;

public class DriverVersions {

    public static DriverVersions ISE14_1 = create14_1();
    public static DriverVersions ISE14_2 = create14_2();
    public static DriverVersions ISE14_3 = create14_3();
    public static DriverVersions ISE14_4 = create14_4();
    public static DriverVersions ISE14_5 = create14_5();
    public static DriverVersions ISE14_6 = create14_6();
	public static DriverVersions ISE14_7 = create14_7();

    public String mss;

    // general, board-independent
    public String mss_os_standalone;

    public String mss_generic;
    public String mss_gpio;

    // lwip library
    public String mss_lwip_lib_name;
    public String mss_lwip_lib;

    // virtex6-specific
    public String mss_cpu_mb;

    public String mss_intc;
    public String mss_v6_ddrx;
    public String mss_bram;
    public String mss_timer_ctr;

    public String mss_uartlite;
    public String mss_ethernetlite;

    // zed-specific
    public String mss_cpu_ca9;

    public String mss_devcfg;
    public String mss_dmaps;
    public String mss_emacps;
    public String mss_gpiops;
    public String mss_qspips;
    public String mss_scugic;
    public String mss_scutimer;
    public String mss_scuwdt;
    public String mss_ttcps;
    public String mss_uartps;
    public String mss_usbps;

    private DriverVersions() { }

    private static DriverVersions create14_1() {
        DriverVersions versions = new DriverVersions();

        versions.mss                   = "2.2.0";

        // general
        versions.mss_os_standalone     = "3.08.a";

        versions.mss_generic           = "1.00.a";
        versions.mss_gpio              = "3.00.a";

        // lwip library
        versions.mss_lwip_lib_name     = "lwip140";
        versions.mss_lwip_lib          = "1.03.a";

        // virtex6-specific
        versions.mss_cpu_mb            = "1.14.a";

        versions.mss_bram              = "3.01.a";
        versions.mss_intc              = "2.05.a";
        versions.mss_timer_ctr         = "2.04.a";
        versions.mss_v6_ddrx           = "2.00.a";

        versions.mss_uartlite          = "2.00.a";
        versions.mss_ethernetlite      = "3.03.a";

        return versions;
    }

    private static DriverVersions create14_2() {
        DriverVersions versions = create14_1();
        return versions;
    }

    private static DriverVersions create14_3() {
        DriverVersions versions = create14_2();
        return versions;
    }

    private static DriverVersions create14_4() {
        DriverVersions versions = create14_3();

        // FIXME It might be a good idea to move board-specific stuff to the board-backend
        // This version list should only contain independent versions
        // This probably means only xsdk and maybe the mss version
        // Other things should be in the board, though they still depend on the ise version
        // So far I'm not sure how to do this with the current interface wrapping

        // a) central position for all core versions (i.e. here)
        //      + only one instance to change
        //      - many, many versions
        // b) each board maintains its own version list for relevant cores
        //      + more readable, easier to see which cores are important
        //      - possible duplication of versions for some boards

        // zed-specific
        versions.mss_cpu_ca9       = "1.01.a";

        versions.mss_devcfg        = "2.01.a";
        versions.mss_dmaps         = "1.04.a";
        versions.mss_gpiops        = "1.01.a";
        versions.mss_qspips        = "2.00.a";
        versions.mss_scugic        = "1.02.a";
        versions.mss_scutimer      = "1.02.a";
        versions.mss_scuwdt        = "1.02.a";
        versions.mss_ttcps         = "1.01.a";

        versions.mss_emacps        = "1.02.a";
        versions.mss_uartps        = "1.03.a";
        versions.mss_usbps         = "1.04.a";

        return versions;
    }

    private static DriverVersions create14_5() {
        DriverVersions versions = create14_4();
        return versions;
    }

    private static DriverVersions create14_6() {
        DriverVersions versions = create14_5();
        return versions;
    }
	
	//FIXME: Adapt this to possible changes in ISE 14.7
	private static DriverVersions create14_7() {
        DriverVersions versions = create14_6();
        return versions;
    }
}
