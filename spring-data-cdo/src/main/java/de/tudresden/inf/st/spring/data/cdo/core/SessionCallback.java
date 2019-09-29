package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.spring.data.cdo.CdoClientSession;
import de.tudresden.inf.st.spring.data.cdo.CdoOperations;

/**
 * @author Dominik Grzelak
 */
public interface SessionCallback<T> {

    /**
     * Executes all the given operations inside the same session.
     * <p>
     * The {@literal operations} argument shall contain a session. It is
     * usually an instance which can be created by calling {@link CdoOperations#withSession(CdoClientSession)}
     *
     * @param operations a {@link CdoOperations} instance containing a valid {@link CdoClientSession}.
     * @return return value
     */
    T execute(CdoOperations operations);
}
