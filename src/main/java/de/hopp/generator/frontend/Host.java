package de.hopp.generator.frontend;

import de.hopp.generator.backends.host.cpp.CPP;
import de.hopp.generator.backends.host.HostBackend;

public enum Host {
    CPP(new CPP());

    // one instance of the backend
    private HostBackend instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param instance one instance of the backend
     */
    Host(HostBackend instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public HostBackend getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static Host fromName(String name) {
        for(Host backend : Host.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no host backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(Host backend : Host.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }
}
