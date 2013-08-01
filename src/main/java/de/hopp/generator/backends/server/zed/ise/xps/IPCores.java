package de.hopp.generator.backends.server.zed.ise.xps;

import static de.hopp.generator.backends.server.zed.ise.xps.MHSUtils.add;
import static de.hopp.generator.parser.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getWidth;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FilenameUtils.getName;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.backends.server.zed.ise.ISEUtils;
import de.hopp.generator.backends.unparser.MHSUnparser;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.frontend.*;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.MHSFile;
/**
 * Handles generation and deployment of files required to describe IPCores
 * for the XPS toolsuite.
 *
 * @author Thomas Fischer
 * @since 24.5.2013
 */
public class IPCores {

    /**
     * Generate and deploy all files required to describe an IPCore.
     *
     * These required files are:
     * - the .mpd file describing the interface of the core
     * - the .pao file referencing the sources of the core and
     * - the vhdl sources themselves
     * @param core The core for which files should be deployed.
     * @param config Configuration of this run, containing required directories and log level.
     * @throws IOException if an Exception occurred with an underlying file operation.
     * @throws UsageError if an invalid combination of bdl attributes is encountered.
     */
    public static void deployCore(Core core, Configuration config) throws IOException, UsageError {
        File coresDir = new File(ISEUtils.edkDir(config), "pcores");

        MHSFile mpdFile;      // content for target mpd file
        String  paoFile = ""; // content for target pao file

        // required names
        String name = core.name();
        String fullName = name + "_v" + core.version().replace('.', '_');

        // required directories
        File coreDir     = new File(coresDir, fullName);
        File coreDataDir = new File(coreDir, "data");
        File coreSrcDir  = new File(new File(coreDir, "hdl"), "vhdl");

        // generate mpd and add it to mpd list
        mpdFile = createCoreMPD(core);

        // add sources to pao file
        for(Import source : core.source())
            paoFile += "\nlib " + fullName + " " + FilenameUtils.getBaseName(source.file()) + " vhdl";

        // skip deployment phase if this is only a dryrun
        if(config.dryrun());

        // deploy mpd file
        File target = new File(coreDataDir, name + "_v2_1_0" + ".mpd");
        StringBuffer buffer = new StringBuffer();
        MHSUnparser mhsUnparser = new MHSUnparser(buffer);
        mhsUnparser.visit(mpdFile);
        write(target, buffer);

        // deploy pao file
        target = new File(coreDataDir, name + "_v2_1_0" + ".pao");
        write(target, paoFile);

        // deploy sources
        for(Import source : core.source()) {
            target = new File(coreSrcDir, getName(source.file()));
            copyFile(new File(source.file()), target);
        }
    }

    /**
     * Translates the direction attribute of the port in a boolean value.
     * @param direction Direction attribute of the port.
     * @return true if in-going, false otherwise.
     * @throws UsageError If the port is bi-directional, since those
     *                    ports are not supported using AXI4 stream interfaces.
     */
    private static boolean direction(Direction direction) throws UsageError {
        return direction.Switch(new Direction.Switch<Boolean, UsageError>() {
            public Boolean   CaseIN(  IN term) { return true; }
            public Boolean  CaseOUT( OUT term) { return false; }
            public Boolean CaseDUAL(DUAL term) throws UsageError {
                throw new UsageError("invalid direction for virtex6");
            }
        });
    }

    /**
     * Creates the mpd file of a core. The file has still to be added to the mpd list.
     * @param core The core, for which an mpd file should be created
     * @return The generated mpd file
     * @throws UsageError Something went wrong during generation. This can be the case,
     *                    if unexpected attributes occurred in the core description.
     */
    protected static MHSFile createCoreMPD(Core core) throws UsageError {

        Block block = Block(core.name());

        block = add(block, Attributes(
            Attribute(OPTION(), Assignment("IPTYPE", Ident("PERIPHERAL"))),
            Attribute(OPTION(), Assignment("IMP_NETLIST", Ident("TRUE"))),
            Attribute(OPTION(), Assignment("IP_GROUP", Ident("USER"))),
            Attribute(OPTION(), Assignment("HDL", Ident("VHDL"))),
            Attribute(OPTION(), Assignment("STYLE", Ident("HDL")))
//            Attribute(OPTION(), Assignment("DESC", STR(core.name())))
        ));

        for(final Port port : core.ports())
            // splitting in several methods has only advantages in terms of code readability... (if at all)
            block = add(block, port.Switch(new Port.Switch<Attributes, UsageError>() {
                public Attributes CaseCLK(CLK term) { return generatePort(term); }
                public Attributes CaseRST(RST term) { return generatePort(term); }
                public Attributes CaseAXI(AXI term) throws UsageError {
                    return generatePort(term);
                }
            }));
        return MHSFile(Attributes(), block);
    }

    private static Attributes generatePort(AXI port) throws UsageError {
        int bitwidth = getWidth(port);
        boolean direct = direction(port.direction());

        Attributes attrs = Attributes();

        // add bus interface
        attrs = attrs.add(Attribute(BUS_IF(),
            Assignment("BUS", Ident(port.name())),
            Assignment("BUS_STD", Ident("AXIS")),
            Assignment("BUS_TYPE", Ident(direct ? "TARGET" : "INITIATOR"))
        ));

        // add protocol parameter
        attrs = attrs.add(Attribute(PARAMETER(),
            Assignment("C_"+port.name()+"_PROTOCOL", Ident("GENERIC")),
            Assignment("DT", Ident("string")),
            Assignment("TYPE", Ident("NON_HDL")),
            Assignment("ASSIGNMENT", Ident("CONSTANT")),
            Assignment("BUS", Ident(port.name()))
        ));

        // add width parameter
        attrs = attrs.add(Attribute(PARAMETER(),
            Assignment("C_"+port.name()+"_TDATA_WIDTH", Number(bitwidth)),
            Assignment("DT", Ident("integer")),
            Assignment("TYPE", Ident("NON_HDL")),
            Assignment("ASSIGNMENT", Ident("CONSTANT")),
            Assignment("BUS", Ident(port.name()))
        ));

        // add ready port
        attrs = attrs.add(Attribute(PORT(),
            Assignment(port.name() + "_tready", Ident("TREADY")),
            Assignment("DIR", Ident(direct ? "O" : "I")),
            Assignment("BUS", Ident(port.name()))
            ));

        // add valid port
        attrs = attrs.add(Attribute(PORT(),
            Assignment(port.name() + "_tvalid", Ident("TVALID")),
            Assignment("DIR", Ident(direct ? "I" : "O")),
            Assignment("BUS", Ident(port.name()))
            ));

        // add data port
        attrs = attrs.add(Attribute(PORT(),
            Assignment(port.name() + "_tdata", Ident("TDATA")),
            Assignment("DIR", Ident(direct ? "I" : "O")),
            Assignment("BUS", Ident(port.name())),
            Assignment("VEC", Range(bitwidth-1, 0))
            ));

        // add last port
        attrs = attrs.add(Attribute(PORT(),
            Assignment(port.name() + "_tlast", Ident("TLAST")),
            Assignment("DIR", Ident(direct ? "I" : "O")),
            Assignment("BUS", Ident(port.name()))
            ));

        return attrs;
    }

    private static Attributes generatePort(CLK port) {
        return Attributes(Attribute(PORT(),
            Assignment(port.name(), STR("")),
            Assignment("DIR", Ident("I")),
            Assignment("SIGIS", Ident("CLK")),
            Assignment("ASSIGNMENT", Ident("REQUIRE"))
        ));
    }

    private static Attributes generatePort(RST port) {
        return Attributes(Attribute(PORT(),
            Assignment(port.name(), STR("")),
            Assignment("DIR", Ident("I")),
            Assignment("SIGIS", Ident("RST")),
            Assignment("ASSIGNMENT", Ident("REQUIRE"))
        ));
    }
}
