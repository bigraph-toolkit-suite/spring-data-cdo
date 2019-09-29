package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.core.CdoExceptionTranslator;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoDatabase;
import org.eclipse.emf.cdo.server.IRepository;
import org.springframework.beans.factory.DisposableBean;

/**
 * Factory to create {@link IRepository} instances from a {@link CdoClient} instance.
 *
 * @author Dominik Grzelak
 */
public class SimpleCdoDbFactory extends CdoDbFactorySupport<CdoClient> implements DisposableBean {

    public SimpleCdoDbFactory(String connectionString) {
        this(new CdoServerConnectionString((connectionString)));
    }

    public SimpleCdoDbFactory(CdoServerConnectionString connectionString) {
        this(CdoClients.create(connectionString), connectionString.getRepoName());
    }

    public SimpleCdoDbFactory(CdoClient cdoClient, String repositoryName) {
        super(cdoClient, repositoryName, new CdoExceptionTranslator());
    }

    @Override
    public CdoDatabase doGetCdoRepository(String reponame) {
        return getCdoClient().getRepository(reponame);
    }

    @Override
    public CdoDatabase getRepository() {
        return getCdoClient().getRepository(getRepositoryName());
    }

    public CdoClientSession getSession(CdoClientSessionOptions options) {
        return getCdoClient().startSession(options);
    }

    @Override
    protected void closeClient() {
        getCdoClient().close();
//        session.close();
//        connector.disconnect();
    }
}
