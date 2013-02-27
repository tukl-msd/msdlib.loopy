package de.hopp.generator.backends.server.virtex6;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.BoardUtils.hasGpioComponent;
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
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MProcedure;

/**
 * Generation backend for a server-side driver for the Virtex 6 ML 605 platform.
 * This visitor generates a server-side driver for this platform, which can communicate
 * with any client-side driver.
 * @author Thomas Fischer
 */
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
    
    public String getName() {
        return "Virtex6ML605";
    }
    
    public void generate(Board board, Configuration config, ErrorCollection errors) {
        this.config = config;
        this.errors = errors;
        this.IO = config.IOHANDLER();
        this.debug = config.debug();
        
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
            if(hasGpioComponent(board)) {
                File target = new File(new File(serverSrc, "components"), "gpio");
                copy(sourceDir + '/' + "gpio" + '/' + "gpio.h", new File(target, "gpio.h"), IO);
                copy(sourceDir + '/' + "gpio" + '/' + "gpio.c", new File(target, "gpio.c"), IO);
            }
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy generic GPIO sources due to:\n" + e.getMessage()));
            return;
        }
        
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
                "switch(target) {"
            ), MQuoteInclude("fsl.h")));
        axi_read  = MProcedure(MDocumentation(Strings(
                "Read a value from an AXI stream."),
                PARAM("val", "Pointer to the memory area, where the read value will be stored."),
                PARAM("target", "Target stream identifier.")
            ), MModifiers(PRIVATE()), MType("int"), "axi_read", MParameters(
                MParameter(VALUE(), MPointerType(MType("int")), "val"), MParameter(VALUE(), MType("int"), "target")
            ), MCode(Strings(
                "// YES, this is ridiculous... THANKS FOR NOTHING, XILINX!",
                "switch(target) {"
            ), MQuoteInclude("fsl.h")));
    }
    
    public MFile getComponentsFile() {
        return components;
    }
    public MFile getConstantsFile() {
        return constants;
    }
    
    public void visit(Board board) {
        
        // add the debug constant
        addConst("DEBUG", debug ? "1" : "0", "Indicates, if additional messages should be logged on the console.");
        
        // add queue size constants
        addConst("HW_QUEUE_SIZE", "256", "Size of the queues implemented in hardware.");
        addConst("SW_QUEUE_SIZE", "1024", "Size of the queues on the microblaze.");
        addConst("ITERATION_COUNT", "SW_QUEUE_SIZE", "");
        
        // add protocol version constant
        addConst("PROTO_VERSION", "1", "Denotes protocol version, that should be used for sending messages.");
        
        // visit board components
        visit(board.components());

        addConst("IN_STREAM_COUNT", String.valueOf(axiStreamIdMaster), "Number of in-going stream interfaces.");
        addConst("OUT_STREAM_COUNT", String.valueOf(axiStreamIdSlave), "Number of out-going stream interfaces.");
        
        // add init and reset procedures to the source file
        components = add(components, init);
        components = add(components, reset);
        
        // finish axi read and write procedures
        axi_write = addLines(axi_write, MCode(Strings(
                "default: xil_printf(\"ERROR: unknown axi stream port %d\", target);",
                "}", "// should be call by value --> reuse the memory address...",
                "fsl_isinvalid(target);",
                "return target;")));
        axi_read  = addLines(axi_read,  MCode(Strings(
                "default: xil_printf(\"ERROR: unknown axi stream port %d\", target);",
                "}", "// should be call by value --> reuse the memory address...",
                "fsl_isinvalid(target);",
                "return target;")));
        
        // add axi read and write procedures
        components = add(components, axi_write);
        components = add(components, axi_read);
        
        // unparse the generated MFiles
//        File serverSrc = new File(config.serverDir(), "src");
        File compSrc   = new File(serverSrc,   "components");
        printMFile(constants,  serverSrc, UnparserType.HEADER, errors);
        printMFile(components,   compSrc, UnparserType.HEADER, errors);
        printMFile(components,   compSrc, UnparserType.C,      errors);
    }
    
    public void visit(Components comps) {
        for(Component c : comps) visit(c);            
    }

    public void visit(PCIE term)     { }
    public void visit(ETHERNET term) { }
    public void visit(ETHERNET_LITE term) {
        // deploy Ethernet medium files
        try {
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
    }

    public void visit(UART term) {
        // deploy UART/USB medium files
        try {
            File target = new File(serverSrc, "medium");
            copy(sourceDir + '/' + "uart.c", new File(target, "medium.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy uart sources due to:\n" + e.getMessage()));
        }
    }
    
    public void visit(LEDS term) {
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
                MForwardDecl("int init_LED();")));
    }

    public void visit(SWITCHES term) {
        // deploy switch files
        try {
            File target = new File(new File(serverSrc, "components"), "gpio");
            copy(sourceDir + '/' + "gpio" + '/' + "switch.h", new File(target, "switch.h"), IO);
            copy(sourceDir + '/' + "gpio" + '/' + "switch.c", new File(target, "switch.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy switch GPIO sources due to:\n" + e.getMessage()));
        }
            
        // add switch init to component init
        init = addLines(init, MCode(Strings("init_switch();"),
                MForwardDecl("int init_switch();")));

        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(MDocumentation(Strings(
                    "Callback procedure for the switch gpio component.",
                    "This procedure is called, whenever the state of the switch component changes."
                )), MModifiers(PRIVATE()), MVoid(), "callbackSwitches", MParameters(), MCode(Strings(
                     "// Test application: set LED state to Switch state",
                     "set_LED(read_switch());"
                ), MQuoteInclude("xbasic_types.h"),
                   MForwardDecl("u32 read_switch();"),
                   MForwardDecl("void set_LED(u32 state);"))));
    }

    public void visit(BUTTONS term) {
        // deploy button files
        try {
            File target = new File(new File(serverSrc, "components"), "gpio");
            copy(sourceDir + '/' + "gpio" + '/' + "button.h", new File(target, "button.h"), IO);
            copy(sourceDir + '/' + "gpio" + '/' + "button.c", new File(target, "button.c"), IO);
        } catch(IOException e) {
            errors.addError(new GenerationFailed("Failed to copy button GPIO sources due to:\n" + e.getMessage()));
        }
            
        // add button init to component init
        init = addLines(init, MCode(Strings("init_button();"),
                MForwardDecl("int init_button();")));
        
        // add callback procedure to component file
        // TODO user-defined code and additional documentation
        components = add(components, MProcedure(MDocumentation(Strings(
                    "Callback procedure for the pushbutton gpio component.",
                    "This procedure is called, whenever the state of the pushbutton component changes."
                )), MModifiers(PRIVATE()), MVoid(), "callbackButtons", MParameters(), MCode(Strings(
                     "// Test application: print out some text",
                     "xil_printf(\"\\nhey - stop pushing!! %d\", read_button());"
                ), MQuoteInclude("xbasic_types.h"), MForwardDecl("u32 read_button();"))));
    }
    
    public void visit(VHDL term) {
        // generate read and write procedures for all ports of the vhdl component
        for(@SuppressWarnings("unused") String instance : term.instances()) {
            for(Port port : term.core().ports()) {
                if(port instanceof IN) {
                    if(axiStreamIdMaster>15) continue; //TODO: throw exception: to many master interfaces / in-going ports
                    axi_write = addLines(axi_write, MCode(Strings(
                            "case "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+
                            ": putfslx(val, "+(axiStreamIdMaster>9?"":" ")+axiStreamIdMaster+", FSL_NONBLOCKING); break;"
                            )));
                    axiStreamIdMaster++;
                } else if(port instanceof OUT) {
                    if(axiStreamIdSlave>15) continue; //TODO: throw exception: to many slave interfaces / out-going ports
                    axi_read  = addLines(axi_read,  MCode(Strings(
                            "case "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+
                            ": getfslx(val, "+(axiStreamIdSlave>9?"":" ")+axiStreamIdSlave+", FSL_NONBLOCKING); break;"
                            )));
                    axiStreamIdSlave++;
                } else {
                    // TODO: throw exception: invalid port type for AXI Stream
                }
            }
        }
    }
    
    public void visit(VHDLCore term) { }
    public void visit(Ports term)    { }
    public void visit(IN term)       { }
    public void visit(OUT term)      { }
    public void visit(DUAL term)     { }
    
    // literals
    public void visit(Integer term)   { }
    public void visit(Instances term) { }
    public void visit(String term)    { }

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