package de.hopp.generator.backends;

import static de.hopp.generator.model.Model.MFileInFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.unparser.CUnparser;
import de.hopp.generator.backends.unparser.CppUnparser;
import de.hopp.generator.backends.unparser.HUnparser;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.frontend.*;
import de.hopp.generator.model.MFile;
import de.hopp.generator.model.MFileInFile;

public class BackendUtils {

    public static final int defaultQueueSizeSW = 1024;
    public static final int defaultQueueSizeHW = 64;
    public static final int defaultWidth = 32;
    
    public enum UnparserType { HEADER, C, CPP }
    
    public static MFileInFile.Visitor<InvalidConstruct> createUnparser(UnparserType type, StringBuffer buf, String name) {
        switch(type) {
        case HEADER : return new   HUnparser(buf, name);
        case C      : return new   CUnparser(buf, name);
        case CPP    : return new CppUnparser(buf, name);
        }
        throw new IllegalStateException();
    }
    
    public static void printMFile(MFile mfile, UnparserType type, ErrorCollection errors) {
        
        File target = new File (mfile.directory());
        
        // setup buffer
        StringBuffer buf = new StringBuffer(16384);
        
        // get unparser instance
        MFileInFile.Visitor<InvalidConstruct> visitor = createUnparser(type, buf, mfile.name());
        
        // unparse to buffer
        try {
            visitor.visit(MFileInFile(mfile));
        } catch (InvalidConstruct e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
               
        // append model name and file extension according to used unparser
        switch(type) {
        case HEADER : target = new File(target, mfile.name() +   ".h"); break;
        case C      : target = new File(target, mfile.name() +   ".c"); break;
        case CPP    : target = new File(target, mfile.name() + ".cpp"); break;
        default     : throw new IllegalStateException("invalid unparser");
        }
        
        // print buffer contents to file
        try {
            printBuffer(buf, target);
        } catch(IOException e) {
            errors.addError(new GenerationFailed(e.getMessage()));
        }
    }
    
    /**
     * prints the content of a StringBuffer into a file.
     * Creates the file and directories if required.
     * @param buf the buffer to be printed
     * @param target file into which the buffer content should be printed
     * @throws IOException error during file creation or the print
     */
    public static void printBuffer(StringBuffer buf, File target) throws IOException {
        // create the files and parent directories if they don't exist
        if(target.getParentFile() != null && ! target.getParentFile().exists())
            target.getParentFile().mkdirs();
        if(! target.exists())
            target.createNewFile();
            
        // write output into file
        FileWriter fileWriter = new FileWriter(target);
        new BufferedWriter(fileWriter).append(buf).flush();
        fileWriter.close();
    }
    
    public static void doxygen(File dir, IOHandler IO, ErrorCollection errors) {
        BufferedReader input = null;
        
        try {
            String line;
            // TODO probably need .exe extension for windows?
            // TODO would be cleaner to do this with scripts, i guess (but this would require parameter passing...)
            // run doxygen in the provided directory
            Process p = new ProcessBuilder("doxygen", "doxygen.cfg").directory(dir).start();
            input     = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // wait for the process to terminate and store the result
            int rslt = p.waitFor();
            
            // if something went wrong, print a warning
            if(rslt != 0) {
                errors.addWarning(new Warning("failed to generate api specification at " +
                        dir.getPath() + ".run in verbose mode for more information"));
//                if(! config.VERBOSE()) return; // this line is just for performance, but bloats method signature :(
            }
            
            // if verbose, echo the output of doxygen
            while ((line = input.readLine()) != null)
               IO.verbose("      " + line);
            
        } catch (IOException e) {
            errors.addWarning(new Warning("failed to generate api specification at " + 
                    dir.getPath() + "due to: " + e.getMessage()));
        } catch (InterruptedException e) {
            errors.addWarning(new Warning("failed to generate api specification at " + 
                    dir.getPath() + "due to: " + e.getMessage()));
        } finally {
            try { 
                if(input != null) input.close();
            } catch(IOException e) { /* well... memory leak... */ }
        }
    }
    
    public static CorePos getCore(InstancePos inst) {
        String coreName = inst.core().term();
        String coreVer  = inst.version().term();
        
        // return the core, if it exists
        for(CorePos c : inst.root().cores())
            if(c.name().term().equals(coreName) && c.version().term().equals(coreVer))
                return c;
        
        // otherwise, throw an exception (should never happen due to sanity checks)
        throw new IllegalStateException();
    }
    
    public static PortPos getPort(BindingPos bind) {
        String portName = bind.port().term();

        // throw an exception, if the parent is not a core instance
        if(!(bind.parent().parent() instanceof InstancePos)) throw new IllegalStateException();
        InstancePos inst = ((InstancePos)bind.parent().parent());

        // return the port, if it exists
        for(PortPos p : getCore(inst).ports())
            if(p.name().term().equals(portName)) return p;

        // otherwise, throw an exception (should never happen due to sanity checks)
        throw new IllegalStateException();
    }
    
    public static boolean isPolling(CPUAxisPos axis) {
        // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return true;
        
        // otherwise, the port isn't polling
        return false;
    }
    
    public static int getPollingCount(CPUAxisPos axis) {
        // check, if there is a poll option, return if found
        for(Option opt : axis.opts().term())
            if(opt instanceof POLL) return ((POLL)opt).count();
        
        // otherwise, the port isn't polling. Return 0
        return 0;
    }
    
    public static int getSWQueueSize(CPUAxisPos axis) {
        // if there is a local definition, return that
        for(Option opt : axis.opts().term())
            if(opt instanceof SWQUEUE) return ((SWQUEUE)opt).qsize();
        
        // if there is no local definition but a global one, return that
        for(Option opt : axis.root().opts().term())
            if(opt instanceof SWQUEUE) return ((SWQUEUE)opt).qsize();
        
        // otherwise, return the default queue size
        return defaultQueueSizeSW;
    }
    
    public static int getHWQueueSize(CPUAxisPos axis) {
        // if there is a local definition, return that
        for(Option opt : axis.opts().term())
            if(opt instanceof HWQUEUE) return ((HWQUEUE)opt).qsize();
        
        // if there is no local definition but a global one, return that
        for(Option opt : axis.root().opts().term())
            if(opt instanceof HWQUEUE) return ((HWQUEUE)opt).qsize();
        
        // otherwise, return the default queu size
        return defaultQueueSizeHW;
    }
    
    public static int getWidth(CPUAxisPos axis) {
        // the bitwidth option has to be set at the port definition
        for(Option opt : getPort(axis).opts().term())
            if(opt instanceof BITWIDTH) return ((BITWIDTH)opt).bit();
            
        // if it is not set, return the default width
        return defaultWidth;
    }
    
    public static int maxQueueSize(BDLFile file) {
        int size = 0;
        
        for(Option opt : file.opts())
            if(opt instanceof SWQUEUE) size = Math.max(size, ((SWQUEUE)opt).qsize());
        
        for(Instance inst : file.insts())
            for(Binding bind : inst.bind())
                for(Option opt : bind.opts())
                    if(opt instanceof SWQUEUE) size = Math.max(size, ((SWQUEUE)opt).qsize());
        
        return size;
    }
    
}
