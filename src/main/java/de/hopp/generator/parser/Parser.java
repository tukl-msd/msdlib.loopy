package de.hopp.generator.parser;

import static de.hopp.generator.board.BoardSpec.Board;
import static de.hopp.generator.board.BoardSpec.ETHERNET_LITE;
import static de.hopp.generator.board.BoardSpec.UART;
import de.hopp.Configuration;
import de.hopp.generator.board.Board;

public class Parser {

    Configuration config;
    
    public Parser(Configuration config) {
        this.config = config;
    }
    
    public Board parse() {
        return defaultBoard();
    }
    
    private static Board defaultBoard() {
        return Board(UART(), ETHERNET_LITE());
    }
    
}
