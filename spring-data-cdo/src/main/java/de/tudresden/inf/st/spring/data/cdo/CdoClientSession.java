package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;

/**
 * wrapper object for {@link CDONet4jSession} to encapsulate the internal object
 * <p>
 * "A CDOSession is your connection to a single repository on a CDO server.
 * Before you can do anything with CDO you need to open a session."
 *
 * @author Dominik Grzelak
 */
public class CdoClientSession {

    private final CDONet4jSession delegate;
    private CdoClientSessionOptions options;

    public CdoClientSession(CDONet4jSession delegate) {
        this.delegate = delegate;
    }

    public CDONet4jSession getDelegate() {
        return delegate;
    }

    public CdoClientSession setOptions(CdoClientSessionOptions options) {
        this.options = options;
        return this;
    }

    public CdoClientSessionOptions getOptions() {
        return options;
    }
}
