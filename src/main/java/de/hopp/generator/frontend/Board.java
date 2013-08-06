package de.hopp.generator.frontend;

import de.hopp.generator.backends.board.BoardIF;
import de.hopp.generator.backends.board.virtex.virtex6.Virtex6;
import de.hopp.generator.backends.board.zed.Zed;

public enum Board {
    VIRTEX6(new Virtex6());
//    ZED(new Zed());

    // one instance of the backend
    private BoardIF instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param instance one instance of the backend
     */
    Board(BoardIF instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public BoardIF getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static Board fromName(String name) {
        for(Board backend : Board.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no board backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(Board backend : Board.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }
}