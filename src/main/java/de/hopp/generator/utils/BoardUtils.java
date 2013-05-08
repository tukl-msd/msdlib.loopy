package de.hopp.generator.utils;

import katja.common.NE;
import de.hopp.generator.frontend.*;

public class BoardUtils {
    
    public static final int defaultQueueSizeSW = 1024;
    public static final int defaultQueueSizeHW = 64;
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
    
    public static String printBoard(BDLFile board) {
        String rslt = "parsed the following board";
        rslt += "\n  -debug: " + (debug(board) ? "yes" : "no");
        rslt += "\n  -" + board.medium().Switch(new Medium.Switch<String, NE>() {
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
        // TODO medium parameters
        for(de.hopp.generator.frontend.GPIO gpio : board.gpios())
            rslt += "\n  -GPIO component " + gpio.name();
        for(Instance inst : board.insts())
            rslt += "\n  -VHDL Component " + inst.name() + " (" + inst.core() + ")";
        return rslt;
    }
    
//    public static String printBoard(Board board) {
//        String rslt = "board has the following components";
//        for(Component comp : board.components()) {
//            rslt += comp.Switch(new Component.Switch<String, NE>(){
//                public String CaseUART(UART term) {
//                    return "\n    -uart";
//                }
//                public String CaseETHERNET_LITE(ETHERNET_LITE term) {
//                    return "\n    -ethernet lite";
//                }
//                public String CaseETHERNET(ETHERNET term) {
//                    return "\n    -ethernet";
//                }
//                public String CasePCIE(PCIE term) {
//                    return "\n    -pcie";
//                }
//                public String CaseLEDS(LEDS term) {
//                    return "\n    -leds";
//                }
//                public String CaseSWITCHES(SWITCHES term) {
//                    return "\n    -switches";
//                }
//                public String CaseBUTTONS(BUTTONS term) {
//                    return "\n    -buttons";
//                }
//                public String CaseVHDL(VHDL term) {
//                    String s = "";
//                    for(String instance: term.instances())
//                        s += "\n    -VHDL Component: " + instance + " (" + term.core().file() + ")";
//                    return s;
//                }
//            });
//        }
//        return rslt;
//    }
    
    private static boolean debug(BDLFile board) {
        boolean debug = false;
        for(Option opt : board.opts()) if(opt instanceof DEBUG) debug = true;
        return debug;
    }

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
    
    public static PortPos getPort(BindingPos bind) {
        String portName = bind.port().term();

        // throw an exception, if the parent is not a core instance
        if(!(bind.parent().parent() instanceof InstancePos)) throw new IllegalStateException();
        InstancePos inst = ((InstancePos)bind.parent().parent());

        // return the port, if it exists
        for(PortPos p : getCore(inst).ports())
            if(p.name().term().equals(portName)) return p;

        // otherwise, throw an exception (should never happen due to sanity checks)
        throw new IllegalStateException();
    }
    
    public static boolean isPolling(CPUAxisPos axis) {
        // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return true;
        
        // otherwise, the port isn't polling
        return false;
    }
    
    private static int getPollingCount32(CPUAxisPos axis) {
     // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return ((POLL)opt).count();
        
        // otherwise, the port isn't polling. Return 0
        return 0;
    }
    
    /**
     * Get the defined value queue size parameter for this cpu axis.
     * 
     * The queue size is assumed to be specified in the actual bitwidth of connected the port.
     * However, this method returns the corresponding size for queue with bitwidth of 32-bit.
     * @param axis
     * @return The size of a 32-bit queue required to hold the number of values requested by the user.
     */
    public static int getPollingCount(CPUAxisPos axis) {
        return getPollingCount32(axis) * (int)Math.ceil(getWidth(axis) / 32.0); 
    }

    /**
     * Get the defined software queue size parameter for this cpu axis.
     * @param axis
     * @return The queue size for the bound port specified by the user.
     */
    private static int getSWQueueSize32(CPUAxisPos axis) {
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
     * Get the defined software queue size parameter for this cpu axis.
     * 
     * The queue size is assumed to be specified in the actual bitwidth of connected the port.
     * However, this method returns the corresponding size for queue with bitwidth of 32-bit.
     * This is required for calculating the size of value queues of polling ports and board-side
     * software queues in general.
     * @param axis
     * @return The size of a 32-bit queue required to hold the number of values requested by the user.
     */
    public static int getSWQueueSize(CPUAxisPos axis) {
        return getSWQueueSize32(axis) * (int)Math.ceil(getWidth(axis) / 32.0);
    }
    
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
    
    public static int getWidth(CPUAxisPos axis) {
        // the bitwidth option has to be set at the port definition
        for(Option opt : getPort(axis).opts().term())
            if(opt instanceof BITWIDTH) return ((BITWIDTH)opt).bit();
            
        // if it is not set, return the default width
        return defaultWidth;
    }
    
    public static int maxQueueSize(BDLFile file) {
        int size = 0;
        
        for(Option opt : file.opts())
            if(opt instanceof SWQUEUE) size = Math.max(size, ((SWQUEUE)opt).qsize());
        
        for(Instance inst : file.insts())
            for(Binding bind : inst.bind())
                for(Option opt : bind.opts())
                    if(opt instanceof SWQUEUE) size = Math.max(size, ((SWQUEUE)opt).qsize());
        
        return size;
    }
}
