package de.hopp.generator.backends.workflow.ise.gpio;

import static de.hopp.generator.model.mhs.MHS.Assignment;
import static de.hopp.generator.model.mhs.MHS.Attribute;
import static de.hopp.generator.model.mhs.MHS.Block;
import static de.hopp.generator.model.mhs.MHS.Ident;
import static de.hopp.generator.model.mhs.MHS.PARAMETER;
import de.hopp.generator.model.mhs.Block;

/**
 *
 * @author Thomas Fischer
 * @since 2.8.2013
 */
public class GpioUtils {

    public static Block getMSSBlock(String instance, String version) {
        return Block("DRIVER",
            Attribute(PARAMETER(), Assignment("DRIVER_NAME", Ident("gpio"))),
            Attribute(PARAMETER(), Assignment("DRIVER_VER",  Ident(version))),
            Attribute(PARAMETER(), Assignment("HW_INSTANCE", Ident(instance)))
        );
    }
}
