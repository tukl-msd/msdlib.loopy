package de.hopp.generator.backends.workflow.ise.xps;

import de.hopp.generator.model.mhs.MHSFile;

/**
 * Model of the clock generator of the board.
 *
 * This can model an axi-connected IP core or some external source.
 * It provides identifiers for requested frequencies.
 *
 * @author Thomas Fischer
 * @since 22.8.2013
 */
public interface Clock {
    /**
     * Determines the name of the port providing a clock with the requested frequency.
     * The clock will be buffered and not variable phased.
     *
     * Adds the port to the clocks port list. Can throw an exception, if the
     * clock does not support these parameters or is "full".
     *
     * @param frequency Frequency of the required clock.
     * @return The name of a port providing a clock with the requested parameters
     * @throw IllegalArgumentException If this frequency is not supported by the clock type
     *      or the clock component has reached its maximal number of clock ports.
     * @throw IllegalStateException If a new port has been requested after writing the clock component to the mhs.
     */
    public String getClockPort(int frequency);
    /**
     * Determines the name of the port providing a clock with these parameters.
     *
     * Adds the port to the clocks port list. Can throw an exception, if the
     * clock does not support these parameters or is "full".
     * Some clock implementations might only provide a fixed set of configurations
     * and throw exceptions for all others.
     *
     * @param frequency Frequency of the required clock.
     * @param buffered Flag if the clock should be buffered.
     * @param variablePhase Flag if the clock should have variable phase.
     * @return The name of a port providing a clock with the requested parameters
     * @throw IllegalArgumentException If these parameter combination is not supported by the clock type
     *      or the clock component has reached its maximal number of clock ports.
     * @throw IllegalStateException If a new port has been requested after writing the clock component to the mhs.
     */
    // TODO provide some more useful exceptions and try/catch them ;)
    public String getClockPort(int frequency, boolean buffered, boolean variablePhase);

    /**
     * Returns the mhs blocks required to setup the clock generator.
     *
     * This can be a complete mhs block or just some parameters to be used
     * in another component.
     *
     * @param versions versions of all IP cores of the used ISE version
     * @return mhs An mhs file to be embedded in the runs mhs file.
     */
    public MHSFile getMHS(IPCoreVersions versions);
}
