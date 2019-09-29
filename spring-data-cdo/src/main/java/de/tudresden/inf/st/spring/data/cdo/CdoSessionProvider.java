package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoSessionProvider {
    /**
     * Obtain a {@link CdoClientSession} with the given options.
     *
     * @param options
     * @return
     */
    CdoClientSession getSession(CdoClientSessionOptions options);
}
