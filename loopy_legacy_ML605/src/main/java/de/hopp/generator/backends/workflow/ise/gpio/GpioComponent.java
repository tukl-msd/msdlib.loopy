package de.hopp.generator.backends.workflow.ise.gpio;


/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public interface GpioComponent extends de.hopp.generator.backends.board.GpioComponent {
    public String instID();

    public String getUCFConstraints();

    public String getINTCPort();

    public abstract String portID();

    // SDK
    public String deviceID();

    public abstract String deviceIntrChannel();

}
