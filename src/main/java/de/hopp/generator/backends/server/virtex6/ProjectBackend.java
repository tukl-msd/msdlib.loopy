package de.hopp.generator.backends.server.virtex6;

import de.hopp.generator.backends.server.virtex6.ise.ISE_14_1;
import de.hopp.generator.backends.server.virtex6.ise.ISE_14_4;

public enum ProjectBackend {
    ISE_14_1(new ISE_14_1()),
    ISE_14_4(new ISE_14_4());

    // one instance of the backend
    private ProjectBackendIF instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param object one instance of the backend
     */
    ProjectBackend(ProjectBackendIF instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public ProjectBackendIF getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static ProjectBackend fromName(String name) {
        for(ProjectBackend backend : ProjectBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no virtex6 project backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(ProjectBackend backend : ProjectBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }
}
