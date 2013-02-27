package de.hopp.generator.backends.client;

import static de.hopp.generator.backends.BackendUtils.doxygen;
import static de.hopp.generator.backends.BackendUtils.printMFile;
import static de.hopp.generator.model.Model.*;
import static de.hopp.generator.utils.Files.copy;
import static de.hopp.generator.utils.Model.add;
import static de.hopp.generator.utils.Model.addDoc;
import static de.hopp.generator.utils.Model.addInit;
import static de.hopp.generator.utils.Model.addParam;

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
import de.hopp.generator.model.MInitList;

/**
 * Generation backend for a host-side C++ driver.
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
    
    // MOAR integers...
    private int      pi = 0,      po = 0;
    private int     gpi = 0,     gpo = 0;
    private int core_pi = 0, core_po = 0;
    
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
        File clientApi = new File(clientSrc, "api");
        printMFile(comps, clientApi, UnparserType.HEADER, errors);
        printMFile(comps, clientApi, UnparserType.CPP, errors);
        
        // generate & print the constants file with the debug flag
        printMFile(MFile(MDocumentation(Strings(
                    "Defines several constants used by the client."
                )), "constants", MDefinitions(
                    MDefinition(MDocumentation(Strings(
                            "If set, enables additional console output for debugging purposes"
                    )), MModifiers(PUBLIC()), "DEBUG", config.debug() ? "1" : "0"),
                    MDefinition(MDocumentation(Strings(
                            "Defines the size of the server queues.",
                            "This is equivalent with the maximal number of values, that should be send in one message"
                    )), MModifiers(PUBLIC()), "QUEUE_SIZE", "20"),
                    MDefinition(MDocumentation(Strings(
                            "The number of in-going component ports"
                    )), MModifiers(PUBLIC()),  "IN_PORT_COUNT", String.valueOf(pi)),
                    MDefinition(MDocumentation(Strings(
                            "The number of out-going component ports"
                    )), MModifiers(PUBLIC()), "OUT_PORT_COUNT", String.valueOf(po)),
                    MDefinition(MDocumentation(Strings(
                            "The number of gpi components"
                    )), MModifiers(PUBLIC()), "GPI_COUNT", String.valueOf(gpi)),
                    MDefinition(MDocumentation(Strings(
                            "The number of gpo components"
                    )), MModifiers(PUBLIC()), "GPO_COUNT", String.valueOf(gpo))
                ), MStructs(), MEnums(), MAttributes(), MProcedures()),
                clientSrc, UnparserType.HEADER, errors);
    }
    
    public void visit(Components comps) {
        for(Component c : comps) visit(c);
    }
    
    public void visit(UART term) {
//        comps = add(comps, MAttribute(MDocumentation(Strings()), MModifiers(PRIVATE()),
//                MPointerType(MType("interface")), "intrfc",
//                MCodeFragment("new uart()", MQuoteInclude("interface.h"))));
    }
    
    public void visit(ETHERNET_LITE term) {
//        comps = add(comps, MAttribute(MDocumentation(Strings()), MModifiers(PRIVATE()),
//                MPointerType(MType("interface")), "intrfc",
//                MCodeFragment("new ethernet(\"192.168.1.10\", 8844)", MQuoteInclude("interface.h"))));
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
                "gpio_leds", MInitList(Strings(String.valueOf(gpi)), MQuoteInclude("gpio.h"))));
        gpi++;
    }
    
    public void visit(SWITCHES term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's switch component.",
                    "This object is used to read the state of the switches of the board."
                )), MModifiers(), MType("switches"),
                "gpio_switches", MInitList(Strings(String.valueOf(gpo)), MQuoteInclude("gpio.h"))));
        gpo++;
    }
    
    public void visit(BUTTONS term) {
        comps = add(comps, MAttribute(MDocumentation(Strings(
                    "The board's button component.",
                    "This object is used to read the state of the buttons of the board."
                )), MModifiers(), MType("buttons"),
                "gpio_buttons", MInitList(Strings(String.valueOf(gpo)), MQuoteInclude("gpio.h"))));
        gpo++;
    }
    
    public void visit(VHDL vhdl) {
        // generate a class for the vhdl core
        visit(vhdl.core());
        
        
        // iterate over instances
        for(String instance : vhdl.instances()) {
            MInitList init = MInitList(Strings());
            
            // add ports
            for(Port p : vhdl.core().ports()) {
                init = add(init, p.Switch(new Port.Switch<MInitList, NE>() {
                    MInitList init = MInitList(Strings());
                    public MInitList CaseIN(IN p)     { addInPort();  return init; }
                    public MInitList CaseOUT(OUT p)   { addOutPort(); return init; }
                    public MInitList CaseDUAL(DUAL p) { 
                        addInPort(); addOutPort();
                        return init;
                    }
                    private void addInPort() {
                        init = add(init, String.valueOf(pi));
                        pi++;
                    }
                    private void addOutPort() {
                        init = add(init, String.valueOf(po));
                        po++;
                    }
                }));
            }
            
            // add attribute to component file
            comps = add(comps, MAttribute(MDocumentation(Strings(
                    "An instance of the #" + vhdl.core().file() + " core."
                )), MModifiers(PUBLIC()), MType(vhdl.core().file()), instance, init));

            // and increment the port counts
//            pi += core_pi; po += core_po;
        }
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
                )), MModifiers(PUBLIC()), MParameters(), MMemberInits(), MCode(Strings()));
        destructor  = MDestr(MDocumentation(Strings(
                    "Destructor for #" + core.file() + " cores.",
                    "Deletes registered ports and unregisters the core from the communication medium."
                )), MModifiers(PUBLIC()), MParameters(), MCode(Strings()));

//        core_pi = 0;
//        core_po = 0;
        
        visit(core.ports());
        
        comp  = add(comp,  constructor);
        comp  = add(comp,  destructor);
        comps = add(comps, comp);
    }
    
    public void visit(Ports ports) {
        for(Port p : ports) { visit(p); }
    }
    
//    public void visit(IN   port) { addPort(port.name(),   "in", "An in-going");      core_pi++; }
//    public void visit(OUT  port) { addPort(port.name(),  "out", "An out-going");     core_po++; }
//    public void visit(DUAL port) { addPort(port.name(), "dual", "A bi-directional"); core_pi++; core_po++; }
    public void visit(IN   port) {
        addPort(port.name(),   "in", "An in-going AXI-Stream port.", true);
    }
    public void visit(OUT  port) {
        addPort(port.name(),  "out", "An out-going AXI-Stream port.", true); 
    }
    public void visit(DUAL port) {
        addPort(port.name(), "dual", "A bi-directional AXI-Stream port.", false);
    }
    
    private void addPort(String name, String type, String docPart, boolean single) {
        comp = add(comp, MAttribute(MDocumentation(Strings(
                    docPart, "Communicate with the #" + comp.name() + " core through this port."
                )), MModifiers(PUBLIC()), MType(type),
                name, MCodeFragment("", MQuoteInclude("component.h"))));
        
        if(single) {
            constructor = constructor.replaceDoc(constructor.doc().replaceTags(constructor.doc().tags().add(
                        PARAM(name, "Id of the port")
                    )));
            constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name));
            constructor = addInit(constructor, MMemberInit(name, name));
        } else {
            constructor = constructor.replaceDoc(constructor.doc().replaceTags(constructor.doc().tags().addAll(MTags(
                    PARAM(name + "_in",  "Id of the in-going part of the port"),
                    PARAM(name + "_out", "Id of the out-going part of the port")
                ))));
            constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name + "_in"));
            constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name + "_out"));
            constructor = addInit(constructor, MMemberInit(name, name + "_in", name + "_out"));
        }
        
//        destructor  = addLines( destructor, MCode(Strings("delete " + name + ";")));
    }

    public void visit(Instances term) { }
    public void visit(Integer term)   { }
    public void visit(String term)    { }
}
