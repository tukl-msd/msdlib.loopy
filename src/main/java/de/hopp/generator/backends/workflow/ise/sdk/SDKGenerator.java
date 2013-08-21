package de.hopp.generator.backends.workflow.ise.sdk;

import static de.hopp.generator.backends.workflow.ise.ISEUtils.sdkAppDir;
import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.cpp.CPP.*;
import static de.hopp.generator.utils.BoardUtils.*;
import static de.hopp.generator.utils.CPPUtils.add;
import static de.hopp.generator.utils.CPPUtils.addLines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.model.*;
import de.hopp.generator.model.BDLFilePos.Visitor;
import de.hopp.generator.model.cpp.MCode;
import de.hopp.generator.model.cpp.MFile;
import de.hopp.generator.model.cpp.MInclude;
import de.hopp.generator.model.cpp.MProcedure;
import de.hopp.generator.model.cpp.Strings;
import de.hopp.generator.model.mhs.Block;
import de.hopp.generator.model.mhs.MHS;
import de.hopp.generator.model.mhs.MHSFile;
/**
 * Generation backend for Xilinx SDK.
 *
 * This backend is responsible for board-dependent files required to
 * build up the .elf file.
 * This includes generation of the non-generic, board-dependent sources
 * (i.e. constants and components files) as well as a deployment list
 * of several generic, board-dependent sources (e.g. GPIO devices).
 *
 *
 *
 * Since there seem to be no fundamental changes in the SDK since 14.1,
 * only one backend is provided here, which is assumed to be compatible with all
 * generated bitfiles. If this proves not to be the case, rename the SDK accordingly
 * and provide a structure similar to the XPS backends.
 *
 * @author Thomas Fischer
 */
public abstract class SDKGenerator extends Visitor<NE> implements SDK {

    protected final ErrorCollection errors;
    protected final DriverVersions versions;

    // target folders
    protected final File targetSrc;

    // generic files to copy
    protected Map<File, File> deployFiles;

    // generated files
    protected MFile components;
    protected MFile constants;
    protected MFile scheduler;
    protected MHSFile mssFile;
    protected String lScript;

    // parts of the generated files
    protected MProcedure init;
    protected MProcedure reset;

    // counter variables
    protected int axiStreamIdMaster = 0;
    protected int axiStreamIdSlave  = 0;

    protected int gpiCount = 0;
    protected int gpoCount = 0;

    protected final int DHCP_MAX_ATTEMPTS = 10;

    public void generate(BDLFilePos board) {
        visit(board);
    }

    public MFile getComponents() {
        return components;
    }

    public MFile getConstants() {
        return constants;
    }

    public MFile getScheduler() {
        return scheduler;
    }

    public MHSFile getMSS() {
        return mssFile;
    }

    public String getLScript() {
        return lScript;
    }

    public Map<File, File> getFiles() {
        return deployFiles;
    }

    public SDKGenerator(Configuration config, DriverVersions versions, ErrorCollection errors) {
        this.errors = errors;
        this.versions = versions;
//        this.board = (ISEBoard)config.board();


        // set src directory
        targetSrc = new File(sdkAppDir(config), "src");

        // setup files and methods
        deployFiles = new HashMap<File, File>();
        setupFiles();
        setupMethods();
        setupMSS();
    }

    private void setupFiles() {
        components = MFile(MDocumentation(Strings(
                "Contains component-specific initialisation and processing procedures."
            )), "components", new File(targetSrc, "components").getPath(), MPreProcDirs(),
            MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
        constants  = MFile(MDocumentation(Strings(
                "Defines several constants used by the server.",
                "This includes medium-specific configuration."
            )), "constants", targetSrc.getPath(), MPreProcDirs(
                // TODO do this too, using the debug enum
                MDef(MDocumentation(Strings("Severity value of errors.")),
                    MModifiers(PUBLIC()), "SEVERITY_ERROR", "0"),
                MDef(MDocumentation(Strings("Severity value of warnings.")),
                    MModifiers(PUBLIC()), "SEVERITY_WARN", "1"),
                MDef(MDocumentation(Strings("Severity value of info messages.")),
                    MModifiers(PUBLIC()), "SEVERITY_INFO", "2"),
                MDef(MDocumentation(Strings("Severity value of debug messages.")),
                    MModifiers(PUBLIC()), "SEVERITY_FINE", "3"),
                MDef(MDocumentation(Strings("Severity value of finer debug messages.")),
                    MModifiers(PUBLIC()), "SEVERITY_FINER", "4"),
                MDef(MDocumentation(Strings("Severity value of finest debug messages.")),
                    MModifiers(PUBLIC()), "SEVERITY_FINEST", "5", MBracketInclude(PUBLIC(), "stdio.h"))
            ),
            MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
    }

    private void setupMethods() {
        init  = MProcedure(MDocumentation(Strings(
                "Initialises all components on this board.",
                "This includes gpio components and user-defined IPCores,",
                "but not the communication medium this board is attached with."
            )), MModifiers(), MVoid(), "init_components", MParameters(), MCode(Strings("int status;")));
        reset = MProcedure(MDocumentation(Strings(
                "Resets all components in this board.",
                "This includes gpio components and user-defined IPCores,",
                "but not the communication medium, this board is attached with."
            )), MModifiers(), MVoid(), "reset_components", MParameters(), MCode(Strings()));
    }

    private void setupMSS() {
        mssFile = getDefaultMSS();
    }

    protected abstract MHSFile getDefaultMSS();

    // We assume all imports to be accumulated at the parser
    public void visit(ImportsPos  term) { }
    public void visit(OptionsPos  term) { }

    public void visit(BDLFilePos board) {

        LogSeverity severity = getLogSeverity(board.logs().board());

        // add debug constant
        // FIXME has to be more complex than a single int. Need to encode where to send debug messages
        // TODO do this too, using the debug enum
        int debug = severity.Switch(new LogSeverity.Switch<Integer, NE>() {
            public Integer CaseERROR(ERROR term)   { return 0; }
            public Integer CaseWARN(WARN term)     { return 1; }
            public Integer CaseINFO(INFO term)     { return 2; }
            public Integer CaseFINE(FINE term)     { return 3; }
            public Integer CaseFINER(FINER term)   { return 4; }
            public Integer CaseFINEST(FINEST term) { return 5; }
        });

        addConst("SEVERITY", String.valueOf(debug),
            "Indicates, if additional messages should be logged on the console.");
//        if(debug) addConst("loopy_print(...)", "xil_printf(__VA_ARGS__)",
//            "With the chosen debug level, debug output will be sent over the JTAG cable.",
//            MBracketInclude("stdio.h")
//        );

        addLoggingMacros(debug);

        // FIXME this does NOT regard the maximal protocol size. The size is also ignored within the board-side driver.
        // It is however essential to handle this somehow (maybe that also was the RNG issue with requests above 60k).
        // add queue size constant
        addConst("MAX_OUT_SW_QUEUE_SIZE", String.valueOf(maxOutQueueSize(board)),
                "Maximal size of out-going software queues.");

        // add protocol version constant
        addConst("PROTO_VERSION", "1", "Denotes protocol version, that should be used for sending messages.");

        // visit board components
        visit(board.medium());
        visit(board.scheduler());
        visit(board.gpios());
        visit(board.cores());
        visit(board.insts());

        // add stream count constants
        addConst("IN_STREAM_COUNT", String.valueOf(axiStreamIdMaster), "Number of in-going stream interfaces.");
        addConst("OUT_STREAM_COUNT", String.valueOf(axiStreamIdSlave), "Number of out-going stream interfaces.");

        // add gpio count constants
        addConst("gpi_count", String.valueOf(gpiCount), "Number of gpi components");
        addConst("gpo_count", String.valueOf(gpoCount), "Number of gpo components");

        // add init and reset procedures to the source file
        components = add(components, init);
        components = add(components, reset);

        // add axi read and write procedures

        components = add(components, getAxiWrite());
        components = add(components, getAxiRead());
    }

    protected abstract MProcedure getAxiWrite();
    protected abstract MProcedure getAxiRead();

    /* Log everything up to value, skip afterwards */
    private void addLoggingMacros(int value) {
        final String[] name   = { "error", "warn", "info", "fine", "finer", "finest" };
        final String[] plural = { "errors", "warnings", "info messages", "fine info messages",
                  "finer info messages", "finest info messages" };

        for(int i = 0; i <= value; i++)
            // TODO do this too, using the debug enum
            addConst("log_"+name[i]+"(...)",
                // TODO this is currently very annoying,
                // since it tries to send debug messages despite no open connection,
                // spams connection errors and timeouts icmp messages...
                //"send_debug("+i+", __VA_ARGS__)",
                "xil_printf(\"\\n\"); xil_printf(__VA_ARGS__)",
                "With the chosen debug level, "+plural[i]+
                " will be reported to the host driver over Ethernet.",
                MForwardDecl(PRIVATE(), "void send_debug(unsigned char type, const char *format, ...)"));
        for(int i = value+1; i < name.length; i++)
            addConst("log_"+name[i]+"(...)", "",
                "With the chosen debug level, "+plural[i]+
                " will not be reported to the host driver.");
    }

    public void visit(ETHERNETPos term) {
        // deploy Ethernet medium files
        deployFiles.put(new File("deploy/board/generic/sdk/ethernet.c"), new File(new File(targetSrc, "medium"), "medium.c"));

        // add Ethernet-specific constants
        for(MOptionPos opt : term.opts())
            opt.Switch(new MOptionPos.Switch<String, NE>() {
                public String CaseMACPos(MACPos term) {
                    addMAC(term.val().term()); return null;
                }
                public String CaseIPPos(IPPos term) {
                    addIP("IP", term.val().term(), "IP address"); return null;
                }
                public String CaseMASKPos(MASKPos term) {
                    addIP("MASK", term.val().term(), "network mask"); return null;
                }
                public String CaseGATEPos(GATEPos term) {
                    addIP("GW", term.val().term(), "standard gateway"); return null;
                }
                public String CasePORTIDPos(PORTIDPos term) {
                    addConst("PORT", term.val().term().toString(), "The port for this boards TCP-connection.");
                    return null;
                }
                public String CaseTOUTPos(TOUTPos term) {
                    return null;
                }
                public String CaseDHCPPos(DHCPPos term) {
                    // set dhcp flag
                    addConst("DHCP", "1", "DHCP flag");
                    addConst("DHCP_MAX_ATTEMPTS",
                        term.tout().term().intValue() == -1
                            ? String.valueOf(DHCP_MAX_ATTEMPTS)
                            : term.tout().term().toString(),
                        "DHCP timeout (in seconds)");
                    return null;
                }
            });
        addConst("TIMEOUT", String.valueOf(getTimeout(term)), "Reception timeout for an attempt to free memory.");

        // add Ethernet driver and lwip library to bsp
        mssFile = add(mssFile, getEthernetDriver(term));
    }

    protected abstract MHSFile getEthernetDriver(ETHERNETPos term);

    protected MHSFile getLWIPLibrary(String proc_inst, ETHERNETPos term) {
        Block lwip = MHS.Block("LIBRARY",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("LIBRARY_NAME", MHS.Ident(versions.mss_lwip_lib_name))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("LIBRARY_VER", MHS.Ident(versions.mss_lwip_lib))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("PROC_INSTANCE", MHS.Ident(proc_inst))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("TCP_SND_BUF", MHS.Number(tcp_sndbufSize(term)*4)))
        );

        if(hasDHCP(term)) {
          lwip = lwip.replaceAttributes(lwip.attributes()
              .add(MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("LWIP_DHCP", MHS.Ident("true"))))
              .add(MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DHCP_DOES_ARP_CHECK", MHS.Ident("true"))))
          );
        }

        return MHS.MHSFile(MHS.Attributes(), lwip);
    }

    public void visit(UARTPos term) {

    }

    public void visit(PCIEPos term) {

    }

    protected void addGPIO(GpioComponent gpio, Code callback) {
        deployFiles.put(new File("deploy/board/generic/sdk/gpio.h"), new File(targetSrc, "components/gpio.h"));
        deployFiles.put(new File("deploy/board/generic/sdk/gpio.c"), new File(targetSrc, "components/gpio.c"));

        if(!gpio.isGPI() && !gpio.isGPO()) throw new IllegalStateException(
            "No direction specified for GPIO component " + gpio.id() + "." +
            " This is a bug in the GPIO specification of the component inside the generator." +
            " Please report this error.");

        if(gpio.isGPI()) {
         // add gpi id definition
            components = add(components, MDef(MDocumentation(Strings(
                    "GPI ID of the " + gpio.id() + " component"
                )), MModifiers(PRIVATE()), "gpi_" + gpio.id(), String.valueOf(gpiCount)));

            // add exception handler method
            components = add(components, createExceptionHandler(gpio, callback));

            // generate code for init method
            try {
                init = addLines(init, initGPI(gpio));
            } catch (ParserError e) { errors.addError(e); }

            // increment gpi count
            gpiCount++;
        } else {
            // add gpo id definition
            components = add(components, MDef(MDocumentation(Strings(
                "GPO ID of the " + gpio.id() + " component"
            )), MModifiers(PRIVATE()), "gpo_" + gpio.id(), String.valueOf(gpoCount)));

            // generate code for init method
            try {
                init = addLines(init, initGPO(gpio));
            } catch (ParserError e) { errors.addError(e); }

            // increment gpo count
            gpoCount++;
        }

        mssFile = add(mssFile, gpio.getMSS(versions));
    }

    private MProcedure createExceptionHandler(final GpioComponent gpio, Code callback) {
        MCode body = MCode(
            Strings("XGpio *GpioPtr = (XGpio *)CallbackRef;", "").addAll(
                callback.Switch(new Code.Switch<Strings, NE>() {
                    public Strings CaseDEFAULT(DEFAULT term) {
                        return Strings(
                            "// transmit gpi state change to host",
                            "send_gpio(gpi_" + gpio.id() + ", gpio_read(gpi_" + gpio.id() + "));"
                        );
                    }
                    public Strings CaseUSER_DEFINED(USER_DEFINED term) {
                        return Strings(
                            "// user-defined callback code"
                        ).addAll(term.content());
                    }
                })
            ).addAll(Strings("",
                "// Clear the Interrupt",
                "XGpio_InterruptClear(GpioPtr, GPIO_CHANNEL1);"
            ))

        );

        // add default methods for gpio communication
        body = body.replaceNeeded(MIncludes(
            MForwardDecl(PRIVATE(), "int gpio_read(int target)"),
            MForwardDecl(PRIVATE(), "int gpio_write(int target, int val)"),
            MForwardDecl(PRIVATE(), "void send_gpio(unsigned char gid, unsigned char val)")
        ));

        return MProcedure(MDocumentation(Strings(
                "The Xil_ExceptionHandler to be used as callback for the " + gpio.id() + " component.",
                "This handler executes user-defined code and clears the interrupt."
            )), MModifiers(PRIVATE()),
            MVoid(), "GpioHandler_" + gpio.id(), MParameters(
                MParameter(VALUE(), MPointerType(MType("void")), "CallbackRef")
            ), body);
    }

    private MCode initGPI(GpioComponent gpio) throws ParserError {
        String name = gpio.id();
        return MCode(Strings(
            "#if DEBUG",
            "xil_printf(\"\\ninitialise " + name + " ...\");",
            "#endif /* DEBUG */",
            "status = XGpio_Initialize(&gpi_components[gpi_" + name + "], " + gpio.deviceID() + ");",
            "if(status != XST_SUCCESS) {",
            "#if DEBUG",
            "    xil_printf(\" error setting up " + name + ": %d\", status);",
            "#endif /* DEBUG */",
            "    return;",
            "}",
            "XGpio_SetDataDirection(&gpi_components[gpi_" + name + "], GPIO_CHANNEL1, 0x0);",
            "status = GpioIntrSetup(&gpi_components[gpi_" + name + "], " + gpio.deviceID() + ",",
            "    " + gpio.deviceIntrChannel() + ", GPIO_CHANNEL1, GpioHandler_" + name + ");",
            "if(status != XST_SUCCESS) {",
            "#if DEBUG",
            "    xil_printf(\" error setting up " + name + ": %d\", status);",
            "#endif /* DEBUG */",
            "    return;",
            "}",
            "#if DEBUG",
            "    xil_printf(\" done\");",
            "#endif /* DEBUG */",
            ""
        ), MQuoteInclude(PRIVATE(), "gpio.h"), MQuoteInclude(PRIVATE(), "xparameters.h"));
    }

    private MCode initGPO(GpioComponent gpio) throws ParserError {
        String name = gpio.id();
        return MCode(Strings(
            "#if DEBUG",
            "    xil_printf(\"\\ninitialise " + name + " ...\");",
            "#endif /* DEBUG */",
            "status = XGpio_Initialize(&gpo_components[gpo_" + name + "], " + gpio.deviceID() + ");",
            "if(status != XST_SUCCESS) {",
            "#if DEBUG",
            "    xil_printf(\" error setting up " + name + ": %d\", status);",
            "#endif /* DEBUG */",
            "    return;",
            "}",
            "XGpio_SetDataDirection(&gpo_components[gpo_" + name + "], GPIO_CHANNEL1, 0x0);",
            "#if DEBUG",
            "    xil_printf(\" done\");",
            "#endif /* DEBUG */",
            ""
        ), MQuoteInclude(PRIVATE(), "gpio.h"), MQuoteInclude(PRIVATE(), "xparameters.h"));
    }

//    private String hwVersion(GPIO gpio) throws ParserError {
//        if(gpio.name().equals("leds"))          return version_gpio_leds;
//        else if(gpio.name().equals("switches")) return version_gpio_switches;
//        else if(gpio.name().equals("buttons"))  return version_gpio_buttons;
//
//        throw new ParserError("Unknown GPIO device " + gpio.name() + " for Virtex6", gpio.pos());
//    }

    public void visit(final CPUAxisPos axis) {
        AXIPos port = getPort(axis);

        int width = getWidth(axis);
        boolean d = port.direction() instanceof INPos;
//        boolean up = ((width < 32) && !d) || ((width > 32) && d);

        String axisGroup   = d ? "m" + axiStreamIdMaster : "s" + axiStreamIdSlave;

        if(getHWQueueSize(axis) > 0) mssFile = add(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup + "_queue")))
        ));

        if(width < 32) mssFile = add(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup + "_mux")))
        ));
        else if(width > 32) mssFile = add(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(versions.mss_generic))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup + "_mux")))
        ));

        port.direction().termDirection().Switch(new Direction.Switch<Boolean, NE>() {
            public Boolean CaseIN(IN term) {
                addWriteStream(axis); return null;
            }
            public Boolean CaseOUT(OUT term) {
                addReadStream(axis); return null;
            }
            public Boolean CaseDUAL(DUAL term) {
                addWriteStream(axis); addReadStream(axis); return null;
            }
        });
    }

    private void addWriteStream(CPUAxisPos axis) {
        if(axiStreamIdMaster>15) {
            errors.addError(new ParserError("too many writing AXI stream interfaces for Virtex6", "", -1));
            return;
        }

        init = addLines(init, MCode(
            Strings("inQueue[" + axiStreamIdMaster + "] = createQueue(" + getSWQueueSize32(axis) + ");"),
            MQuoteInclude(PRIVATE(), "../io.h")
        ));
        axiStreamIdMaster++;
    }

    private void addReadStream(CPUAxisPos axis) {
        if(axiStreamIdSlave>15) {
            errors.addError(new ParserError("too many reading AXI stream interfaces for Virtex6", "", -1));
            return;
        }

        init = addLines(init, MCode(
            Strings("outQueueCap[" + axiStreamIdSlave + "] = " + getSWQueueSize32(axis) + ";"),
            MQuoteInclude(PRIVATE(), "../io.h")
        ));

        init = addLines(init, MCode(
            Strings("isPolling[" + axiStreamIdSlave + "] = " + (isPolling(axis) ? 1 : 0) + ";"),
            MQuoteInclude(PRIVATE(), "../io.h")
        ));

        init = addLines(init, MCode(
            Strings("pollCount[" + axiStreamIdSlave + "] = " + getPollingCount32(axis) + ";"),
            MQuoteInclude(PRIVATE(), "../io.h")
        ));

        axiStreamIdSlave++;
    }

    // list types
    public void visit(GPIOsPos     term) { for(    GPIOPos gpio : term) visit(gpio); }
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }
    public void visit(BindingsPos  term) { for( BindingPos bind : term) visit(bind); }
    public void visit(MOptionsPos  term) { for( MOptionPos opt  : term) visit(opt);  }

    // general (handled before this visitor)
    public void visit(ImportPos term)  { }

    // scheduler (irrelevant for host-side driver
    public void visit(SchedulerPos term) {
        if(term.code() instanceof DEFAULTPos) scheduler = MFile(MDocumentation(Strings(
                "A primitive scheduler.",
                "Reads values from the medium, shifts values between Microblaze and VHDL components,",
                "and writes results back to the medium."
            ), AUTHOR("Thomas Fischer"), SINCE("18.02.2013")
            ), "scheduler", targetSrc.getPath(), MPreProcDirs(), MStructs(), MEnums(), MAttributes(), MProcedures(
                MProcedure(
                    MDocumentation(Strings(
                        "Starts the scheduling loop.",
                        "The scheduling loop performs the following actions in each iteration:",
                        " - read and process messages from the medium",
                        " - write values from Microblaze input queue to hardware input queue for each input stream",
                        " - write values from hardware output queue to the medium (caches several values before sending)"
                    )), MModifiers(), MVoid(), "schedule", MParameters(), defaultScheduler()
                )
            )
         );
        else scheduler = MFile(MDocumentation(Strings()
            ), "scheduler", targetSrc.getPath(), MPreProcDirs(), MStructs(), MEnums(), MAttributes(), MProcedures(
                MProcedure(
                    MDocumentation(Strings()), MModifiers(), MVoid(), "schedule", MParameters(), MCode(
                        Strings().addAll(((USER_DEFINED)term.code().term()).content()),
                        MQuoteInclude(PRIVATE(), "constants.h"),
                        MQuoteInclude(PRIVATE(), "queueUntyped.h"),
                        MQuoteInclude(PRIVATE(), "io.h"),
                        MForwardDecl(PRIVATE(), "int medium_read()"),
                        MForwardDecl(PRIVATE(), "int axi_write ( int val, int target )"),
                        MForwardDecl(PRIVATE(), "int axi_read ( int *val, int target )")
                    )
                )
            )
        );
    }

    private MCode defaultScheduler() {
        return MCode(
            Strings(
                "unsigned int pid;",
                "unsigned int i;",
                "",
                "while(1) {",
                "    // receive all available packages from the interface",
                "    // esp stores data packages in sw queue",
                "    while(medium_read()) { }",
                "    ",
                "    // write data from sw queue to hw queue (if possible)",
                "    for(pid = 0; pid < IN_STREAM_COUNT; pid++) {",
                "        for(i = 0; i < inQueue[pid]->cap; i++) {",
                "            // go to next port if the sw queue is empty",
                "            if(inQueue[pid]->size == 0) break;",
                "            ",
                "            // try to write, skip if the hw queue is full",
                "            if(axi_write(peek(inQueue[pid]), pid)) {",
                "                  log_fine(\"failed to write to AXI stream\");",
                "                break;",
                "            }",
                "            ",
                "            // remove the read value from the queue",
                "            take(inQueue[pid]);",
                "            ",
                "            // if the queue was full beforehand, poll",
                "            if(inQueue[pid]->size == inQueue[pid]->cap - 1) send_poll(pid);",
                "        }",
                "    }",
                "    ",
                "    // read data from hw queue (if available) and cache in sw queue",
                "    // flush sw queue, if it's full or the hw queue is empty",
                "    for(pid = 0; pid < OUT_STREAM_COUNT; pid++) {",
                "        for(i = 0; i < outQueueCap[pid] && ((!isPolling[pid]) || pollCount[pid] > 0); i++) {",
//                "            int val = 0;",
//                "            ",
                "            // try to read, break if if fails",
                "            if(axi_read(&outQueue[outQueueSize], pid)) break;",
                "            ",
                "            // otherwise increment the queue size counter",
//                "            outQueue[outQueueSize] = val;",
                "            outQueueSize++;",
                "            ",
                "            // decrement the poll counter (if the port was polling)",
                "            if(isPolling[pid]) pollCount[pid]--;",
                "        }",
                "        // flush sw queue",
                "        if(flush_queue(pid)) {",
                "            // in this case, sending failed. Terminate (reasons have already been printed)",
                "            xil_printf(\"\\nterminating...\");",
                "            return;",
                "        }",
                "        outQueueSize = 0;",
                "    }",
                "}"
            ),
            MQuoteInclude(PRIVATE(), "constants.h"),
            MQuoteInclude(PRIVATE(), "queueUntyped.h"),
            MQuoteInclude(PRIVATE(), "io.h"),
            MForwardDecl(PRIVATE(), "int medium_read()"),
            MForwardDecl(PRIVATE(), "int axi_write ( int val, int target )"),
            MForwardDecl(PRIVATE(), "int axi_read ( int *val, int target )")
        );
    }

    // code blocks (handled directly when occurring)
    public void visit(DEFAULTPos term)      { }
    public void visit(USER_DEFINEDPos term) { }

    // missing declaration
    public void visit(NONEPos term) { }

    // options (handled directly inside the board or port if occurring)
    public void visit(HWQUEUEPos  arg0) { }
    public void visit(SWQUEUEPos  arg0) { }
    public void visit(BITWIDTHPos term) { }
    public void visit(POLLPos     term) { }

    // logger options
    public void visit(LogsPos     term) { }
    public void visit(CONSOLEPos  term) { }
    public void visit(FILEPos     term) { }

    public void visit(ERRORPos   term) { }
    public void visit(WARNPos    term) { }
    public void visit(INFOPos    term) { }
    public void visit(FINEPos    term) { }
    public void visit(FINERPos   term) { }
    public void visit(FINESTPos  term) { }

    // same goes for medium options
    public void visit(MACPos    term) { }
    public void visit(IPPos     term) { }
    public void visit(MASKPos   term) { }
    public void visit(GATEPos   term) { }
    public void visit(TOUTPos   term) { }
    public void visit(DHCPPos   term) { }
    public void visit(PORTIDPos term) { }

    // cores
    // we do not need to visit cores here, since a class will be created
    // for each instance directly connected to the boards CPU, not for each core
    public void visit(CoresPos term) { }
    public void visit(CorePos  term) { }

    // ports (see above)
    public void visit(PortsPos term) { }
    public void visit(CLKPos   term) { }
    public void visit(RSTPos   term) { }
    public void visit(AXIPos   term) { }
    public void visit(INPos    term) { }
    public void visit(OUTPos   term) { }
    public void visit(DUALPos  term) { }

    // ignore instances here, just visit port bindings
    public void visit(InstancePos term) { visit(term.bind()); }
    // component axis (these get ignored... that's the whole point)
    public void visit(AxisPos     term) { }

    // position
    public void visit(PositionPos term) { }

    // literals
    public void visit(StringsPos term) { }
    public void visit(StringPos  term) { }
    public void visit(IntegerPos term) { }
    public void visit(BooleanPos term) { }

    private void addIP(String id, String ipString, String doc) {
        String[] ip = ipString.split("\\.");
        addConst(id + "_1", ip[0], "The first  8 bits of the " + doc + " of this board.");
        addConst(id + "_2", ip[1], "The second 8 bits of the " + doc + " of this board.");
        addConst(id + "_3", ip[2], "The third  8 bits of the " + doc + " of this board.");
        addConst(id + "_4", ip[3], "The fourth 8 bits of the " + doc + " of this board.");
    }

    private void addMAC(String macString) {
        String[] mac = macString.split(":");
        addConst("MAC_1", "0x" + mac[0], "The first  8 bits of the MAC address of this board.");
        addConst("MAC_2", "0x" + mac[1], "The second 8 bits of the MAC address of this board.");
        addConst("MAC_3", "0x" + mac[2], "The third  8 bits of the MAC address of this board.");
        addConst("MAC_4", "0x" + mac[3], "The fourth 8 bits of the MAC address of this board.");
        addConst("MAC_5", "0x" + mac[4], "The fifth  8 bits of the MAC address of this board.");
        addConst("MAC_6", "0x" + mac[5], "The sixth  8 bits of the MAC address of this board.");
    }

    private void addConst(String id, String val, String doc, MInclude ... needed) {
        constants = add(constants, MDef(MDocumentation(Strings(doc)), MModifiers(PUBLIC()), id, val, needed));
    }
}
