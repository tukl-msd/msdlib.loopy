package de.hopp.generator.backends.board.virtex.virtex6.gpio;


/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioSwitches extends GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "switches"; }
    public int width()     { return 8;          }
    public boolean isGPI() { return true;       }
    public boolean isGPO() { return false;      }

    public String getUCFConstraints() {
        return "\nNET " + portID() + "[0] LOC = \"D22\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[1] LOC = \"C22\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[2] LOC = \"L21\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[3] LOC = \"L20\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[4] LOC = \"C18\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[5] LOC = \"B18\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[6] LOC = \"K22\" | IOSTANDARD = \"LVCMOS15\";" +
               "\nNET " + portID() + "[7] LOC = \"K21\" | IOSTANDARD = \"LVCMOS15\";\n";
    }
}
