package de.hopp.generator.backends.board.virtex.virtex6.gpio;


/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioLEDs extends GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "leds"; }
    public int width()     { return 8;      }
    public boolean isGPI() { return false;  }
    public boolean isGPO() { return true;   }

    public String getUCFConstraints() {
        return "\nNET " + portID() + "[0] LOC = \"AC22\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[1] LOC = \"AC24\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[2] LOC = \"AE22\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[3] LOC = \"AE23\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[4] LOC = \"AB23\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[5] LOC = \"AG23\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[6] LOC = \"AE24\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[7] LOC = \"AD24\" | IOSTANDARD = \"LVCMOS25\";\n";
    }
}
