package de.hopp.generator.backends.server.zed;

import java.util.LinkedList;
import java.util.List;

import de.hopp.generator.Configuration;
import de.hopp.generator.ErrorCollection;
import de.hopp.generator.IOHandler;
import de.hopp.generator.backends.server.ServerBackend;
import de.hopp.generator.exceptions.ExecutionFailed;
import de.hopp.generator.frontend.BDLFilePos;

public class Zed implements ServerBackend {

    private ProjectBackendIF project = null;

    public Zed() {
    }

    public String getName() {
        return "zed";
    }

    public void printUsage(IOHandler IO) {
        IO.println(" -p --project <name>   selects the project backend for generation of the");
        IO.println("                       board-side driver. Depending on the selected backend,");
        IO.println("                       different tools and versions are used to construct");
        IO.println("                       the board-side driver files");

        for(ProjectBackend project : ProjectBackend.values()) {
            IO.println();
            IO.println("  " + getName() + " Project backend: " + project.getInstance().getName());
            IO.println();
            project.getInstance().printUsage(IO);
        }
    }

    public Configuration parseParameters(Configuration config, String[] args) {
        // parse backend specific parameters...
        // basically everything, that was not handled by the system...

        IOHandler IO = config.IOHANDLER();

        // store remaining arguments
        List<String> remaining = new LinkedList<String>();

        // go through all parameters
        for(int i = 0; i < args.length; i++) {
            // BACKEND flags
            if(args[i].equals("-p") || args[i].equals("--project")) {
                if(i + 1 >= args.length) {
                    IO.error("no argument left for "+args[i]);
                    throw new ExecutionFailed();
                }
                try {
                    project = ProjectBackend.fromName(args[++i]).getInstance();
                } catch(IllegalArgumentException e) {
                    IO.error(e.getMessage());
                    throw new ExecutionFailed();
                }
            } else remaining.add(args[i]);
        }

        config.setUnusued(remaining.toArray(new String[0]));
        return config;
    }

    public void generate(BDLFilePos board, Configuration config, ErrorCollection errors) {
        // start the project generation backend
        if(project != null) {
            // run the project generator
            project.generate(board, config, errors);
        }
    }
}
