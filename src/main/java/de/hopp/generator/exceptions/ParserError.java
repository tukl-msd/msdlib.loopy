package de.hopp.generator.exceptions;

@SuppressWarnings("serial")
public class ParserError extends Error {

    public ParserError(String message, String file, int line) {
        super(message + (! file.equals("") ? " in file " + file +
                (line != -1 ? (" at line " + line) : "") : ""));
    }

}
