package de.hopp.generator.backends.board.zed.gpio;

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
        return "\nNET " + portID() + "[0] LOC = \"T22\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[1] LOC = \"T21\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[2] LOC = \"U22\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[3] LOC = \"U21\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[4] LOC = \"V22\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[5] LOC = \"W22\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[6] LOC = \"U19\" | IOSTANDARD = \"LVCMOS33\";" +
               "\nNET " + portID() + "[7] LOC = \"U14\" | IOSTANDARD = \"LVCMOS33\";\n";
    }
}
