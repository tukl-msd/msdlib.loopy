package de.hopp.generator.backends.workflow.ise.gpio;

import de.hopp.generator.backends.workflow.ise.xps.IPCoreVersions;
import de.hopp.generator.parser.MHSFile;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public interface GpioComponent extends de.hopp.generator.backends.board.GpioComponent {

    public MHSFile getMHS(IPCoreVersions versions);

    public abstract String getINTCPort();

    public abstract String getUCFConstraints();

    public abstract String hwInstance();

    public abstract String deviceID();

    public abstract String deviceIntrChannel();

}
