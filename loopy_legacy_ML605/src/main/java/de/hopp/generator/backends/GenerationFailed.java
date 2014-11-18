package de.hopp.generator.backends;

import de.hopp.generator.exceptions.Error;

@SuppressWarnings("serial")
public class GenerationFailed extends Error {

    public GenerationFailed(String message) {
        super(message);
    }

}
