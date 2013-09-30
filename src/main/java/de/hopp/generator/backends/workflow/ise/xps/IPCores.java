package de.hopp.generator.backends.workflow.ise.xps;

import static de.hopp.generator.backends.workflow.ise.xps.MHSUtils.add;
import static de.hopp.generator.model.mhs.MHS.*;
import static de.hopp.generator.utils.BoardUtils.getWidth;
import static de.hopp.generator.utils.Files.deploy;
import static de.hopp.generator.utils.Files.deployContent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import de.hopp.generator.Configuration;
import de.hopp.generator.backends.workflow.ise.ISEUtils;
import de.hopp.generator.exceptions.UsageError;
import de.hopp.generator.model.*;
import de.hopp.generator.model.mhs.Attributes;
import de.hopp.generator.model.mhs.Block;
import de.hopp.generator.model.mhs.MHSFile;
import de.hopp.generator.model.unparser.MHSUnparser;
/**
 * Handles generation and deployment of files required to describe
 * user-specified IPCores for the XPS synthesis tool.
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
     *
     * Will check if these files already exist and are identical to the new files
     * to dpeloy. Will not dpeloy any files, if this is the case.
     *
     * @param core The core for which files should be deployed.
     * @param config Configuration of this run, containing required directories and log level.
     * @return true if new files have been deployed by this step, false otherwise
     * @throws IOException if an Exception occurred with an underlying file operation.
     * @throws UsageError if an invalid combination of bdl attributes is encountered.
     */
    public static boolean deployCore(Core core, Configuration config) throws IOException, UsageError {
        config.IOHANDLER().verbose("  deploying sources for core " + core.name());

        boolean newFiles = false;

        File coresDir = new File(ISEUtils.edkDir(config), "pcores");

        MHSFile mpdContent;      // content for target mpd file
        String  paoContent = ""; // content for target pao file
        String  bbdContent = "Files\n"; // content for target bdd file

        /** Map for sources to be deployed. Key value is the target file. */
        Map<File, File> deploySources = new HashMap<File, File>();

        // required names
        String name = core.name();
        String fullCoreName = name + "_v" + core.version().replace('.', '_');

        // required directories
        File projectDataDir = new File("data");

        File coreDir        = new File(coresDir, fullCoreName);
        File coreDataDir    = new File(coreDir, "data");
        File coreVHDLDir    = new File(new File(coreDir, "hdl"), "vhdl");
        File coreVerilogDir = new File(new File(coreDir, "hdl"), "verilog");
        File coreNetlistDir = new File(coreDir, "netlist");

        // generate mpd and add it to mpd list
        mpdContent = createCoreMPD(core);

        // add sources to pao file
        for(Import source : core.source()) {
            FileType fileType = FileType.fromFilename(source.file());

            // add line to a management file if required
            switch(fileType) {
            case VHDL:
                paoContent += "\nlib " + fullCoreName + " " + FilenameUtils.getBaseName(source.file()) + " vhdl";
                deploySources.put(
                    new File(coreVHDLDir,
                        FilenameUtils.getName(source.file())),
                    new File(source.file()));
                break;
            case Verilog:
                // TODO find out correct line...
                paoContent += "\nlib " + fullCoreName + " " + FilenameUtils.getBaseName(source.file()) + " verilog";
                deploySources.put(
                    new File(coreVerilogDir,
                        FilenameUtils.getName(source.file())),
                    new File(source.file()));
                break;
            case NGC:
                bbdContent += "\n" + FilenameUtils.getName(source.file());
                deploySources.put(
                    new File(coreNetlistDir,
                        FilenameUtils.getName(source.file())),
                    new File(source.file()));
                break;
            case UCF:
                throw new UsageError(".ucf files are not supported yet...");
            }
        }

        // skip deployment phase if this is only a dryrun
        if(config.dryrun()) return newFiles;

        // deploy mpd file
        File target = new File(coreDataDir, name + "_v2_1_0" + ".mpd");
        StringBuffer buffer = new StringBuffer();
        MHSUnparser mhsUnparser = new MHSUnparser(buffer);
        mhsUnparser.visit(mpdContent);
        newFiles = deployContent(buffer, target, config.IOHANDLER()) || newFiles;

        // deploy sources
        for(Entry<File, File> entry : deploySources.entrySet()) {
            newFiles = deploy(entry.getValue(), entry.getKey(), config.IOHANDLER()) || newFiles;
        }

        // deploy other core management files
        File paoFile = new File(coreDataDir, name + "_v2_1_0" + ".pao");
        newFiles = deployContent(paoContent, paoFile, config.IOHANDLER()) || newFiles;

        File bbdFile = new File(coreDataDir, name + "_v2_1_0" + ".bbd");
        newFiles = deployContent(bbdContent, bbdFile, config.IOHANDLER()) || newFiles;

        return newFiles;
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
            Attribute(OPTION(), Assignment("HDL", Ident("MIXED")))
//            Attribute(OPTION(), Assignment("STYLE", Ident("MIX"))),
//            Attribute(OPTION(), Assignment("RUN_NGCBUILD", Ident("TRUE")))
//            Attribute(OPTION(), Assignment("DESC", STR(core.name())))
        ));

        // TODO what about verilog combinations?
        // TODO this is not the most generic solution, but always adding these lines and an empty bbd file
        //      leads to xps build fails ):
        if(containsVHDL(core)) {
            if(containsNetlist(core)) {
                block = add(block, Attribute(OPTION(), Assignment("STYLE", Ident("MIX"))));
                block = add(block, Attribute(OPTION(), Assignment("RUN_NGCBUILD", Ident("TRUE"))));
            } else {
                block = add(block, Attribute(OPTION(), Assignment("STYLE", Ident("HDL"))));
            }
        }

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

    private static boolean containsVHDL(Core core) throws UsageError {
        for(Import imp : core.source())
            if(FileType.fromFilename(imp.file()) == FileType.VHDL)
                return true;

        return false;
    }

    private static boolean containsVerilog(Core core) throws UsageError{
        for(Import imp : core.source())
            if(FileType.fromFilename(imp.file()) == FileType.Verilog)
                return true;

        return false;
    }

    private static boolean containsNetlist(Core core) throws UsageError{
        for(Import imp : core.source())
            if(FileType.fromFilename(imp.file()) == FileType.NGC)
                return true;

        return false;
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

    enum FileType {
        VHDL("vhd"), Verilog("v"), NGC("ngc"), UCF("ucf");

        private String extension;

        FileType(String extension) {
            this.extension = extension;
        }

        static FileType fromFilename(String filename) throws UsageError {
            String extension = FilenameUtils.getExtension(filename);
            for(FileType fileType : FileType.values())
                if(fileType.extension.equals(extension))
                    return fileType;

            throw new UsageError("File extension " + extension + " is unknown for sourcefile");
        }
    }
}
