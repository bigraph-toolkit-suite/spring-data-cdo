package de.tudresden.inf.st.spring.data.cdo.server;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.emf.cdo.internal.server.bundle.OM;
import org.eclipse.emf.cdo.internal.server.mem.MEMStoreFactory;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IRepository.Props;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.net4j.CDONet4jServerUtil;
import org.eclipse.emf.cdo.server.ocl.OCLQueryHandler;
import org.eclipse.emf.cdo.spi.server.RepositoryConfigurator;
import org.eclipse.emf.cdo.spi.server.RepositoryFactory;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.TransportConfigurator;
import org.eclipse.net4j.acceptor.IAcceptor;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.http.HTTPUtil;
import org.eclipse.net4j.http.internal.server.HTTPAcceptor;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.container.IPluginContainer;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.OSGiApplication;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CDOStandaloneServer extends OSGiApplication {

    public static void main(String[] args) {
//        CDOStandaloneServer server = new CDOStandaloneServer("repo1");
        CDOStandaloneServer server = new CDOStandaloneServer(new File("spring-data-cdo/src/test/resources/config/cdo-server2.xml"));
        try {
            CDOStandaloneServer.start(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(final CDOStandaloneServer server) throws Exception {
        String[] equinoxArgs = {}; //"-console", "--add-modules=ALL-SYSTEM"}; //"-noExit"
        BundleContext context = EclipseStarter.startup(equinoxArgs, null);
        server.start(getApplicationContext(context));
    }

    public static IApplicationContext getApplicationContext(BundleContext context) {
        Collection<ServiceReference<IApplicationContext>> references;
        try {
            references = context.getServiceReferences(IApplicationContext.class, "(eclipse.application.type=main.thread)");
        } catch (InvalidSyntaxException e) {
            return null;
        }
        if (references == null || references.isEmpty())
            return null;
        // assumes the application context is available as a service
        ServiceReference<IApplicationContext> firstRef = references.iterator().next();
        IApplicationContext result = context.getService(firstRef);
        if (result != null) {
            context.ungetService(firstRef);
            return result;
        }
        return null;
    }

    public static final String ID = OM.BUNDLE_ID + ".app";
    private String repositoryName = null;
    private IRepository[] repositories;
    private IAcceptor[] acceptors;
    private IConnector connector = null;
    private CDONet4jSession session = null;
    private IManagedContainer container = null;
    private File serverConfigFile;

    public CDOStandaloneServer(String repositoryName) {
        super(ID);
        this.repositoryName = repositoryName;
    }

    public CDOStandaloneServer(File serverConfigFile) {
        super(ID);
        this.serverConfigFile = serverConfigFile;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        OM.LOG.info("CDO Server starting");
//        File file = OMPlatform.INSTANCE.getConfigFile("cdo-server.xml");
        String filename = Objects.nonNull(serverConfigFile) ? "file:" + serverConfigFile.toString() :
                "classpath:" + OMPlatform.INSTANCE.getConfigFolder() + "/cdo-server.xml";
        File configFile = null;
        try {
            configFile = ResourceUtils.getFile(filename);
        } catch (FileNotFoundException ignored) { /* ignored */}

        if (Objects.nonNull(configFile) && configFile.exists()) {
            OM.LOG.info("configure by file");
            OCLQueryHandler.Factory oclFactory = new OCLQueryHandler.Factory();
            RepositoryConfigurator repositoryConfigurator = new RepositoryConfigurator(
                    IPluginContainer.INSTANCE);
            repositoryConfigurator.getStoreFactories().put("mem", new MEMStoreFactory());
            repositoryConfigurator.getRepositoryFactories().put("default", new RepositoryFactory());
            repositoryConfigurator.getContainer().registerFactory(oclFactory);
            repositories = repositoryConfigurator.configure(configFile);
            if (repositories == null || repositories.length == 0) {
                OM.LOG.warn("No repositories configured");
            }

            TransportConfigurator net4jConfigurator = new TransportConfigurator(
                    IPluginContainer.INSTANCE);

            TCPUtil.prepareContainer(net4jConfigurator.getContainer());
            HTTPUtil.prepareContainer(net4jConfigurator.getContainer());
            Net4jUtil.prepareContainer(net4jConfigurator.getContainer());
            net4jConfigurator.getContainer().registerFactory(new HTTPAcceptor.DescriptionParserFactory());
            CDONet4jUtil.prepareContainer(net4jConfigurator.getContainer());
            CDONet4jServerUtil.prepareContainer(net4jConfigurator.getContainer());
//            net4jConfigurator.getContainer().getFactoryRegistry().put(oclFactory.getKey(), oclFactory);
//            net4jConfigurator.getContainer().registerFactory(oclFactory);
            acceptors = net4jConfigurator.configure(configFile);
            if (acceptors == null || acceptors.length == 0) {
                OM.LOG.warn("No acceptors configured");
            }
        } else {
            OM.LOG.warn("CDO server configuration not found " + "config/cdo-server.xml");
            initializeConnector();
        }
        OM.LOG.info("CDO Server started");
    }

    @Override
    protected void doStop() throws Exception {
        OM.LOG.info("CDO Server stopping");
        if (acceptors != null) {
            for (IAcceptor acceptor : acceptors) {
                acceptor.close();
            }
        }

        if (repositories != null) {
            for (IRepository repository : repositories) {
                LifecycleUtil.deactivate(repository);
            }
        }

        OM.LOG.info("CDO Server stopped");
        super.doStop();
    }

    public IConnector initializeConnector() {
        return initializeConnector(createMemStore());
    }

    protected IAcceptor createAcceptor(IManagedContainer container) {
        // TCPUtil.getAcceptor(container, "0.0.0.0:2036");
        // JVMUtil.getAcceptor(container, "default");
        return (IAcceptor) container.getElement(
                "org.eclipse.net4j.acceptors",
                "tcp",
                "0.0.0.0:2036"
        );
    }

    public IConnector initializeConnector(IStore store) {
        if (connector == null) {
            OCLQueryHandler.Factory oclFactory = new OCLQueryHandler.Factory();
            container = IPluginContainer.INSTANCE;
            Net4jUtil.prepareContainer(container);
            CDONet4jUtil.prepareContainer(container);
            CDONet4jServerUtil.prepareContainer(container);
            TCPUtil.prepareContainer(container);
            HTTPUtil.prepareContainer(container);
            container.registerFactory(oclFactory);

            // Initialize Acceptor
            if (acceptors == null) {
                IAcceptor acceptor = createAcceptor(container);
                acceptors = new IAcceptor[1];
                acceptors[0] = acceptor;
            }


            // Initialize Repository
            if (repositories == null) {
                IRepository repository = createRepository(store);
                CDOServerUtil.addRepository(container, repository);
                repositories = new IRepository[1];
                repositories[0] = repository;
            }

            // initialize Connector
            return getConnector(container);
        }
        return connector;
    }

    public IConnector getConnector(IManagedContainer container) {
        if (connector == null) {
            connector = TCPUtil.getConnector(container, "localhost");
            //connector = JVMUtil.getConnector(IPluginContainer.INSTANCE, "default");
        }
        return connector;
    }

    private IRepository createRepository(IStore store) {
        Map<String, String> props = new HashMap<>();
        props.put(Props.SUPPORTING_AUDITS, "true");
        props.put(Props.SUPPORTING_BRANCHES, "true");
//		props.put(Props.VERIFYING_REVISIONS, "false");

        IRepository repo = CDOServerUtil.createRepository(repositoryName, store, props);
        return repo;
    }

    private IStore createMemStore() {
        return org.eclipse.emf.cdo.server.mem.MEMStoreUtil.createMEMStore();
    }
}