package de.hopp.generator.exceptions;

import de.hopp.generator.frontend.Position;

@SuppressWarnings("serial")
public class ParserError extends Error {

    public ParserError(String message, String file, int line) {
        super(message + (! file.equals("") ? " in file " + file +
                (line != -1 ? (" at line " + line) : "") : ""));
    }

    private static String printPosition(Position pos) {
        return " in file " + pos.filename() + " at line " + pos.line();
    }

    public ParserError(String message, Position pos) {
        super(message + printPosition(pos));
    }

    /**
     * Constructor for duplicates, where only a single occurrence is allowed
     * @param message Message (without position information) to be printed
     * @param pos1 Position of the first (and probably "correct") occurrence
     * @param pos2 Position of the second (and probably duplicate) occurrence
     */
    public ParserError(String message, Position pos1, Position pos2) {
        super(message + pos2 +  "(first defined" + printPosition(pos1) + ")");
    }

//    public ParserError(String message, Position ... pos) {
//        super(message);
//
//
//        if(pos.length == 0) {
//            super(message);
//        } else if (pos.length() == 1) {
//            super(message);
//        } else {
//            super(message);
//        }
//    }
//
//    @Override
//    public String getMessage() {
//        String message = super.getMessage();
//
//        for(Position p : pos) {d            message += " in file " + p.filename() + " at line " + p.line();
//        }
//        return message;
//    }

}
