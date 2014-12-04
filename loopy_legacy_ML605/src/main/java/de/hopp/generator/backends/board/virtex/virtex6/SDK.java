package de.hopp.generator.backends.board.virtex.virtex6;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.cpp.CPP.*;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.board.virtex.virtex6.gpio.Gpio;
import de.hopp.generator.backends.workflow.ise.sdk.DriverVersions;
import de.hopp.generator.model.ETHERNETPos;
import de.hopp.generator.model.GPIOPos;
import de.hopp.generator.model.cpp.MProcedure;
import de.hopp.generator.model.mhs.MHS;
import de.hopp.generator.model.mhs.MHSFile;

/**
 * Basic SDK generator for a Xilinx Virtex 6 board.
 *
 * Though supporting several minor ISE versions, no modifications of the sources for the SDK
 * build are required except version numbers of the drivers in the .mss file.
 * This SDK generator can therefore be used for multiple ISE versions.
 *
 * @author Thomas Fischer
 *
 */
public class SDK extends de.hopp.generator.backends.workflow.ise.sdk.SDKGenerator {

    private final String proc_inst = "microblaze_0";

    public SDK(Configuration config, DriverVersions versions, ErrorCollection errors) {
        super(config, versions, errors);
    }

    @Override
    public void visit(GPIOPos term) {
        addGPIO(Gpio.fromString(term.name().term()).getInstance(), term.callback().termCode());
    }

    @Override
    protected MHSFile getDefaultMSS() {
        return MHS.MHSFile(
            MHS.Attributes(
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("VERSION", MHS.Ident(versions.mss)))
            ), MHS.Block("OS",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_NAME", MHS.Ident("standalone"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_VER", MHS.Ident(versions.mss_os_standalone))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("PROC_INSTANCE", MHS.Ident(proc_inst))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDIN", MHS.Ident("rs232_uart_1"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDOUT", MHS.Ident("rs232_uart_1")))
            ), MHS.Block("PROCESSOR",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("cpu"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_cpu_mb))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(proc_inst)))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("tmrctr"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_timer_ctr))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("axi_timer_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("v6_ddrx"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_v6_ddrx))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ddr3_sdram")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("bram"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_bram))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("microblaze_0_d_bram_ctrl")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("bram"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_bram))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("microblaze_0_i_bram_ctrl")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("intc"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_intc))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("microblaze_0_intc")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("uartlite"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_uartlite))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("debug_module")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("uartlite"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_uartlite))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("rs232_uart_1")))
            )
        );
    }

    @Override
    protected MHSFile getEthernetDriver(ETHERNETPos term) {
        MHSFile driver = MHS.MHSFile(MHS.Attributes(), MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("emaclite"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_ethernetlite))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ethernet_lite")))
        ));

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
          "putdfslx(val, target, FSL_NONBLOCKING);",
          "",
          "int rslt = 1;",
          "fsl_isinvalid(rslt);",
          "log_finer(\" (invalid: %d)\", rslt);",
          "return rslt;"
        ), MQuoteInclude(PRIVATE(), "fsl.h"), MQuoteInclude(PRIVATE(), "../constants.h")));
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
            "if(target > " + (axiStreamIdSlave-1) + ") {",
            "    log_error(\"unknown axi stream port %d\", target);",
            "    return 1;",
            "}",
            "getdfslx(*val, target, FSL_NONBLOCKING);",
            "",
            "log_finer(\"\\n %d\", *val);",
            "int rslt = 1;",
            "fsl_isinvalid(rslt);",
            "log_finer(\" (invalid: %d)\", rslt);",
            "return rslt;"
        ), MQuoteInclude(PRIVATE(), "fsl.h"), MQuoteInclude(PRIVATE(), "../constants.h")));
    }
}
