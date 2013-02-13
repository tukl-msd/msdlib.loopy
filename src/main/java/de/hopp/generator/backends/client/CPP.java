package de.hopp.generator.backends.client;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.Files.copy;
import static de.hopp.generator.utils.Model.add;
import static de.hopp.generator.utils.Model.addDoc;
import static de.hopp.generator.utils.Model.addInit;
import static de.hopp.generator.utils.Model.addLines;

import java.io.File;
import java.io.IOException;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.Backend;
import de.hopp.generator.backends.BackendUtils.UnparserType;
import de.hopp.generator.backends.GenerationFailed;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.model.MClass;
import de.hopp.generator.model.MConstr;
import de.hopp.generator.model.MDestr;
import de.hopp.generator.model.MFile;

/**
 * Generation backend for a client-side C++ driver.
 * This visitor generates a C++ API for communication with an arbitrary board-side driver.
 * @author Thomas Fischer
 */
public class CPP extends Visitor<NE> implements Backend {

    private Configuration   config;
    private ErrorCollection errors;
    
    private MFile comps;

    // temp variables for construction of local methods of VHDL components
    private MClass  comp;
    private MConstr constructor;
    private MDestr  destructor;
    
    // local variables for global default methods
//    private MMethod clean;
    
    public CPP() {
        comps = MFile(MDocumentation(Strings()), "components", MDefinitions(), MStructs(),
                MEnums(), MAttributes(), MProcedures(), MClasses());
    }
    
    public String getName() {
        return "C++";
    }
    
    public void generate(Board board, Configuration config, ErrorCollection errors) {
        this.config = config;
        this.errors = errors;
        
        // deploy generic client code
        try {
            copy("deploy/client/cpp", config.clientDir(), config.IOHANDLER());
        } catch(IOException e) {
            errors.addError(new GenerationFailed(""));
            return;
        }
       
        // generate and deploy board-specific MFiles
        visit(board);
        
        // generate api specification
        config.IOHANDLER().println("  generate client-side api specification ... ");
        doxygen(config.clientDir(), config.IOHANDLER(), errors);
    }
    
    public void visit(Board board) {
        comps = addDoc(comps, "Describes user-defined IPCores and instantiates all cores present within this driver.");
        visit(board.components());
        
        // unparse generated MFiles
        File clientSrc = new File(config.clientDir(), "src");
        printMFile(comps, clientSrc, UnparserType.HEADER, errors);
        printMFile(comps, clientSrc, UnparserType.CPP, errors);
        
        // generate & print the constants file with the debug flag
        printMFile(MFile(MDocumentation(Strings(
                    "Defines several constants used by the client."
                )), "constants", MDefinitions(
                    MDefinition(MDocumentation(Strings(
                            "If set, enables additional console output for debugging purposes"
                    )), MModifiers(PUBLIC()), "DEBUG", config.debug() ? "1" : "0"
                )), MStructs(), MEnums(), MAttributes(), MProcedures()),
                new File(config.clientDir(), "src"), UnparserType.HEADER, errors);
    }
    
    public void visit(Components comps) {
        for(Component c : comps) visit(c);
    }
    
    public void visit(UART term) {
        comps = add(comps, MAttribute(MDocumentation(Strings()), MModifiers(PRIVATE()),
                MPointerType(MType("interface")), "intrfc",
                MCodeFragment("new uart()", MQuoteInclude("interface.h"))));
    }
    
    public void visit(ETHERNET_LITE term) {
        comps = add(comps, MAttribute(MDocumentation(Strings()), MModifiers(PRIVATE()),
                MPointerType(MType("interface")), "intrfc",
                MCodeFragment("new ethernet(\"192.168.1.10\", 8844)", MQuoteInclude("interface.h"))));
    }
    
    public void visit(ETHERNET term) {
        // TODO Auto-generated method stub
    }
    
    public void visit(PCIE term) {
        // TODO Auto-generated method stub
    }
    
    public void visit(LEDS term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's LED component.",
                    "This object is used to manipulate the state of the LEDs of the board."
                )), MModifiers(), MType("leds"),
                "gpio_leds", MInitList(Strings("intrfc"), MQuoteInclude("gpio.h"))));
    }
    
    public void visit(SWITCHES term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's switch component.",
                    "This object is used to read the state of the switches of the board."
                )), MModifiers(), MType("switches"),
                "gpio_switches", MInitList(Strings("intrfc"), MQuoteInclude("gpio.h"))));
    }
    
    public void visit(BUTTONS term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's button component.",
                    "This object is used to read the state of the buttons of the board."
                )), MModifiers(), MType("buttons"),
                "gpio_buttons", MInitList(Strings("intrfc"), MQuoteInclude("gpio.h"))));
    }
    
    public void visit(VHDL vhdl) {
        // generate a class for the vhdl core
        visit(vhdl.core());
        
        // add an attribute for each used name
        for(String instance : vhdl.instances())
            comps = add(comps, MAttribute(MDocumentation(Strings(
                    "An instance of the #" + vhdl.core().file() + " core."
                )), MModifiers(PUBLIC()),
                MType(vhdl.core().file()), instance,
                MInitList(Strings("intrfc"))));
    }
    
    public void visit(VHDLCore core) {
        comp = MClass(MDocumentation(Strings(
                    "An abstract representation of a(n) #" + core.file() + " core."
                ), SEE("components.h for a list of specific core instances within this board driver.")
                ), MModifiers(), core.file(), MExtends(MExtend(PRIVATE(), MType("component"))),
                MStructs(), MEnums(), MAttributes(), MMethods());

        constructor = MConstr(MDocumentation(Strings(
                    "Constructor for #" + core.file() + " cores.",
                    "Creates a new " + core.file() + " instance on a board attached to the provided communication medium."
                ), MTags(PARAM("intrfc", "The communication medium, the cores board is attached with.")
                )), MModifiers(PUBLIC()), MParameters(
                    MParameter(VALUE(), MPointerType(MType("interface")), "intrfc")
                ), MInit(MConstrCall("component", "intrfc")), MCode(Strings()));
        destructor  = MDestr(MDocumentation(Strings(
                    "Destructor for #" + core.file() + " cores.",
                    "Deletes registered ports and unregisters the core from the communication medium."
                )), MModifiers(PUBLIC()), MParameters(), MCode(Strings()));
        
        visit(core.ports());
        
        comp  = add(comp,  constructor);
        comp  = add(comp,  destructor);
        comps = add(comps, comp);
    }
    
    public void visit(Ports ports) {
        for(Port p : ports) { visit(p); }
    }
    
    public void visit(IN   port) { addPort(port.name(),   "in", "An in-going"); }
    public void visit(OUT  port) { addPort(port.name(),  "out", "An out-going"); }
    public void visit(DUAL port) { addPort(port.name(), "dual", "A bi-directional"); }
    
    private void addPort(String name, String type, String docPart) {
        comp = add(comp, MAttribute(MDocumentation(Strings(
                    docPart + " AXI-Stream port.",
                    "Communicate with the #" + comp.name() + " core through this port."
                )), MModifiers(PUBLIC()), MType(type),
                name, MCodeFragment("", MQuoteInclude("component.h"))));
//        constructor = addInit(constructor, MMemberInit(name, "new " + type + "()"));
//        destructor  = addLines( destructor, MCode(Strings("delete " + name + ";")));
    }

    public void visit(Instances term) { }
    public void visit(Integer term)   { }
    public void visit(String term)    { }
}
