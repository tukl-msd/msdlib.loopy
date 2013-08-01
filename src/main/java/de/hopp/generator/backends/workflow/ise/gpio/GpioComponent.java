package de.hopp.generator.backends.workflow.ise.gpio;

import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Block;

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
    public String id();

    /**
     * Returns the number of bits required to represent this components state.
     *
     * This also determines the width of the bitset representing the components state,
     * that is returned when reading from or required as input for writing to the
     * component.
     *
     * @return The number of bits required to represent this components state.
     */
    public int width();

    public boolean isGPI();

    public boolean isGPO();

    public Attribute getMHSAttribute();

    public Block getMHSBlock(String version);

    public String getINTCPort();

    public String getUCFConstraints();

    public String hwInstance();

    public String deviceID();

    public String deviceIntrChannel();
}
