package de.hopp.generator.backends.board;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public interface GpioComponent {
    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public abstract String id();

    /**
     * Returns the number of bits required to represent this components state.
     *
     * This also determines the width of the bitset representing the components state,
     * that is returned when reading from or required as input for writing to the
     * component.
     *
     * @return The number of bits required to represent this components state.
     */
    public abstract int width();

    public abstract boolean isGPI();

    public abstract boolean isGPO();
}
