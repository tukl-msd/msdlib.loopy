package de.hopp.generator.backends.server.virtex6.ise.sdk;

import static de.hopp.generator.backends.BackendUtils.defaultQueueSizeHW;
import static de.hopp.generator.backends.BackendUtils.defaultQueueSizeSW;
import static de.hopp.generator.backends.server.virtex6.ise.ISE.sourceDir;
import static de.hopp.generator.backends.server.virtex6.ise.ISEUtils.sdkDir;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.BoardUtils.getPort;
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
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MProcedure;

/**
 * Generates the board-specific, non-generic files of the
 * Virtex 6 board-side driver.
 * This includes the constant file and the components file.
 * The files are represented by C file ASTs, which are generated
 * by visiting each component of the BDL file describing board design.
 * @author Thomas Fischer
 */
public class Virtex6BDLVisitor extends Visitor<NE> {
    
    private static File gpioSrc = new File(sourceDir, "gpio");
    
    private ErrorCollection errors;
    
    // target folders
    private File targetDir;
    private File targetSrc;
    
    // generic files to copy
    protected Map<File, File> files;
    
    // generated files
    protected MFile components;
    protected MFile constants;
    
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
    
    public MFile getComponents() {
        return components;
    }
    
    public MFile getConstants() {
        return constants;
    }
    
    public Map<File, File> getFiles() {
        return files;
    }
    
    public Virtex6BDLVisitor(Configuration config, ErrorCollection errors) {
        this.errors = errors;
        
        // set directories
        this.targetDir = sdkDir(config);
        targetSrc = new File(targetDir, "src");
        
        // setup files and methods
        files = new HashMap<File, File>();
        setupFiles();
        setupMethods();
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
        addConst("ITERATION_COUNT", "SW_QUEUE_SIZE", "Maximal number of shifts between mb and hw queues per schedule cycle");
        
        // add protocol version constant
        addConst("PROTO_VERSION", "1", "Denotes protocol version, that should be used for sending messages.");
        
        // add gpio utils file, if gpio components are present
        if(!term.gpios().isEmpty()) {
            File target = new File(new File(targetSrc, "components"), "gpio");
            files.put(new File(gpioSrc, "gpio.h"), new File(target, "gpio.h"));
            files.put(new File(gpioSrc, "gpio.c"), new File(target, "gpio.c"));
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
        files.put(new File(sourceDir, "ethernet.c"), new File(new File(targetSrc, "medium"), "medium.c"));
        
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
    }
    
    public void visit(UARTPos term) {
        
    }
    
    public void visit(PCIEPos term) {
        
    }

    @Override
    public void visit(GPIOPos term) {
        switch(term.name().term()) {
        case "leds":     addLEDs();     break;
        case "switches": addSwitches(); break;
        case "buttons":  addButtons();  break;
        default: errors.addError(new ParserError("Unknown GPIO device " + term.name().term() + " for Virtex6", "", -1));
        }
    }

    @Override
    public void visit(CPUAxisPos term) {
        PortPos port = getPort(term.root(), ((InstancePos)term.parent().parent()).core().term(), term.port().term());
        port.direction().termDirection().Switch(new Direction.Switch<Boolean, NE>() {
            public Boolean CaseIN(IN term) {
                addWriteStream();
                return null;
            }
            public Boolean CaseOUT(OUT term) {
                addReadStream();
                return null;
            }
            public Boolean CaseDUAL(DUAL term) {
                addWriteStream();
                addReadStream();
                return null;
            }
        });
    }
    
    private void addWriteStream() {
        if(axiStreamIdMaster>15) {
            errors.addError(new ParserError("too many writing AXI stream interfaces for Virtex6", "", -1));
            return;
        }
        axi_write = addLines(axi_write, MCode(Strings(
                "case "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+
                ": putfslx(val, "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+", FSL_NONBLOCKING); break;"
                )));
        axiStreamIdMaster++;
    }

    private void addReadStream() {
        if(axiStreamIdSlave>15) {
            errors.addError(new ParserError("too many reading AXI stream interfaces for Virtex6", "", -1));
            return;
        }
        axi_read  = addLines(axi_read,  MCode(Strings(
                "case "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+
                ": getfslx(*val, "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+", FSL_NONBLOCKING); break;"
                )));
        axiStreamIdSlave++;
    }
    
    // list types
    public void visit(GPIOsPos     term) { for(    GPIOPos gpio : term) visit(gpio); }
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }
    public void visit(BindingsPos  term) { for( BindingPos bind : term) visit(bind); }
    public void visit(MOptionsPos  term) { for( MOptionPos opt  : term) visit(opt);  }
    
    // general (handled before this visitor)
    public void visit(ImportPos term) { }
    public void visit(BackendPos term) { }
    
    // scheduler (irrelevant for host-side driver
    public void visit(SchedulerPos term) { }
    
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
        files.put(new File(gpioSrc, "led.h"), new File(target, "led.h"));
        files.put(new File(gpioSrc, "led.c"), new File(target, "led.c"));

            // add LED init to component init
        init = addLines(init, MCode(Strings("init_LED();"),
                MForwardDecl("int init_LED()")));
    }

    public void addSwitches() {
        // deploy switch files
        File target = new File(new File(targetSrc, "components"), "gpio");
        files.put(new File(gpioSrc, "switches.h"), new File(target, "switches.h"));
        files.put(new File(gpioSrc, "switches.c"), new File(target, "switches.c"));
            
        // add switch init to component init
        init = addLines(init, MCode(Strings("init_switches();"),
                MForwardDecl("int init_switches()")));

        components = add(components, MDefinition(MDocumentation(Strings(
                "Identifier of the switch component"
                )), MModifiers(PRIVATE()), "gpo_switches", String.valueOf(gpoCount++)));
        
        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(MDocumentation(Strings(
                    "Callback procedure for the switch gpio component.",
                    "This procedure is called, whenever the state of the switch component changes."
                )), MModifiers(PRIVATE()), MVoid(), "callback_switches", MParameters(), MCode(Strings(
                     "// Test application: set LED state to Switch state",
                     "set_LED(read_switches());",
                     "send_gpio(gpo_switches, read_switches());"
                ), MQuoteInclude("xbasic_types.h"),
                   MForwardDecl("u32 read_switch()"),
                   MForwardDecl("void set_LED(u32 state)"),
                   MForwardDecl("void send_gpio(unsigned char gid, unsigned char state)"))));
    }

    private void addButtons() {
        // deploy button files
        File target = new File(new File(targetSrc, "components"), "gpio");
        files.put(new File(gpioSrc, "button.h"), new File(target, "button.h"));
        files.put(new File(gpioSrc, "button.c"), new File(target, "button.c"));
            
        // add button init to component init
        init = addLines(init, MCode(Strings("init_buttons();"),
                MForwardDecl("int init_buttons()")));
        
        components = add(components, MDefinition(MDocumentation(Strings(
                "Identifier of the button component"
                )), MModifiers(PRIVATE()), "gpo_buttons", String.valueOf(gpoCount++)));
        
        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(MDocumentation(Strings(
                    "Callback procedure for the pushbutton gpio component.",
                    "This procedure is called, whenever the state of the pushbutton component changes."
                )), MModifiers(PRIVATE()), MVoid(), "callback_buttons", MParameters(), MCode(Strings(
                     "// Test application: print out some text",
                     "xil_printf(\"\\nhey - stop pushing!! %d\", read_buttons());",
                     "send_gpio(gpo_buttons, read_buttons());"
                ), MQuoteInclude("xbasic_types.h"), MForwardDecl("u32 read_buttons()"))));
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
}
