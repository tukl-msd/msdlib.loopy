package de.hopp.generator.frontend;

import de.hopp.generator.backends.host.cpp.CPP;


public enum HostBackend {
    CPP(new CPP());

    // one instance of the backend
    private de.hopp.generator.backends.host.HostBackend instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param object one instance of the backend
     */
    HostBackend(de.hopp.generator.backends.host.HostBackend instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public de.hopp.generator.backends.host.HostBackend getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static HostBackend fromName(String name) {
        for(HostBackend backend : HostBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no host backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(HostBackend backend : HostBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }
}
