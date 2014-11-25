package de.hopp.generator.backends.board.zed.gpio;

/**
 *
 * @author Thomas Fischer
 * @since 10.6.2013
 */
public class GpioButtons extends GpioComponent {

    /**
     * Returns the identifier used to create this GPIO component.
     *
     * The identifier is mainly used in the .bdl file and the parser.
     * Keep the names consistent (for now)!
     * The generator itself works with this GPIO enum.
     *
     * @return The identifier used to create this GPIO component.
     */
    public String id()     { return "buttons"; }
    public int width()     { return 5;         }
    public boolean isGPI() { return true;      }
    public boolean isGPO() { return false;     }

    public String getUCFConstraints() {
        return  "\nNET " + portID() + "[0] LOC = \"P16\" | IOSTANDARD = \"LVCMOS25\";" +
                "\nNET " + portID() + "[1] LOC = \"R16\" | IOSTANDARD = \"LVCMOS25\";" +
                "\nNET " + portID() + "[2] LOC = \"N15\" | IOSTANDARD = \"LVCMOS25\";" +
                "\nNET " + portID() + "[3] LOC = \"R18\" | IOSTANDARD = \"LVCMOS25\";" +
                "\nNET " + portID() + "[4] LOC = \"T18\" | IOSTANDARD = \"LVCMOS25\";\n";
    }
}
