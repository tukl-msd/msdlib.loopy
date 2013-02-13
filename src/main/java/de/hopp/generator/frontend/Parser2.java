package de.hopp.generator.frontend;

import static de.hopp.generator.board.BoardSpec.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java_cup.runtime.Symbol;
import de.hopp.generator.Configuration;
import de.hopp.generator.IOHandler;
import de.hopp.generator.board.Board;
import de.hopp.generator.frontend.Lexer;
import de.hopp.generator.frontend.parser;
import de.hopp.generator.parser.MHSFile;

public class Parser2 {

    Configuration config;
    IOHandler IO;
    
    public Parser2(Configuration config) {
        this.config = config;
        this.IO = config.IOHANDLER();
    }
    
    public Board parse(File f) {
        try {
            IO.println("  parsing .mhs file ...");

            // setup lexer and parser
            FileReader fr = new FileReader(f);
            Lexer lex     = new Lexer(fr);
            parser parser = new parser(lex);
            
            // parse
            Symbol ast = parser.parse();
            MHSFile mhs = (MHSFile) ast.value;
            
            IO.println("  generating board model ...");
            MHSVisitor visitor = new MHSVisitor();
            visitor.visit(mhs);
            
//            return visitor.getCurrentBoard();
            return defaultBoard();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return defaultBoard();
    }
    
    private static Board defaultBoard() {
//        return Board(ETHERNET_LITE(IP(192,168,1,10),IP(255,255,255,0), IP(192,168,1,1),8844), LEDS(), SWITCHES(), BUTTONS());
        return Board(UART(), BUTTONS(), SWITCHES(), LEDS(),
                VHDL(VHDLCore("add", Ports(IN("in1"), IN("in2"), OUT("sum"))), Instances(
                        "adder_a",
                        "adder_b")),
                VHDL(VHDLCore("mult", Ports(IN("in1"), IN("in2"), OUT("prod"))), Instances("mult_a")),
                VHDL(VHDLCore("store", Ports(DUAL("p"))), Instances("s_1"))
            );
    }
    
}
