package de.hopp.generator.backends.board;

import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.Memory;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public interface BoardBackend {
    /**
     * The name of this backend. The name is automatically used to select the backend
     * from the command line interface of the driver generator.
     *
     * There has to be a folder with the same name as the board backend in
     * resources/deploy/server/boards containing board-specific files. Especially the
     * following files HAVE to be provided in there, since they are explicitly checked
     * during generation:
     *   - app/lscript
     *   - app/.cproject
     *   - app/src/platform_config.h
     *  (- an implementation of platform.h corresponding to the boards processor)
     *   - app_bsp/.sdkproject
     *   - app_bsp/ligen.options
     *  (- app_bsp/.cproject)
     * @return The name of the backend.
     */
    public String getName();

    public void printUsage(IOHandler IO);

    public GpioComponent getGpio(String name);

    public Memory getMemory();
}
