package de.hopp.generator.frontend;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.exceptions.ParserError;
import de.hopp.generator.exceptions.ParserWarning;

import static de.hopp.generator.frontend.BDL.*;

public class Parser2 {

    private ErrorCollection errors;
    
    public Parser2(ErrorCollection errors) {
        this.errors = errors;
    }
    
    public BDLFile parse(File file){
        // assume, that a flex/cup combination exists to provide a valid BDLFile
        
        // we furthermore assume, that THIS bdl file is complete and imports are already resolved
        // (as in: one object modelling the board described by ALL these files together)
        BDLFile bdl = testfile();
        
        // perform sanity checks on the parsed bdl file
        sanityCheck(bdl);

        // abort, if errors occurred
//        if(errors.hasErrors()) throw new ParserError("Fatal error while parsing board description file");
        
        // return its results
        return bdl;
    }
    
    private BDLFile testfile() {
        return BDLFile(Imports(), Backends(), Constants(), DEFAULT(), Medium("ethernet", "mac AA:BB:CC:DD:EE:FF"),
                Cores(
                   Core("adder", Strings("sample2.bdf"), Port("in1", IN()), Port("out1", OUT()), Port("in2", IN())),
                   Core("fifo",  Strings("sample2.bdf"), Port("in1", IN()), Port("out1", OUT())),
                   Core("rng",   Strings("sample2.bdf"), Port("in1", IN()), Port("out1", OUT()))
                ), GPIOs(
                    GPIO("leds",     OUT(), Code(Strings())),
                    GPIO("switches",  IN(), Code(Strings())),
                    GPIO("buttons",   IN(), Code(Strings()))
                ), Instances(
                    Instance("rng_a",   "rng",   CPUAxis("in1"), CPUAxis("out1")),
                    Instance("rng_b",   "rng",   CPUAxis("in1"), CPUAxis("out1")),
                    Instance("adder_a", "adder", CPUAxis("in1"), CPUAxis("out1"), CPUAxis("in2")),
                    Instance("fifo_a",  "fifo",  CPUAxis("in1"), CPUAxis("out1"))
                ));
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
                    errors.addError(new ParserError("Referenced sourcefile " + sourcefile + " does not exist"));
            }
        }
        
        // check for duplicate core identifiers
        Map<String, Core> cores = new HashMap<String, Core>();
        for(Core core : bdf.cores()) {
            if(cores.keySet().contains(core.name())) errors.addError(new ParserError("Duplicate core " + core.name()));
            else cores.put(core.name(), core);
            
            // check for duplicate port identifiers
            Map<String, Port> ports = new HashMap<String, Port>();
            for(Port port : core.ports()) {
                if(ports.containsKey(port.name())) errors.addError(new ParserError("Duplicate port identifier " + port.name()));
                else ports.put(port.name(), port);
            }
        }
            
        // check declaration of referenced cores
        for(Instance inst : bdf.instances()) {
            if(!cores.containsKey(inst.core())) errors.addError(new ParserError("Instantiated undefined core " + inst.core())); 
        
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
        for(Instance inst : bdf.instances())
            if(instances.containsKey(inst.name())) errors.addError(new ParserError("Duplicate instance identifier " + inst.name()));
            else instances.put(inst.name(), inst);
        instances.clear();
        
        // check for duplicate gpio instances
        Map<String, GPIO> gpios = new HashMap<String, GPIO>();
        for(GPIO gpio : bdf.gpios())
            if(gpios.containsKey(gpio.name())) errors.addError(new ParserError("Duplicate GPIO instance " + gpio.name()));
            else gpios.put(gpio.name(), gpio);
        gpios.clear();
        
        // check axis connection count (one for cpu axis, two for others)
        Map<String, Integer> connections = new HashMap<String, Integer>();
        for(Instance inst : bdf.instances())
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
                        " ports. Only two ports can be connected with a single axis."));
        } connections.clear();
        
        // check for invalid attribute combinations
        boolean sw = false, hw = false;
        
        // invalid options for the board in general
        for(Constant c : bdf.constants()) {
            // poll is simply not allowed
            if(c instanceof POLL) errors.addError(new ParserError("encountered option \"poll\" as board option")); 
            // neither is bitwidth
            else if(c instanceof BITWIDTH) errors.addError(new ParserError("encountered option \"width\" as board option"));
            // swqueue and hwqueue are allowed to occur at most once
            else if(c instanceof SWQUEUE)
                if(sw) errors.addError(new ParserError("duplicate board option \"swqueue\""));
                else sw = true;
            else if(c instanceof HWQUEUE)
                if(hw) errors.addError(new ParserError("duplicate board option \"swqueue\""));
                else hw = true;
        }
        
        boolean poll, width;
        // invalid options for port specifications
        for(Core core : bdf.cores()) {
            for(Port port : core.ports()) {
                sw = false; hw = false; poll = false; width = false;
                for(Constant c : port.constants()) {
                    if(c instanceof POLL)
                        // poll is not allowed to occur at in-going ports
                        if(port instanceof IN) errors.addError(new ParserError("encountered option \"poll\" at in-going port")); 
                        // at out-going ports it must occur at most once
                        else if(poll) errors.addError(new ParserError("duplicate port option \"poll\""));
                        else poll = true;
                    // bitwidth is allowed to occur at most once
                    else if(c instanceof BITWIDTH)
                        if(width) errors.addError(new ParserError("duplicate port option \"bitwidth\""));
                        else width = true;
                    // swqueue and hwqueue are allowed to occur at most once
                    else if(c instanceof SWQUEUE)
                        if(sw) errors.addError(new ParserError("duplicate port option \"swqueue\""));
                        else sw = true;
                    else if(c instanceof HWQUEUE)
                        if(hw) errors.addError(new ParserError("duplicate port option \"hwqueue\""));
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



