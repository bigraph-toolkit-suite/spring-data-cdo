package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.CdoClientSessionOptions;
import org.bigraphs.spring.data.cdo.core.CdoExceptionTranslator;
import org.bigraphs.spring.data.cdo.repository.CdoDatabase;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.net4j.util.lifecycle.LifecycleException;
import org.springframework.beans.factory.DisposableBean;

import java.util.Optional;

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

    /**
     * To create a client, see also {@link CdoClients}.
     *
     * @param cdoClient      a valid cdo client
     * @param repositoryName the name of the repository
     */
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

    Optional<CdoClientSession> instance = Optional.empty();

    public CdoClientSession getSession(CdoClientSessionOptions options) throws LifecycleException {
        if (instance.isEmpty()) {
            instance = Optional.of(getCdoClient().startSession(options));
        }
        return instance.get();
    }


    @Override
    protected void closeClient() {
        getCdoClient().close();
//        session.close();
    }
}
