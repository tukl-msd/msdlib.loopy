package de.hopp.generator.utils;

import katja.common.NE;
import de.hopp.generator.board.*;
import de.hopp.generator.frontend.BDLFile;
import de.hopp.generator.frontend.BDLFilePos;
import de.hopp.generator.frontend.CorePos;
import de.hopp.generator.frontend.Instance;
import de.hopp.generator.frontend.PortPos;

public class BoardUtils {
    
    public static boolean hasLEDs(Board board) {
        for(Component comp : board.components())
            if(comp instanceof LEDS) return true;
        return false;
    }
    public static boolean hasSwitches(Board board) {
        for(Component comp : board.components())
            if(comp instanceof SWITCHES) return true;
        return false;
    }
    public static boolean hasButtons(Board board) {
        for(Component comp : board.components())
            if(comp instanceof BUTTONS) return true;
        return false;
    }
    
    public static boolean hasGpioComponent(Board board) {
        for(Component comp : board.components()) {
            if(comp.Switch(new Component.Switch<Boolean, NE>() {
                public Boolean CaseUART(UART term)                   { return false; }
                public Boolean CaseETHERNET_LITE(ETHERNET_LITE term) { return false; }
                public Boolean CaseETHERNET(ETHERNET term)           { return false; }
                public Boolean CasePCIE(PCIE term)                   { return false; }
                public Boolean CaseLEDS(LEDS term)                   { return true;  }
                public Boolean CaseSWITCHES(SWITCHES term)           { return true;  }
                public Boolean CaseBUTTONS(BUTTONS term)             { return true;  }
                public Boolean CaseVHDL(VHDL term)                   { return false; }
            })) return true;
        }
        return false;
    }
    
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
        rslt += "\n  -medium " + board.medium().name();
        // TODO medium parameters
        for(de.hopp.generator.frontend.GPIO gpio : board.gpios())
            rslt += "\n  -GPIO component " + gpio.name();
        for(Instance inst : board.insts())
            rslt += "\n  -VHDL Component " + inst.name() + " (" + inst.core() + ")";
        return rslt;
    }
    
    public static String printBoard(Board board) {
        String rslt = "board has the following components";
        for(Component comp : board.components()) {
            rslt += comp.Switch(new Component.Switch<String, NE>(){
                public String CaseUART(UART term) {
                    return "\n    -uart";
                }
                public String CaseETHERNET_LITE(ETHERNET_LITE term) {
                    return "\n    -ethernet lite";
                }
                public String CaseETHERNET(ETHERNET term) {
                    return "\n    -ethernet";
                }
                public String CasePCIE(PCIE term) {
                    return "\n    -pcie";
                }
                public String CaseLEDS(LEDS term) {
                    return "\n    -leds";
                }
                public String CaseSWITCHES(SWITCHES term) {
                    return "\n    -switches";
                }
                public String CaseBUTTONS(BUTTONS term) {
                    return "\n    -buttons";
                }
                public String CaseVHDL(VHDL term) {
                    String s = "";
                    for(String instance: term.instances())
                        s += "\n    -VHDL Component: " + instance + " (" + term.core().file() + ")";
                    return s;
                }
            });
        }
        return rslt;
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
