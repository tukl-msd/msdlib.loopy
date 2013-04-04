package de.hopp.generator.exceptions;

import de.hopp.generator.frontend.Position;

@SuppressWarnings("serial")
public class ParserError extends Error {

    public ParserError(String message, String file, int line) {
        super(message + (! file.equals("") ? " in file " + file +
                (line != -1 ? (" at line " + line) : "") : ""));
    }
    
    public ParserError(String message, Position pos) {
        super(message + " in file " + pos.filename() + " at line " + pos.line());
    }

}
