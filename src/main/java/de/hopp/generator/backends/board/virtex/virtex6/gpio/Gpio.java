package de.hopp.generator.backends.board.virtex.virtex6.gpio;

import de.hopp.generator.backends.workflow.ise.gpio.GpioComponent;

/**
 * Enum class for virtex6 GPIO components.
 *
 * Wraps all GPIO components known to the generator in an enum.
 * While an enum like this is not generally necessary, it might be useful in some cases.
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public enum Gpio {
    BUTTONS(new GpioButtons()),
    SWITCHES(new GpioSwitches()),
    LEDS(new GpioLEDs());

    private GpioComponent instance;

    private Gpio(GpioComponent inst) {
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
    public static Gpio fromString(String id) throws IllegalArgumentException {
        for(Gpio gpio : Gpio.values()) if(id.equals(gpio.instance.id())) return gpio;

        String error = "unknown GPIO identifier " + id + ". Valid ones are: ";

        for(Gpio gpio : Gpio.values()) {
            error += gpio.id();
            error += ", ";
        }
        if(Gpio.values().length > 0) error = error.substring(0, error.length() - 2);
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

    public GpioComponent getInstance() {
        return instance;
    }
}


