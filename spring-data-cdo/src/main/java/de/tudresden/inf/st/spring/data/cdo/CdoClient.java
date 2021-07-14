package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.CdoClientOptions;
import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.config.CdoCredentials;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoDatabase;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoDatabaseImpl;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.revision.CDORevisionCache;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.net4j.ReconnectingCDOSessionConfiguration;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IRepositorySynchronizer;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.db.CDODBUtil;
import org.eclipse.emf.cdo.server.db.mapping.IMappingStrategy;
import org.eclipse.emf.cdo.server.net4j.CDONet4jServerUtil;
import org.eclipse.emf.cdo.server.net4j.FailoverAgent;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSessionConfigurationFactory;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.acceptor.IAcceptor;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.db.IDBAdapter;
import org.eclipse.net4j.db.h2.H2Adapter;
import org.eclipse.net4j.http.HTTPUtil;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.container.IPluginContainer;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Client prepares all necessary components for the physical connection to the server, and provides some functionality
 * to start a new session, for example.
 * <p>
 * Refer also to the factory {@link CdoClients} for creating an instance.
 *
 * @author Dominik Grzelak
 * @see CdoClients
 */
public class CdoClient {

    @NonNull
    private final CdoServerAddress addr;
    @NonNull
    private final CdoClientOptions cdoClientOptions;
    @Nullable
    private final CdoCredentials cdoCredentials;

    protected final transient IManagedContainer container;
    public final transient List<IConnector> connectors = new ArrayList<>();

    static {
        OMPlatform.INSTANCE.setDebugging(true);
        OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
        OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
        //The following lines are not needed if the extension
        //registry (OSGi/Equinox) is running
        Net4jUtil.prepareContainer(IPluginContainer.INSTANCE); // Prepare the Net4j kernel
        TCPUtil.prepareContainer(IPluginContainer.INSTANCE); // Prepare the TCP support
        HTTPUtil.prepareContainer(IPluginContainer.INSTANCE); // Prepare the HTTP support
        CDONet4jUtil.prepareContainer(IPluginContainer.INSTANCE); // Prepare the CDO server
        CDONet4jServerUtil.prepareContainer(IPluginContainer.INSTANCE); // Prepare the CDO server
    }

    /**
     * Creates an instance based on a CDO server on localhost and default port.
     * Relies on the empty constructor of {@link CdoServerAddress}.
     */
    public CdoClient() {
        this(new CdoServerAddress());
    }

    public CdoClient(String host, int port) {
        this(new CdoServerAddress(
                        CdoServerAddress.builder()
                                .setDescription(host)
                                .setPort(port)
                )
        );
    }

    public CdoClient(@NonNull CdoServerAddress addr) {
        this(addr, null, new CdoClientOptions());
    }

    public CdoClient(@NonNull CdoServerAddress addr, @Nullable CdoCredentials credentials) {
        this(addr, credentials, new CdoClientOptions());
    }

    public CdoClient(@NonNull CdoServerAddress addr, @Nullable CdoCredentials cdoCredentials,
                     @NonNull CdoClientOptions options) {
        this.addr = addr;
        this.cdoCredentials = cdoCredentials;
        this.cdoClientOptions = options;
        this.container = createContainer();
    }

    public CdoClient(CdoClientOptions options) {
        this(options.getAddr(), options.getCdoCredentials(), options);
    }

    public CdoServerAddress getAddr() {
        return addr;
    }

    public CdoClientOptions getCdoClientOptions() {
        return cdoClientOptions;
    }

    @Nullable
    public CdoCredentials getCdoCredentials() {
        return cdoCredentials;
    }

    public CdoClientSession startSession(CdoClientSessionOptions options) {
        Assert.notNull(options, "CdoClientSessionOptions must not be null");
        return startSession(createSessionConfiguration(addr), options, options.getRepository());
    }

    public CdoClientSession startSession(CDONet4jSessionConfiguration config, CdoClientSessionOptions options, String repoName) {
        if (config.getConnector() == null) {
            IConnector connector = createConnector(addr);
            config.setConnector(connector);
            connectors.add(connector);
        }

        config.setRepositoryName(repoName);
        config.setActivateOnOpen(true);

        assert config.getRepositoryName() != null;
        assert config.getConnector() != null;
        CdoClientSession cdoClientSession = new CdoClientSession(config.openNet4jSession());
        cdoClientSession.setOptions(options);
        return cdoClientSession;
    }


    protected CDOSessionConfigurationFactory createSessionConfigurationFactory(final CdoServerAddress addr) {
        return new FailoverAgent() {
            @Override
            protected CDOSessionConfiguration createSessionConfiguration(String a, String b) {
                return CdoClient.this.createSessionConfiguration(addr);
            }
        };
    }

    protected CDONet4jSessionConfiguration createSessionConfiguration(CdoServerAddress connectorDescription) {
        IConnector connector = createConnector(connectorDescription);
        connectors.add(connector);
        // CDONet4jSessionConfiguration config = CDONet4jUtil.createNet4jSessionConfiguration();
        ReconnectingCDOSessionConfiguration config = CDONet4jUtil.createReconnectingSessionConfiguration(
                connectorDescription.getFullConnectorDescription(),
                "", // (!) repository name has to be set later
                this.container);
        config.setConnector(connector);
        config.setMaxReconnectAttempts(5);
        config.setPassiveUpdateEnabled(true);
        config.setPassiveUpdateMode(CDOCommonSession.Options.PassiveUpdateMode.ADDITIONS);

        config.setRevisionManager(CDORevisionUtil.createRevisionManager(CDORevisionCache.NOOP)); //CDORevisionCache.NOOP
        return config;
    }

    /**
     * " Net4j IConnector, which represents the physical connection to the server,"
     *
     * @param addr
     * @return
     */
    protected IConnector createConnector(CdoServerAddress addr) {
        IConnector connector = Net4jUtil.getConnector(
                this.container,
                addr.getTransportType(),
                addr.getFullConnectorDescription()
        );
        //Net4jUtil.getConnector(IPluginContainer.INSTANCE, "tcp", "repos.foo.org:2036");
        //OR: createAcceptor()
        return connector;
    }

    //TODO probably also used elsewhere
    protected IRepositorySynchronizer createRepositorySynchronizer(CdoServerAddress addr) {
        CDOSessionConfigurationFactory factory = createSessionConfigurationFactory(addr);
        IRepositorySynchronizer synchronizer = CDOServerUtil.createRepositorySynchronizer(factory);
        synchronizer.setRetryInterval(2);//TODO put in Options
        synchronizer.setMaxRecommits(10);//TODO put in Options
        synchronizer.setRecommitInterval(2);//TODO put in Options
        return synchronizer;
    }

    public CDOTransaction openTransaction(CdoClientSession session, ResourceSet resourceSet) {
        CDOTransaction transaction = session.getDelegate().openTransaction(resourceSet);
        return transaction;
    }

    public CDOTransaction openNewTransaction(CdoClientSession session) {
        CDOTransaction transaction = session.getDelegate().openTransaction();
        return transaction;
    }

    public void commitTransaction(CDOTransaction transaction) throws CommitException {
        transaction.commit();
    }

    /**
     * Closes all connections from the CDO repository.
     * <p>
     * It removes all created containers, connectors, etc. that were created by the client.
     */
    //TODO also remove/close sessions
    public void close() {
        for (int i = connectors.size() - 1; i >= 0; i--) {
            connectors.get(i).close();
            connectors.remove(i);
        }
        container.clearElements();
        //        this.serverSessionPool.close();
        //        this.cluster.close();C
    }

    /**
     * Checks if at least one IConnector created by this client is active.
     *
     * @return {@code true}, if at least one IConnector is active, otherwise {@code false}
     */
    public boolean isConnected() {
        for (int i = 0; i < connectors.size(); i++) {
            LifecycleUtil.waitForActive(connectors.get(i), 1000);
            if (LifecycleUtil.isActive(connectors.get(i)))
                return true;
        }
        return false;
    }

    public void closeSession(CdoClientSession session) {
        session.getDelegate().close();
    }

    IRepository createRepository(String repoName) {
        IStore store = createStore(repoName);
        Map<String, String> properties = createRepositoryProperties(repoName);
        //see: http://git.eclipse.org/c/cdo/cdo.git/tree/plugins/org.eclipse.emf.cdo.examples/src/org/eclipse/emf/cdo/examples/server/FailoverExample.java
        IRepositorySynchronizer synchronizer = createRepositorySynchronizer(addr);//TODO maybe the repomust be added to the sessionconfiguration
        IRepository repository = CDOServerUtil.createRepository(repoName, store, properties);
        CDOServerUtil.addRepository(IPluginContainer.INSTANCE, repository);
        return repository;
    }

    Iterable listDatabases(CdoClientSession session) {
        return null;
    }

    //TODO implement createStore()
    private static IStore createStore(String name) {
//        JdbcDataSource dataSource = new JdbcDataSource();
//        dataSource.setURL("jdbc:h2:database/" + name);

        IMappingStrategy mappingStrategy = CDODBUtil.createHorizontalMappingStrategy(true, true);
        IDBAdapter dbAdapter = new H2Adapter();
//        IDBConnectionProvider dbConnectionProvider = dbAdapter.createConnectionProvider(dataSource);
        return null; //CDODBUtil.createStore(mappingStrategy, dbAdapter, dbConnectionProvider);
    }

    private IStore createMemStore() {
        return org.eclipse.emf.cdo.server.mem.MEMStoreUtil.createMEMStore();
    }

    private IRepository createRepository(IStore store, String repositoryName) {
        Map<String, String> props = new HashMap<>();
        props.put(IRepository.Props.SUPPORTING_AUDITS, "true");
        props.put(IRepository.Props.SUPPORTING_BRANCHES, "true");
//		props.put(Props.VERIFYING_REVISIONS, "false");

        IRepository repo = CDOServerUtil.createRepository(repositoryName, store, props);
        return repo;
    }

    public static Map<String, String> createRepositoryProperties(String name) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(IRepository.Props.OVERRIDE_UUID, name);
        props.put(IRepository.Props.SUPPORTING_AUDITS, "true");
        props.put(IRepository.Props.SUPPORTING_BRANCHES, "true");
        return props;
    }

    public static IManagedContainer createContainer() {
        //IPluginContainer.INSTANCE; // also a managed container - ^is already prepared statically above^
        IManagedContainer container = ContainerUtil.createContainer();
        ContainerUtil.prepareContainer(container);
        Net4jUtil.prepareContainer(container); // Register Net4j factories
        TCPUtil.prepareContainer(container); // Register TCP factories
        HTTPUtil.prepareContainer(container); // Register HTTP factories
        CDONet4jUtil.prepareContainer(container); // Register CDO client factories
        CDONet4jServerUtil.prepareContainer(container); // Register CDO server factories
        container.activate();
        return container;
    }

    protected IAcceptor createAcceptor(CdoServerAddress addr) {
        return (IAcceptor) container.getElement(
                addr.getProductGroup(), //e.g., "org.eclipse.net4j.acceptors"
                addr.getTransportType(), //e.g., "tcp"
                addr.getFullConnectorDescription() //addr.getDescription() + ":" + addr.getPort() //ip:port
        );
    }


    public CdoDatabase getRepository(String reponame) {
        return new CdoDatabaseImpl(reponame);
    }
}
