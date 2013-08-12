package de.hopp.generator.backends.board.zed;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.Model.*;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.board.zed.gpio.Gpio;
import de.hopp.generator.backends.workflow.ise.sdk.DriverVersions;
import de.hopp.generator.frontend.ETHERNETPos;
import de.hopp.generator.frontend.GPIOPos;
import de.hopp.generator.model.MProcedure;
import de.hopp.generator.parser.MHS;
import de.hopp.generator.parser.MHSFile;

public class SDK extends de.hopp.generator.backends.workflow.ise.sdk.SDKGenerator {

    private final String proc_inst = "ps7_cortexa9_0";

    public SDK(Configuration config, DriverVersions versions, ErrorCollection errors) {
        super(config, versions, errors);
    }

    @Override
    public void visit(GPIOPos term) {
        addGPIO(Gpio.fromString(term.name().term()).getInstance(), term.callback().termCode());
    }

    @Override
    protected MHSFile getDefaultMSS() {
        return MHS.MHSFile(MHS.Attributes(
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("VERSION", MHS.Ident(versions.mss)))
            ), MHS.Block("OS",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_NAME", MHS.Ident("standalone"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_VER", MHS.Ident(versions.mss_os_standalone))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("PROC_INSTANCE", MHS.Ident(proc_inst))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDIN", MHS.Ident("ps7_uart_1"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDOUT", MHS.Ident("ps7_uart_1")))
            ), MHS.Block("PROCESSOR",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("cpu_cortexa9"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_cpu_ca9))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(proc_inst)))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_afi_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_afi_1")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_afi_2")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_afi_3")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ddr_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ddrc_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("devcfg"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_devcfg))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_dev_cfg_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("dmaps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_dmaps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_dma_ns")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("dmaps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_dmaps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_dma_s")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("gpiops"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_gpiops))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_gpio_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_iop_bus_config_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("qspips"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_qspips))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_qspi_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_qspi_linear_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ram_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ram_1")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("scugic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_scugic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_scugic_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("scutimer"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_scutimer))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_scutimer_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("scuwdt"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_scuwdt))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_scuwdt_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_sd_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_slcr_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("ttcps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_ttcps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ttc_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("uartps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_uartps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_uart_1")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("usbps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_usbps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_usb_0")))
            )
        );
    }

    @Override
    protected MHSFile getEthernetDriver(ETHERNETPos term) {
        MHSFile driver = MHS.MHSFile(MHS.Attributes(),
            MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("emacps"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_emacps))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ps7_ethernet_0")))
            )
        );

        return add(driver, getLWIPLibrary(proc_inst, term));
    }

    @Override
    protected MProcedure getAxiWrite() {
        return MProcedure(MDocumentation(Strings(
            "Write a value to an AXI stream."),
            PARAM("val", "Value to be written to the stream."),
            PARAM("target", "Target stream identifier.")
        ), MModifiers(PRIVATE()), MType("int"), "axi_write", MParameters(
            MParameter(VALUE(), MType("int"), "val"), MParameter(VALUE(), MType("int"), "target")
        ), MCode(Strings(
          "log_finer(\"\\nwriting to in-going port %d (value: %d) ...\", target, val);",
          "if(target > " + (axiStreamIdMaster-1) + ") {",
          "    log_error(\"unknown axi stream port %d\", target);",
          "    return 1;",
          "}",
          "// putdfslx(val, target, FSL_NONBLOCKING);",
          "",
          "int rslt = 1;",
          "// fsl_isinvalid(rslt);",
          "log_finer(\" (invalid: %d)\", rslt);",
          "return rslt;"
        ), MQuoteInclude(PRIVATE(), "../constants.h")));
    }

    @Override
    protected MProcedure getAxiRead() {
        return MProcedure(MDocumentation(Strings(
            "Read a value from an AXI stream."),
            PARAM("val", "Pointer to the memory area, where the read value will be stored."),
            PARAM("target", "Target stream identifier.")
        ), MModifiers(PRIVATE()), MType("int"), "axi_read", MParameters(
            MParameter(VALUE(), MPointerType(MType("int")), "val"), MParameter(VALUE(), MType("int"), "target")
        ), MCode(Strings(
            "log_finer(\"\\nreading from out-going port %d ...\", target);",
            "if(target > 0) {",
            "    log_error(\"unknown axi stream port %d\", target);",
            "    return 1;",
            "}",
            "// getdfslx(*val, target, FSL_NONBLOCKING);",
            "",
            "log_finer(\"\\n %d\", *val);",
            "int rslt = 1;",
            "// fsl_isinvalid(rslt);",
            "log_finer(\" (invalid: %d)\", rslt);",
            "return rslt;"
        ), MQuoteInclude(PRIVATE(), "../constants.h")));
    }
}
