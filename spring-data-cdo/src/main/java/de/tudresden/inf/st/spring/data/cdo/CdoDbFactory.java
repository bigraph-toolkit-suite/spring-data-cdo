package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.core.CdoExceptionTranslator;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoDatabase;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * @author Dominik Grzelak
 */
public interface CdoDbFactory extends CdoSessionProvider {

    CdoDatabase getRepository();

    @Override
    CdoClientSession getSession(CdoClientSessionOptions options);

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
