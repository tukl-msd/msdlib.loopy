package de.hopp.generator.utils;

import katja.common.NE;
import de.hopp.generator.frontend.*;

/**
 * Provides several utility methods concerning the board model.
 * 
 * This mainly concerns easy access to (referenced) elements, that are
 * required to exist by definition.
 * 
 * @author Thomas Fischer
 */
public class BoardUtils {
    
    /** The default size for board-side software queues. */
    public static final int defaultQueueSizeSW = 1024;
    /** The default size for board-side hardware queues. */
    public static final int defaultQueueSizeHW = 64;
    /** The default bitwidth of ports. */
    public static final int defaultWidth = 32;
    
//    public static boolean hasLEDs(Board board) {
//        for(Component comp : board.components())
//            if(comp instanceof LEDS) return true;
//        return false;
//    }
//    public static boolean hasSwitches(Board board) {
//        for(Component comp : board.components())
//            if(comp instanceof SWITCHES) return true;
//        return false;
//    }
//    public static boolean hasButtons(Board board) {
//        for(Component comp : board.components())
//            if(comp instanceof BUTTONS) return true;
//        return false;
//    }
//    
//    public static boolean hasGpioComponent(Board board) {
//        for(Component comp : board.components()) {
//            if(comp.Switch(new Component.Switch<Boolean, NE>() {
//                public Boolean CaseUART(UART term)                   { return false; }
//                public Boolean CaseETHERNET_LITE(ETHERNET_LITE term) { return false; }
//                public Boolean CaseETHERNET(ETHERNET term)           { return false; }
//                public Boolean CasePCIE(PCIE term)                   { return false; }
//                public Boolean CaseLEDS(LEDS term)                   { return true;  }
//                public Boolean CaseSWITCHES(SWITCHES term)           { return true;  }
//                public Boolean CaseBUTTONS(BUTTONS term)             { return true;  }
//                public Boolean CaseVHDL(VHDL term)                   { return false; }
//            })) return true;
//        }
//        return false;
//    }
//    
//    public static boolean hasGpiComponent(Board board) {
//        for(Component comp : board.components()) {
//            if(comp.Switch(new Component.Switch<Boolean, NE>() {
//                public Boolean CaseUART(UART term)                   { return false; }
//                public Boolean CaseETHERNET_LITE(ETHERNET_LITE term) { return false; }
//                public Boolean CaseETHERNET(ETHERNET term)           { return false; }
//                public Boolean CasePCIE(PCIE term)                   { return false; }
//                public Boolean CaseLEDS(LEDS term)                   { return false; }
//                public Boolean CaseSWITCHES(SWITCHES term)           { return true;  }
//                public Boolean CaseBUTTONS(BUTTONS term)             { return true;  }
//                public Boolean CaseVHDL(VHDL term)                   { return false; }
//            })) return true;
//        }
//        return false;
//    }
    
    /**
     * Generates a String representing the provided board description file.
     * @param board The board description to be printed.
     * @return A string representing the provided board description.
     */
    public static String printBoard(BDLFilePos board) {
        String rslt = "parsed the following board";
        rslt += "\n  -debug: " + (isDebug(board) ? "yes" : "no");
        rslt += "\n  -" + board.medium().termMedium().Switch(new Medium.Switch<String, NE>() {
            public String CaseNONE(NONE term) {
                return "no medium";
            }
            public String CaseETHERNET(ETHERNET term) {
                return "medium Ethernet" + printOptions(term.opts());
            }
            public String CaseUART(UART term) {
                return "medium UART" + printOptions(term.opts());
            }
            public String CasePCIE(PCIE term) {
                return "medium PCIE" + printOptions(term.opts());
            }
            private String printOptions(MOptions opts) {
                String rslt = "";
                for(MOption opt : opts) {
                    rslt += "\n    -" + opt.Switch(new MOption.Switch<String, NE>() {
                        public String CaseMAC(MAC term)       { return "mac  " + term.val(); }
                        public String CaseIP(IP term)         { return "ip   " + term.val(); }
                        public String CaseMASK(MASK term)     { return "mask " + term.val(); }
                        public String CaseGATE(GATE term)     { return "gate " + term.val(); }
                        public String CasePORTID(PORTID term) { return "port " + term.val(); }
                    });
                }
                return rslt;
            }
        });
        for(de.hopp.generator.frontend.GPIO gpio : board.gpios().term())
            rslt += "\n  -GPIO component " + gpio.name();
        for(Instance inst : board.insts().term())
            rslt += "\n  -VHDL Component " + inst.name() + " (" + inst.core() + ")";
        return rslt;
    }
    
    /**
     * Checks if the debug flag has been set in a board description file.
     * @param board The board description to be checked.
     * @return true, if the debug flag has been set, false otherwise.
     */
    public static boolean isDebug(BDLFilePos board) {
        boolean debug = false;
        for(OptionPos opt : board.opts()) if(opt instanceof DEBUGPos) debug = true;
        return debug;
    }
    
    /**
     * Get the core referenced by a specific instance.
     * @param inst The instance.
     * @return The core referenced by the instance.
     * @throws IllegalStateException If no core with the given core identifier exists.
     * The existence of such a core has to be guaranteed by the frontend.
     */
    public static CorePos getCore(InstancePos inst) {
        String coreName = inst.core().term();
        String coreVer  = inst.version().term();
        
        // return the core, if it exists
        for(CorePos c : inst.root().cores())
            if(c.name().term().equals(coreName) && c.version().term().equals(coreVer))
                return c;
        
        // otherwise, throw an exception (should never happen due to sanity checks)
        throw new IllegalStateException();
    }
    
    /**
     * Get the port declaration referenced by a specific port binding.
     * @param bind The port binding.
     * @return The port declaration referenced by the port binding.
     * @throws IllegalStateException If no port declaration with the given port identifier
     * exists within the parent core declaration. The existence of such a port declaration
     * has to be guaranteed by the frontend.
     */
    public static AXIPos getPort(BindingPos bind) {
        String portName = bind.port().term();

        // throw an exception, if the parent is not a core instance
        if(!(bind.parent().parent() instanceof InstancePos)) throw new IllegalStateException();
        InstancePos inst = ((InstancePos)bind.parent().parent());

        // return the port, if it exists
        for(PortPos p : getCore(inst).ports())
            if(p.name().term().equals(portName) && p instanceof AXIPos) return (AXIPos)p;

        // otherwise, throw an exception (should never happen due to sanity checks)
        throw new IllegalStateException();
    }
    
    /**
     * Get the clock frequency of the provided core
     * @param core A core.
     * @return The clock frequency of the core.
     * @throws IllegalStateException If no clock port has been specified.
     * The existence of a clock port declaration has to be guaranteed by the frontend.
     */
    public static int getClockFrequency(CorePos core) {
        for(Port port : core.ports().term())
            if(port instanceof CLK) return ((CLK)port).frequency();
        
        throw new IllegalStateException();
    }
    
    /**
     * Get the clock port of the provided core.
     * @param core A core.
     * @return The clock port of the core.
     */
    public static CLK getClockPort(CorePos core) {
        for(Port port : core.ports().term())
            if(port instanceof CLK) return ((CLK)port);
        
        throw new IllegalStateException();
    }
    
    /**
     * Get the reset port of the provided core.
     * @param core A core.
     * @return The reset port of the core.
     */
    public static RST getResetPort(CorePos core) {
        for(Port port : core.ports().term())
            if(port instanceof RST) return ((RST)port);
        
        throw new IllegalStateException();
    }
    
    /**
     * Get the reset polarity of the provided core.
     * @param core A core.
     * @return true, if the reset polarity of the core is set to 1, false if set to 0.
     * @throws IllegalStateException If no reset port has been specified.
     * The existence of a reset port declaration has to be guaranteed by the frontend.
     */
    public static boolean getRSTPolarity(CorePos core) {
        for(Port port : core.ports().term())
            if(port instanceof RST) return ((RST)port).polarity();
        
        throw new IllegalStateException();
    }
    
    /**
     * Checks if a cpu port binding is polling or forwarding.
     * @param axis A cpu binding.
     * @return true, if the port is polling, false otherwise.
     */
    public static boolean isPolling(CPUAxisPos axis) {
        // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return true;
        
        // otherwise, the port isn't polling
        return false;
    }
    
    /**
     * Get the defined value queue size parameter for this cpu binding.
     * 
     * This method returns the user-defined value queue size in the actual
     * bitwidth of the corresponding port.
     * @param axis A cpu binding.
     * @return The value queue size for the bound port specified by the user.
     */
    private static int getPollingCount(CPUAxisPos axis) {
        // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return ((POLL)opt).count();
        
        // otherwise, the port isn't polling. Return 0
        return 0;
    }
    
    /**
     * Get the defined value queue size parameter for this cpu binding.
     * 
     * The queue size is assumed to be specified in the actual bitwidth of connected the port.
     * However, this method returns the corresponding size for queue with bitwidth of 32-bit.
     * @param axis A cpu binding.
     * @return The size of a 32-bit queue required to hold the number of values requested by the user.
     */
    public static int getPollingCount32(CPUAxisPos axis) {
        return getPollingCount(axis) * (int)Math.ceil(getWidth(axis) / 32.0); 
    }
    
    /**
     * Get the defined software queue size parameter for this cpu binding.
     * 
     * This method returns the user-defined queue size in the actual bitwidth
     * of the corresponding port.
     * @param axis A cpu binding.
     * @return The queue size for the bound port specified by the user.
     */
    private static int getSWQueueSize(CPUAxisPos axis) {
        // if there is a local definition, return that
        for(Option opt : axis.opts().term())
            if(opt instanceof SWQUEUE) return ((SWQUEUE)opt).qsize();
        
        // if there is no local definition but a global one, return that
        for(Option opt : axis.root().opts().term())
            if(opt instanceof SWQUEUE) return ((SWQUEUE)opt).qsize();
        
        // otherwise, return the default queue size
        return defaultQueueSizeSW;
    }
    
    /**
     * Get the defined software queue size parameter for this cpu binding.
     * 
     * The queue size is assumed to be specified in the actual bitwidth of connected the port.
     * However, this method returns the corresponding size for queue with bitwidth of 32-bit.
     * This is required for calculating the size of value queues of polling ports and board-side
     * software queues in general.
     * @param axis A cpu binding.
     * @return The size of a 32-bit queue required to hold the number of values requested by the user.
     */
    public static int getSWQueueSize32(CPUAxisPos axis) {
        return getSWQueueSize(axis) * (int)Math.ceil(getWidth(axis) / 32.0);
    }
    
    /**
     * Get the defined hardware queue size parameter fo this cpu binding.
     * 
     * The size is calculated in the following order:
     *  - hardware queue size declaration at the binding itself
     *  - hardware queue size declaration at the bound port
     *  - hardware queue size declaration at file root
     *  - default hardware queue size
     *  
     *  The first defined hardware queue size is taken as size for the specific binding.
     * @param axis The cpu binding.
     * @return The hardware queue size of the binding.
     */
    public static int getHWQueueSize(CPUAxisPos axis) {
        // if there is a local definition, return that
        for(Option opt : axis.opts().term())
            if(opt instanceof HWQUEUE) return ((HWQUEUE)opt).qsize();
        
        // if there is no local definition but a global one, return that
        for(Option opt : axis.root().opts().term())
            if(opt instanceof HWQUEUE) return ((HWQUEUE)opt).qsize();
        
        // otherwise, return the default queue size
        return defaultQueueSizeHW;
    }
    
    /**
     * Get the direction specifier of the port referenced by a specific cpu binding.
     * @param axis The cpu binding.
     * @return The direction of the cpu binding.
     */
    public static DirectionPos getDirection(CPUAxisPos axis) {
        return getPort(axis).direction();
    }
    
    /**
     * Get the bitwidth of a cpu port binding.
     * 
     * The width is specified at the port declaration inside the core declaration.
     * If the width is not explicitly defined, the standard width of 32-bit is assumed.
     * 
     * @param axis The cpu port binding.
     * @return The bitwidth of the referenced port declaration.
     */
    public static int getWidth(CPUAxisPos axis) {
        return getWidth(getPort(axis).term());
    }
    
    /**
     * Get the bitwidth of an AXI port.
     * @param port An AXI port.
     * @return The bitwidth of the port.
     */
    public static int getWidth(AXI port) {
        // the bitwidth option has to be set at the port definition
        for(Option opt : port.opts())
            if(opt instanceof BITWIDTH) return ((BITWIDTH)opt).bit();
        
        // if it is not set, return the default width
        return defaultWidth;
    }
    
    /**
     * Calculates the greatest used software queue size.
     * 
     * This is required for memory allocation on the board-side driver,
     * since only a single out-going software queue will be allocated,
     * which has to capable of holding all these elements.
     * @param file The complete board description.
     * @return The maximal used queue size.
     */
    public static int maxQueueSize(BDLFilePos file) {
        int size = 0;
        
        for(InstancePos inst : file.insts())
            for(BindingPos bind : inst.bind())
                if(bind instanceof CPUAxisPos)
                    if(getPort(bind).direction() instanceof OUTPos)
                        size = Math.max(size, getSWQueueSize((CPUAxisPos)bind));
        
        return size;
    }
}
