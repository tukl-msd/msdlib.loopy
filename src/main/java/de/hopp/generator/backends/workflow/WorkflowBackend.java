package de.hopp.generator.backends.workflow;

import de.hopp.generator.backends.Backend;
import de.hopp.generator.backends.board.BoardBackend;

/**
 *
 * @author Thomas Fischer
 * @since 1.8.2013
 */
public interface WorkflowBackend extends Backend {
    public Class<? extends BoardBackend> getBoardInterface();
}
