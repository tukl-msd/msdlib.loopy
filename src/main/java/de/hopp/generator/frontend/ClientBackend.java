package de.hopp.generator.frontend;

import de.hopp.generator.backends.client.cpp.CPP;


public enum ClientBackend {
    CPP(new CPP());
    
    // one instance of the backend
    private de.hopp.generator.backends.client.ClientBackend instance;
    
    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param object one instance of the backend
     */
    ClientBackend(de.hopp.generator.backends.client.ClientBackend instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public de.hopp.generator.backends.client.ClientBackend getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static ClientBackend fromName(String name) {
        for(ClientBackend backend : ClientBackend.values())
            if(backend.instance.getName().equals(name)) return backend;

        throw new IllegalArgumentException("no host backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(ClientBackend backend : ClientBackend.values())
            if(backend.instance.getName().equals(name)) return true;

        return false;
    }
}
