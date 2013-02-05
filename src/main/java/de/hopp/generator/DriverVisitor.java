package de.hopp.generator;

import static de.hopp.generator.model.Model.MAttributes;
import static de.hopp.generator.model.Model.MClasses;
import static de.hopp.generator.model.Model.MCode;
import static de.hopp.generator.model.Model.MDefinition;
import static de.hopp.generator.model.Model.MDefinitions;
import static de.hopp.generator.model.Model.MDocumentation;
import static de.hopp.generator.model.Model.MEnums;
import static de.hopp.generator.model.Model.MFile;
import static de.hopp.generator.model.Model.MForwardDecl;
import static de.hopp.generator.model.Model.MModifiers;
import static de.hopp.generator.model.Model.MParameters;
import static de.hopp.generator.model.Model.MProcedure;
import static de.hopp.generator.model.Model.MProcedures;
import static de.hopp.generator.model.Model.MQuoteInclude;
import static de.hopp.generator.model.Model.MStructs;
import static de.hopp.generator.model.Model.MType;
import static de.hopp.generator.model.Model.MVoid;
import static de.hopp.generator.model.Model.PRIVATE;
import static de.hopp.generator.model.Model.PUBLIC;
import static de.hopp.generator.model.Model.Strings;
import static de.hopp.generator.utils.BoardUtils.hasGpioComponent;
import static de.hopp.generator.utils.Files.copy;
import static de.hopp.generator.utils.Model.add;
import static de.hopp.generator.utils.Model.addLines;

import java.io.File;
import java.io.IOException;

import de.hopp.generator.board.BUTTONS;
import de.hopp.generator.board.Board;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.board.Component;
import de.hopp.generator.board.Components;
import de.hopp.generator.board.DUAL;
import de.hopp.generator.board.ETHERNET;
import de.hopp.generator.board.ETHERNET_LITE;
import de.hopp.generator.board.IN;
import de.hopp.generator.board.LEDS;
import de.hopp.generator.board.OUT;
import de.hopp.generator.board.PCIE;
import de.hopp.generator.board.Ports;
import de.hopp.generator.board.SWITCHES;
import de.hopp.generator.board.UART;
import de.hopp.generator.board.VHDL;
import de.hopp.generator.board.VHDLCore;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MProcedure;

public class DriverVisitor extends Visitor<IOException>{

    private Configuration config;
    private IOHandler IO;
    
    private boolean debug;
    
    private MFile components;
    private MFile constants;
    
    private MProcedure init;
    
    // TODO split this into component communication and medium setup as well...
    // might be more difficult, since interrupt setup is interleaved for gpio components ):
    public DriverVisitor(Configuration config) {
        this.config = config;
        this.IO = config.IOHANDLER();
        
        // extract debug flag
        this.debug = config.debug();
        
        // setup basic methods
        components = MFile(MDocumentation(Strings()), "components", MDefinitions(), MStructs(),
                MEnums(), MAttributes(), MProcedures(), MClasses());
        constants  = MFile(MDocumentation(Strings()), "constants", MDefinitions(), MStructs(),
                MEnums(), MAttributes(), MProcedures(), MClasses());
        init  = MProcedure(MDocumentation(Strings()), MModifiers(), MVoid(), "init_components", 
                MParameters(), MCode(Strings()));
    }
    
    public MFile getComponentsFile() {
        return components;
    }
    public MFile getConstantsFile() {
        return constants;
    }
    
    public void visit(Board board) throws IOException {
        
        // add the debug constant
        constants = add(constants,
                MDefinition(MDocumentation(Strings()), MModifiers(PUBLIC()), "DEBUG", debug ? "1" : "0"));
        
        // add gpio source file, if gpio components are present
        if(hasGpioComponent(board)) {
            File target = new File(new File(config.serverDir(), "components"), "gpio");
            copy("deploy/server/components/gpio/gpio.h", new File(target, "gpio.h"), IO);
            copy("deploy/server/components/gpio/gpio.c", new File(target, "gpio.c"), IO);
        }
        
        // visit board components
        visit(board.components());
        
        // add the init procedure to the source file
        components = add(components, init);
    }
    
    public void visit(Components comps) throws IOException {
        for(Component c : comps) visit(c);            
    }

    public void visit(ETHERNET_LITE term) throws IOException {
        File target = new File(config.serverDir(), "medium");
        copy("deploy/server/medium/ethernet.h", new File(target, "ethernet.h"), IO);
        copy("deploy/server/medium/ethernet.c", new File(target, "ethernet.c"), IO);

        // add Ethernet specific constants
        addIP("IP",   config.getIP());
        addIP("MASK", config.getMask());
        addIP("GW",   config.getGW());
        addMAC(config.getMAC());
        addConst("PORT", "8844");
        
    }

    public void visit(ETHERNET term) { }
    public void visit(UART term)     { }
    public void visit(PCIE term)     { }

    public void visit(LEDS term) throws IOException {
        File target = new File(new File(config.serverDir(), "components"), "gpio");
        copy("deploy/server/components/gpio/led.h", new File(target, "led.h"), IO);
        copy("deploy/server/components/gpio/led.c", new File(target, "led.c"), IO);
        
        init = addLines(init, MCode(Strings("init_LED();"),
                MForwardDecl("int init_LED();")));
    }

    public void visit(SWITCHES term) throws IOException {
        File target = new File(new File(config.serverDir(), "components"), "gpio");
        copy("deploy/server/components/gpio/switch.h", new File(target, "switch.h"), IO);
        copy("deploy/server/components/gpio/switch.c", new File(target, "switch.c"), IO);
        
        init = addLines(init, MCode(Strings("init_switch();"),
                MForwardDecl("int init_switch();")));
     
        components = add(components, MProcedure(MDocumentation(Strings()), MModifiers(PRIVATE()), MVoid(),
                "callbackSwitches", MParameters(), MCode(Strings(
                     "// Test application: set LED state to Switch state",
                     "set_LED(read_switch());"
                ), MQuoteInclude("xbasic_types.h"),
                   MForwardDecl("u32 read_switch();"),
                   MForwardDecl("void set_LED(u32 state);"))));
    }

    public void visit(BUTTONS term) throws IOException {
        File target = new File(new File(config.serverDir(), "components"), "gpio");
        copy("deploy/server/components/gpio/button.h", new File(target, "button.h"), IO);
        copy("deploy/server/components/gpio/button.c", new File(target, "button.c"), IO);
    
        init = addLines(init, MCode(Strings("init_button();"),
                MForwardDecl("int init_button();")));
        
        components = add(components, MProcedure(MDocumentation(Strings()), MModifiers(PRIVATE()), MVoid(),
                "callbackButtons", MParameters(), MCode(Strings(
                     "// Test application: print out some text",
                     "xil_printf(\"\\nhey - stop pushing!! %d\", read_button());"
                ), MQuoteInclude("xbasic_types.h"), MForwardDecl("u32 read_button();"))));
    }
    
    // These currently shouldn't do anything.
    // Might get relevant after introducing AXI Stream communication into server
    public void visit(VHDLCore term) { }
    public void visit(VHDL term)     { }
    public void visit(Ports term)    { }
    public void visit(IN term)       { }
    public void visit(OUT term)      { }
    public void visit(DUAL term)     { }
    
    // literals
    public void visit(de.hopp.generator.board.Strings term) { }
    public void visit(String term) { }

    private static String unparseIP(int val) {
        // exclude invalid ip addresses 
        if(val > 255 || val < 0) throw new IllegalStateException();

        String rslt = new String();
        
        // add spaces for better formatting
        if(val < 100) {
            rslt += " ";
            if(val < 10) rslt += " ";
        }
        // append value
        return rslt + val;
    }
    
//    private String unparseMAC(String[] mac) {
//        return "0x" + mac[0] + ", 0x" + mac[1] + ", 0x" + mac[2] +
//             ", 0x" + mac[3] + ", 0x" + mac[4] + ", 0x" + mac[5];
//    }
//    
//    private String unparseIP(int[] ip) {
//        return unparseIP(ip[0]) + ", " + unparseIP(ip[1]) + ", " +
//               unparseIP(ip[2]) + ", " + unparseIP(ip[3]);
//    }

    private void addIP(String id, int[] ip) {
        addConst(id + "_1", "" + ip[0]);
        addConst(id + "_2", "" + ip[1]);
        addConst(id + "_3", "" + ip[2]);
        addConst(id + "_4", "" + ip[3]);
    }
    
    private void addMAC(String[] mac) {
        addConst("MAC_1", "0x" + mac[0]);
        addConst("MAC_2", "0x" + mac[1]);
        addConst("MAC_3", "0x" + mac[2]);
        addConst("MAC_4", "0x" + mac[3]);
        addConst("MAC_5", "0x" + mac[4]);
        addConst("MAC_6", "0x" + mac[5]);
    }
    
    private void addConst(String id, String val) {
        constants = add(constants, MDefinition(MDocumentation(Strings()), MModifiers(PUBLIC()), id, val));
    }

}