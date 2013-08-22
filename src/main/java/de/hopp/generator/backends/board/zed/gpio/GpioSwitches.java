package de.hopp.generator.backends.board.zed.gpio;

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
        return "\nNET " + portID() + "[0] LOC = \"F22\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[1] LOC = \"G22\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[2] LOC = \"H22\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[3] LOC = \"F21\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[4] LOC = \"H19\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[5] LOC = \"H18\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[6] LOC = \"H17\" | IOSTANDARD = \"LVCMOS25\";" +
               "\nNET " + portID() + "[7] LOC = \"M15\" | IOSTANDARD = \"LVCMOS25\";\n";
    }
}
