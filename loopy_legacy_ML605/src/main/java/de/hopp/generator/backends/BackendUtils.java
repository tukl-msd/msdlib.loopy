package de.hopp.generator.backends;

import static de.hopp.generator.model.cpp.CPP.MFileInFile;
import static org.apache.commons.io.FileUtils.write;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.exceptions.InvalidConstruct;
import de.hopp.generator.exceptions.Warning;
import de.hopp.generator.model.cpp.MFile;
import de.hopp.generator.model.cpp.MFileInFile;
import de.hopp.generator.model.unparser.CUnparser;
import de.hopp.generator.model.unparser.CPPUnparser;
import de.hopp.generator.model.unparser.HUnparser;
import de.hopp.generator.model.unparser.MHSUnparser;
import de.hopp.generator.model.mhs.MHSFile;

public class BackendUtils {

    public enum UnparserType { HEADER, C, CPP }

    public static MFileInFile.Visitor<InvalidConstruct> createUnparser(UnparserType type, StringBuffer buf, String name) {
        switch(type) {
        case HEADER : return new   HUnparser(buf, name);
        case C      : return new   CUnparser(buf, name);
        case CPP    : return new CPPUnparser(buf, name);
        }
        throw new IllegalStateException();
    }
    public static void printMFile(MFile mfile, UnparserType type) throws InvalidConstruct, IOException {

        // setup target file
        File target;
        switch(type) {
        case HEADER : target = new File(mfile.directory(), mfile.name() +   ".h"); break;
        case C      : target = new File(mfile.directory(), mfile.name() +   ".c"); break;
        case CPP    : target = new File(mfile.directory(), mfile.name() + ".cpp"); break;
        default     : throw new IllegalStateException("invalid unparser");
        }

        // setup buffer
        StringBuffer buf = new StringBuffer(16384);

        // get unparser instance
        MFileInFile.Visitor<InvalidConstruct> visitor = createUnparser(type, buf, mfile.name());

        // unparse to buffer
        visitor.visit(MFileInFile(mfile));

        // write buffer contents to file
        write(target, buf.toString());
    }

    public static void printMFile(MHSFile mfile, File target) throws IOException {

        // setup buffer
        StringBuffer buf = new StringBuffer(16384);

        // get unparser instance
        MHSUnparser visitor = new MHSUnparser(buf);

        // unparse to buffer
        visitor.visit(mfile);

        // write buffer contents to file
        write(target, buf);
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
                if(! IO.config.VERBOSE()) return;
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
}
