package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.CdoClientSessionOptions;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.session.CDOSession;

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

    public CDOSession getCdoSession() {
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
