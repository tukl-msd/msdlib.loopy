package de.hopp.generator.exceptions;

import de.hopp.generator.model.Position;

@SuppressWarnings("serial")
public class ParserWarning extends Warning {

    public ParserWarning(String message) {
        super(message);
    }

    public ParserWarning(String message, String file, int line) {
        super(message + (! file.equals("") ? " in file " + file +
                (line != -1 ? (" at line " + line) : "") : ""));
    }

    public ParserWarning(String message, Position pos) {
        super(message + " in file " + pos.filename() + " at line " + pos.line());
    }

}
