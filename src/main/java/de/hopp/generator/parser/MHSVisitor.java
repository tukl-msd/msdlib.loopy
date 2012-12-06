package de.hopp.generator.parser;

import katja.common.NE;

import de.hopp.generator.board.*;

import static de.hopp.generator.board.BoardSpec.*;

public class MHSVisitor extends MHSFile.Visitor<NE> {

    /* mircoblaze for slave and master interfaces */
    private static final String MICROBLAZE    = "microblaze";
    
    /* communication peripherals */
    private static final String UART          = "axi_uartlite";
    private static final String ETHERNET_LITE = "axi_ethernetlite";
    private static final String ETHERNET      = "axi_ethernet";
    private static final String PCIE          = "??";
    
    /* other peripherals */
    private static final String IO_PERIPHERAL = "axi_gpio";
    private static final String LEDS          = "LEDs_8Bits";
    private static final String SWITCHES      = "DIP_Switches_8Bits";
    private static final String BUTTONS       = "Push_Buttons_5Bits"; 
    
    /* attribute types */
    private static final String BUS_IF        = "BUS_INTERFACE";
    private static final String PARAMETER     = "PARAMETER";
    
    /* idents for microblaze interface attributes */
    private static final String SLAVE_PREFIX  = "S";
    private static final String SLAVE_SUFFIX  = "_AXIS";
    private static final String MASTER_PREFIX = "M";
    private static final String MASTER_SUFFIX = "_AXIS";
   
    /* idents for other peripheral attributes */
    private static final String INSTANCE      = "INSTANCE";
    private static final String BASEADDR      = "C_BASEADDR";
    
    private Board board;
    
    enum Type {
        UNKNOWN, LEDS, SWITCHES, BUTTONS
    }
    
    public Board getCurrentBoard() {
        return board;
    }
    
    public void visit(MHSFile term) {
        
        // create a new board
        board = Board();
        
        // currently, we don't do anything with direct attributes of the file
//        visit(term.attributes());
        
        // visit blocks
        visit(term.blocks());
    }

    public void visit(Attributes term) { for(Attribute a : term) visit(a); }
    public void visit(Blocks term)     { for(Block     b : term) visit(b); }

    public void visit(Block term) {
        
        // blocks mark additional peripherals. Check if it's one we are interested in.
        // if yes, add it and all it's significant parameters to the board model.
        Component c = null;
        
        if(term.name().equals(MICROBLAZE)) {
            // TODO here we need to check how many slave and master connections there are...
        } else if(term.name().equals(UART)) {
            c = UART();
        } else if(term.name().equals(ETHERNET_LITE)) {
            c = ETHERNET_LITE();
        } else if(term.name().equals(ETHERNET)) {
            c = ETHERNET();
        } else if(term.name().equals(PCIE)) {            
            c = PCIE();
        } else if(term.name().equals(IO_PERIPHERAL)) {
            
            String baseAddr = null;
            Type type = Type.UNKNOWN;
            
            for(Attribute a : term.attributes()) {
                if(a.type() instanceof PARAMETER) {
                    for(Assignment assgn : a.assignments()) {
                        if(assgn.name().equals(INSTANCE)) {
                            if(! (assgn.expression() instanceof Ident)) throw new RuntimeException();
                            String ident = ((Ident)assgn.expression()).val();
                            if(ident.equals(LEDS)) type = Type.LEDS;
                            else if(ident.equals(SWITCHES)) type = Type.SWITCHES;
                            else if(ident.equals(BUTTONS)) type = Type.BUTTONS;
                        } else if(assgn.name().equals(BASEADDR)) {
                            if(! (assgn.expression() instanceof MemAddr)) throw new RuntimeException();
                            baseAddr = ((MemAddr)assgn.expression()).val();
                        }
                    }
                }
            }
            
            // if the o/p component type is unknown, this component is not interesting for the board model
            if(type == Type.UNKNOWN) return;
            
            // if it is an interesting one, but the base address is not specified, the .mhs file was invalid
            if(baseAddr == null) throw new RuntimeException();
            
            switch(type) {
            case LEDS    : c = LEDS(baseAddr);     break;
            case SWITCHES: c = SWITCHES(baseAddr); break;
            case BUTTONS : c = BUTTONS(baseAddr);  break;
            }
        } 
        if(c != null) board = board.replaceComponents(board.components().add(c)); 
        
    }
    
    public void visit(Attribute term)     { }
    public void visit(Assignments term)   { }
    public void visit(String term)        { }
    public void visit(BUS_INTERFACE term) { }
    public void visit(PARAMETER term)     { }
    public void visit(PORT term)          { }
    public void visit(Assignment term)    { }
    public void visit(AndExp term)        { }
    public void visit(DotList term)       { }
    public void visit(Ident term)         { }
    public void visit(Number term)        { }
    public void visit(MemAddr term)       { }
    public void visit(Range term)         { }
    public void visit(Integer term)       { }

}
