package de.hopp.generator.backends.board.zed.gpio;

public abstract class GpioComponent implements de.hopp.generator.backends.workflow.ise.gpio.GpioComponent {
    // XPS
    public String instID() {
        return id() + "_" + width() + "Bits";
    }

    public String portID() {
        return instID() + "_" + (isGPI() ? "I" : "") + (isGPO() ? "O" : "");
    }

    public String getINTCPort() {
        return instID() + "_IP2INTC_Irpt";
    }

    // SDK
    public String deviceID() {
        return "XPAR_" + instID().toUpperCase() + "_DEVICE_ID";
    }

    public static final String intcPrefix = "XPAR_FABRIC";

    public String deviceIntrChannel() {
        return intcPrefix + "_" + getINTCPort().toUpperCase() + "_INTR";
    }
}
