package de.hopp.generator.backends.server.virtex6;

import katja.common.NE;
import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.board.*;
import de.hopp.generator.board.Board.Visitor;

/**
 * Generation backend for a project for Xilix XPS version 14.1.
 * This includes an .mhs file describing the board as well as several default
 * components like parameterised queues and DeMUXes.
 * @author Thomas Fischer
 */
public class XPS_14_1 extends Visitor<NE> implements ProjectBackend {

    public String getName() {
        return "xps14.1";
    }
    
    @Override
    public void generate(Board board, Configuration config, ErrorCollection errors) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void visit(Board term) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void visit(Components term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(VHDL term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(UART term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(ETHERNET_LITE term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(ETHERNET term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(PCIE term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(LEDS term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(SWITCHES term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(BUTTONS term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(VHDLCore term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(Instances term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(String term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(Ports term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(IN term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(OUT term) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(DUAL term) {
        // TODO Auto-generated method stub
        
    }
}
