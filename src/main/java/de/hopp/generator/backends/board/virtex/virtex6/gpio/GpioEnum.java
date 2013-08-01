package de.hopp.generator.backends.board.virtex.virtex6.gpio;

import static de.hopp.generator.parser.MHS.Assignment;
import static de.hopp.generator.parser.MHS.Attribute;
import static de.hopp.generator.parser.MHS.Block;
import static de.hopp.generator.parser.MHS.Ident;
import static de.hopp.generator.parser.MHS.PARAMETER;
import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Block;

/**
 * Enum class for GPIO components.
 *
 * Wraps all GPIO components known to the generator in an enum.
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public enum GpioEnum {
    BUTTONS(new GpioButtons()),
    SWITCHES(new GpioSwitches()),
    LEDS(new GpioLEDs());

    private GpioComponent instance;

    private GpioEnum(GpioComponent inst) {
        instance = inst;
    }

    /**
     * Returns the GPIO component corresponding to the provided id string.
     *
     * @param id The id of a GPIO component.
     * @return The GPIO component corresponding to the provided id string.
     * @throws IllegalArgumentException If no GPIO component with the provided
     *                                  name exists.
     */
    public static GpioEnum fromString(String id) throws IllegalArgumentException {
        for(GpioEnum gpio : GpioEnum.values()) if(id.equals(gpio.instance.id())) return gpio;

        String error = "unknown GPIO identifier " + id + ". Valid ones are: ";

        for(GpioEnum gpio : GpioEnum.values()) {
            error += gpio.id();
            error += ", ";
        }
        if(GpioEnum.values().length > 0) error = error.substring(0, error.length() - 2);
        throw new IllegalArgumentException(error);
    }

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id() {
        return instance.id();
    }

    /**
     * Returns the number of bits required to represent this components state.
     *
     * This also determines the width of the bitset representing the components state,
     * that is returned when reading from or required as input for writing to the
     * component.
     *
     * @return The number of bits required to represent this components state.
     */
    public int width() {
        return instance.width();
    }

    public boolean isGPI() {
        return instance.isGPI();
    }

    public boolean isGPO() {
        return instance.isGPO();
    }

    public Attribute getMHSAttribute() {
        return instance.getMHSAttribute();
    }

    public Block getMHSBlock(String version) {
        return instance.getMHSBlock(version);
    }

    public String getINTCPort() {
        return instance.getINTCPort();
    }

    public String getUCFConstraints() {
        return instance.getUCFConstraints();
    }

    public Block getMSSBlock(String version) {
        return Block("DRIVER",
            Attribute(PARAMETER(), Assignment("DRIVER_NAME", Ident("gpio"))),
            Attribute(PARAMETER(), Assignment("DRIVER_VER",  Ident(version))),
            Attribute(PARAMETER(), Assignment("HW_INSTANCE", Ident(instance.hwInstance())))
        );
    }

    public String deviceID() {
        return instance.deviceID();
    }

    public String deviceIntrChannel() {
        return instance.deviceIntrChannel();
    }
}


