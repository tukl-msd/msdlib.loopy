package de.hopp.generator.frontend;

import de.hopp.generator.backends.workflow.WorkflowBackend;
import de.hopp.generator.backends.workflow.ise.ISE_14_1;
import de.hopp.generator.backends.workflow.ise.ISE_14_4;
import de.hopp.generator.backends.workflow.ise.ISE_14_6;
import de.hopp.generator.backends.workflow.ise.ISE_14_7;


public enum Workflow {
    ISE_14_1(new ISE_14_1()),
    ISE_14_4(new ISE_14_4()),
    ISE_14_6(new ISE_14_6());
	ISE_14_7(new ISE_14_7());

    // one instance of the backend
    private WorkflowBackend instance;

    /**
     * Each backend token has one instance of the backend, backends should be stateless
     * @param instance one instance of the backend
     */
    Workflow(WorkflowBackend instance) {
        this.instance = instance;
    }

    /**
     * Returns an instance of this backend
     * @return an instance of this backend
     */
    public WorkflowBackend getInstance() {
        return instance;
    }

    /**
     * Returns the backend token to given name
     * @param name the name of the backend
     * @return the backend token, if it exists, throws an exception otherwise
     */
    public static Workflow fromName(String name) {
        for(Workflow backend : Workflow.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return backend;

        throw new IllegalArgumentException("no workflow backend exists with given name");
    }

    /**
     * Returns whether a backend with given name exists or not
     * @param name the name of the backend
     * @return whether a backend with given name exists or not
     */
    public static boolean exists(String name) {
        for(Workflow backend : Workflow.values())
            if(backend.instance.getName().toUpperCase().equals(name.toUpperCase())) return true;

        return false;
    }

    @Override
    public String toString() {
        return instance.getName();
    }
}
