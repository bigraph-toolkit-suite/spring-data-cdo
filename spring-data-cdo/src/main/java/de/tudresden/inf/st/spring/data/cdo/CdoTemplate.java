package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.core.*;
import de.tudresden.inf.st.spring.data.cdo.core.event.*;
import de.tudresden.inf.st.spring.data.cdo.core.listener.CdoNewObjectsActionDelegate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.CdoSessionActionDelegate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.DefaultCdoSessionListener;
import de.tudresden.inf.st.spring.data.cdo.core.listener.ResourceContentAdapter;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.CdoListenerFilter;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.FilterCriteria;
import de.tudresden.inf.st.spring.data.cdo.core.mapping.CdoMappingContext;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDORevisionDeltaImpl;
import org.eclipse.emf.cdo.internal.common.revision.delta.CDOSetFeatureDeltaImpl;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.*;
import org.eclipse.emf.cdo.view.CDOAdapterPolicy;
import org.eclipse.emf.cdo.view.CDOInvalidationPolicy;
import org.eclipse.emf.cdo.view.CDOQuery;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.internal.cdo.object.CDOLegacyAdapter;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.net4j.util.concurrent.IRWLockManager;
import org.eclipse.net4j.util.event.IListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

//import org.springframework.data.mapping.callback.EntityCallbacks;

/**
 * Primary implementation of {@link CdoOperations}.
 * <p>
 * See: https://github.com/spring-projects/spring-data-commons/wiki/developer-guide#create-a-simplerepository-implementing-crudrepository-or-pagingandsortingrepository
 * "For manipulationg domain model instances, i.e. setting and getting property values, use an accessor that you can get from a PersistentEntity:"
 * <p>
 * Locks:
 * Read lock (Shared lock): Any other transaction can read but not write.
 * Write lock (Exclusive lock): Other transactions can neither read nor write
 *
 * @author Dominik Grzelak
 */
public class CdoTemplate implements CdoOperations, ApplicationContextAware, ApplicationEventPublisherAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdoTemplate.class);

    private static final Collection<String> ITERABLE_CLASSES;

    static {

        Set<String> iterableClasses = new HashSet<>();
        iterableClasses.add(List.class.getName());
        iterableClasses.add(Collection.class.getName());
        iterableClasses.add(Iterator.class.getName());

        ITERABLE_CLASSES = Collections.unmodifiableCollection(iterableClasses);
    }

    private final CdoClientSessionOptions cdoSessionOptions;
    private final PersistenceExceptionTranslator exceptionTranslator;

    private boolean publishEvents = true;
    private CdoDbFactory cdoDbFactory;
    private final Objenesis objenesis;

    private MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> mappingContext;
    private CdoConverter cdoConverter;
    //    @Nullable
//    private EntityCallbacks entityCallbacks;
    private final SpelAwareProxyProjectionFactory projectionFactory;
    @Nullable
    private ApplicationEventPublisher eventPublisher;
    @Nullable
    private ResourceLoader resourceLoader;
    @Nullable
    private ClassLoader classLoader;

    public CdoTemplate(CdoClient cdoClient, String repositoryName) {
        this(new SimpleCdoDbFactory(cdoClient, repositoryName), null, null);
    }

    public CdoTemplate(CdoDbFactory cdoDbFactory) {
        this(cdoDbFactory, null, null);
    }

    public CdoTemplate(CdoDbFactory cdoDbFactory, @Nullable CdoClientSessionOptions sessionOptions) {
        this(cdoDbFactory, null, sessionOptions);
    }

    public CdoTemplate(CdoDbFactory cdoDbFactory,
                       @Nullable CdoConverter cdoConverter,
                       @Nullable CdoClientSessionOptions sessionOptions) {
        this.cdoDbFactory = cdoDbFactory;

        if (Objects.isNull(sessionOptions)) {
            this.cdoSessionOptions = CdoClientSessionOptions.builder().setRepository(cdoDbFactory.getRepository().getName()).build();
        } else {
            this.cdoSessionOptions = sessionOptions;
        }

        this.objenesis = new ObjenesisStd(true);

        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        this.cdoConverter = Objects.isNull(cdoConverter) ? getDefaultMappingCdoConverter(cdoDbFactory) : cdoConverter;
        this.mappingContext = this.cdoConverter.getMappingContext();
        this.exceptionTranslator = cdoDbFactory.getExceptionTranslator();
    }

    private CdoTemplate(CdoDbFactory dbFactory, CdoTemplate that) {
        this.cdoDbFactory = dbFactory;
        this.projectionFactory = that.projectionFactory;
        this.cdoConverter = that.cdoConverter;
        this.mappingContext = that.mappingContext;
        this.objenesis = that.objenesis;
        this.cdoSessionOptions = that.cdoSessionOptions;
        this.exceptionTranslator = that.exceptionTranslator;
    }

    private static MappingCdoConverter getDefaultMappingCdoConverter(CdoDbFactory factory) {
        CdoMappingContext mappingContext = new CdoMappingContext();
//        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.setSimpleTypeHolder(SimpleTypeHolder.DEFAULT);
        mappingContext.afterPropertiesSet();
        MappingCdoConverter cdoConverter = new MappingCdoConverter(mappingContext);
        cdoConverter.afterPropertiesSet();
        return cdoConverter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

//        if (entityCallbacks == null) {
//            setEntityCallbacks(EntityCallbacks.create(applicationContext));
//        }

        if (mappingContext instanceof ApplicationEventPublisherAware) {
            ((ApplicationEventPublisherAware) mappingContext).setApplicationEventPublisher(applicationContext);
        }

        resourceLoader = applicationContext;

        projectionFactory.setBeanFactory(applicationContext);
        if (Objects.nonNull(applicationContext.getClassLoader()))
            projectionFactory.setBeanClassLoader(applicationContext.getClassLoader());
    }

//    public void setEntityCallbacks(EntityCallbacks entityCallbacks) {
//        Assert.notNull(entityCallbacks, "EntityCallbacks must not be null!");
//        this.entityCallbacks = entityCallbacks;
//    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }


    @Override
    public MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public SessionBoundCdoTemplate withSession(CdoClientSession session) {
        Assert.notNull(session, "ClientSession must not be null!");
        return new SessionBoundCdoTemplate(session, CdoTemplate.this);
    }

    @Override
    public CDOPackageRegistry getCDOPackageRegistry() {
        return execute(session -> {
            return session.getDelegate().getPackageRegistry();
        });
    }

    //see: https://www.baeldung.com/spring-data-crud-repository-save
    @Override
    public <T> T insert(T objectToSave) {
        Assert.notNull(objectToSave, "ObjectToSave must not be null!");
        ensureNotIterable(objectToSave);
        return insert(objectToSave, getResourcePathFrom(ClassUtils.getUserClass(objectToSave)));
    }

    @Override
    public <T> T insert(T objectToSave, String pathName) {
        Assert.notNull(objectToSave, "ObjectToSave must not be null!");
        Assert.notNull(pathName, "pathName must not be null!");
        ensureNotIterable(objectToSave);
        return (T) doInsert(pathName, objectToSave, this.cdoConverter);
    }

    /**
     * Performs an update of the given entity at the specified resource path.
     * <p>
     * The latest revision in the CDO is acquired and compared with the entity. Based upon the delta, the update is
     * performed.
     * <p>
     * If pathName resolves to a non existing resource path within a repository a new one will be created
     *
     * @param entity           the entity to update
     * @param repoResourcePath the resource path where the entity resides //TODO is not evaluated currently (!)
     * @param <T>              the type of the entity
     * @return the updated entity
     */
    @Override
    public <T> T save(T entity, final String repoResourcePath) {
        final Class<?> rawType = ClassUtils.getUserClass(entity);
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);

        T savedResult = execute(session -> {
            EObject internalValue = null;
            final CDOID cdoid;
            if (persistentEntity.isNativeCdoOrLegacyMode()) {
                internalValue = (EObject) entity;
//                Assert.isTrue(persistentEntity.isInheritedCDOObject() || persistentEntity.isInheritedLegacyObject(), "Error: invalid CDO entity");
                cdoid = Optional.ofNullable(CDOUtil.getCDOObject(internalValue).cdoID())
                        .<IllegalStateException>orElseThrow(() -> {
                            throw new IllegalStateException("Could not obtain identifier!");
                        });
            } else {
                internalValue = (EObject) cdoConverter.getInternalValue(persistentEntity, entity, EObjectModel.class);
//                String uriFragment = EcoreUtil.getURI(internalValue).fragment(); //resource.getURIFragment(internalValue);
                CdoPersistentProperty requiredIdProperty = persistentEntity.getRequiredIdProperty();
                Object idObject = persistentEntity.getPropertyAccessor(entity).getProperty(requiredIdProperty);
                cdoid = ensureIDisCDOID(Optional.ofNullable(idObject).<IllegalStateException>orElseThrow(() -> {
                    throw new IllegalStateException("Could not obtain identifier!");
                }));
            }

            ////TODO this causes possibly a stackoverflow exception later when removing all objects
//            resource.getResourceSet().getPackageRegistry().put(null, Optional.ofNullable(internalValue).<IllegalStateException>orElseThrow(() -> {
//                throw new IllegalStateException("The persistent entity model of the class was null. Maybe it was not properly annotated? Null values cannot be saved.");
//            }));
            CDOTransaction transaction = null;
            try {
                //see: https://www.eclipse.org/forums/index.php/t/203394/

                //Compute delta first between the current object and the latest revision in the store
                CDOObject objectToUpdate = CDOUtil.getCDOObject(internalValue);
                //objectToUpdate.cdoResource().getURI();repoResourcePath.split("/").equals(objectToUpdate.cdoResource().getURI().segments())
                CDORevision currentRevision = objectToUpdate.cdoRevision();
                if (Objects.nonNull(currentRevision)) {
                    // PartialCollectionLoadingNotSupportedException: List contains proxy elements
                    session.getDelegate().options().setCollectionLoadingPolicy(CDOUtil.createCollectionLoadingPolicy(0, 300));
                    CDOBranchVersion branchVersion = currentRevision.getBranch().getVersion(currentRevision.getVersion());
                    CDORevision oldRevision = session.getDelegate().getRevisionManager()
                            .getRevisionByVersion(cdoid, branchVersion, 0, true);
                    CDORevisionDelta delta = currentRevision.compare(oldRevision);

                    // safest approach taken to update the historical object:
                    // Open a second audit view that gets the latest object
                    transaction = (CDOTransaction) objectToUpdate.cdoView();
                    CDOSession session2 = transaction.getSession();
                    CDOView audit = session2.openView(currentRevision);
                    EObject historicalObject = audit.getObject(objectToUpdate);
                    // Lock the object in question and perform the update
                    // We must copy selected features over determined by the delta above
                    // Note/Question: not all feature delta types makes sense or must be supported (?)
                    transaction.lockObjects(Collections.singleton(CDOUtil.getCDOObject(historicalObject)),
                            IRWLockManager.LockType.WRITE, session.getOptions().getWriteLockoutTimeout());
                    for (Map.Entry<EStructuralFeature, CDOFeatureDelta> featureDelta : ((CDORevisionDeltaImpl) delta).getFeatureDeltaMap().entrySet()) {
                        Object newValue = ((CDOSetFeatureDeltaImpl) featureDelta.getValue()).getValue();
                        switch (featureDelta.getValue().getType()) {
                            case SET:
                                historicalObject.eSet(featureDelta.getKey(), newValue);
                                break;
                            case UNSET:
                                historicalObject.eSet(featureDelta.getKey(), null);
                                break;
                            case REMOVE:
                                EcoreUtil.remove(historicalObject, featureDelta.getKey(), newValue);
                                break;
                            case ADD:
                            case LIST:
                            case MOVE:
                            case CONTAINER:
                            case CLEAR:
                                throw new UnsupportedOperationException();
                            default:
                                continue;
                        }
                    }
                } else {
                    transaction = openTransaction(session);
                    CDOResource resource = transaction.getResource(repoResourcePath, true);
                    CDOObject object = transaction.getObject(cdoid);
                    transaction.lockObjects(Collections.singleton(CDOUtil.getCDOObject(object)),
                            IRWLockManager.LockType.WRITE, session.getOptions().getWriteLockoutTimeout());
                    resource.getContents().remove(object);
                    resource.getContents().add(objectToUpdate);
                }
                // not necessary, automatic unlock after commit
//                CDOUtil.getCDOObject(oldDBObject).cdoWriteLock().unlock();
                transaction.commit();
            } catch (NullPointerException e) {
                throw new InvalidDataAccessResourceUsageException(e.toString());
            } catch (CommitException e) {
                throw new DataIntegrityViolationException(e.toString());
            } catch (InterruptedException e) {
                throw new OptimisticLockingFailureException(
                        String.format("Cannot save entity with ID %s to repository %s. Has it been modified meanwhile?",
                                Objects.nonNull(cdoid) ? cdoid.toURIFragment() : "NULL", repoResourcePath), e);
            } finally {
                closeTransaction(transaction);
            }
            return (T) entity;
        });

        AfterSaveEvent<?> eventAfter = new AfterSaveEvent<>(savedResult, null, repoResourcePath);
        maybePublishEvent(eventAfter);

        return savedResult;
    }

    private void closeTransaction(@Nullable CDOTransaction transaction) {
//        Optional.ofNullable(transaction).filter(x -> !x.isClosed()).ifPresent(Closeable::close);
    }

    private void closeView(@Nullable CDOView view) {
//        Optional.ofNullable(view).filter(x -> !x.isClosed()).ifPresent(Closeable::close);
    }

    private CDOTransaction openTransaction(CdoClientSession session) {
        CDOTransaction trans = session.getDelegate().openTransaction();
        trans.getSession().options().setLockNotificationMode(CDOCommonSession.Options.LockNotificationMode.ALWAYS);
        trans.getSession().options().setGeneratedPackageEmulationEnabled(session.getOptions().isGeneratedPackageEmulationEnabled());
        return trans;
    }

    @Nullable
    @Override
    public <T, ID> T find(final ID entityID, Class<T> javaClassType, final String resourcePath) {
        Assert.notNull(entityID, "ID of Entity meach.getID()ust not be null!");
        //TODO allow also string-typed ids and convert here accordingly
        ensureIDisCDOID(entityID);

        CDOID cdoid = (CDOID) entityID;

        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);
        Assert.notNull(persistentEntity, "CDO Persistent entity must not be null!");
        final boolean explicitCDOObject = persistentEntity.isNativeCDOObject();
        final boolean isLegacy = persistentEntity.isLegacyObject();

        T execute = execute(session -> {
            CDOView cdoView = null;
            EObject object;
            try {
                cdoView = session.getDelegate().openView();
                object = cdoView.getObject(cdoid);
                if (Objects.isNull(object))
                    throw new DataNotFoundException("Data couldn't be retrieved with id=" + cdoid);
                if (explicitCDOObject) {
                    Assert.isTrue(ClassUtils.isAssignable(ClassUtils.getUserClass(object), javaClassType), "Domain class type cannot be assigned to class type of the corresponding CDO object ");
//                    if (ClassUtils.isAssignable(ClassUtils.getUserClass(object), javaClassType)) { //TODO make this part of a Query
                    return (T) object; //javaClassType.cast(object);
                } else if (isLegacy) {
                    if (object instanceof CDOLegacyAdapter) {
                        return javaClassType.cast(((CDOLegacyAdapter) object).cdoInternalInstance());
                    } else return null;
                } else {
                    T read = cdoConverter.read(javaClassType, object);
                    Assert.notNull(read, "CdoConverter returned null while reading EObject");
                    return read;
                }
            } finally {
                closeView(cdoView);
            }
        });
        return (T) execute;
    }

    @Override
    public <T> List<T> findAll(Class<T> javaClassType, final String repoResourcePath) {
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");
        final boolean explicitCDOObject = persistentEntity.isNativeCDOObject();
        final boolean isLegacy = persistentEntity.isLegacyObject();

        return execute(session -> {
            List<T> collection = new LinkedList<>();
            CDOView cdoView = session.getDelegate().openView();
            CdoPersistentProperty persistentProperty;
            Class classFor;
            if (!explicitCDOObject && !isLegacy) {
                persistentProperty = persistentEntity.getRequiredEObjectModelProperty();
                if (Objects.nonNull(persistentProperty.getClassFor())) {
                    classFor = persistentProperty.getClassFor();
                } else {
                    classFor = EObject.class; //TODO retrieve from member var
                }
            } else {
                classFor = EObject.class;
                persistentProperty = null;
            }
            try {
                CDOResource resource = cdoView.getResource(repoResourcePath, true);
                resource.getContents()
                        .forEach(eachObject -> {

                            if (explicitCDOObject || isLegacy) {
                                if (ClassUtils.isAssignable(ClassUtils.getUserClass(eachObject), javaClassType)) { //TODO make this part of a Query
                                    collection.add((T) javaClassType.cast(eachObject));
//                                    return javaClassType.cast(eachObject);
                                }
                            } else {
                                if (objectMatchesCriteria(persistentProperty, eachObject, classFor,
                                        persistentEntity.getNsUri(), persistentEntity.getPackageName())) { //TODO make this part of a Query
                                    T read = cdoConverter.read(javaClassType, eachObject);
                                    Assert.notNull(read, "CdoConverter returned null while reading EObject");
                                    collection.add(read);
                                }
                            }
                        });
                return collection;
            } catch (InvalidURIException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    @Override
    public <T> CDORevisionHolder<T> getRevision(T entity) {
        Assert.notNull(entity, "Entity must not be null!");
        return getRevision(entity, getResourcePathFrom(entity.getClass()));
    }

    @Override
    public <T> CDORevisionHolder<T> getRevision(T entity, String resourcePath) {
        Assert.notNull(entity, "Entity must not be null!");
        final Class<?> rawType = ClassUtils.getUserClass(entity);
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);
        CDOID cdoid;
        if (persistentEntity.isNativeCdoOrLegacyMode()) {
            cdoid = Optional.ofNullable(CDOUtil.getCDOObject((EObject) entity).cdoID())
                    .<IllegalStateException>orElseThrow(() -> {
                        throw new IllegalStateException("Could not obtain identifier!");
                    });
        } else {
            CdoPersistentProperty requiredIdProperty = persistentEntity.getRequiredIdProperty();
            Object idObject = persistentEntity.getPropertyAccessor(entity).getProperty(requiredIdProperty);
            cdoid = ensureIDisCDOID(Optional.ofNullable(idObject).<IllegalStateException>orElseThrow(() -> {
                throw new IllegalStateException("Could not obtain identifier!");
            }));
        }
        return getRevisionById(cdoid, resourcePath);
    }

    @Override
    public <T, ID> CDORevisionHolder<T> getRevisionById(@NonNull ID id, String resourcePath) {
        Assert.notNull(id, "ID must not be null!");
        //TODO allow also string-typed ids and convert here accordingly
        ensureIDisCDOID(id);

        CDOID cdoid = (CDOID) id;
        CDORevisionHolder<T> revisionContainerResult = execute(session -> {
            CDORevisionHolder<T> revisionContainer = CDORevisionHolder.create();
            CDOTransaction transaction = openTransaction(session);
            CDOObject latestObject;
            try {
                CDOResource resource = transaction.getResource(resourcePath, true);
                latestObject = transaction.getObject(cdoid);
                if (Objects.nonNull(resource) && !resource.getContents().contains(latestObject)) {
                    throw new DataNotFoundException(String.format("Entry with ID %s not found at %s", cdoid, resourcePath));
                }
            } catch (InvalidURIException e) {
                throw new DataNotFoundException(String.format("Entry with ID %s not found at %s", cdoid, e.getURI()));
            }
            Class<T> rawType = (Class<T>) ClassUtils.getUserClass(latestObject);
            final CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);
            final boolean explicitCDOObject = persistentEntity.isNativeCDOObject();
            final boolean isLegacy = persistentEntity.isLegacyObject();
            if (isLegacy) {
                rawType = (Class<T>) ClassUtils.getUserClass(((CDOLegacyAdapter) latestObject).cdoInternalInstance());
            }

            CDORevision cdoRevision = latestObject.cdoRevision();
            CDOBranch head = transaction.getBranch().getHead().getBranch();
            for (int version = cdoRevision.getVersion(); version > 0; version--) {
                CDORevision revisionByVersion = CDOUtil.getRevisionByVersion(latestObject, head, version);
                CDOObject object = session.getDelegate().openView(revisionByVersion).getObject(cdoid);
//                CDOObject object = session.getDelegate().openView(head, CDOBranchPoint.UNSPECIFIED_DATE, latestObject.cdoResource().getResourceSet()).getObject(cdoid);
                if (explicitCDOObject || isLegacy) {
                    if (object instanceof CDOLegacyAdapter) {
                        InternalEObject eachObject = ((CDOLegacyAdapter) object).cdoInternalInstance();
                        Assert.isTrue(ClassUtils.isAssignable(ClassUtils.getUserClass(eachObject), rawType),
                                "Object from database cannot be cast to " + rawType);
                        if (ClassUtils.isAssignable(ClassUtils.getUserClass(eachObject), rawType)) {
                            T cast = rawType.cast(eachObject);
                            revisionContainer.add(cast, revisionByVersion);
                        }
                    } else {
                        Assert.isTrue(ClassUtils.isAssignable(ClassUtils.getUserClass(object), rawType),
                                "Object from database cannot be cast to " + rawType);
                        if (ClassUtils.isAssignable(ClassUtils.getUserClass(object), rawType)) {
                            T cast = rawType.cast(object);
                            revisionContainer.add(cast, revisionByVersion);
                        }
                    }

                } else {
                    T read = cdoConverter.read(rawType, object);
                    Assert.notNull(read, "CdoConverter returned null while reading EObject");
                    revisionContainer.add(read, revisionByVersion);
                }
            }
            return revisionContainer;
        });
        return revisionContainerResult;
    }

    @Override
    public <T> long countAll(final Class<T> javaType, final EPackage context, final String resourcePath) {
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");
        return execute(session -> {
            CDOView cdoView = null;
            String className = null;
            try {
                cdoView = session.getDelegate().openView();

                CDOQuery oclQuery = null;
                if (!persistentEntity.isNativeCdoOrLegacyMode()) {
                    Class<?> classFor = persistentEntity.getRequiredEObjectModelProperty().getClassFor();
                    Assert.notNull(classFor, "classFor property must not be null. Maybe it isn't defined for EObjectModel property.");
                    EClass eClassifier = (EClass) context.getEClassifier(classFor.getSimpleName());
                    className = classFor.getSimpleName();
                    oclQuery = createQuery(cdoView, className + ".allInstances()->size()", eClassifier);
//                    oclQuery = createQuery(cdoView, "self.oclType().allInstances()->size()", eClassifier);
                } else {
                    className = javaType.getSimpleName();
//                    Assert.isTrue(persistentEntity.isLegacyObject() || persistentEntity.isNativeCDOObject(), "Entity is not a subclass of EObject or CDOObject.");
                    EClass eClassifier = (EClass) context.getEClassifier(className);
                    Assert.notNull(eClassifier, String.format("EClass %s couldn't be obtained from the provided EPackage context", className));
                    oclQuery = createQuery(cdoView, className + ".allInstances()->size()", eClassifier);
//                    oclQuery = createQuery(cdoView, "self.oclType().allInstances()->size()", eClassifier);
                }

                Object resultValue = oclQuery.getResultValue();
                if (resultValue instanceof Integer) {
                    return new Long((Integer) resultValue);
                }
                return (Long) resultValue;
            } catch (Exception e) {
                if (e.getMessage().contains("SemanticException") &&
                        Objects.nonNull(className)) {
                    if (e.getMessage().contains(className))
                        return 0L;
                }
                if (e instanceof RuntimeException)
                    throw potentiallyConvertRuntimeException((RuntimeException) e, exceptionTranslator);
                return -1L;
            } finally {
                closeView(cdoView);
            }
        });
    }

    private CDOQuery createQuery(CDOTransaction transaction, String queryString, EObject context, boolean considerDirty) {
        CDOQuery query = transaction.createQuery("ocl", queryString, context, considerDirty);
        query.setParameter("cdoLazyExtents", false);
        return query;
    }

    private CDOQuery createQuery(CDOView view, String queryString, EObject context) {
        CDOQuery query = view.createQuery("ocl", queryString, context);
        query.setParameter("cdoLazyExtents", false);
        return query;
    }

    private InternalCDORevision readRevision(InternalCDOObject cdoObject) {
        InternalCDORevision revision = CDOStateMachine.INSTANCE.read(cdoObject);
        if (revision == null) {
            throw new IllegalStateException("revision == null");
        } else {
            return revision;
        }
    }

    @Override
    public <T> T save(T entity) {
        //TODO generate path if no one is given
        //the path must be updated to the object
        return this.save(entity, getResourcePathFrom(ClassUtils.getUserClass(entity)));
    }

    @Override
    public <T> Collection<T> insertAll(Collection<? extends T> objectsToSave) {
        return doInsertAll(objectsToSave, cdoConverter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> insertAll(Collection<? extends T> batchToSave, String resourcePath) {
        Assert.notNull(batchToSave, "BatchToSave must not be null!");
        Assert.notNull(resourcePath, "ResourcePath must not be null!");
        return (Collection<T>) doInsertBatch(resourcePath, batchToSave, this.cdoConverter);
    }

    /**
     * Collect objects in the list and group them by a common resource path name in order to later call
     * doInsertBatch individually.
     * <p>
     * Objects to be saved can be mixed types of {@literal T}.
     *
     * @param listToSave
     * @param cdoConverter
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> Collection<T> doInsertAll(Collection<? extends T> listToSave, CdoConverter cdoConverter) {
        Map<String, List<T>> elementsByCollection = new HashMap<>();
        List<T> savedObjects = new ArrayList<>(listToSave.size());

        for (T element : listToSave) {
            if (Objects.isNull(element)) {
                continue;
            }

            String collection = getResourcePathFrom(ClassUtils.getUserClass(element));
            List<T> collectionElements = elementsByCollection.computeIfAbsent(collection, k -> new ArrayList<>());
            collectionElements.add(element);
        }

        for (Map.Entry<String, List<T>> entry : elementsByCollection.entrySet()) {
            savedObjects.addAll(doInsertBatch(entry.getKey(), entry.getValue(), cdoConverter));
        }

        return savedObjects;
    }

    protected <T> Collection<T> doInsertBatch(String repoResourcePath, Collection<? extends T> batchToSave, CdoConverter cdoConverter) {
        Assert.notNull(cdoConverter, "CdoConverter must not be null!");

        final List<EObject> documentList = new ArrayList<>();
//        List<T> initializedBatchToSave = new ArrayList<>(batchToSave.size());
        for (T uninitialized : batchToSave) {
            Class<?> rawType = ClassUtils.getUserClass(uninitialized);
            CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);
            EObject internalValue;
            if (persistentEntity.isNativeCdoOrLegacyMode()) {
                internalValue = (EObject) uninitialized;
            } else {
                internalValue = (EObject) this.cdoConverter.getInternalValue(persistentEntity, uninitialized, EObjectModel.class);
            }

            BeforeSaveEvent<T> event = new BeforeSaveEvent<>(uninitialized, internalValue, repoResourcePath);
            maybePublishEvent(event);
            documentList.add(internalValue);
        }

        List<EObject> savedEObjects = insertEObjectList(repoResourcePath, documentList);
//        List<T> arr = new ArrayList<>(batchToSave);

        List<T> savedObjects = new ArrayList<>(documentList.size());
        int i = 0;
        for (T obj : batchToSave) {
            if (i < savedEObjects.size()) {
//                T objectToSave = arr.get(i);
                EObject bla = savedEObjects.get(i);
                Class<?> rawType = ClassUtils.getUserClass(obj);
                CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);
                CDOID identifier = null;
                if (!persistentEntity.isNativeCdoOrLegacyMode()) {
                    GeneratingIdAccessor generatingIdAccessor;
                    generatingIdAccessor = new GeneratingIdAccessor(
                            obj,
                            persistentEntity,
                            DefaultIdentifierGenerator.INSTANCE,
                            this.cdoConverter
                    );
                    Object identifier0 = generatingIdAccessor.getIdentifier();
                    if (Objects.nonNull(identifier0)) {
                        if (ClassUtils.isAssignable(InternalCDORevision.class, identifier0.getClass())) {
                            identifier = ((InternalCDORevision) identifier0).getID();
                        }
//                        if (ClassUtils.isAssignable(CDOID.class, identifier0.getClass()) &&
//                                Objects.nonNull(transaction.getObject((CDOID) identifier0))) {
//                            throw new CommitException();
//                        }
                    }
                    identifier = CDOUtil.getCDOObject(bla).cdoID();
                    generatingIdAccessor.getOrSetProvidedIdentifier(identifier);
                    savedObjects.add(obj);
                } else {
                    savedObjects.add((T) obj);
                }
            } else {
                savedObjects.add((T) obj);
            }
            i++;
        }
        return savedObjects;
    }

    protected List<EObject> insertEObjectList(final String resourcePath, final List<EObject> documents) {
        execute(session -> {
            CDOTransaction transaction = openTransaction(session);
            try {
                System.out.println("CDOTransaction is = " + transaction);
                CDOResource resource;
                try {
                    resource = transaction.getResource(resourcePath, true);
                } catch (InvalidURIException e) {
                    resource = transaction.createResource(resourcePath);
                }
                resource.getContents().addAll(documents);
                CDOCommitInfo commit = transaction.commit();

            } catch (CommitException e) {
                throw new DuplicateKeyException(
                        "Cannot insert existing Collection of objects in a single batch write.",
                        e);
            } finally {
                closeTransaction(transaction);
            }
            return null;
        });

        return documents;
    }

    protected <T> T doInsert(String repoResourcePath, T objectToSave, CdoWriter<T> writer) {

        Class<?> rawType = ClassUtils.getUserClass(objectToSave);
        CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);

//        objectToSave = maybeCallBeforeSave(objectToSave);
        EObject internalValue;

        // decide between explicit CDOObjects or custom user-defined objects
        if (persistentEntity.isNativeCdoOrLegacyMode()) { //persistentEntity.isNativeCDOObject() || persistentEntity.isLegacyObject()) {
//            Assert.isTrue(persistentEntity.isInheritedCDOObject() || persistentEntity.isInheritedLegacyObject(), "Invalid entity");
            internalValue = (EObject) objectToSave;
        } else {
            internalValue = (EObject) cdoConverter.getInternalValue(persistentEntity, objectToSave, EObjectModel.class);
        }

        BeforeSaveEvent<T> event = new BeforeSaveEvent<>(objectToSave, internalValue, repoResourcePath);
        maybePublishEvent(event);

        //real physical write:
        return execute(session -> {
            CDOID identifier = null;
            CDOTransaction transaction = null;
            try {
//                URI uri = EcoreUtil.getURI(internalValue);
//                System.out.println("ecoreURI to save: " + uri);
//                session.getDelegate().getPackageRegistry().putEPackage(cdoObject);
//                System.out.println("Session is = " + session);
                transaction = openTransaction(session);
//                System.out.println("CDOTransaction is = " + transaction);
//                boolean repoPathExists = true;
                CDOResource resource;
                try {
                    resource = transaction.getResource(repoResourcePath, true);
//                    System.out.println("Get resource: " + resource);
                } catch (InvalidURIException e) {
//                    repoPathExists = false;
                    resource = transaction.createResource(repoResourcePath);
//                    System.out.println("Create new resource: " + resource);
                }

//                resource.getResourceSet().getPackageRegistry().put(null, internalValue);
//                System.out.println("resource.cdoRevision().getID(): " + resource.cdoRevision().getID());

                resource.getContents().add(internalValue);
                CDOCommitInfo commit = transaction.commit();

                // only for non-explicit CDOObjects or LegacyCDOObjects: set the CDOID manually for the custom class' ID attribute
                if (!persistentEntity.isNativeCdoOrLegacyMode()) {
                    GeneratingIdAccessor generatingIdAccessor;
                    generatingIdAccessor = new GeneratingIdAccessor(
                            objectToSave,
                            persistentEntity,
                            DefaultIdentifierGenerator.INSTANCE,
                            cdoConverter
                    );
                    Object identifier0 = generatingIdAccessor.getIdentifier();
                    if (Objects.nonNull(identifier0)) {
                        if (ClassUtils.isAssignable(InternalCDORevision.class, identifier0.getClass())) {
                            identifier = ((InternalCDORevision) identifier0).getID();
                        }
                        if (ClassUtils.isAssignable(CDOID.class, identifier0.getClass()) && Objects.nonNull(transaction.getObject((CDOID) identifier0))) {
                            throw new CommitException();
                        }
                    }
                    identifier = CDOUtil.getCDOObject(internalValue).cdoID();
                    generatingIdAccessor.getOrSetProvidedIdentifier(identifier);
                }
                //TODO: commit.getTimeStamp() add to entity
                //objectToSave and executedResult
                AfterSaveEvent<?> eventAfter = new AfterSaveEvent<>(objectToSave, internalValue, repoResourcePath);
                maybePublishEvent(eventAfter);
                return objectToSave;
            } catch (CommitException e) {
                throw new DuplicateKeyException(
                        String.format("Cannot insert existing object with id %s!. Please use update.", identifier), e);
            } finally {
                closeTransaction(transaction);
            }
        });
    }

//    @SuppressWarnings("unchecked")
//    protected <T> T maybeCallBeforeSave(T object) {
//
//        if (null != entityCallbacks) {
//            return entityCallbacks.callback(BeforeSaveCallback.class, object);
//        }
//
//        return object;
//    }


    @Override
    public void createResourcePath(String resourcePath) {
        Assert.hasText(resourcePath, "resourcePath name must not be null or empty!");
        execute(session -> {
            try {
                CDOTransaction cdoTransaction = openTransaction(session);
                CDOResource orCreateResource = cdoTransaction.getOrCreateResource(resourcePath);
                cdoTransaction.commit();
//                CDOResource resource = cdoTransaction.getResource(resourcePath);
//                resource.delete(Collections.emptyMap());
            } catch (InvalidURIException e) {
                throw new EmptyResultDataAccessException("Resource path couldn't be created.", 1, e);
            } catch (ConcurrentAccessException e) {
                e.printStackTrace();
            } catch (CommitException e) {
                e.printStackTrace();
            } catch (CDOException e) {
                throw new CreateResourceFailedException("CDO resource path=" + resourcePath + " couldn't be created." +
                        "Maybe some folder in the resource path already is a node in the repository.");
            }
            return null;
        });
    }

    @Override
    public void removeResourcePath(String resourcePath, boolean recursive) {
        Assert.hasText(resourcePath, "resourcePath name must not be null or empty!");
        execute(session -> {
            try {
                CDOTransaction cdoTransaction = openTransaction(session);
//                CDOResourceFolder resourceFolder = cdoTransaction.getResourceFolder(resourcePath);
                CDOResource resourceFolder = cdoTransaction.getResource(resourcePath);
                if (recursive) {
                    CDOResourceFolder parent = resourceFolder.getFolder();
                    while (Objects.nonNull(parent)) {
                        CDOResourceFolder folder = parent.getFolder();
                        parent.delete(Collections.emptyMap());
                        parent = folder;
                    }
                }
                resourceFolder.delete(Collections.emptyMap());
                cdoTransaction.commit();
            } catch (InvalidURIException e) {
                throw new EmptyResultDataAccessException(e.getMessage(), 1, e);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Couldn't delete resource path=" + resourcePath, e);
            } catch (ConcurrentAccessException e) {
                e.printStackTrace();
            } catch (CommitException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void removeResourcePath(final String resourcePath) {
        removeResourcePath(resourcePath, false);
    }

    @Override
    public <T> CdoDeleteResult remove(T entity, String resourcePath) {
        Assert.notNull(entity, "entity must not be null!");
        Assert.hasText(resourcePath, "resourcePath name must not be null or empty!");
        return doRemove(entity, resourcePath);
    }

    @Override
    public <T> CdoDeleteResult removeAll(Class<T> javaType) {
        Assert.notNull(javaType, "Class must not be null!");
        return doRemove(javaType, getResourcePathFrom(javaType));
    }

    @Override
    public <T> CdoDeleteResult removeAll(final Class<T> javaType, final String resourcePath) {
        return doRemove(javaType, resourcePath);
    }


    @Override
    public CdoDeleteResult removeAll(String resourcePath) {
        Assert.notNull(resourcePath, "resourcePath must not be null!");
        return execute(session -> {
            try {
                CDOTransaction cdoTransaction = openTransaction(session);
                CDOResource resource = cdoTransaction.getResource(resourcePath);
                resource.getContents().clear();
                CDOCommitInfo commit = cdoTransaction.commit();
                if (resource.getContents().size() == 0) {
                    return CdoDeleteResult.acknowledged(commit.getDetachedObjects().size());
                }
            } catch (InvalidURIException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("The resource path %s couldn't be removed. Maybe it doesn't exists.", resourcePath), e);
                }
            } catch (CommitException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("An error occurred when removing resource path", e);
                }
            }
            return CdoDeleteResult.unacknowledged();
        });
    }

    //TODO: this is also a query-typed action
    private <T> CdoDeleteResult doRemove(final Class<T> classType, final String resourcePath) {
        maybePublishEvent(new BeforeDeleteEvent<>(classType, null, resourcePath));

        CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(classType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");

        String nsUri = persistentEntity.getNsUri();
        String packageName = persistentEntity.getPackageName();

        return execute(session -> {
            CDOTransaction cdoTransaction = null;
//            Collection<EObject> toRemove = new LinkedList<>();
            try {
                cdoTransaction = openTransaction(session);
                CDOResource resource = cdoTransaction.getResource(resourcePath);

                List<CDOObject> toRemove = resource.getContents().stream()
                        .filter(each -> {
                            if (persistentEntity.isNativeCdoOrLegacyMode()) {
                                if (ClassUtils.isAssignable(ClassUtils.getUserClass(each), classType)) { //TODO make this part of a Query
//                            toRemove.add(each);
                                    return true;
                                }
                            } else {
                                CdoPersistentProperty persistentProperty = persistentEntity.getRequiredEObjectModelProperty();
                                Assert.notNull(persistentProperty.getClassFor(), "classFor property must not be null for EObjectModel property");
                                if (objectMatchesCriteria(persistentProperty, each, persistentProperty.getClassFor(), nsUri, packageName)) { //TODO make this part of a Query
//                            toRemove.add(each);
                                    return true;
                                }
                            }
                            return false;
                        })
                        .map(CDOUtil::getCDOObject)
                        .collect(Collectors.toList());

                if (toRemove.size() > 0) {
                    // Exclusive lock to these objects
                    //.stream().map(CDOUtil::getCDOObject).collect(Collectors.toList())
                    cdoTransaction.lockObjects(toRemove, IRWLockManager.LockType.WRITE, session.getOptions().getWriteLockoutTimeout());
                    boolean b = resource.getContents().removeAll(toRemove.stream().map(CDOUtil::getEObject).collect(Collectors.toList()));
                    EcoreUtil.deleteAll(toRemove, true);
                    Assert.isTrue(b, "Objects were not removed.");
                    cdoTransaction.commit();
                }
                return CdoDeleteResult.acknowledged(toRemove.size());
            } catch (InvalidURIException e) { // when the resource path doesn't exists
//                potentiallyConvertRuntimeException(e, exceptionTranslator);
                return CdoDeleteResult.unacknowledged(e);
            } catch (CommitException | InterruptedException e) {
                // when the lock failed  or when commiting e.g., because of concurrent access
                return CdoDeleteResult.unacknowledged(e);
            } finally {
                closeTransaction(cdoTransaction);
                maybePublishEvent(new AfterDeleteEvent<>(classType, null, resourcePath));
            }
        });
    }

    //TODO based on a query: entity is then just a constraint (eg forEntity)
    //TODO: add some user-defined constraints like retries when locks exists and so on
    private <T> CdoDeleteResult doRemove(final T entity, final String resourcePath) {
        Assert.notNull(entity, "Entity to be removed must not be null!");
        maybePublishEvent(new BeforeDeleteEvent<>("PUT-query-object-here", null, resourcePath));

        ClassTypeInformation<?> classTypeInfo = ClassTypeInformation.from(ClassUtils.getUserClass(entity));
        CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(classTypeInfo.getType());
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");

        return execute(delegate -> {
            CDOTransaction transaction = null;
            CDOResource resource = null;
            CDOID cdoid = null;
            CdoDeleteResult deleteResult = null;
            try {
                transaction = openTransaction(delegate);
                resource = transaction.getResource(resourcePath);
                EObject remoteCdoObject;
                if (persistentEntity.isNativeCdoOrLegacyMode()) { //persistentEntity.isNativeCDOObject() || persistentEntity.isLegacyObject()) {
                    //entity shall be equal to remoteCdoObject
//                    cdoid = ((CDOObject) entity).cdoID();
                    cdoid = CDOUtil.getCDOObject((EObject) entity).cdoID();
                } else { // is annotated
                    cdoid = (CDOID) persistentEntity.getIdentifierAccessor(entity).getRequiredIdentifier();
                }
                //TODO:
//                resource.cdoResource().getResourceSet().getEObject()
                remoteCdoObject = transaction.getObject(cdoid);
                if (Objects.isNull(remoteCdoObject))
                    throw new DataNotFoundException("Object with ID=" + cdoid + " not found.");
                if (!resource.getURI().equals(remoteCdoObject.eResource().getURI())) {
                    throw new DataNotFoundException(String.format("Entity with ID=%s cannot be located within the resource path=%s. Actual URI of the object is=%s",
                            cdoid, resourcePath, remoteCdoObject.eResource().getURI()));
                }
                CDOUtil.getCDOObject(remoteCdoObject).cdoWriteLock().lock(delegate.getOptions().getWriteLockoutTimeout());
                resource.getContents().remove(remoteCdoObject);
                EcoreUtil.delete(remoteCdoObject, true);
                CDOCommitInfo commit = transaction.commit();

                // "Whenever an object is detached from the graph it looses all its CDO-specific properties: id, state, view and revision."
                // However, for non-native CDO objects we have to unset the ID property manually
                if (!persistentEntity.isNativeCdoOrLegacyMode()) { //!persistentEntity.isNativeCDOObject() && !persistentEntity.isLegacyObject()
                    persistentEntity.getPropertyAccessor(entity).setProperty(persistentEntity.getRequiredIdProperty(), null);
                    // override the "resetted" model property from the entity with the CDO one
                    // the CDO-specific stuff is unsetted already
                    if (ClassUtils.isAssignable(CDOLegacyAdapter.class, remoteCdoObject.getClass())) {
                        remoteCdoObject = CDOUtil.getEObject(remoteCdoObject);
                    } else {
                        remoteCdoObject = CDOUtil.getCDOObject(remoteCdoObject);
                    }
                    persistentEntity.getPropertyAccessor(entity).setProperty(persistentEntity.getRequiredEObjectModelProperty(), remoteCdoObject);
                }
//                else if (persistentEntity.isLegacyObject()) {
//                    CdoPersistentEntity<?> persistentEntity0 = mappingContext.getPersistentEntity(CDOUtil.getCDOObject((EObject) entity).getClass());
//                    Assert.notNull(persistentEntity0, "Persistent entity of a object in legacy mode must not be null.");
//                    persistentEntity0.getPropertyAccessor(CDOUtil.getCDOObject((EObject) entity)).setProperty(persistentEntity0.getRequiredIdProperty(), null);
//                }

                deleteResult = CdoDeleteResult.acknowledged(1);
            } catch (InvalidURIException e) {
                deleteResult = CdoDeleteResult.unacknowledged();
            } catch (CommitException e) {
                e.printStackTrace();
                deleteResult = CdoDeleteResult.unacknowledged();
            } catch (ObjectNotFoundException e) {
                throw new DataNotFoundException(e.toString());
            } catch (TimeoutException e) {
                throw new OptimisticLockingFailureException(
                        String.format("Cannot save entity with ID %s to repository %s. Has it been modified meanwhile?",
                                Objects.nonNull(cdoid) ? cdoid.toURIFragment() : "NULL", resourcePath), e);
            } finally {
                closeTransaction(transaction);
                maybePublishEvent(new AfterDeleteEvent<>("query-here", null, resourcePath));
            }
            return deleteResult;
        });
    }

    @Override
    public <T> CdoDeleteResult remove(T entity) {
        Assert.notNull(entity, "Entity must not be null!");
        return remove(entity, getResourcePathFrom(entity.getClass()));
    }

    @Override
    public <T extends CdoSessionActionDelegate<?>> IListener addListener(CdoListenerFilter filter, T action) {
        return attachListener(filter, action);
    }

    @Override
    public <T extends CdoSessionActionDelegate<?>> IListener addListeners(CdoListenerFilter filter, T... actions) {
        return attachListener(filter, actions);
    }

    // some convenient methods ...
//    @Override
//    public <ID> IListener addListener(ID entityID, CdoEventBasedActionDelegate action) {
//        final CDOID cdoid = (CDOID) entityID;
//        CdoListenerFilter filter = CdoListenerFilter.filter(new FilterCriteria().byCdoId((CDOID) entityID));
//        return attachListener(filter, action);
//    }
//
//    @Override
//    public IListener addListener(String resourcePath, CdoEventBasedActionDelegate action) {
//        CdoListenerFilter filter = CdoListenerFilter.filter(new FilterCriteria().byRepositoryPath(resourcePath));
//        return attachListener(filter, action);
//    }

    protected <T extends CdoSessionActionDelegate<?>> DefaultCdoSessionListener attachListener(@Nullable CdoListenerFilter filter, T... actions) {
        return execute(session -> {
            CDOView view = session.getDelegate().openView();
            applyCDOViewOptions(view);
            view.getSession().options().setPassiveUpdateEnabled(true);
            view.getSession().options().setPassiveUpdateMode(CDOCommonSession.Options.PassiveUpdateMode.INVALIDATIONS);

            Map<T, Boolean> actionApplied = new LinkedHashMap<>();
            for (T eachAction : actions) {
                actionApplied.put(eachAction, false);
            }

            // Assign actions to specific filters if possible
            if (filter != null) {
                Set<String> repositoryPaths = new LinkedHashSet<>();
                for (FilterCriteria filterCriteria : filter.getCriteria().values()) {
                    String repositoryPath = filterCriteria.getRepositoryPath();
                    if (repositoryPath != null) {
                        repositoryPaths.add(repositoryPath);
                    }
                }
                if (repositoryPaths.size() > 0) {
                    // Repository Path filter can only process 'CdoNewObjectsActionDelegate' actions
                    List<CdoNewObjectsActionDelegate> suitableActions = actionApplied.keySet().stream()
                            .filter(x -> x instanceof CdoNewObjectsActionDelegate)
                            .map(x -> {
                                actionApplied.replace(x, true);
                                return (CdoNewObjectsActionDelegate) x;
                            })
                            .collect(Collectors.toCollection(ArrayList<CdoNewObjectsActionDelegate>::new));
                    if (suitableActions.size() > 0) {
                        ResourceContentAdapter resourceContentAdapter = new ResourceContentAdapter(suitableActions);
                        for (String repoPath : repositoryPaths) {
                            if (repoPath != null) {
                                CDOResource resource = null;
                                // This is necessary in case the listeners are added before an actual object is stored within a specific repository path
                                try {
                                    resource = view.getResource(repoPath, true);
                                } catch (InvalidURIException e) {
                                    try {
                                        // ... then we need to create the resource first
                                        CDOTransaction cdoTransaction = openTransaction(session);
                                        resource = cdoTransaction.getOrCreateResource(repoPath);
                                        cdoTransaction.commit();
                                        resource = view.getResource(repoPath, true);
                                    } catch (CommitException commitException) {
                                        throw new RuntimeException(commitException);
                                    }
                                } finally {
                                    assert resource != null;
                                }
                                resource.eAdapters().add(resourceContentAdapter);
                            }
                        }
                    }
                }
            }

            DefaultCdoSessionListener cdoSessionListener = filter != null ?
                    new DefaultCdoSessionListener(filter, "") :
                    new DefaultCdoSessionListener("");
//            cdoSessionListener.setAction(Arrays.asList(actions));
            // Collect the remainder actions that were not previously "consumed" by the filters
            List<T> collect = actionApplied.entrySet().stream().filter(x -> !x.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());
            cdoSessionListener.setAction((List<CdoSessionActionDelegate<?>>) collect);
            view.getSession().addListener(cdoSessionListener);

            return cdoSessionListener;
        });
    }

    private void applyCDOViewOptions(CDOView view) {
        view.options().addChangeSubscriptionPolicy(CDOAdapterPolicy.ALL);
        view.options().setInvalidationNotificationEnabled(true);
        view.options().setLoadNotificationEnabled(true);
        view.options().setDetachmentNotificationEnabled(true);
        view.options().setInvalidationPolicy(CDOInvalidationPolicy.DEFAULT);
        view.options().setLockNotificationEnabled(true);
    }


    /**
     * Executes an arbitrary storage operation. It provides all necessary resources for the given callback.
     * <p>
     * The calling methods supply mostly an anonymous class.
     *
     * @param action
     * @param <T>
     * @return
     */
    public <T> T execute(CdoCallback<T> action) {
        //TODO use sessionprovider or something similar or manage transaction here
        //with a transaction manager? see redis
//        if (enableTransactionSupport) {
//            // only bind resources in case of potential transaction synchronization
//            conn = RedisConnectionUtils.bindConnection(factory, enableTransactionSupport);
//        } else {
//            conn = RedisConnectionUtils.getConnection(factory);
//        }
//
//        boolean existingConnection = TransactionSynchronizationManager.hasResource(factory);
        //for us its not only the connection but the session
        try {
            CdoClientSession session = cdoDbFactory.getSession(this.cdoSessionOptions);
            boolean existingSession = false;
            T result = action.doInCdo(session);
            return postProcessResult(result, session, existingSession);
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        } finally {
            //TODO release session or connection...
        }
    }

    public CdoDbFactory getCdoDbFactory() {
        return cdoDbFactory;
    }

    private <E extends CdoMappingEvent<T>, T> E maybePublishEvent(E event) {
        //TODO && (eventTypesToPublish.isEmpty() || eventTypesToPublish.contains(event.getClass())): filter events to publish
        if (Objects.nonNull(eventPublisher) && publishEvents) {
            eventPublisher.publishEvent(event);
        }
        return event;
    }

    protected void ensureNotIterable(@Nullable Object o) {
        if (null != o) {
            if (o.getClass().isArray() || ITERABLE_CLASSES.contains(o.getClass().getName())) {
                throw new IllegalArgumentException("Cannot use a collection here.");
            }
        }
    }

    protected CDOID ensureIDisCDOID(@NonNull Object o) {
        ensureIDisCDOID(ClassUtils.getUserClass(o));
        return (CDOID) o;
    }

    protected void ensureIDisCDOID(@NonNull Class<?> rawType) {
        if (ClassUtils.isAssignable(CDOID.class, rawType)) {
            return;
        }
        boolean b = Arrays.stream(rawType.getInterfaces()).anyMatch(x -> ClassUtils.isAssignable(CDOID.class, x));
        if (!b) {
            throw new IllegalArgumentException("ID must be of class CDOID.");
        }
    }

    //TODO: later move to "QueryAction" related class ...
    public boolean objectMatchesCriteria(CdoPersistentProperty persistentProperty, EObject each, Class<?> classFor, @Nullable String nsUri, @Nullable String packageName) {
        boolean hasMatch = false;
        //is dynamic ecore class?
        if (ClassUtils.isAssignable(EObject.class, persistentProperty.getType()) && ClassUtils.isAssignable(EObject.class, each.getClass())) { //checkIfDynamicEmfClass_NonLegacyClass(persistentProperty.getType()) && checkIfDynamicEmfClass_NonLegacyClass(each.getClass())) {
            hasMatch = classFor.getSimpleName().equals(each.eClass().getName());
            if (StringUtils.hasText(packageName)) {
                hasMatch = hasMatch && each.eClass().getEPackage().getName().equals(packageName);
            }
            if (StringUtils.hasText(nsUri)) {
                hasMatch = hasMatch && each.eClass().getEPackage().getNsURI().equals(nsUri);
            }
        } else if (each instanceof EPackage && (ClassTypeInformation.from(persistentProperty.getRawType()).getType().equals(EPackage.class))) { //is an epackage?
            hasMatch = ((EPackage) each).getNsURI().equals(nsUri);
            if (StringUtils.hasText(packageName)) {
                hasMatch = hasMatch && ((EPackage) each).getName().equals(packageName);
            }
            if (StringUtils.hasText(nsUri)) {
                hasMatch = hasMatch && ((EPackage) each).getNsURI().equals(nsUri);
            }
            //TODO: match also prefix
        }
//        else { // is a concrete user-defined (custom) ecore class (not dynamically created)?
//            Class<?> type = persistentProperty.getType(); //ClassTypeInformation.from(persistentProperty.getRawType()).getType();
//            hasMatch = ClassUtils.isAssignable(classFor, each.getClass()) && ClassUtils.isAssignable(classFor, type);
//
////                    Arrays.stream(classFor)
////                    .anyMatch(x -> ClassUtils.isAssignable(x, each.getClass()))
////                    && Arrays.stream(classFor)
////                    .anyMatch(x -> ClassUtils.isAssignable(x, type));
////            hasMatch = Arrays.asList(classFor).contains(each.getClass()) && Arrays.asList(classFor).contains(ClassTypeInformation.from(persistentProperty.getRawType()).getType());
//        }
        return hasMatch;
    }

    @Override
    public CdoConverter getConverter() {
        return this.cdoConverter;
    }

    @Nullable
    protected <T> T postProcessResult(@Nullable T result, CdoClientSession session, boolean existingSession) {
        return result;
    }

    private Object executeSession(SessionCallback<?> session) {
        Assert.isTrue(this instanceof SessionBoundCdoTemplate, "CdoOperation is not bound to a session!");
        return session.execute(this);
    }

    /**
     * Tries to convert the given {@link RuntimeException} into a {@link DataAccessException} but returns the original
     * exception if the conversation failed. Thus allows safe re-throwing of the return value.
     *
     * @param ex                  the exception to translate
     * @param exceptionTranslator the {@link PersistenceExceptionTranslator} to be used for translation
     * @return
     */
    private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
                                                                       PersistenceExceptionTranslator exceptionTranslator) {
        RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
        return resolved == null ? ex : resolved;
    }

    @Override
    public void destroy() throws Exception {
        //TODO close all opened sessions and transactions or whatever
//        System.out.println("template destroy");
    }

    /**
     * A cdo template with a cdo session bounded to it.
     * Useful for concatenated operations. To be used with executeSession() method.
     */
    static class SessionBoundCdoTemplate extends CdoTemplate {

        private final CdoTemplate delegate;
        private final CdoClientSession session;

        /**
         * @param session must not be {@literal null}.
         * @param that    must not be {@literal null}.
         */
        SessionBoundCdoTemplate(CdoClientSession session, CdoTemplate that) {

            super(that.getCdoDbFactory().withSession(session), that);

            this.delegate = that;
            this.session = session;
        }
    }
}
