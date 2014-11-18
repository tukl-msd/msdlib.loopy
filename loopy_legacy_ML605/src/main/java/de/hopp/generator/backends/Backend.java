package de.hopp.generator.backends;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.model.BDLFilePos;

public interface Backend {

    /**
     * The name of this backend. The name is automatically used to select the backend
     * from the command line interface of the driver generator.
     * @return The name of the backend.
     */
    String getName();

    /**
     * Prints usage help of the backend on the console.
     * @param IO IOHandler used for handling printing to console.
     */
    void printUsage(IOHandler IO);

    /**
     * Parses backend-specific parameters.
     * Unparsable parameters are to be stored within the configuration for further treatment in other backends.
     * @param config Configuration of this generator run so far
     * @param args Parameters to be parsed
     * @return Updated configuration, including unparsed parameters
     */
    Configuration parseParameters(Configuration config, String[] args);

    /**
     * Starts the generation process of this backend.
     * This includes model transformation and unparsing.
     * @param board Input board model
     * @param config Configuration of this generator run
     * @param errors ErrorCollection of this generator run
     */
    void generate(BDLFilePos board, Configuration config, ErrorCollection errors);
}
