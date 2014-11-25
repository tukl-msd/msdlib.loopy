package de.hopp.generator.exceptions;

@SuppressWarnings("serial")
public class UsageError extends Error {

    public UsageError(String message) {
        super(message);
    }

}
