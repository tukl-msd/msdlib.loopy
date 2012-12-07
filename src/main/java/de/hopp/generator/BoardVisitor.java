package de.hopp.generator;

import katja.common.NE;
import de.hopp.generator.board.BUTTONS;
import de.hopp.generator.board.Board;
import de.hopp.generator.board.Board.Visitor;
import de.hopp.generator.board.Component;
import de.hopp.generator.board.Components;
import de.hopp.generator.board.ETHERNET;
import de.hopp.generator.board.ETHERNET_LITE;
import de.hopp.generator.board.LEDS;
import de.hopp.generator.board.PCIE;
import de.hopp.generator.board.SWITCHES;
import de.hopp.generator.board.UART;
import de.hopp.generator.model.*;

import static de.hopp.generator.model.Model.*;

public class BoardVisitor extends Visitor<NE>{

    // TODO use these flags
    private boolean debug;
    private boolean includedConversions;
    private boolean includedInterruptSetup;
    
    private MFile file;
    
    private MMethod init;
    
    public BoardVisitor(boolean debug) {
        
        this.debug = debug;

        includedConversions    = false;
        includedInterruptSetup = false;
        
        file = MFile("name", MDefinitions(), MStructs(), MEnums(), MAttributes(), MMethods(), MClasses());
        
        init = MMethod(MModifiers(), MType("int"), "init", MParameters(), MCode(Strings()));
    }
    
    public MFile getFile() {
        return file;
    }
    
    public void visit(Board term) {
        // visit all components
        visit(term.components());
        
        // add the init method to the file
        file = append(file, init);
        
        // add the cleanup method to the file
        MMethod debugMethod = MMethod(MModifiers(), MType("int"), "cleanup", MParameters(), MCode(Strings()));
        if(debug) debugMethod = appendCode(debugMethod,
                MCode(Strings("if(DEBUG == 1) xil_printf(\"\\ncleaning up\");", "")));
        debugMethod = appendCode(debugMethod, MCode(Strings(
                "// cleanup platform",
                "cleanup_platform();",
                "",
                "return XST_SUCCESS;"
                )));
        file = append(file, debugMethod);
        
        Strings debugCode = Strings();
        if(debug) debugCode = debugCode.addAll(Strings("if(DEBUG == 1) xil_printf(\"\\ncleaning up\");", ""));
        debugCode = debugCode.addAll(Strings(
                "// cleanup platform",
                "cleanup_platform();",
                "",
                "return XST_SUCCESS;"
                ));
        file = append(file, MMethod(MModifiers(), MType("int"), "cleanup", MParameters(), MCode(debugCode)));

        
        
        
        // add the main method to the file
        Strings initCode = Strings();
        if(debug) initCode = initCode.addAll(Strings("if(DEBUG == 1) xil_printf(\"\\nstarting up\\n\");", ""));
        initCode = initCode.addAll(Strings(
                "// initialize everything",
                "init();",
                "",
                "// set LED state to Switch state",
                "setLEDRAW(readSwitchesRAW());",
                "",
                "// cleanup",
                "cleanup_platform();",
                "",
                "return 0;"
                ));
        file = append(file, MMethod(MModifiers(), MType("int"), "main", MParameters(), MCode(initCode)));
        
    }
    
    public void visit(Components term) {
        for(Component c : term) visit(c); 
    }
    
    // TODO communication interfaces (not implemented yet)
    public void visit(UART term)          { }
    public void visit(ETHERNET_LITE term) { }
    public void visit(ETHERNET term)      { }
    public void visit(PCIE term)          { }

    // Gpio components
    public void visit(LEDS term) {
        // TODO auto-generated method stub
    }
    public void visit(SWITCHES term) {
        // TODO auto-generated method stub
    }
    public void visit(BUTTONS term) {
        // TODO auto-generated method stub
    }

    // literals
    public void visit(String term) { }

    private static MFile append(MFile file, MDefinition def) {
        return file.replaceDefs(file.defs().add(def));
    }
    private static MFile append(MFile file, MAttribute attr) {
        return file.replaceAttributes(file.attributes().add(attr));
    }
    private static MFile append(MFile file, MStruct struct) {
        return file.replaceStructs(file.structs().add(struct));
    }
    private static MFile append(MFile file, MMethod method) {
        return file.replaceMethods(file.methods().add(method));
    }
    private static MFile append(MFile file, MEnum menum) {
        return file.replaceEnums(file.enums().add(menum));
    }
    private static MMethod appendCode(MMethod method, MCode code) {
        return method.replaceBody(MCode(
                method.body().lines().addAll(code.lines()),
                method.body().needed().addAll(code.needed())));
    }
}
