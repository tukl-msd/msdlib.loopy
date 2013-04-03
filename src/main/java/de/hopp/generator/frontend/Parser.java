package de.hopp.generator.frontend;

import static de.hopp.generator.frontend.BDL.*;

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
        
    private BDLFile testfile() {
        return BDLFile(Imports(), Backends(), Options(), 
                Cores(
                   Core("adder", "1.00.a", 0, Strings("sample2.bdf"), Port("in1",IN(),0), Port("out1",OUT(),0), Port("in2",IN(),0)),
                   Core("fifo",  "1.00.a", 0, Strings("sample2.bdf"), Port("in1",IN(),0), Port("out1",OUT(),0)),
                   Core("rng",   "1.00.a", 0, Strings("sample2.bdf"), Port("in1",IN(),0), Port("out1",OUT(),0))
                ), GPIOs(
                    GPIO("leds",     OUT(),0),
                    GPIO("switches",  IN(),0),
                    GPIO("buttons",   IN(),0)
                ), Instances(
                    Instance("rng_a",   "rng",   0, CPUAxis("in1",0), CPUAxis("out1",0)),
                    Instance("rng_b",   "rng",   0, CPUAxis("in1",0), CPUAxis("out1",0)),
                    Instance("adder_a", "adder", 0, CPUAxis("in1",0), CPUAxis("out1",0), CPUAxis("in2",0)),
                    Instance("fifo_a",  "fifo",  0, CPUAxis("in1",0), CPUAxis("out1",0))
                ), Medium("ethernet", 0, "mac AA:BB:CC:DD:EE:FF"), DEFAULT());
    }
    
    private void sanityCheck(BDLFile bdf) {
        // what to do here (sequentially) and what to do in visitor? ... 
        
        // check for only single instances of medium and scheduler
        // this cannot be specified with the current katja file... 
        // --> be more liberal in bdl.katja or more restrictive in bdl.cup?
        
        // check existence of referenced core sources
        for(Core core : bdf.cores()) {
            for(String source : core.source()) {
                File sourcefile = new File(source);
                if(!sourcefile.exists() || !sourcefile.isFile())
                    errors.addError(new ParserError("Referenced sourcefile " + sourcefile + " does not exist", "", -1));
            }
        }
        
        // check for duplicate core identifiers
        Map<String, Core> cores = new HashMap<String, Core>();
        for(Core core : bdf.cores()) {
            if(cores.keySet().contains(core.name())) errors.addError(new ParserError("Duplicate core " + core.name(), "", -1));
            else cores.put(core.name(), core);
            
            // check for duplicate port identifiers
            Map<String, Port> ports = new HashMap<String, Port>();
            for(Port port : core.ports()) {
                if(ports.containsKey(port.name())) errors.addError(new ParserError("Duplicate port identifier " + port.name(), "", -1));
                else ports.put(port.name(), port);
            }
        }
            
        // check declaration of referenced cores
        for(Instance inst : bdf.insts()) {
            if(!cores.containsKey(inst.core())) {
                errors.addError(new ParserError("Instantiated undefined core " + inst.core(), "", -1));
                continue;
            }
        
            // check connection of all declared ports
            for(Port port : cores.get(inst.core()).ports()) {
                boolean connected = false;
                for(Binding binding : inst.bind()) if(binding.port().equals(port.name())) connected = true;
                if(!connected) errors.addWarning(new ParserWarning("Port " + port.name() + " of " +
                        inst.core() + " instance " + inst.name() + " is not connected"));
            }
        } cores.clear();
        
        // check for duplicate instance identifiers
        Map<String, Instance> instances = new HashMap<String, Instance>();
        for(Instance inst : bdf.insts())
            if(instances.containsKey(inst.name())) errors.addError(new ParserError("Duplicate instance identifier " + inst.name(), "", -1));
            else instances.put(inst.name(), inst);
        instances.clear();
        
        // check for duplicate gpio instances
        Map<String, GPIO> gpios = new HashMap<String, GPIO>();
        for(GPIO gpio : bdf.gpios())
            if(gpios.containsKey(gpio.name())) errors.addError(new ParserError("Duplicate GPIO instance " + gpio.name(), "", -1));
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
                errors.addWarning(new ParserWarning("Axis " + axis + " is only connected to a single port."));
            if(connections.get(axis).compareTo(2) > 0)
                errors.addError(new ParserError("Axis " + axis + " is connected to " + connections.get(axis) +
                        " ports. Only two ports can be connected with a single axis.", "", -1));
        } connections.clear();
        
        // check for invalid attribute combinations
        boolean sw = false, hw = false;
        
        // invalid options for the board in general
        for(Option o : bdf.opts()) {
            // poll is simply not allowed
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
        }
        
        boolean poll, width;
        // invalid options for port specifications
        for(Core core : bdf.cores()) {
            for(Port port : core.ports()) {
                sw = false; hw = false; poll = false; width = false;
                for(Option o : port.opts()) {
                    if(o instanceof POLL)
                        // poll is not allowed to occur at in-going ports
                        if(port instanceof IN) errors.addError(new ParserError("encountered option \"poll\" at in-going port", "", -1)); 
                        // at out-going ports it must occur at most once
                        else if(poll) errors.addError(new ParserError("duplicate port option \"poll\"", "", -1));
                        else poll = true;
                    // bitwidth is allowed to occur at most once
                    else if(o instanceof BITWIDTH)
                        if(width) errors.addError(new ParserError("duplicate port option \"bitwidth\"", "", -1));
                        else width = true;
                    // swqueue and hwqueue are allowed to occur at most once
                    else if(o instanceof SWQUEUE)
                        if(sw) errors.addError(new ParserError("duplicate port option \"swqueue\"", "", -1));
                        else sw = true;
                    else if(o instanceof HWQUEUE)
                        if(hw) errors.addError(new ParserError("duplicate port option \"hwqueue\"", "", -1));
                        else hw = true;
                }
            }
        }
        
//        // check options of Ethernet medium for validity
//        if(bdf.medium() instanceof Ethernet) {
//            Ethernet medium = (Ethernet)bdf.medium();
//            checkIP(medium.ip().ip());
//            checkIP(medium.mask().ip());
//            checkIP(medium.gate().ip());
//            checkMAC(medium.mac().mac());
//        }
    }
    
//    private void checkIP(String ip) {
//        String[] parts = ip.split("\\.");
//        if(parts.length < 4)
//            errors.addError(new ParserError("ip consists of only " + parts.length + " parts (expected 4)"));
//        if(parts.length > 4)
//            errors.addError(new ParserError("ip consists of " + parts.length + " parts (expected 4)"));
//        for(int i = 0; i < parts.length; i++) {
//            String part = parts[i];
//            try {
//                int intpart = Integer.valueOf(part);
//                if(intpart <   0)
//                    errors.addError(new ParserError("part " + (i+1) + " of ip is smaller than 0"));
//                if(intpart > 255)
//                    errors.addError(new ParserError("part " + (i+1) + " of ip is greater than 255"));
//            } catch (NumberFormatException e) {
//                errors.addError(new ParserError("part " + (i+1) + " of ip is not a valid decimal number"));
//            }
//        }
//    }
//    
//    private void checkMAC(String mac) {
//        String[] parts = mac.split(":");
//        if(parts.length < 6)
//            errors.addError(new ParserError("mac consists of only " + parts.length + " parts (expected 6)"));
//        if(parts.length > 6)
//            errors.addError(new ParserError("mac consists of " + parts.length + " parts (expected 6)"));
//        for(int i = 0; i < parts.length; i++) {
//            String part = parts[i];
//            try {
//                int intpart = Integer.parseInt(part, 16);
//                if(intpart <   0) // < 0x00
//                    errors.addError(new ParserError("part " + (i+1) + " of mac is smaller than 0x00"));
//                if(intpart > 255) // > 0xFF
//                    errors.addError(new ParserError("part " + (i+1) + " of mac is smaller than 0xFF"));
//            } catch(NumberFormatException e) {
//                errors.addError(new ParserError("part " + (i+1) + " of mac is not a valid hexadecimal number"));
//            }
//        }
//    }
}



