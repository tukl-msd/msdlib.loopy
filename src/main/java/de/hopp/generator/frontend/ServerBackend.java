package de.hopp.generator.frontend;

import de.hopp.generator.backends.server.virtex6.Virtex6;

public enum ServerBackend {
    VIRTEX6(new Virtex6());
//    VIRTEX6ML605_XPS_14_1(new Virtex6ML605(new XPS_14_1()));

    // one instance of the backend
    private de.hopp.generator.backends.server.ServerBackend instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param object one instance of the backend
     */
    ServerBackend(de.hopp.generator.backends.server.ServerBackend instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public de.hopp.generator.backends.server.ServerBackend getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static ServerBackend fromName(String name) {
        for(ServerBackend backend : ServerBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no board backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(ServerBackend backend : ServerBackend.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }
}
