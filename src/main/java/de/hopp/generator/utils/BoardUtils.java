package de.hopp.generator.utils;

import katja.common.NE;
import de.hopp.generator.frontend.*;

public class BoardUtils {
    
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

    /**
     * Returns the unique port with given port id and core id
     * @param file
     * @param coreID
     * @param portID
     * @return
     */
    public static PortPos getPort(BDLFilePos file, String coreID, String portID) {
        CorePos core = null;
        for(CorePos c : file.cores()) {
            if(c.name().term().equals(coreID)) {
                core = c; break;
            }
        }
        if(core == null) return null;
        
        PortPos port = null;
        for(PortPos p : core.ports()) {
            if(p.name().term().equals(portID)) {
                port = p; break;
            }
        }
        return port;
    }
}
