package de.hopp.generator.utils;

import katja.common.NE;
import de.hopp.generator.board.BUTTONS;
import de.hopp.generator.board.Board;
import de.hopp.generator.board.Component;
import de.hopp.generator.board.ETHERNET;
import de.hopp.generator.board.ETHERNET_LITE;
import de.hopp.generator.board.LEDS;
import de.hopp.generator.board.PCIE;
import de.hopp.generator.board.SWITCHES;
import de.hopp.generator.board.UART;
import de.hopp.generator.board.VHDL;

public class BoardUtils {
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
    
    public static boolean hasGpiComponent(Board board) {
        for(Component comp : board.components()) {
            if(comp.Switch(new Component.Switch<Boolean, NE>() {
                public Boolean CaseUART(UART term)                   { return false; }
                public Boolean CaseETHERNET_LITE(ETHERNET_LITE term) { return false; }
                public Boolean CaseETHERNET(ETHERNET term)           { return false; }
                public Boolean CasePCIE(PCIE term)                   { return false; }
                public Boolean CaseLEDS(LEDS term)                   { return false; }
                public Boolean CaseSWITCHES(SWITCHES term)           { return true;  }
                public Boolean CaseBUTTONS(BUTTONS term)             { return true;  }
                public Boolean CaseVHDL(VHDL term)                   { return false; }
            })) return true;
        }
        return false;
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
                    for(String name : term.names())
                        s += "\n    -VHDL Component: " + name + " (" + term.core().file() + ")";
                    return s;
                }
            });
        }
        return rslt;
    }
}
