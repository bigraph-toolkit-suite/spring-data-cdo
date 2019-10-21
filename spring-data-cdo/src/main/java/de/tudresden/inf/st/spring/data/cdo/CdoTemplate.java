package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import de.tudresden.inf.st.spring.data.cdo.config.CdoClientSessionOptions;
import de.tudresden.inf.st.spring.data.cdo.core.*;
import de.tudresden.inf.st.spring.data.cdo.core.event.*;
import de.tudresden.inf.st.spring.data.cdo.core.mapping.CdoMappingContext;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.*;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.internal.cdo.object.CDOLegacyAdapter;
import org.eclipse.emf.internal.cdo.object.CDOLegacyWrapper;
import org.eclipse.emf.internal.cdo.object.DynamicCDOObjectImpl;
import org.eclipse.emf.internal.cdo.view.CDOStateMachine;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.net4j.util.concurrent.IRWLockManager;
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
//    private static final Logger LOGGER = LoggerFactory.getLogger(CdoTemplate.class);

    private static final Collection<String> ITERABLE_CLASSES;
    @Deprecated
    private static final Collection<Class> DYNAMIC_ECORE_CLASSES;
    private static final Collection<Class> CDO_LEGACY_CLASSES;

    static {

        Set<String> iterableClasses = new HashSet<>();
        iterableClasses.add(List.class.getName());
        iterableClasses.add(Collection.class.getName());
        iterableClasses.add(Iterator.class.getName());

        Set<Class> dynamicClasses = new HashSet<>();
        dynamicClasses.add(DynamicCDOObjectImpl.class);
        dynamicClasses.add(DynamicEObjectImpl.class);
        dynamicClasses.add(InternalEObject.class);
        dynamicClasses.add(BasicEObjectImpl.class);
        dynamicClasses.add(EObjectImpl.class);

        Set<Class> cdoLegacyClasses = new HashSet<>();
        cdoLegacyClasses.add(CDOLegacyWrapper.class);
        cdoLegacyClasses.add(CDOLegacyAdapter.class);

        ITERABLE_CLASSES = Collections.unmodifiableCollection(iterableClasses);
        DYNAMIC_ECORE_CLASSES = Collections.unmodifiableCollection(dynamicClasses);
        CDO_LEGACY_CLASSES = Collections.unmodifiableCollection(cdoLegacyClasses);
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
     * if pathName resolves to a non existing resource path within a repository a new one will be created
     *
     * @param entity
     * @param repoResourcePath
     * @param <T>
     * @return
     */
    @Override
    public <T> T save(T entity, final String repoResourcePath) {
        final Class<?> rawType = ClassUtils.getUserClass(entity);
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);

        T savedResult = execute(session -> {
            CDOView cdoView = session.getDelegate().openView();
            CDOTransaction trans = openTransaction(session);

            CDOResource resource = trans.getOrCreateResource(repoResourcePath);
            EObject internalValue;
            EObject oldDBObject;
            CDOID cdoid;
            if (persistentEntity.isExplicitCDOObject()) {
                internalValue = (EObject) entity;
                cdoid = CDOUtil.getCDOObject(internalValue).cdoID();
                Optional.ofNullable(cdoid).<IllegalStateException>orElseThrow(() -> {
                    throw new IllegalStateException("Could not obtain identifier!");
                });
//                InternalCDORevision internalCDORevision1 = readRevision((InternalCDOObject) internalValue);
            } else {
                internalValue = (EObject) cdoConverter.getInternalValue(persistentEntity, entity, EObjectModel.class);
//                String uriFragment = EcoreUtil.getURI(internalValue).fragment(); //resource.getURIFragment(internalValue);
                cdoid = (CDOID) persistentEntity.getIdentifierAccessor(entity).getRequiredIdentifier();
            }
            Optional.ofNullable(internalValue).<IllegalStateException>orElseThrow(() -> {
                throw new IllegalStateException("The persistent entity model of the class was null. Maybe it was not properly annotated? Null values cannot be saved.");
            });

            resource.getResourceSet().getPackageRegistry().put(null, internalValue);
            try {
                oldDBObject = cdoView.getObject(cdoid);
                if (Objects.isNull(oldDBObject))
                    throw new DataNotFoundException("Object with ID=" + cdoid + " not found.");

                CDOUtil.getCDOObject(oldDBObject).cdoWriteLock().lock(session.getOptions().getWriteLockoutTimeout());
                resource.getContents().add(internalValue);
                resource.getContents().remove(oldDBObject);
                // not necessary, automatic unlock after commit
                CDOUtil.getCDOObject(oldDBObject).cdoWriteLock().unlock();
                trans.commit();
            } catch (CommitException e) {
                throw new DataIntegrityViolationException(e.toString());
            } catch (TimeoutException e) {
                throw new OptimisticLockingFailureException(
                        String.format("Cannot save entity with ID %s to repository %s. Has it been modified meanwhile?",
                                Objects.nonNull(cdoid) ? cdoid.toURIFragment() : "NULL", repoResourcePath), e);
            } finally {
//                session.getDelegate().close();
            }
            return (T) entity;
        });

        AfterSaveEvent<?> eventAfter = new AfterSaveEvent<>(savedResult, null, repoResourcePath);
        maybePublishEvent(eventAfter);

        return savedResult;
    }

//    protected boolean checkIfLegacyEObject(@Nullable Class<?> o) {
//        if (Objects.nonNull(o)) {
////            return DYNAMIC_ECORE_CDO_CLASSES.contains(o);
//            return CDO_LEGACY_CLASSES.contains(o);
//        }
//        return false;
//    }

    private CDOTransaction openTransaction(CdoClientSession session) {
        CDOTransaction trans = session.getDelegate().openTransaction();
        trans.getSession().options().setLockNotificationMode(CDOCommonSession.Options.LockNotificationMode.ALWAYS);
        trans.getSession().options().setGeneratedPackageEmulationEnabled(session.getOptions().isGeneratedPackageEmulationEnabled());
        return trans;
    }

    @Nullable
    @Override
    public <T, ID> T find(final ID entityID, Class<T> javaClassType, final String resourcePath) {
        Assert.notNull(entityID, "ID of Entity must not be null!");
        //TODO allow also string-typed ids and convert here accordingly
        ensureIDisCDOID(entityID);

        CDOID cdoid = (CDOID) entityID;

        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);
        Assert.notNull(persistentEntity, "CDO Persistent entity must not be null!");
        final boolean explicitCDOObject = persistentEntity.isExplicitCDOObject();

        T execute = execute(session -> {
            CDOView cdoView = null;
            EObject object;
            try {
                cdoView = session.getDelegate().openView();
                object = cdoView.getObject(cdoid);
                if (Objects.isNull(object))
                    throw new DataNotFoundException("Data couldn't be retrieved with id=" + cdoid);
//                final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);

//                session.getDelegate().getPackageRegistry().getSubTypes();
                if (explicitCDOObject) {
                    Assert.isTrue(ClassUtils.isAssignable(ClassUtils.getUserClass(object), javaClassType), "Domain class type cannot be assigned to class type of the corresponding CDO object ");
//                    if (ClassUtils.isAssignable(ClassUtils.getUserClass(object), javaClassType)) { //TODO make this part of a Query
                    return javaClassType.cast(object);
                } else {
//                    if (objectMatchesCriteria(persistentProperty, object, classFor,
//                            persistentEntity.getNsUri(), persistentEntity.getPackageName())) { //TODO make this part of a Query
//                    if(checkIfDynamicEmfClass(object.getClass())) {
                    T read = cdoConverter.read(javaClassType, object);
                    Assert.notNull(read, "CdoConverter returned null while reading EObject");
                    return read;
                }
            } finally {
                if (Objects.nonNull(cdoView))
                    cdoView.close();
            }
        });
        return (T) execute;
    }

    @Override
    public <T> List<T> findAll(Class<T> javaClassType, final String repoResourcePath) {
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");
        final boolean explicitCDOObject = persistentEntity.isExplicitCDOObject();
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

    private static InternalCDORevision readRevision(InternalCDOObject cdoObject) {
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


    protected <T> T doInsert(String repoResourcePath, T objectToSave, CdoWriter<T> writer) {

        Class<?> rawType = ClassUtils.getUserClass(objectToSave);
        CdoPersistentEntity<?> persistentEntity = mappingContext.getRequiredPersistentEntity(rawType);

//        objectToSave = maybeCallBeforeSave(objectToSave);
        EObject internalValue;

        // decide between explicit CDOObjects or custom user-defined objects
        if (persistentEntity.isExplicitCDOObject() || persistentEntity.isLegacyObject()) {
            internalValue = (EObject) objectToSave;
        } else {
            internalValue = (EObject) cdoConverter.getInternalValue(persistentEntity, objectToSave, EObjectModel.class);
        }

        BeforeSaveEvent<T> event = new BeforeSaveEvent<>(objectToSave, internalValue, repoResourcePath);
        maybePublishEvent(event);

        //real physical write:
        T executedResult = execute(session -> {
            CDOID identifier = null;
            try {
                CDOObject cdoObject = CDOUtil.getCDOObject(internalValue);
//                URI uri = EcoreUtil.getURI(internalValue);
//                System.out.println("ecoreURI to save: " + uri);
//                session.getDelegate().getPackageRegistry().putEPackage(cdoObject);
                System.out.println("Session is = " + session);
                CDOTransaction transaction = openTransaction(session);
                System.out.println("CDOTransaction is = " + transaction);
                boolean repoPathExists = true;
                CDOResource resource;
                try {
                    resource = transaction.getResource(repoResourcePath, true);
                    System.out.println("Get resource: " + resource);
                } catch (InvalidURIException e) {
                    repoPathExists = false;
                    resource = transaction.createResource(repoResourcePath);
                    System.out.println("Create new resource: " + resource);
                }

//                resource.getResourceSet().getPackageRegistry().put(null, internalValue);

//                System.out.println("resource.cdoRevision().getID(): " + resource.cdoRevision().getID());
                GeneratingIdAccessor generatingIdAccessor;
                if (persistentEntity.isLegacyObject()) {
                    generatingIdAccessor = new GeneratingIdAccessor(
                            cdoObject,
                            mappingContext.getRequiredPersistentEntity(ClassUtils.getUserClass(cdoObject)),
                            DefaultIdentifierGenerator.INSTANCE,
                            cdoConverter
                    );
                } else {
                    generatingIdAccessor = new GeneratingIdAccessor(
                            objectToSave,
                            persistentEntity,
                            DefaultIdentifierGenerator.INSTANCE,
                            cdoConverter
                    );
                }

                resource.getContents().add(internalValue); //internalValue);
                CDOCommitInfo commit = transaction.commit();

                // only for non-explicit CDOObjects or LegacyCDOObjects: set the CDOID manually for the custom class' ID attribute
                if (!persistentEntity.isExplicitCDOObject() && !persistentEntity.isLegacyObject()) {
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
                return objectToSave;
            } catch (CommitException e) {
                throw new DuplicateKeyException(
                        String.format("Cannot insert existing object with id %s!. Please use update.", identifier), e);
            } finally {
//                session.getDelegate().close();
            }
        });

        //TODO: commit.getTimeStamp() add to entity
        //objectToSave and executedResult

        AfterSaveEvent<?> eventAfter = new AfterSaveEvent<>(executedResult, internalValue, repoResourcePath);
        maybePublishEvent(eventAfter);

        return executedResult;
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
            } catch (CommitException e) {
                e.printStackTrace();
            }
            return CdoDeleteResult.unacknowledged();
        });
    }

    //TODO: this is also a query-typed action
    private <T> CdoDeleteResult doRemove(final Class<T> classType, final String resourcePath) {
        maybePublishEvent(new BeforeDeleteEvent<>(classType, null, resourcePath));

        CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(classType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null.");
        boolean explicitCDOObject = persistentEntity.isExplicitCDOObject();
        boolean isLegacyObject = persistentEntity.isLegacyObject();

        Class<?> classFor;
        CdoPersistentProperty persistentProperty;
        String nsUri = persistentEntity.getNsUri();
        String packageName = persistentEntity.getPackageName();

        if (!explicitCDOObject && !isLegacyObject) {
            persistentProperty = persistentEntity.getRequiredEObjectModelProperty(); //(EObjectModel.class);
//            Assert.isTrue(Objects.nonNull(persistentProperty) && Objects.nonNull(persistentProperty.getClassFor()));
            if (Objects.nonNull(persistentProperty.getClassFor())) {
                classFor = persistentProperty.getClassFor();
            } else {
                classFor = EObject.class; // TODO retrieve the type of the member variable
            }
        } else {
            classFor = EObject.class;
            persistentProperty = null; // not important at this stage
        }

        return execute(session -> {
            try {
                CDOTransaction cdoTransaction = openTransaction(session);
                CDOResource resource;
                resource = cdoTransaction.getResource(resourcePath);
                Collection<EObject> toRemove = new LinkedList<>();
                for (EObject each : resource.getContents()) {
                    if (explicitCDOObject || isLegacyObject) {
                        if (ClassUtils.isAssignable(ClassUtils.getUserClass(each), classType)) { //TODO make this part of a Query
                            toRemove.add(each);
                        }
                    } else {
                        if (objectMatchesCriteria(persistentProperty, each, classFor, nsUri, packageName)) { //TODO make this part of a Query
                            toRemove.add(each);
                        }
                    }
                }
                // Exclusive lock to these objects
                cdoTransaction.lockObjects(toRemove.stream().map(CDOUtil::getCDOObject).collect(Collectors.toList()), IRWLockManager.LockType.WRITE, session.getOptions().getWriteLockoutTimeout());
                boolean b = resource.getContents().removeAll(toRemove);
                Assert.isTrue(b, "Objects were not removed.");
                cdoTransaction.commit();
                return CdoDeleteResult.acknowledged(toRemove.size());
            } catch (InvalidURIException e) { // when the resource path doesn't exists
//                potentiallyConvertRuntimeException(e, exceptionTranslator);
                return CdoDeleteResult.unacknowledged(e);
            } catch (CommitException | InterruptedException e) {
                // when the lock failed  or when commiting e.g., because of concurrent access
                return CdoDeleteResult.unacknowledged(e);
            } finally {
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
            CdoDeleteResult deleteResult;
            CDOTransaction transaction = openTransaction(delegate);
            CDOResource resource;
            CDOID cdoid = null;
            try {
                resource = transaction.getResource(resourcePath);
                EObject remoteCdoObject;
                if (persistentEntity.isExplicitCDOObject()) {
                    //entity shall be equal to remoteCdoObject
                    cdoid = ((CDOObject) entity).cdoID();
                } else if (persistentEntity.isLegacyObject()) {
                    cdoid = CDOUtil.getCDOObject((EObject) entity).cdoID();
                } else {
                    cdoid = (CDOID) persistentEntity.getIdentifierAccessor(entity).getRequiredIdentifier();
                }
                remoteCdoObject = transaction.getObject(cdoid);
                if (Objects.isNull(remoteCdoObject))
                    throw new DataNotFoundException("Object with ID=" + cdoid + " not found.");
                CDOUtil.getCDOObject(remoteCdoObject).cdoWriteLock().lock(delegate.getOptions().getWriteLockoutTimeout());
                EcoreUtil.delete(remoteCdoObject, true);
                resource.getContents().remove(remoteCdoObject);
                CDOCommitInfo commit = transaction.commit();

                // "Whenever an object is detached from the graph it looses all its CDO-specific properties: id, state, view and revision."
                // However, for non-explicit CDO objects we have to unset the ID property manually
                if (!persistentEntity.isExplicitCDOObject() && !persistentEntity.isLegacyObject()) {
                    persistentEntity.getPropertyAccessor(entity).setProperty(persistentEntity.getRequiredIdProperty(), null);
                    // override the "resetted" model property from the entity with the CDO one
                    // the CDO-specific stuff is unsetted already
                    if (ClassUtils.isAssignable(CDOLegacyAdapter.class, remoteCdoObject.getClass())) {
                        remoteCdoObject = CDOUtil.getEObject(remoteCdoObject);
                    } else {
                        remoteCdoObject = CDOUtil.getCDOObject(remoteCdoObject);
                    }
                    persistentEntity.getPropertyAccessor(entity).setProperty(persistentEntity.getRequiredEObjectModelProperty(), remoteCdoObject);
                } else if (persistentEntity.isLegacyObject()) {
                    CdoPersistentEntity<?> persistentEntity0 = mappingContext.getPersistentEntity(CDOUtil.getCDOObject((EObject) entity).getClass());
                    Assert.notNull(persistentEntity0, "Persistent entity of a object in legacy mode must not be null.");
                    persistentEntity0.getPropertyAccessor(CDOUtil.getCDOObject((EObject) entity)).setProperty(persistentEntity0.getRequiredIdProperty(), null);
                }

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

    //TODO:MOVE
    protected boolean checkIfDynamicEmfClass_NonLegacyClass(@Nullable Class<?> o) {
        if (null != o) {
            return DYNAMIC_ECORE_CLASSES.contains(o);
        }
        return false;
    }

    protected void ensureNotIterable(@Nullable Object o) {
        if (null != o) {
            if (o.getClass().isArray() || ITERABLE_CLASSES.contains(o.getClass().getName())) {
                throw new IllegalArgumentException("Cannot use a collection here.");
            }
        }
    }

    protected void ensureIDisCDOID(@NonNull Object o) {
        Class<?> rawType = ClassUtils.getUserClass(o);
        boolean b = Arrays.stream(rawType.getInterfaces()).anyMatch(x -> ClassUtils.isAssignable(CDOID.class, x));
        if (!b) {
            throw new IllegalArgumentException("ID must be of class CDOID.");
        }
    }

    //TODO: later move to "QueryAction" related class ...
    public boolean objectMatchesCriteria(CdoPersistentProperty persistentProperty, EObject each, Class<?> classFor, String nsUri, String packageName) {
        boolean hasMatch;
        //is dynamic ecore class?
        if (checkIfDynamicEmfClass_NonLegacyClass(ClassTypeInformation.from(persistentProperty.getRawType()).getType()) && checkIfDynamicEmfClass_NonLegacyClass(each.getClass())) {
            hasMatch = classFor.getSimpleName().equals(each.eClass().getName());
//                    Arrays.stream(classFor)
//                    .anyMatch(x -> x.getSimpleName().equals(each.eClass().getName()));
        } else if (each instanceof EPackage && (ClassTypeInformation.from(persistentProperty.getRawType()).getType().equals(EPackage.class))) { //is an epackage?
            hasMatch = ((EPackage) each).getNsURI().equals(nsUri);
            if (StringUtils.hasText(packageName))
                hasMatch = hasMatch && ((EPackage) each).getName().equals(packageName);
        } else { // is a concrete user-defined (custom) ecore class (not dynamically created)?
            Class<?> type = ClassTypeInformation.from(persistentProperty.getRawType()).getType();
            hasMatch = ClassUtils.isAssignable(classFor, each.getClass()) && ClassUtils.isAssignable(classFor, type);

//                    Arrays.stream(classFor)
//                    .anyMatch(x -> ClassUtils.isAssignable(x, each.getClass()))
//                    && Arrays.stream(classFor)
//                    .anyMatch(x -> ClassUtils.isAssignable(x, type));
//            hasMatch = Arrays.asList(classFor).contains(each.getClass()) && Arrays.asList(classFor).contains(ClassTypeInformation.from(persistentProperty.getRawType()).getType());
        }
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
