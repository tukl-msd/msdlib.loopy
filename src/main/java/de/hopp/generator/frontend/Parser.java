package de.hopp.generator.frontend;

import static org.apache.commons.io.FilenameUtils.getBaseName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import java_cup.runtime.Symbol;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.ParserWarning;
import de.hopp.generator.exceptions.UsageError;

public class Parser {

    private ErrorCollection errors;

    public Parser(ErrorCollection errors) {
        this.errors = errors;
    }

    public BDLFile parse(File file){
        // resolve all imports (recursively) and construct one big BDLFile
        LinkedList<File> files = new LinkedList<File>();
        Set<String> fileNames = new HashSet<String>();
        files.add(file);

        BDLFile bdl = parse(files, fileNames);

        // abort, if errors occurred
        if(errors.hasErrors()) return null;

        // perform sanity checks on the parsed bdl file
        sanityCheck(bdl);

        // abort, if errors occurred
//        if(errors.hasErrors()) throw new ParserError("Fatal error while parsing board description file");

        // return its results
        return bdl;
    }

    private BDLFile parse(LinkedList<File> files, Set<String> fileNames) {
        // if the file list is empty, return null
        if(files.isEmpty()) return null;

        // get the first file from the list and remove it
        File file = files.remove();

        try {
            if(fileNames.contains(file.getCanonicalPath())) return parse(files, fileNames);

            // construct scanner and parser
            InputStream input = new FileInputStream(file);
            BDLFileScanner scanner = new BDLFileScanner(input);
            BDLFileParser parser = new BDLFileParser(scanner);

            // set attributes of the parser
            parser.setErrorCollection(errors);
            parser.setFilename(file.getCanonicalPath());

            // parse the file
            try {
                Symbol symbol = parser.parse();

                // abort, if errors occurred while parsing
                if(errors.hasErrors()) return null;

                // otherwise, cast the result to a BDLFile
                BDLFile bdl = (BDLFile) symbol.value;

                // add all imports of this file
                for(Import imp : bdl.imports()) files.add(new File(imp.file()));

                // return merge of this file and recursive call
                return merge(bdl, parse(files, fileNames));
            } catch(Exception e) {
                e.printStackTrace();
                errors.addError(new ParserError("Encountered error while parsing: " + e.getMessage(), file.getCanonicalPath(), -1));
            }
        } catch(IOException e) {
            errors.addError(new UsageError("File not found " + file.getPath()));
        }
        return null;
    }

    private BDLFile merge(BDLFile file1, BDLFile file2) {
        // if the second file is null, return the first file
        if(file2 == null) return file1;

        return file1;
    }

    /**
     * Checks several conditions, which are assumed to hold for the model.
     * Adds errors to the error collection of the parser, if conditions
     * do not hold. May also add warnings for less severe conditions, that
     * are not necessarily required by the model but may indicate user errors
     * nonetheless or require the users attention otherwise.
     *  - existence of all referenced source files, cores and ports
     *  - no duplicate identifiers for cores, ports or instances
     *  - correct connection of all ports
     *    - no duplicate connections
     *    - no unconnected ports or dangling axis
     *    - at most two ports at the same axis
     *  - no duplicate options or unexpected options in wrong context
     *  - correct format of string options (esp Ethernet options)
     * @param bdf
     */
    private void sanityCheck(BDLFile bdf) {
        // iterate over all cores...
        Map<String, Core> cores = new HashMap<String, Core>();
        for(Core core : bdf.cores()) {
            Set<String> sources = new HashSet<String>();
            for(Import source : core.source()) {
                // check existence of referenced core sources
                File sourcefile = new File(source.file());
                if(!sourcefile.exists() || !sourcefile.isFile())
                    errors.addError(new ParserError("Referenced sourcefile " + sourcefile + " does not exist", source.pos()));

                // check for duplicate filenames
                if(!sources.add(getBaseName(source.file()).toLowerCase())) {
                    errors.addError(new ParserError("name of sourcefile " + source.file() +
                        " clashes (case-insensitive) with other sourcefile", source.pos()));
                }
            }

            // check for existence of a source named after the core
//            if(! sources.contains(core.name().toLowerCase())) errors.addError(new ParserError(
//                "core " + core.name() + " does not contain a top level source file named after the core.", core.pos()
//            ));

            // check for duplicate core identifiers
            if(cores.keySet().contains(core.name())) errors.addError(new ParserError("Duplicate core " + core.name(), core.pos()));
            else cores.put(core.name(), core);

            // check for duplicate port identifiers
            Map<String, Port> ports = new HashMap<String, Port>();
            for(Port port : core.ports()) {
                if(ports.containsKey(port.name())) errors.addError(new ParserError("Duplicate port identifier " + port.name(), port.pos()));
                else ports.put(port.name(), port);
            }
        }

        // iterate over all instances...
        Map<String, Instance> instances = new HashMap<String, Instance>();
        for(Instance inst : bdf.insts()) {

            // check declaration of referenced core
            if(!cores.containsKey(inst.core())) {
                errors.addError(new ParserError("Instantiated undefined core " + inst.core(), inst.pos()));
                continue;
            }

            // check declaration of referenced ports
            for(Binding b : inst.bind()) {
                boolean exists = false;

                for(Port port : cores.get(inst.core()).ports())
                    if(port.name().equals(b.port())) exists = true;

                if(!exists) errors.addError(new ParserError("Binding to non-existing port " + b.port(), b.pos()));
            }

            // check for duplicate instance identifiers
            if(instances.containsKey(inst.name())) errors.addError(new ParserError("Duplicate instance identifier " + inst.name(), inst.pos()));
            else instances.put(inst.name(), inst);

            // check connection of all declared axi ports
            for(Port port : cores.get(inst.core()).ports()) {
                // skip non-axi ports (i.e. clk and rst)
                if(! (port instanceof AXI)) continue;

                boolean connected = false;
                for(Binding bind : inst.bind()) if(bind.port().equals(port.name())) connected = true;

                // add a warning for unconnected ports
                if(!connected) errors.addWarning(new ParserWarning("Port " + port.name() + " of " +
                        inst.core() + " instance " + inst.name() + " is not connected", inst.pos()));
            }
        }
        cores.clear();
        instances.clear();

        // check for duplicate gpio instances
        Map<String, GPIO> gpios = new HashMap<String, GPIO>();
        for(GPIO gpio : bdf.gpios())
            if(gpios.containsKey(gpio.name())) errors.addError(new ParserError("Duplicate GPIO instance " + gpio.name(), gpio.pos()));
            else gpios.put(gpio.name(), gpio);
        gpios.clear();

        // check axis connection count (one for cpu axis, two for others)
        Map<String, Integer> connections = new HashMap<String, Integer>();
        for(Instance inst : bdf.insts())
            for(Binding bind : inst.bind()) {
                if(bind instanceof CPUAxis) continue;
                // increment counter for this axis
                else if(connections.containsKey(((Axis)bind).axis()))
                    connections.put(((Axis)bind).axis(), connections.get(bind)+1);
                else connections.put(((Axis)bind).axis(), 1);
            }
        for(String axis : connections.keySet()) {
            if(connections.get(axis).compareTo(2) < 0)
                errors.addWarning(new ParserWarning("Axis " + axis + " is only connected to a single port.", "", -1));
            if(connections.get(axis).compareTo(2) > 0)
                errors.addError(new ParserError("Axis " + axis + " is connected to " + connections.get(axis) +
                        " ports. Only two ports can be connected with a single axis.", "", -1));
        } connections.clear();

        // check for invalid options
        boolean sw = false, hw = false, debug_host = false, debug_board = false;

        // invalid options for the board in general
        for(Option o : bdf.opts()) {
            // poll is simply not allowed here
            if(o instanceof POLL) errors.addError(new ParserError("encountered option \"poll\" as board option", "", -1));
            // neither is bitwidth
            else if(o instanceof BITWIDTH) errors.addError(new ParserError("encountered option \"width\" as board option", "", -1));
            // swqueue and hwqueue are allowed to occur at most once
            else if(o instanceof SWQUEUE)
                if(sw) errors.addError(new ParserError("duplicate board option \"swqueue\"", "", -1));
                else sw = true;
            else if(o instanceof HWQUEUE)
                if(hw) errors.addError(new ParserError("duplicate board option \"swqueue\"", "", -1));
                else hw = true;
            // at most a single instance of the log parameter for host and board
            else if(o instanceof LOG)
                if(((LOG)o).host())
                    if(debug_host)
                        errors.addError(new ParserError("duplicate board optione \"log host\"", "", -1));
                    else debug_host = true;
                else
                    if(debug_board)
                        errors.addError(new ParserError("duplicate board optione \"log board\"", "", -1));
                    else debug_board = true;
        }

        boolean poll, width;
        // invalid options for port specifications
        for(Core core : bdf.cores()) {
            boolean clk = false, rst = false;
            for(Port port : core.ports()) {
                if(port instanceof CLK) {
                    if(clk) errors.addError(new ParserError("Duplicate clk port binding for core", port.pos()));
                    else clk = true;
                    continue;
                    // TODO some sort of counter for non-default clock values?
                }
                if(port instanceof RST) {
                    if(rst) errors.addError(new ParserError("Duplicate rst port binding for core", port.pos()));
                    else rst = true;
                    continue;
                }

                sw = false; hw = false; poll = false; width = false;
                for(Option o : ((AXI)port).opts()) {
                    if(o instanceof POLL)
                        // poll is not allowed to occur at in-going ports
                        if(port instanceof IN) errors.addError(new ParserError("encountered option \"poll\" at in-going port", port.pos()));
                        // at out-going ports it must occur at most once
                        else if(poll) errors.addError(new ParserError("duplicate port option \"poll\"", port.pos()));
                        else poll = true;
                    // bitwidth is allowed to occur at most once
                    else if(o instanceof BITWIDTH)
                        if(width) errors.addError(new ParserError("duplicate port option \"bitwidth\"", port.pos()));
                        else width = true;
                    // swqueue and hwqueue are allowed to occur at most once
                    else if(o instanceof SWQUEUE)
                        if(sw) errors.addError(new ParserError("duplicate port option \"swqueue\"", port.pos()));
                        else sw = true;
                    else if(o instanceof HWQUEUE)
                        if(hw) errors.addError(new ParserError("duplicate port option \"hwqueue\"", port.pos()));
                        else hw = true;
                }
            }

            if(!clk) errors.addError(new ParserError("core declaration is missing clock port", core.pos()));
            if(!rst) errors.addError(new ParserError("core declaration is missing reset port", core.pos()));
        }

        // check, if the scheduler or gpio callbacks have been overridden and add appropriate warning
        if(bdf.scheduler().code() instanceof USER_DEFINED) errors.addWarning(new ParserWarning(
                "Default scheduler was overridden. Note that no guarantees can be made for user-defined schedulers.",
                bdf.scheduler().pos()));
        // TODO what exactly do we override here? board-side or host-side gpio behaviour?
        // TODO what exactly IS default behaviour?
        //       host-side: none
        //       board-side: send value to host. Host does "something" with it...
        //     --> I GUESS, we want to override board-side behaviour... e.g. reset on click or write the value
        //         into some component
        //     This CAN be done via a host-side thread, but is REALLY inefficient due to a complete communication cycle
        //      plus potential queue overhead...
        for(GPIO gpio : bdf.gpios()) if(gpio.callback() instanceof USER_DEFINED)
            errors.addWarning(new ParserWarning("Default gpio callback behaviour was overridden", gpio.pos()));



//        // check options of Ethernet medium for validity
        if(bdf.medium() instanceof ETHERNET) {
            ETHERNET medium = (ETHERNET)bdf.medium();
            boolean mac = false, ip = false, mask = false, gate = false, port = false;
            for(MOption opt : medium.opts()) {
                if(opt instanceof MAC) {
                    if(mac) {
                        errors.addError(new ParserError("duplicate mac address attribute", opt.pos()));
                        continue;
                    } mac = true;
                    MAC o = (MAC)opt;
                    checkMAC(o.val(), opt.pos());
                } else if(opt instanceof IP) {
                    if(ip) {
                        errors.addError(new ParserError("duplicate ip address attribute", opt.pos()));
                        continue;
                    } ip = true;
                    IP o = (IP)opt;
                    checkIP(o.val(), opt.pos());
                } else if(opt instanceof MASK) {
                    if(mask) {
                        errors.addError(new ParserError("duplicate network mask attribute", opt.pos()));
                        continue;
                    } mask = true;
                    MASK o = (MASK)opt;
                    checkIP(o.val(), opt.pos());
                } else if(opt instanceof GATE) {
                    if(gate) {
                        errors.addError(new ParserError("duplicate gateway attribute", opt.pos()));
                        continue;
                    } gate = true;
                    GATE o = (GATE)opt;
                    checkIP(o.val(), opt.pos());
                } else if(opt instanceof PORTID) {
                    if(port) {
                        errors.addError(new ParserError("duplicate port attribute", opt.pos()));
                        continue;
                    } port = true;
//                    PORTID o = (PORTID)opt;
                }
            }
            if(!mac)  errors.addError(
                new ParserError("Ethernet specification is missing mac address attribute", medium.pos()));
            if(!ip)   errors.addError(
                new ParserError("Ethernet specification is missing ip address attribute", medium.pos()));
            if(!mask) errors.addError(
                new ParserError("Ethernet specification is missing network mask attribute", medium.pos()));
            if(!gate) errors.addError(
                new ParserError("Ethernet specification is missing gateway attribute", medium.pos()));
            if(!port) errors.addError(
                new ParserError("Ethernet specification is missing port attribute", medium.pos()));
        }
   }

//  /** converts ip addresses represented as string arrays int integer arrays.
//  * Also does validity checks for the given ip address.
//  * @throws IllegalArgumentException The given string array does not fulfill
//  * format requirements for ip addresses, i.e. wrong size or wrong contents.
//  */
// private static int[] convertIPAddress(String[] ip) throws IllegalArgumentException {
//     int[] targ = new int[4];
//     if(ip.length != 4) throw new IllegalArgumentException("invalid ip address: must contain 4 components");
//     for(String s : ip) if(s.length() < 1 || s.length() > 3)
//         throw new IllegalArgumentException("invalid ip address: each component has to be 1-3 characters long");
//     for(int i = 0; i<ip.length; i++) {
//         try {
//             int j = Integer.valueOf(ip[i]);
//             if (j < 0 | j > 255)
//                 throw new IllegalArgumentException("invalid ip address: components can only range from 0 to 255");
//             targ[i] = j;
//         } catch (NumberFormatException e) {
//             throw new IllegalArgumentException("invalid ip address: components have to be decimal numbers");
//         }
//     }
//     return targ;
// }
//private static boolean isHexString(String s) {
//for(char c : s.toCharArray()) {
//   if(!isHexCharacter(c)) return false;
//}
//return true;
//}
//
//private static boolean isHexCharacter(char c) {
//return c >= 0 || c <= 9 || c >= 'a' || c <= 'f' || c >= 'A' || c <= 'F';
//}

    private void checkIP(String ip, Position pos) {
        String[] parts = ip.split("\\.");
        if(parts.length < 4)
            errors.addError(new ParserError("ip consists of only " + parts.length + " parts (expected 4)", pos));
        if(parts.length > 4)
            errors.addError(new ParserError("ip consists of " + parts.length + " parts (expected 4)", pos));
        for(int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                int intpart = Integer.valueOf(part);
                if(intpart <   0)
                    errors.addError(new ParserError("part " + (i+1) + " of ip is smaller than 0", pos));
                if(intpart > 255)
                    errors.addError(new ParserError("part " + (i+1) + " of ip is greater than 255", pos));
            } catch (NumberFormatException e) {
                errors.addError(new ParserError("part " + (i+1) + " of ip is not a valid decimal number", pos));
            }
        }
    }

    private void checkMAC(String mac, Position pos) {
        String[] parts = mac.split(":");
        if(parts.length < 6)
            errors.addError(new ParserError("mac consists of only " + parts.length + " parts (expected 6)", pos));
        if(parts.length > 6)
            errors.addError(new ParserError("mac consists of " + parts.length + " parts (expected 6)", pos));
        for(int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                int intpart = Integer.parseInt(part, 16);
                if(intpart <   0) // < 0x00
                    errors.addError(new ParserError("part " + (i+1) + " of mac is smaller than 0x00", pos));
                if(intpart > 255) // > 0xFF
                    errors.addError(new ParserError("part " + (i+1) + " of mac is smaller than 0xFF", pos));
            } catch(NumberFormatException e) {
                errors.addError(new ParserError("part " + (i+1) + " of mac is not a valid hexadecimal number", pos));
            }
        }
    }
}



