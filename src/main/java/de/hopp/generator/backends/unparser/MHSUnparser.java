package de.hopp.generator.backends.unparser;

import katja.common.NE;
import de.hopp.generator.parser.*;
import de.hopp.generator.parser.Number;

public class MHSUnparser extends MHSFile.Visitor<NE> {

    private IndentStringBuffer buffer;
    
    public MHSUnparser(StringBuffer buffer) {
        this.buffer = new IndentStringBuffer(buffer);
    }
    
    // root file
    public void visit(MHSFile term) {
        visit(term.attributes());
        visit(term.blocks());
    }
    
    // blocks
    public void visit(Blocks term){ for(Block b : term) visit(b); }
    public void visit(Block term) {
        buffer.append("\n\nBEGIN " + term.name());
        buffer.indent();
        visit(term.attributes());
        buffer.unindent();
        buffer.append("\nEND");
    }

    // attributes
    public void visit(Attributes term) { for(Attribute a : term) visit(a); }
    public void visit(Attribute term) {
        visit(term.type());
        visit(term.assign());
    }
    public void visit(OPTION term)    { buffer.append("\nOPTION "); }
    public void visit(BUS_IF term)    { buffer.append("\nBUS_INTERFACE "); }
    public void visit(PARAMETER term) { buffer.append("\nPARAMETER "); }
    public void visit(PORT term)      { buffer.append("\nPORT "); }

    // assignments
    public void visit(Assignments term) { 
        for(Assignment a : term) {
            if(a != term.get(0)) buffer.append(", "); 
            visit(a);
        }
    }
    public void visit(Assignment term) {
        buffer.append(term.name() + " = "); visit(term.expression());
    }

    // value expressions
    public void visit(AndExp term)  {
        for(Value v : term) {
            if(v != term.get(0)) buffer.append(" & ");
            visit(v);
        }
    }
    public void visit(Ident term)   { visit(term.val()); }
    public void visit(STR term)     { 
        buffer.append('\"');
        visit(term.val());
        buffer.append('\"');
    }
    public void visit(Number term)  { visit(term.val()); }
    public void visit(MemAddr term) { visit(term.val()); }
    public void visit(Range term) {
        buffer.append('[');
        visit(term.u());
        buffer.append(':');
        visit(term.l());
        buffer.append(']');
    }
    
    // literals
    public void visit(Integer term) { buffer.append(term.toString()); }
    public void visit(String term)  { buffer.append(term); }
    
}
