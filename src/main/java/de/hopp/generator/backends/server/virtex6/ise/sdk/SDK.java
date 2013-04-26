package de.hopp.generator.backends.server.virtex6.ise.sdk;

import static de.hopp.generator.backends.server.virtex6.ise.ISE.sdkSourceDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkAppDir;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.BoardUtils.*;
import static de.hopp.generator.utils.Model.add;
import static de.hopp.generator.utils.Model.addLines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.*;
import de.hopp.generator.frontend.BDLFilePos.Visitor;
import de.hopp.generator.model.MCode;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MProcedure;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.MHS;
import de.hopp.generator.parser.MHSFile;

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
public class SDK extends Visitor<NE> {
    
    private static File gpioSrc = new File(sdkSourceDir, "gpio");
    
    private ErrorCollection errors;
    
    // target folders
    private File targetDir;
    private File targetSrc;
    
    // generic files to copy
    protected Map<File, File> deployFiles;
    
    // generated files
    protected MFile components;
    protected MFile constants;
    protected MFile scheduler;
    protected MHSFile mssFile;
    
    // parts of the generated files
    private MProcedure init;
    private MProcedure reset;
    private MProcedure axi_write;
    private MProcedure axi_read;
    
    // counter variables
    private int axiStreamIdMaster = 0;
    private int axiStreamIdSlave  = 0;
    
    private int gpiCount = 0;
    private int gpoCount = 0;
    
    // version strings
    protected String version;
    
    protected String version_os;
    protected String version_cpu;
    
    protected String version_intc;
    protected String version_v6_ddrx;
    protected String version_bram_block;
    protected String version_timer_controller;
    
    protected String version_uartlite;
    protected String version_ethernetlite;
    protected String version_lwip_lib_name;
    protected String version_lwip_lib;
    
    protected String version_gpio_leds;
    protected String version_gpio_buttons;
    protected String version_gpio_switches;
    
    protected String version_queue;
    protected String version_resizer;
    
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
    
    public Map<File, File> getFiles() {
        return deployFiles;
    }
    
    public SDK(Configuration config, ErrorCollection errors) {
        this.errors = errors;
        
        // set directories
        this.targetDir = sdkAppDir(config);
        targetSrc = new File(targetDir, "src");
        
        // setup files and methods
        deployFiles = new HashMap<File, File>();
        setupFiles();
        setupMethods();
        setupMSS();
        
        // initialise version strings
        version                   = "2.2.0";
        
        version_os                = "3.08.a";
        version_cpu               = "1.14.a";
        
        version_bram_block        = "3.01.a";
        version_intc              = "2.05.a";
        version_timer_controller  = "2.04.a";
        version_v6_ddrx           = "2.00.a";
        
        version_uartlite          = "2.00.a";
        version_ethernetlite      = "3.03.a";
        
        version_lwip_lib_name     = "lwip140";
        version_lwip_lib          = "1.03.a";
        
        version_gpio_leds         = "3.00.a";
        version_gpio_buttons      = "3.00.a";
        version_gpio_switches     = "3.00.a";
        
        version_queue             = "1.00.a";
        version_resizer           = "1.00.a";
    }
    
    private void setupFiles() {
        components = MFile(MDocumentation(Strings(
                "Contains component-specific initialisation and processing procedures."
            )), "components", new File(targetSrc, "components").getPath(), MDefinitions(),
            MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
        constants  = MFile(MDocumentation(Strings(
                "Defines several constants used by the server.",
                "This includes medium-specific configuration."
            )), "constants", targetSrc.getPath(), MDefinitions(),
            MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
    }
    
    private void setupMethods() {
        init  = MProcedure(MDocumentation(Strings(
                "Initialises all components on this board.",
                "This includes gpio components and user-defined IPCores,",
                "but not the communication medium this board is attached with."
            )), MModifiers(), MVoid(), "init_components", MParameters(), MCode(Strings()));
        reset = MProcedure(MDocumentation(Strings(
                "Resets all components in this board.",
                "This includes gpio components and user-defined IPCores,",
                "but not the communication medium, this board is attached with."
            )), MModifiers(), MVoid(), "reset_components", MParameters(), MCode(Strings()));
        axi_write = MProcedure(MDocumentation(Strings(
                "Write a value to an AXI stream."),
                PARAM("val", "Value to be written to the stream."),
                PARAM("target", "Target stream identifier.")
            ), MModifiers(PRIVATE()), MType("int"), "axi_write", MParameters(
                MParameter(VALUE(), MType("int"), "val"), MParameter(VALUE(), MType("int"), "target")
            ), MCode(Strings(
                "// YES, this is ridiculous... THANKS FOR NOTHING, XILINX!",
                "if(DEBUG) xil_printf(\"\\nwriting to in-going port %d (value: %d) ...\", target, val);",
                "switch(target) {"
            ), MQuoteInclude("fsl.h"), MQuoteInclude("../constants.h")));
        axi_read  = MProcedure(MDocumentation(Strings(
                "Read a value from an AXI stream."),
                PARAM("val", "Pointer to the memory area, where the read value will be stored."),
                PARAM("target", "Target stream identifier.")
            ), MModifiers(PRIVATE()), MType("int"), "axi_read", MParameters(
                MParameter(VALUE(), MPointerType(MType("int")), "val"), MParameter(VALUE(), MType("int"), "target")
            ), MCode(Strings(
                "// YES, this is ridiculous... THANKS FOR NOTHING, XILINX!",
                "if(DEBUG) xil_printf(\"\\nreading from out-going port %d ...\", target);",
                "switch(target) {"
            ), MQuoteInclude("fsl.h"), MQuoteInclude("../constants.h")));
    }
    
    private void setupMSS() {
        mssFile = MHS.MHSFile(
            MHS.Attributes(
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("VERSION", MHS.STR(version)))
            ), MHS.Block("OS",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_NAME", MHS.STR("standalone"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("OS_VER", MHS.STR(version_os))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("PROC_INSTANCE", MHS.STR("microblaze_0"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDIN", MHS.STR("debug_module"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("STDOUT", MHS.STR("debug_module")))
            ), MHS.Block("PROCESSOR",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("cpu"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_cpu))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("microblaze_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("tmrctr"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_timer_controller))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("axi_timer_0")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("v6_ddrx"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_v6_ddrx))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("ddr3_sdram")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("bram"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_bram_block))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("microblaze_0_d_bram_ctrl")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("bram"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_bram_block))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("mikcroblaze_0_i_bram_ctrl")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("intc"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_intc))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("microblaze_0_intc")))
            ), MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.STR("uartlite"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.STR(version_uartlite))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.STR("debug_module")))
            )
        );
    }
    
    // We assume all imports to be accumulated at the parser
    public void visit(ImportsPos  term) { }
    public void visit(BackendsPos term) { }
    public void visit(OptionsPos  term) { }
    
    @Override
    public void visit(BDLFilePos term) {
        // add queue size constants
        boolean debug = false;
        int queueSizeHW = defaultQueueSizeHW, queueSizeSW = defaultQueueSizeSW;
        for(Option o : term.opts().term()) {
           if(o instanceof HWQUEUE) queueSizeHW = ((HWQUEUE)o).qsize();
           if(o instanceof SWQUEUE) queueSizeSW = ((SWQUEUE)o).qsize();
           if(o instanceof DEBUG)   debug = true;
        }
        addConst("DEBUG", debug ? "1" : "0", "Indicates, if additional messages should be logged on the console.");
        addConst("HW_QUEUE_SIZE", String.valueOf(queueSizeHW), "Size of the queues implemented in hardware.");
        addConst("SW_QUEUE_SIZE", String.valueOf(queueSizeSW), "Size of the queues on the microblaze.");
        addConst("ITERATION_COUNT", String.valueOf(maxQueueSize(term.term())),
                "Maximal number of shifts between mb and hw queues per schedule cycle");
        
        // add protocol version constant
        addConst("PROTO_VERSION", "1", "Denotes protocol version, that should be used for sending messages.");
        
        // add gpio utils file, if gpio components are present
        if(!term.gpios().isEmpty()) {
            File target = new File(new File(targetSrc, "components"), "gpio");
            deployFiles.put(new File(gpioSrc, "gpio.h"), new File(target, "gpio.h"));
            deployFiles.put(new File(gpioSrc, "gpio.c"), new File(target, "gpio.c"));
        }
        
        // visit board components
        visit(term.medium());
        visit(term.scheduler());
        visit(term.gpios());
        visit(term.cores());
        visit(term.insts());

        addConst("IN_STREAM_COUNT", String.valueOf(axiStreamIdMaster), "Number of in-going stream interfaces.");
        addConst("OUT_STREAM_COUNT", String.valueOf(axiStreamIdSlave), "Number of out-going stream interfaces.");
        
        // add init and reset procedures to the source file
        components = add(components, init);
        components = add(components, reset);
        
        // finish axi read and write procedures
        axi_write = addLines(axi_write, MCode(Strings(
                "default: xil_printf(\"ERROR: unknown axi stream port %d\", target);",
                "}", "// should be call by value --> reuse the memory address...",
                "int rslt = 1;",
                "fsl_isinvalid(rslt);",
                "if(DEBUG) xil_printf(\" (invalid: %d)\", rslt);",
                "return rslt;")));
        axi_read  = addLines(axi_read,  MCode(Strings(
                "default: xil_printf(\"ERROR: unknown axi stream port %d\", target);",
                "}", "// should be call by value --> reuse the memory address...",
                "if(DEBUG) xil_printf(\"\\n %d\", *val);",
                "int rslt = 1;",
                "fsl_isinvalid(rslt);",
                "if(DEBUG) xil_printf(\" (invalid: %d)\", rslt);",
                "return rslt;")));
        
        // add axi read and write procedures
        components = add(components, axi_write);
        components = add(components, axi_read);
    }

    
    @Override
    public void visit(ETHERNETPos term) {
        // deploy Ethernet medium files
        deployFiles.put(new File(sdkSourceDir, "ethernet.c"), new File(new File(targetSrc, "medium"), "medium.c"));
        
        // add Ethernet-specific constants
        for(MOptionPos opt : term.opts()) {
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
            });
        }
        
        // add Ethernet driver and lwip library to bsp
        mssFile = addBlock(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("emaclite"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(version_ethernetlite))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("ethernet_lite")))
        ));
        
        mssFile = addBlock(mssFile, MHS.Block("LIBRARY",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("LIBRARY_NAME", MHS.Ident(version_lwip_lib_name))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("LIBRARY_VER", MHS.Ident(version_lwip_lib))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("PROC_INSTANCE", MHS.Ident("microblaze_0")))
        ));
    }
    
    public void visit(UARTPos term) {
        
    }
    
    public void visit(PCIEPos term) {
        
    }

    @Override
    public void visit(GPIOPos term) {
        String name = term.name().term();
        if(name.equals("leds")) addLEDs();
        else if(name.equals("switches")) addSwitches();
        else if(name.equals("buttons")) addButtons();
        else errors.addError(new ParserError("Unknown GPIO device " + name + " for Virtex6", "", -1));
    }

    @Override
    public void visit(final CPUAxisPos axis) {
        PortPos port = getPort(axis);

        int width = getWidth(axis);
        boolean d = port.direction() instanceof INPos;
        boolean up = (width < 32 && !d) || (width > 32 && d);

        String axisGroup   = d ? "m" + axiStreamIdMaster : "s" + axiStreamIdSlave;
        
        if(getHWQueueSize(axis) > 0) {
            mssFile = addBlock(mssFile, MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(version_queue))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup + "_queue")))
            ));
        }
        
        if(width < 32) {
            mssFile = addBlock(mssFile, MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(version_resizer))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup +
                        (up ? "upsizer" : "downsizer"))))
            ));
        } else if(width > 32) {
            mssFile = addBlock(mssFile, MHS.Block("DRIVER",
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("generic"))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER", MHS.Ident(version_resizer))),
                MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident(axisGroup +
                        (up ? "upsizer" : "downsizer"))))
            ));
        }
        
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
        axi_write = addLines(axi_write, MCode(Strings(
            "case "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+
            ": putfslx(val, "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+", FSL_NONBLOCKING); break;"
        )));
        
        init = addLines(init, MCode(
            Strings("inQueue[" + axiStreamIdMaster + "] = createQueue(" + getSWQueueSize(axis) + ");"),
            MQuoteInclude("../io.h")
        ));
        axiStreamIdMaster++;
    }

    private void addReadStream(CPUAxisPos axis) {
        if(axiStreamIdSlave>15) {
            errors.addError(new ParserError("too many reading AXI stream interfaces for Virtex6", "", -1));
            return;
        }
        axi_read  = addLines(axi_read,  MCode(Strings(
            "case "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+
            ": getfslx(*val, "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+", FSL_NONBLOCKING); break;"
        )));
        
        init = addLines(init, MCode(
            Strings("outQueueCap[" + axiStreamIdSlave + "] = " + getSWQueueSize(axis) + ";"),
            MQuoteInclude("../io.h")
        ));

        init = addLines(init, MCode(
            Strings("isPolling[" + axiStreamIdSlave + "] = " + (isPolling(axis) ? 1 : 0) + ";"),
            MQuoteInclude("../io.h")
        ));
        
        init = addLines(init, MCode(
            Strings("pollCount[" + axiStreamIdSlave + "] = " + getPollingCount(axis) + ";"),
            MQuoteInclude("../io.h")
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
    public void visit(BackendPos term) { }
    
    // scheduler (irrelevant for host-side driver
    public void visit(SchedulerPos term) {
        if(term.code() instanceof DEFAULTPos) {
            scheduler = MFile(MDocumentation(Strings(
                    "A primitive scheduler.",
                    "Reads values from the medium, shifts values between Microblaze and VHDL components,",
                    "and writes results back to the medium."
                ), AUTHOR("Thomas Fischer"), SINCE("18.02.2013")
                ), "scheduler", targetSrc.getPath(), MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(
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
        } else {
            scheduler = MFile(MDocumentation(Strings()
                ), "scheduler", targetSrc.getPath(), MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(
                    MProcedure(
                        MDocumentation(Strings()), MModifiers(), MVoid(), "schedule", MParameters(), MCode(
                            Strings().addAll(((USER_DEFINED)term.code().term()).content()),
                            MQuoteInclude("constants.h"),
                            MQuoteInclude("queueUntyped.h"),
                            MQuoteInclude("io.h"),
                            MForwardDecl("void medium_read()"),
                            MForwardDecl("int axi_write ( int val, int target )"),
                            MForwardDecl("int axi_read ( int *val, int target )")
                        )
                    )
                )
            );
        }
    }
    
    private MCode defaultScheduler() {
        return MCode(
            Strings(
                "while(1) {",
                "    unsigned int pid;",
                "    unsigned int i;",
                "    ",
                "    // receive a package from the interface (or all?)",
                "    // esp stores data packages in mb queue",
                "    medium_read();",
                "    ",
                "    // write data from mb queue to hw queue (if possible)",
                "    for(pid = 0; pid < IN_STREAM_COUNT; pid++) {",
                "        for(i = 0; i < ITERATION_COUNT; i++) {",
                "            // go to next port if the sw queue is empty",
                "            if(inQueue[pid]->size == 0) break;",
                "            ",
                "            // try to write, skip if the hw queue is full",
                "            if(axi_write(peek(inQueue[pid]), pid)) {",
                "                if(DEBUG) xil_printf(\"\\nfailed to write to AXI stream\");",
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
                "    // read data from hw queue (if available) and cache in mb queue",
                "    // flush sw queue, if it's full or the hw queue is empty",
                "    for(pid = 0; pid < OUT_STREAM_COUNT; pid++) {",
                "        for(i = 0; i < outQueueCap[pid] && ((!isPolling[pid]) || pollCount[pid] > 0); i++) {",
                "            ",
                "            int val = 0;",
                "            ",
                "            // break, if there is no value",
                "            if(axi_read(&val, pid)) break;",
                "            ",
                "            // otherwise store the value in the sw queue",
                "            outQueue[outQueueSize] = val;",
                "            outQueueSize++;",
                "            ",
                "            // decrement the poll counter (if the port was polling)",
                "            if(isPolling[pid]) pollCount[pid]--;",
                "        }",
                "        // flush sw queue",
                "        flush_queue(pid);",
                "        outQueueSize = 0;",
                "    }",
                "}"
            ),
            MQuoteInclude("constants.h"),
            MQuoteInclude("queueUntyped.h"),
            MQuoteInclude("io.h"),
            MForwardDecl("void medium_read()"),
            MForwardDecl("int axi_write ( int val, int target )"),
            MForwardDecl("int axi_read ( int *val, int target )")
        );
    }
    
    // code blocks (handled directly when occurring)
    public void visit(DEFAULTPos term)      { }
    public void visit(USER_DEFINEDPos term) { }
    
    // missing medium declaration
    public void visit(NONEPos term) { }
    
    // options (handled directly inside the board or port if occurring)
    public void visit(HWQUEUEPos  arg0) { }
    public void visit(SWQUEUEPos  arg0) { }
    public void visit(BITWIDTHPos term) { }
    public void visit(DEBUGPos    term) { }
    public void visit(POLLPos     term) { }
    
    // same goes for medium options
    public void visit(MACPos    term) { }
    public void visit(IPPos     term) { }
    public void visit(MASKPos   term) { }
    public void visit(GATEPos   term) { }
    public void visit(PORTIDPos term) { }
    
    // cores
    // we do not need to visit cores here, since a class will be created
    // for each instance directly connected to the boards CPU, not for each core
    public void visit(CoresPos term) { }
    public void visit(CorePos  term) { }
    
    // ports (see above)
    public void visit(PortsPos term) { }
    public void visit(PortPos  term) { }
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

    
    private void addLEDs() {
        // deploy LED files
        File target = new File(new File(targetSrc, "components"), "gpio");
        deployFiles.put(new File(gpioSrc, "led.h"), new File(target, "led.h"));
        deployFiles.put(new File(gpioSrc, "led.c"), new File(target, "led.c"));

            // add LED init to component init
        init = addLines(init, MCode(Strings("init_LED();"),
                MForwardDecl("int init_LED()")));
    
        mssFile = addBlock(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("gpio"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER",  MHS.Ident(version_gpio_leds))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("leds_8bits")))
        ));
    }
    
    public void addSwitches() {
        // deploy switch files
        File target = new File(new File(targetSrc, "components"), "gpio");
        deployFiles.put(new File(gpioSrc, "switches.h"), new File(target, "switches.h"));
        deployFiles.put(new File(gpioSrc, "switches.c"), new File(target, "switches.c"));
            
        // add switch init to component init
        init = addLines(init, MCode(Strings("init_switches();"),
                MForwardDecl("int init_switches()")));

        components = add(components, MDefinition(MDocumentation(Strings(
                "Identifier of the switch component"
                )), MModifiers(PRIVATE()), "gpo_switches", String.valueOf(gpoCount++)));
        
        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(
            MDocumentation(Strings(
                "Callback procedure for the switch gpio component.",
                "This procedure is called, whenever the state of the switch component changes."
            )), MModifiers(PRIVATE()), MVoid(), "callback_switches", MParameters(), MCode(
                Strings(
                    "// Test application: set LED state to Switch state",
                    "set_LED(read_switches());",
                    "send_gpio(gpo_switches, read_switches());"),
                MQuoteInclude("xbasic_types.h"),
                MForwardDecl("u32 read_switch()"),
                MForwardDecl("void set_LED(u32 state)"),
                MForwardDecl("void send_gpio(unsigned char gid, unsigned char state)")
            )
        ));
        
        // add the driver block to the mss file
        mssFile = addBlock(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("gpio"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER",  MHS.Ident(version_gpio_buttons))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("push_buttons_5bits")))
        ));
    }

    private void addButtons() {
        // deploy button files
        File target = new File(new File(targetSrc, "components"), "gpio");
        deployFiles.put(new File(gpioSrc, "button.h"), new File(target, "button.h"));
        deployFiles.put(new File(gpioSrc, "button.c"), new File(target, "button.c"));
            
        // add button init to component init
        init = addLines(init, MCode(Strings("init_buttons();"),
                MForwardDecl("int init_buttons()")));
        
        components = add(components, MDefinition(MDocumentation(Strings(
                "Identifier of the button component"
                )), MModifiers(PRIVATE()), "gpo_buttons", String.valueOf(gpoCount++)));
        
        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(
            MDocumentation(Strings(
                "Callback procedure for the pushbutton gpio component.",
                "This procedure is called, whenever the state of the pushbutton component changes."
            )), MModifiers(PRIVATE()), MVoid(), "callback_buttons", MParameters(), MCode(
                Strings(
                    "// Test application: print out some text",
                    "xil_printf(\"\\nhey - stop pushing!! %d\", read_buttons());",
                    "send_gpio(gpo_buttons, read_buttons());"),
                MQuoteInclude("xbasic_types.h"),
                MForwardDecl("u32 read_buttons()")
            )
        ));
        
        // add the driver block to the mss file
        mssFile = addBlock(mssFile, MHS.Block("DRIVER",
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_NAME", MHS.Ident("gpio"))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("DRIVER_VER",  MHS.Ident(version_gpio_switches))),
            MHS.Attribute(MHS.PARAMETER(), MHS.Assignment("HW_INSTANCE", MHS.Ident("dip_switches_8bits")))
        ));
    }
    
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
    
    private void addConst(String id, String val, String doc) {
        constants = add(constants, MDefinition(MDocumentation(Strings(doc)), MModifiers(PUBLIC()), id, val));
    }
    
    private MHSFile addBlock(MHSFile file, Block block) {
        return file.replaceBlocks(file.blocks().add(block));
    }
}
