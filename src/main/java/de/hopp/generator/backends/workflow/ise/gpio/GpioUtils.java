package de.hopp.generator.backends.workflow.ise.gpio;

import static de.hopp.generator.parser.MHS.Assignment;
import static de.hopp.generator.parser.MHS.Attribute;
import static de.hopp.generator.parser.MHS.Block;
import static de.hopp.generator.parser.MHS.Ident;
import static de.hopp.generator.parser.MHS.PARAMETER;
import de.hopp.generator.parser.Block;

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
