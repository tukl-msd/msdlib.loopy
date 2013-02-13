package de.hopp.generator.backends;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.board.Board;

public interface Backend {
    String getName();
    
    // TODO parse parameters
    // TODO print usage
    
    void generate(Board board, Configuration config, ErrorCollection errors);
}
