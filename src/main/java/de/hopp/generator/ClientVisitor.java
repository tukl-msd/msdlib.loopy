package de.hopp.generator;

import static de.hopp.generator.model.Model.*;

import katja.common.NE;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.board.Strings;
import de.hopp.generator.model.*;

public class ClientVisitor extends Visitor<NE> {

    private Configuration config;
    
//    private MFile file;
    private MFile comps;

    // local methods for construction of more specific components
    private MClass  comp;
    private MConstr constructor;
    private MDestr  destructor;
    
    // local variables for global default methods
//    private MMethod init;
//    private MMethod clean;
//    private MMethod main;
    
    private MDocumentation emptyDoc = MDocumentation(Strings());
    
    public ClientVisitor(Configuration config) {
        this.config = config;
        
        // setup basic methods
//        file  = MFile(emptyDoc, "name", MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
        comps = MFile(emptyDoc, "components", MDefinitions(), MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
//        init  = MProcedure(emptyDoc, MModifiers(), MType("int"), "init", 
//                MParameters(), MCode(Strings("")));
//        clean = MMethod(MDocumentation(Strings()), MModifiers(), MType("int"), "cleanup", 
//                MParameters(), MCode(Strings(""), MInclude("platform.h", QUOTES())));
//        main  = MProcedure(emptyDoc, MModifiers(), MType("int"), "main", 
//                MParameters(), MCode(Strings("", "// initialize board components", "init();")));
    }
    
//    public MFile getFile() {
//        return file;
//    }
    public MFile getCompsFile() {
        return comps;
    }
    
    public void visit(Board board) {
        comps = addDoc(comps, "Describes user-defined IPCores and instantiates all cores present within this driver.");
        visit(board.components());
    }
    public void visit(Components comps) {
        for(Component c : comps) visit(c);
    }
    public void visit(UART term) {
//        comps = add(comps, MAttribute(MDocumentation(Strings()), MModifiers(PRIVATE()),
//                MPointerType(MType("interface")), "intrfc",
//                MCodeFragment("new uart()", MInclude("interface.h", QUOTES()))));
    }
    public void visit(ETHERNET_LITE term) {
        comps = add(comps, MAttribute(emptyDoc, MModifiers(PRIVATE()),
                MPointerType(MType("interface")), "intrfc",
                MCodeFragment("new ethernet(\"192.168.1.10\", 8844)", MInclude("interface.h", QUOTES()))));
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
                )), MModifiers(), MPointerType(MType("leds")),
                "gpio_leds", MCodeFragment("new leds(intrfc)", MInclude("gpio.h", QUOTES()))));
    }
    public void visit(SWITCHES term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's switch component.",
                    "This object is used to read the state of the switches of the board."
                )), MModifiers(), MPointerType(MType("switches")),
                "gpio_switches", MCodeFragment("new switches(intrfc)", MInclude("gpio.h", QUOTES()))));
    }
    public void visit(BUTTONS term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's button component.",
                    "This object is used to read the state of the buttons of the board."
                )), MModifiers(), MPointerType(MType("buttons")),
                "gpio_buttons", MCodeFragment("new buttons(intrfc)", MInclude("gpio.h", QUOTES()))));
    }
    public void visit(VHDL vhdl) {
        // generate a class for the vhdl core
        visit(vhdl.core());
        
        // add an attribute for each used name
        for(String name : vhdl.names())
            comps = add(comps, MAttribute(MDocumentation(Strings(
                    "An instance of the #" + vhdl.core().file() + " core."
                )), MModifiers(PUBLIC()),
                MPointerType(MType(vhdl.core().file())), name, MCodeFragment("new " + vhdl.core().file() + " (intrfc)")));
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
                )), MModifiers(PUBLIC()), MPointerType(MType(type)),
                name, MCodeFragment("", MInclude("component.h", QUOTES()))));
        constructor = addInit(constructor, MMemberInit(name, "new " + type + "()"));
        destructor  = addLines( destructor, MCode(Strings("delete " + name + ";")));
    }
    
    public void visit(Integer term) { }
    public void visit(Strings term) { }
    public void visit(String term)  { }
    
    private static MFile add(MFile file, MClass c) {
        return file.replaceClasses(file.classes().add(c));
    }
    private static MFile add(MFile file, MAttribute a) {
        return file.replaceAttributes(file.attributes().add(a));
    }
    private static MClass add(MClass c, MAttribute a) {
        return c.replaceAttributes(c.attributes().add(a));
    }
    private static MClass add(MClass c, MMethod m) {
        return c.replaceMethods(c.methods().add(m));
    }
    private static MConstr addInit( MConstr c, MMemberInit i ) {
        return c.replaceInit(c.init().replaceVals(c.init().vals().add(i)));
    }
    private static MProcedure addLines(MProcedure m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    private static MConstr addLines(MConstr m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    private static MDestr addLines(MDestr m, MCode c) {
        return m.replaceBody(m.body().replaceLines(m.body().lines().addAll(c.lines()))
                .replaceNeeded(m.body().needed().addAll(c.needed())));
    }
    private static MFile addDoc(MFile file, String line) {
        return file.replaceDoc(file.doc().replaceDoc(file.doc().doc().add(line)));
    }
}
