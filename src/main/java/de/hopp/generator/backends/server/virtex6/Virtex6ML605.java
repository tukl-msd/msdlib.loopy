package de.hopp.generator.backends.server.virtex6;

import static de.hopp.generator.backends.BackendUtils.defaultQueueSizeHW;
import static de.hopp.generator.backends.BackendUtils.defaultQueueSizeSW;
import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.BoardUtils.getPort;
import static de.hopp.generator.utils.Files.copy;
import static de.hopp.generator.utils.Model.add;
import static de.hopp.generator.utils.Model.addLines;

import java.io.File;
import java.io.IOException;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.Backend;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.frontend.*;
import de.hopp.generator.frontend.BDLFilePos.Visitor;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MProcedure;

public class Virtex6ML605 extends Visitor<NE> implements Backend {

    private static String sourceDir = "deploy" + '/' + "server" + '/' + "virtex6ML605";
    
    private ProjectBackend project;
    
    private Configuration config;
    private ErrorCollection errors;
    private IOHandler IO;
    private File serverSrc;
    
    private boolean debug;
    
    private MFile components;
    private MFile constants;
    
    private MProcedure init;
    private MProcedure reset;
    private MProcedure axi_write;
    private MProcedure axi_read;
    
    private int axiStreamIdMaster = 0;
    private int axiStreamIdSlave  = 0;
    
    private int gpiCount = 0;
    private int gpoCount = 0;
    
    public Virtex6ML605() {
        
        // setup basic methods
        components = MFile(MDocumentation(Strings(
                "Contains component-specific initialisation and processing procedures."
            )), "components", MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
        constants  = MFile(MDocumentation(Strings(
                "Defines several constants used by the server.",
                "This includes medium-specific configuration."
            )), "constants", MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
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
    
    public String getName() {
        return "Virtex6ML605";
    }
    
    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {

        this.config = config;
        this.errors = errors;
        this.IO     = config.IOHANDLER();
        this.debug  = config.debug();
        
        serverSrc = new File(config.serverDir(), "src");
        
        // deploy board-independent files and directories
        try { 
            copy(sourceDir + '/' + "generic", config.serverDir(), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy generic sources due to:\n" + e.getMessage()));
            return;
        }
        
        // add gpio utils file, if gpio components are present
        try {
            if(!board.gpios().isEmpty()) {
                File target = new File(new File(serverSrc, "components"), "gpio");
                copy(sourceDir + '/' + "gpio" + '/' + "gpio.h", new File(target, "gpio.h"), IO);
                copy(sourceDir + '/' + "gpio" + '/' + "gpio.c", new File(target, "gpio.c"), IO);
            }
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy generic GPIO sources due to:\n" + e.getMessage()));
            return;
        }

        // add the debug constant
        addConst("DEBUG", debug ? "1" : "0", "Indicates, if additional messages should be logged on the console.");
        
        // generate and deploy board-specific MFiles
        visit(board);
        
        IO.println("  generate server-side api specification ... ");
        doxygen(config.serverDir(), IO, errors);
        
        // TODO for now, no project generation!
//        if(project == null) {
//            errors.addError(new GenerationFailed("No project generation backend specified"));
//            return;
//        }
//        
//        // run the project generator
//        project.generate(board, config, errors);
        
    }
    
    // We assume all imports to be accumulated at the parser
    public void visit(ImportsPos  term) { }
    public void visit(BackendsPos term) { }
    public void visit(OptionsPos  term) { }

    @Override
    public void visit(BDLFilePos term) {
        // add queue size constants
        int queueSizeHW = defaultQueueSizeHW, queueSizeSW = defaultQueueSizeSW;
        for(Option o : term.opts().term()) {
           if(o instanceof HWQUEUE) queueSizeHW = ((HWQUEUE)o).qsize();
           if(o instanceof SWQUEUE) queueSizeSW = ((SWQUEUE)o).qsize();
        }
        addConst("HW_QUEUE_SIZE", String.valueOf(queueSizeHW), "Size of the queues implemented in hardware.");
        addConst("SW_QUEUE_SIZE", String.valueOf(queueSizeSW), "Size of the queues on the microblaze.");
        addConst("ITERATION_COUNT", "SW_QUEUE_SIZE", "Maximal number of shifts between mb and hw queues per schedule cycle");
        
        // add protocol version constant
        addConst("PROTO_VERSION", "1", "Denotes protocol version, that should be used for sending messages.");
        
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
        
        // unparse the generated MFiles
        File compSrc   = new File(serverSrc,   "components");
        printMFile(constants,  serverSrc, UnparserType.HEADER, errors);
        printMFile(components,   compSrc, UnparserType.HEADER, errors);
        printMFile(components,   compSrc, UnparserType.C,      errors);
    }

    
    @Override
    public void visit(MediumPos term) {
        // TODO Auto-generated method stub
        // deploy Ethernet medium files
        switch(term.name().term()) {
        case "ethernet": try {
                File target = new File(serverSrc, "medium");
                copy(sourceDir + '/' + "ethernet.c", new File(target, "medium.c"), IO);
            } catch(IOException e) {
                errors.addError(new GenerationFailed("Failed to copy ethernet sources due to:\n" + e.getMessage()));
            }
                
            // add Ethernet specific constants
            addIP("IP",   config.getIP(), "ip address");
            addIP("MASK", config.getMask(), "subnet mask");
            addIP("GW",   config.getGW(), "standard gateway");
            addMAC(config.getMAC());
            addConst("PORT", "8844", "The port for this boards TCP- connection.");
            break;
        default: errors.addError(new ParserError("unknown medium " + term.name().term()));
        }
    }

    @Override
    public void visit(GPIOPos term) {
        switch(term.name().term()) {
        case "leds":     addLEDs();     break;
        case "switches": addSwitches(); break;
        case "buttons":  addButtons();  break;
        default: errors.addError(new ParserError("Unknown GPIO device " + term.name().term() + " for " + getName()));
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
            errors.addError(new ParserError("too many writing AXI stream interfaces for " + getName()));
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
            errors.addError(new ParserError("too many reading AXI stream interfaces for " + getName()));
            return;
        }
        axi_read  = addLines(axi_read,  MCode(Strings(
                "case "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+
                ": getfslx(*val, "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+", FSL_NONBLOCKING); break;"
                )));
        axiStreamIdSlave++;
    }
    
    @Override
    public void visit(CodePos term) {
        // TODO Auto-generated method stub
        
    }

    // list types
    public void visit(GPIOsPos     term) { for(    GPIOPos gpio : term) visit(gpio); }
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }
    public void visit(BindingsPos  term) { for( BindingPos bind : term) visit(bind); }

    // general (handled before this visitor)
    public void visit(ImportPos term)       { }
    public void visit(de.hopp.generator.frontend.BackendPos term) { }

    // scheduler (handled directly inside the board)
    public void visit(DEFAULTPos term)      { }
    public void visit(USER_DEFINEDPos term) { }
    
    // attributes (handled directly inside the board or port if occurring)
    public void visit(HWQUEUEPos  arg0) { }
    public void visit(SWQUEUEPos  arg0) { }
    public void visit(BITWIDTHPos term) { }
    public void visit(POLLPos     term) { }
    
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
    
    // literals
    public void visit(StringsPos term) { }
    public void visit(StringPos  term) { }
    public void visit(IntegerPos term) { }

    
    private void addLEDs() {
        // deploy LED files
        try {
            File target = new File(new File(serverSrc, "components"), "gpio");
            copy(sourceDir + '/' + "gpio" + '/' + "led.h", new File(target, "led.h"), IO);
            copy(sourceDir + '/' + "gpio" + '/' + "led.c", new File(target, "led.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy LED GPIO sources due to:\n" + e.getMessage()));
        }
            
        // add LED init to component init
        init = addLines(init, MCode(Strings("init_LED();"),
                MForwardDecl("int init_LED()")));
    }

    public void addSwitches() {
        // deploy switch files
        try {
            File target = new File(new File(serverSrc, "components"), "gpio");
            copy(sourceDir + '/' + "gpio" + '/' + "switches.h", new File(target, "switches.h"), IO);
            copy(sourceDir + '/' + "gpio" + '/' + "switches.c", new File(target, "switches.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy switch GPIO sources due to:\n" + e.getMessage()));
        }
            
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
        try {
            File target = new File(new File(serverSrc, "components"), "gpio");
            copy(sourceDir + '/' + "gpio" + '/' + "button.h", new File(target, "button.h"), IO);
            copy(sourceDir + '/' + "gpio" + '/' + "button.c", new File(target, "button.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy button GPIO sources due to:\n" + e.getMessage()));
        }
            
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
    
    private void addIP(String id, int[] ip, String doc) {
        addConst(id + "_1", "" + ip[0], "The first  8 bits of the " + doc + " of this board.");
        addConst(id + "_2", "" + ip[1], "The second 8 bits of the " + doc + " of this board.");
        addConst(id + "_3", "" + ip[2], "The third  8 bits of the " + doc + " of this board.");
        addConst(id + "_4", "" + ip[3], "The fourth 8 bits of the " + doc + " of this board.");
    }
    
    private void addMAC(String[] mac) {
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
