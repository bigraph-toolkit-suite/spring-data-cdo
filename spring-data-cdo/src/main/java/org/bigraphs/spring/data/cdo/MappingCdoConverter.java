package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.annotation.EObjectModel;
import org.bigraphs.spring.data.cdo.core.mapping.AnnotationBasedEPackageResolver;
import org.bigraphs.spring.data.cdo.core.mapping.EPackageResolver;
import org.bigraphs.spring.data.cdo.repository.CdoPersistentEntity;
import org.bigraphs.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.internal.cdo.CDOObjectImpl;
import org.eclipse.emf.internal.cdo.object.CDOLegacyAdapter;
import org.eclipse.emf.internal.cdo.object.CDOLegacyWrapper;
import org.eclipse.emf.internal.cdo.object.DynamicCDOObjectImpl;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Dominik Grzelak
 */
public class MappingCdoConverter implements CdoConverter, ApplicationContextAware, InitializingBean {

    private static final Collection<Class> DYNAMIC_ECORE_CDO_CLASSES;
    private static final Collection<Class> CDO_LEGACY_CLASSES;

    static {

        Set<Class> dynamicClasses = new HashSet<>();
        dynamicClasses.add(DynamicCDOObjectImpl.class);
        dynamicClasses.add(DynamicEObjectImpl.class);
        dynamicClasses.add(InternalEObject.class);
        dynamicClasses.add(InternalCDOObject.class);
        dynamicClasses.add(BasicEObjectImpl.class);
        dynamicClasses.add(CDOObjectImpl.class);
        dynamicClasses.add(EObjectImpl.class);
        Set<Class> cdoLegacyClasses = new HashSet<>();
        cdoLegacyClasses.add(CDOLegacyWrapper.class);
        cdoLegacyClasses.add(CDOLegacyAdapter.class);

        DYNAMIC_ECORE_CDO_CLASSES = Collections.unmodifiableCollection(dynamicClasses);
        CDO_LEGACY_CLASSES = Collections.unmodifiableCollection(cdoLegacyClasses);
    }

    private final Objenesis objenesis;

    private final MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> mappingContext;
    private SpELContext spELContext;
    @Nullable
    private ApplicationContext applicationContext;
    private final EPackageResolver ePackageResolver;
    private final Map<Class<?>, Method> methodResolveCache;

    public MappingCdoConverter(MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> mappingContext) {
        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.ePackageResolver = AnnotationBasedEPackageResolver.INSTANCE;
        this.mappingContext = mappingContext;
        this.methodResolveCache = new HashMap<>();
//        this.spELContext = new SpELContext(DocumentPropertyAccessor.INSTANCE);
        this.objenesis = new ObjenesisStd(true);
    }

    public MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return null;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.spELContext = new SpELContext(this.spELContext, applicationContext);
    }


    @Override
    public void write(@NonNull Object obj, EObject ePackage) {
        Class<?> entityType = ClassUtils.getUserClass(obj.getClass());
        TypeInformation<?> type = ClassTypeInformation.from(entityType);
        System.out.println(type);
        System.out.println(entityType);

        //TODO Use conversion service
        //check if the entity needs an additional type such as _class as for json documents needed, for example
        //here is place to add some missing information

    }

    @Override
    public <S extends Object> S read(@NonNull Class<S> javaClassType, EObject cdoObject) {
        final CdoPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(javaClassType);
        Assert.notNull(persistentEntity, "CdoPersistentEntity must not be null");
        final boolean explicitCDOObject = persistentEntity.isNativeCDOObject();
        final CdoPersistentProperty ePackageFieldProperty = !explicitCDOObject ? persistentEntity.getRequiredEObjectModelProperty() : null;
        final CdoPersistentProperty requiredIdProperty = persistentEntity.getRequiredIdProperty();

//        CdoPersistentProperty persistentProperty = persistentEntity.getPersistentProperty(EObjectModel.class);

        Assert.isTrue(Objects.nonNull(ePackageFieldProperty) && Objects.nonNull(ePackageFieldProperty.getClassFor()),
                "The classFor field must not be null");

        S t = objenesis.newInstance(javaClassType);

        cdoObject = CDOUtil.getCDOObject(cdoObject);
//        persistentEntity.getPropertyAccessor(t).setProperty(ePackageFieldProperty, CDOUtil.getCDOObject(cdoObject));
        if (!ClassUtils.isAssignable(CDOLegacyAdapter.class, ClassUtils.getUserClass(cdoObject))) {
            persistentEntity.getPropertyAccessor(t).setProperty(ePackageFieldProperty, cdoObject);
        } else {
            persistentEntity.getPropertyAccessor(t).setProperty(ePackageFieldProperty, ((CDOLegacyAdapter) cdoObject).cdoInternalInstance());
        }

        persistentEntity.getPropertyAccessor(t).setProperty(requiredIdProperty, ((CDOObject) cdoObject).cdoID());

        //we do not care about other properties! this means any user class could map to this
        //we can however also check the nsUri...
        return javaClassType.cast(t);
    }

    public boolean checkIfLegacyEObject(@Nullable Class<?> o) {
        if (Objects.nonNull(o)) {
//            return DYNAMIC_ECORE_CDO_CLASSES.contains(o);
            return CDO_LEGACY_CLASSES.contains(o);
        }
        return false;
    }

    @Override
    public void afterPropertiesSet() {

    }


    //TODO
    @Override
    public Object getInternalValue(PersistentEntity owner, Object source, Class<? extends Annotation> annotation) {
        //use correct resolver class for specific annotation
        if (annotation.equals(EObjectModel.class)) {
            EObject ePackage = ePackageResolver.resolveEPackageField((CdoPersistentEntity) owner, source, this);
            return ePackage;
        }
        return null;
    }

    public Method resolveEPackageFieldAnnotatedMethod(final Class<?> type) {

        if (methodResolveCache.containsKey(type)) {
            return methodResolveCache.get(type);
        }
        //TODO eventuell einfacher mit: mappingContext.getPersistentEntity(type).getPropertyAccessor()
        //  benötigt nur noch bean und name des feldes
        methodResolveCache.put(type, null);
        ReflectionUtils.doWithMethods(type, method -> methodResolveCache.put(type, method),
                method -> ClassUtils.isAssignable(org.eclipse.emf.ecore.EObject.class, method.getReturnType())
                        && AnnotationUtils.findAnnotation(method, EObjectModel.class) != null);

        return methodResolveCache.get(type);
    }

    @Override
    public Method resolveCDOIDMethod(Class<?> type) {
        if (methodResolveCache.containsKey(type)) {
            return methodResolveCache.get(type);
        }
        methodResolveCache.put(type, null);
        ReflectionUtils.doWithMethods(type, method -> methodResolveCache.put(type, method),
                method -> ClassUtils.isAssignable(CDOID.class, method.getReturnType()));
        return methodResolveCache.get(type);
    }
}
