package de.hopp.generator.backends.workflow.ise.gpio;

import static de.hopp.generator.model.mhs.MHS.*;
import de.hopp.generator.backends.workflow.ise.sdk.DriverVersions;
import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.model.mhs.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public abstract class GpioComponent implements de.hopp.generator.backends.board.GpioComponent {

    public abstract String hwInstance();

    // There might be different ways to specify the gpio component depending on the ise version
    // if this is the case, we need explicit mhs/mss getters here, similar to the pattern in iseboard...
    public abstract MHSFile getMHS(IPCoreVersions versions);

    public abstract String getUCFConstraints();

    public String getINTCPort() {
        return hwInstance().toUpperCase() + "_IP2INTC_Irpt";
    }

    // SDK
    public MHSFile getMSS(DriverVersions versions) {
        return MHSFile(Attributes(), Block("DRIVER",
            Attribute(PARAMETER(), Assignment("DRIVER_NAME", Ident("gpio"))),
            Attribute(PARAMETER(), Assignment("DRIVER_VER",  Ident(versions.mss_gpio))),
            Attribute(PARAMETER(), Assignment("HW_INSTANCE", Ident(hwInstance().toLowerCase())))
            ));
    }

    public String deviceID() {
        return "XPAR_" + hwInstance().toUpperCase() + "_DEVICE_ID";
    }

    public abstract String deviceIntrChannel();

}
