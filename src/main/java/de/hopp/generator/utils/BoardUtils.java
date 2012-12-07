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
            })) return true;
        }
        return false;
    }
}
