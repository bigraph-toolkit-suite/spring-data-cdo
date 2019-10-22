package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoDatabase;
import org.eclipse.net4j.util.lifecycle.LifecycleException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * @author Dominik Grzelak
 */
public abstract class CdoDbFactorySupport<C> implements CdoDbFactory {

    private String repositoryName;
    private C cdoClient;
    private final PersistenceExceptionTranslator exceptionTranslator;

    public CdoDbFactorySupport(C cdoClient, String repositoryName, PersistenceExceptionTranslator exceptionTranslator) {
        this.repositoryName = repositoryName;
        this.cdoClient = cdoClient;
        this.exceptionTranslator = exceptionTranslator;
    }

    public abstract CdoDatabase doGetCdoRepository(String reponame);

    @Override
    public abstract CdoDatabase getRepository();

    public void destroy() throws Exception {
        closeClient();
    }

    @Override
    public CdoDbFactory withSession(CdoClientSession session) {
        return null; //TODO
    }

    @Override
    public abstract CdoClientSession getSession(CdoClientSessionOptions options) throws LifecycleException;

    /**
     * @return the CDO client object.
     */
    protected C getCdoClient() {
        return cdoClient;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    protected abstract void closeClient();

    /*
     * (non-Javadoc)
     * @see de.tudresden.inf.st.bifogtecture.data.cdo.CdoDbFactory#getExceptionTranslator()
     */
    public PersistenceExceptionTranslator getExceptionTranslator() {
        return this.exceptionTranslator;
    }
}
