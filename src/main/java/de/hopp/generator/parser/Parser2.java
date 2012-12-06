package de.hopp.generator.parser;

import static de.hopp.generator.board.BoardSpec.Board;
import static de.hopp.generator.board.BoardSpec.ETHERNET_LITE;
import static de.hopp.generator.board.BoardSpec.UART;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java_cup.runtime.Symbol;
import de.hopp.Configuration;
import de.hopp.generator.board.Board;

import static de.hopp.generator.parser.MHS.*;

public class Parser2 {

    Configuration config;
    
    public Parser2(Configuration config) {
        this.config = config;
    }
    
    public Board parse(File f) {
        try {
            System.out.println("  parsing .mhs file ...");

            // TODO check for existence and if the file is indeed a file
            
            // setup lexer and parser
            FileReader fr = new FileReader(f);
            Lexer lex     = new Lexer(fr);
            parser parser = new parser(lex);
            
            // parse
            Symbol ast = parser.parse();
            MHSFile mhs = (MHSFile) ast.value;
            
            System.out.println("  generating board model ...");
            MHSVisitor visitor = new MHSVisitor();
            visitor.visit(mhs);
            
            return visitor.getCurrentBoard();
            
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return defaultBoard();
    }
    
    private static Board defaultBoard() {
        return Board(UART(), ETHERNET_LITE());
    }
    
}
