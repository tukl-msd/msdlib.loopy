package de.hopp.generator.backends.host.cpp;

import static de.hopp.generator.model.cpp.CPP.*;
import static de.hopp.generator.utils.BoardUtils.defaultQueueSizeHW;
import static de.hopp.generator.utils.BoardUtils.defaultQueueSizeSW;
import static de.hopp.generator.utils.BoardUtils.getPort;
import static de.hopp.generator.utils.BoardUtils.getWidth;
import static de.hopp.generator.utils.BoardUtils.isPolling;
import static de.hopp.generator.utils.CPPUtils.add;
import static de.hopp.generator.utils.CPPUtils.addDoc;
import static de.hopp.generator.utils.CPPUtils.addInit;
import static de.hopp.generator.utils.CPPUtils.addParam;

import java.io.File;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.backends.board.BoardBackend;
import de.hopp.generator.backends.board.GpioComponent;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.model.*;
import de.hopp.generator.model.BDLFilePos.Visitor;
import de.hopp.generator.model.cpp.MClass;
import de.hopp.generator.model.cpp.MConstr;
import de.hopp.generator.model.cpp.MDestr;
import de.hopp.generator.model.cpp.MFile;
import de.hopp.generator.model.cpp.MInitList;

public class CPPBDLVisitor extends Visitor<NE> {

    BoardBackend board;
    ErrorCollection errors;

    // generated files
    MFile comps;
    MFile consts;
    MFile logger;

    // temp variables for construction of local methods of VHDL components
    private MClass  comp;
    private MConstr constructor;
    private MDestr  destructor;

    // MOAR integers...
    private int  pi = 0,  po = 0;
    private int gpi = 0, gpo = 0;

  // local variables for global default methods
//  private MMethod clean;

    public CPPBDLVisitor(Configuration config, ErrorCollection errors) {
        this.board = config.board();
        this.errors = errors;
        String clientSrc = new File(config.hostDir(), "src").getPath();
        String clientApi = new File(clientSrc, "api").getPath();

        comps = MFile(MDocumentation(Strings()), "components", clientApi, MPreProcDirs(),
                MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());
        consts = MFile(MDocumentation(Strings()), "constants", clientSrc, MPreProcDirs(),
                MStructs(), MEnums(), MAttributes(), MProcedures(), MClasses());

        comps  = addDoc(comps,  "Describes user-defined IPCores and instantiates all cores present within this driver.");
        consts = addDoc(consts, "Defines several constants used by the client.");

        logger = MFile(MDocumentation(Strings()), "logger", clientSrc,
            MPreProcDirs(), MStructs(), MEnums(), MAttributes(), MProcedures());
    }

    public void visit(BDLFilePos term) {

        boolean debug = false;
        int queueSizeSW = defaultQueueSizeSW, queueSizeHW = defaultQueueSizeHW;

        for(Option o : term.opts().term()) {
            // duplicates and invalid parameters are already caught by sanity check
            if(o instanceof HWQUEUE) queueSizeHW = ((HWQUEUE)o).qsize();
            if(o instanceof SWQUEUE) queueSizeSW = ((SWQUEUE)o).qsize();
//            if(o instanceof DEBUG)   debug = true;
        }

        // add derived constants
        consts = add(consts, MDef(MDocumentation(Strings(
                "If set, enables additional console output for debugging purposes"
            )), MModifiers(PUBLIC()), "DEBUG", debug ? "1" : "0"));
        consts = add(consts, MDef(MDocumentation(Strings(
                "Defines the default size of the boards hardware queues."
            )), MModifiers(PUBLIC()), "QUEUE_SIZE_HW", String.valueOf(queueSizeHW)));
        consts = add(consts, MDef(MDocumentation(Strings(
                "Defines the default size of the boards software queues.",
                "This is equivalent with the maximal number of values, " +
                "that should be send in one message"
            )), MModifiers(PUBLIC()), "QUEUE_SIZE_SW", String.valueOf(queueSizeSW)));

        visit(term.logs());
        visit(term.medium());
        visit(term.gpios());
        visit(term.scheduler());
        visit(term.cores());
        visit(term.insts());

        consts = add(consts, MDef(MDocumentation(Strings(
                "The number of in-going component ports"
            )), MModifiers(PUBLIC()),  "IN_PORT_COUNT", String.valueOf(pi)));
        consts = add(consts, MDef(MDocumentation(Strings(
                "The number of out-going component ports"
            )), MModifiers(PUBLIC()), "OUT_PORT_COUNT", String.valueOf(po)));
        consts = add(consts, MDef(MDocumentation(Strings(
                "The number of gpi components"
            )), MModifiers(PUBLIC()), "GPI_COUNT", String.valueOf(gpi)));
        consts = add(consts, MDef(MDocumentation(Strings(
                "The number of gpo components"
            )), MModifiers(PUBLIC()), "GPO_COUNT", String.valueOf(gpo)));
    }

    // We assume all imports to be accumulated at the parser
    public void visit(ImportsPos term)   { }

    public void visit(LogsPos term) {
        addLogger("logger_host",  "Host:  ", term.host().termLog());
        addLogger("logger_board", "Board: ", term.board().termLog());
    }

    private void addLogger(final String name, final String prefix, final Log log) {
        MInitList initList = log.Switch(new Log.Switch<MInitList, NE>() {
            public MInitList CaseNONE(NONE term) {
                return MInitList(Strings("NULL", "0", "\"" + prefix + "\""));
            }
            public MInitList CaseCONSOLE(CONSOLE term) {
                return MInitList(Strings("&std::cout", term.sev().sortName(), "\"" + prefix + "\""));
            }
            public MInitList CaseFILE(FILE term) {
                return MInitList(Strings("new std::ofstream(\"" + term.file() + "\")",
                    term.sev().sortName(), "\"" + prefix + "\""));
            }

        });
        logger = add(logger, MAttribute(MDocumentation(Strings()),
            MModifiers(), MType("logger"), name, initList));
    }

    public void visit(OptionsPos term) { }

        public void visit(ETHERNETPos term) {
        for(MOption opt : term.opts().term()) {
            if(opt instanceof IP) {
                consts = add(consts, MDef(
                    MDocumentation(Strings("IP for Ethernet communication")),
                    MModifiers(PUBLIC()), "IP", "\"" + ((IP)opt).val() + "\""));
            } else if(opt instanceof PORTID) {
                consts = add(consts, MDef(
                    MDocumentation(Strings("Data port for Ethernet communication")),
                    MModifiers(PUBLIC()), "PORT", ((PORTID)opt).val().toString()));
            }
        }
    }

    public void visit(UARTPos term) { }

    public void visit(PCIEPos term) { }

        public void visit(GPIOPos term) {
        // construct init block according to GPIO direction
        MInitList init = MInitList(Strings(), MIncludes(MQuoteInclude(PRIVATE(), "gpio.h")));

        GpioComponent gpio;
        try {
            gpio = board.getGpio(term.name().term());
        } catch (IllegalArgumentException e) {
            errors.addError(new ParserError(e.getMessage(), term.pos().term()));
            return;
        }

        if(gpio.isGPI()) init = add(init, String.valueOf(gpi++));
        if(gpio.isGPO()) init = add(init, String.valueOf(gpo++));

        if(gpio.isGPI() && gpio.isGPO()) errors.addError(
            new UsageError("bi-directional gpio components are currently not supported by the c++ client backend"));

        // add attribute for the GPIO component
        comps = add(comps, MAttribute(MDocumentation(Strings(
                "An instance of the #" + gpio.id() + " core."
            )), MModifiers(PUBLIC()), MType("class " + (gpio.isGPI() ? "gpi" : "gpo") + "<" + gpio.width() + ">"),
            "gpio_"+ gpio.id(), init));
    }

    public void visit(InstancePos term) {
        if(!hasCPUConnection(term)) return;

        comp = MClass(MDocumentation(Strings(
                "An abstract representation of the #" + term.name().term() + " core."
            ), SEE("components.h for a list of core instances within this board driver.")
            ), MModifiers(), term.name().term(), MExtends(MExtend(PRIVATE(), MType("component"))),
            MStructs(), MEnums(), MAttributes(), MMethods());

        constructor = MConstr(MDocumentation(Strings(
                "Constructor for the #" + term.name().term() + " core.",
                "Creates a new " + term.name().term() + " instance on a board attached to the provided communication medium."
            )), MModifiers(PUBLIC()), MParameters(), MMemberInits(), MCode(Strings(), MQuoteInclude(PRIVATE(), "component.h")));
        destructor  = MDestr(MDocumentation(Strings(
                "Destructor for the #" + term.name().term() + " core.",
                "Deletes registered ports and unregisters the core from the communication medium."
            )), MModifiers(PUBLIC()), MParameters(), MCode(Strings()));

        MInitList init = MInitList(Strings());
        // add ports
        for(BindingPos bind : term.bind()) {
            if(! (bind instanceof CPUAxisPos)) continue; // skip non-cpu axis
            if(isMasterConnection(bind)) init = add(init, String.valueOf(pi++));
            if(isSlaveConnection(bind))  init = add(init, String.valueOf(po++));
            if(isSlaveConnection(bind))  init = add(init, isPolling((CPUAxisPos)bind) ? "1" : "0");
        }

        // visit bindings to add ports to component
        visit(term.bind());

        // compose class and add to file
        comp  = add(comp,  constructor);
        comp  = add(comp,  destructor);
        comps = add(comps, comp);

        // add attribute to component file
        comps = add(comps, MAttribute(MDocumentation(Strings(
                "An instance of the #" + term.name().term() + " core."
            )), MModifiers(PUBLIC()), MType("class " + term.name().term()), term.name().term(), init));
    }

    public void visit(final CPUAxisPos axis) {
        AXIPos port = getPort(axis);
        final int width = getWidth(axis);
        port.direction().Switch(new DirectionPos.Switch<Object, NE>() {
            public Object CaseINPos(INPos term) {
                addInPort(axis.port().term(), width);
                return null;
            }
            public Object CaseDUALPos(DUALPos term) {
                addDualPort(axis.port().term(), width);
                return null;
            }
            public Object CaseOUTPos(OUTPos term) {
                addOutPort(axis.port().term(), width);
                return null;
            }
        });
    }

    private boolean hasCPUConnection(InstancePos term) {
        for(BindingPos bind : term.bind()) if(isCPUConnection(bind)) return true;
        return false;
    }

    private boolean isCPUConnection(BindingPos term) {
        if(term instanceof CPUAxisPos) return true;
        return false;
    }

    private static boolean isMasterConnection(BindingPos term) {
        AXIPos port = getPort(term);
        return port.direction().Switch(new DirectionPos.Switch<Boolean, NE>() {
            public Boolean CaseDUALPos(DUALPos term) { return true;  }
            public Boolean CaseOUTPos(OUTPos term)   { return false; }
            public Boolean CaseINPos(INPos term)     { return true;  }
        });
    }
    private static boolean isSlaveConnection(BindingPos term) {
        AXIPos port = getPort(term);
        return port.direction().Switch(new DirectionPos.Switch<Boolean, NE>() {
            public Boolean CaseDUALPos(DUALPos term) { return true;  }
            public Boolean CaseOUTPos(OUTPos term)   { return true; }
            public Boolean CaseINPos(INPos term)     { return false;  }
        });
    }

    private void addInPort(String name, int width) {
        comp = add(comp, MAttribute(MDocumentation(Strings(
                "An in-going AXI-Stream port.",
                "Communicate with the #" + comp.name() + " core through this port."
            )), MModifiers(PUBLIC()), MType("inPort<"+width+">"),
            name, MCodeFragment("",
                MQuoteInclude(PUBLIC(), "component.h"),
                MQuoteInclude(PUBLIC(), "portIn.h")
            )
        ));

        constructor = constructor.replaceDoc(constructor.doc().replaceTags(
            constructor.doc().tags().add(PARAM(name, "Id of the port"))
        ));
        constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name));
        constructor = addInit(constructor, MMemberInit(name, name));
    }

    private void addOutPort(String name, int width) {
        comp = add(comp, MAttribute(MDocumentation(Strings(
                "An out-going AXI-Stream port.",
                "Communicate with the #" + comp.name() + " core through this port."
            )), MModifiers(PUBLIC()), MType("outPort<"+width+">"),
            name, MCodeFragment("",
                MQuoteInclude(PUBLIC(), "component.h"),
                MQuoteInclude(PUBLIC(), "portOut.h")
            )
        ));

        constructor = constructor.replaceDoc(constructor.doc().replaceTags(constructor.doc().tags().addAll(MTags(
            PARAM(name, "Id of the port"), PARAM(name + "_poll", "Poll flag of the port")
        ))));
        constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name));
        constructor = addParam(constructor, MParameter(VALUE(), MType("bool"), name + "_poll"));
        constructor = addInit(constructor, MMemberInit(name, name, name + "_poll"));
    }

    private void addDualPort(String name, int width) {
        comp = add(comp, MAttribute(MDocumentation(Strings(
                "An bi-directional AXI-Stream port.",
                "Communicate with the #" + comp.name() + " core through this port."
            )), MModifiers(PUBLIC()), MType("dualPort<"+width+">"),
            name, MCodeFragment("",
                MQuoteInclude(PUBLIC(), "component.h"),
                MQuoteInclude(PUBLIC(), "portIn.h"), // TODO reduce imports - port header including both
                MQuoteInclude(PUBLIC(), "portOut.h")
            )
        ));

        constructor = constructor.replaceDoc(constructor.doc().replaceTags(constructor.doc().tags().addAll(MTags(
            PARAM(name + "_in",  "Id of the in-going part of the port"),
            PARAM(name + "_out", "Id of the out-going part of the port"),
            PARAM(name + "_poll", "Poll flag of the port")
        ))));
        constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name + "_in"));
        constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name + "_out"));
        constructor = addParam(constructor, MParameter(VALUE(), MType("unsigned char"), name + "_poll"));
        constructor = addInit(constructor, MMemberInit(name, name + "_in", name + "_out", name + "_poll"));
    }

    // list types
    public void visit(GPIOsPos     term) { for(    GPIOPos gpio : term) visit(gpio); }
    public void visit(InstancesPos term) { for(InstancePos inst : term) visit(inst); }
    public void visit(BindingsPos  term) { for( BindingPos bind : term) visit(bind); }
    public void visit(MOptionsPos  term) { for( MOptionPos  opt : term) visit( opt); }

    // general (handled before this visitor)
    public void visit(ImportPos term)  { }

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
    public void visit(POLLPos     term) { }

    // logger options
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
    public void visit(AXIPos   term) { }
    public void visit(CLKPos   term) { }
    public void visit(RSTPos   term) { }
    public void visit(INPos    term) { }
    public void visit(OUTPos   term) { }
    public void visit(DUALPos  term) { }

    // component axis (these get ignored... that's the whole point)
    public void visit(AxisPos term) { }

    // positions
    public void visit(PositionPos term) { }

    // literals
    public void visit(IntegerPos term) { }
    public void visit(BooleanPos term) { }
    public void visit(StringsPos term) { }
    public void visit(StringPos  term) { }
}
