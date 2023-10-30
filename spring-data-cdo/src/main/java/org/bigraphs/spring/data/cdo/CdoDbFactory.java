package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.CdoClientSessionOptions;
import org.bigraphs.spring.data.cdo.core.CdoExceptionTranslator;
import org.bigraphs.spring.data.cdo.repository.CdoDatabase;
import org.eclipse.net4j.util.lifecycle.LifecycleException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * @author Dominik Grzelak
 */
public interface CdoDbFactory extends CdoSessionProvider {

    CdoDatabase getRepository();

    @Override
    CdoClientSession getSession(CdoClientSessionOptions options) throws LifecycleException;

    default CdoDbFactory withSession(CdoClientSessionOptions options) {
        return withSession(getSession(options));
    }

    CdoDbFactory withSession(CdoClientSession session);

    /**
     * Exposes a shared {@link CdoExceptionTranslator}.
     *
     * @return will never be {@literal null}.
     */
    PersistenceExceptionTranslator getExceptionTranslator();
}
