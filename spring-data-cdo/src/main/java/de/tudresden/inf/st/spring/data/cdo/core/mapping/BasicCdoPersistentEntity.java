package de.tudresden.inf.st.spring.data.cdo.core.mapping;

import de.tudresden.inf.st.spring.data.cdo.MappingCdoConverter;
import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.internal.cdo.object.CDOLegacyAdapter;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author Dominik Grzelak
 */
public class BasicCdoPersistentEntity<T> extends BasicPersistentEntity<T, CdoPersistentProperty>
        implements CdoPersistentEntity<T> {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final String resourcePath;
    @Nullable
    private final Expression expression;
    @Nullable
    private final Expression expressionNsUri;
    @Nullable
    private final Expression expressionEPackageName;

    private EPackage ePackage;

    private MappingCdoConverter cdoConverter;
    private boolean isInheritedCdoObject = false;
    private boolean isLegacyObject = false;
    private final String nsUri;
    private final String packageNameValue;

    public BasicCdoPersistentEntity(TypeInformation<T> typeInformation, MappingCdoConverter cdoConverter) {
        this(typeInformation, cdoConverter, null); //(o1, o2) -> o1.equals(o2) ? 0 : -1);
    }

    public BasicCdoPersistentEntity(TypeInformation<T> typeInformation, MappingCdoConverter cdoConverter, Comparator<CdoPersistentProperty> comparator) {
        super(typeInformation, comparator);

        this.cdoConverter = cdoConverter;
//        ePackageResolver = AnnotationBasedEPackageResolver.INSTANCE;
        Class<?> rawType = typeInformation.getType();
        String fallback = StringUtils.uncapitalize(rawType.getSimpleName());
        String fallbackNsUri = ""; // empty string fallback 'cause this is rather an optional element
        String fallbackEPackageName = ""; // empty string fallback 'cause this is rather an optional element

        determineIfNativeCdoObject(rawType);

        //TODO valid resourcepath also: cdo://repo1/sample
        //add also a boolean hasRepoInPath() to be checked from cdotemplate later: hat vorrang!
        //find the resource path of the object
        if (this.hasCDOAnnotation()) {
            CDO document = this.getRequiredAnnotation(CDO.class);
            this.resourcePath = StringUtils.hasText(document.path()) ? document.path() : fallback;
            this.expression = detectExpression(document.path());
            this.expressionNsUri = detectExpression(document.nsUri());
            this.expressionEPackageName = detectExpression(document.packageName());
            this.packageNameValue = StringUtils.hasText(document.packageName()) ? document.packageName() : fallbackEPackageName;
            this.nsUri = StringUtils.hasText(document.nsUri()) ? document.nsUri() : fallbackNsUri;
            Class aClass = document.ePackage();
            this.tryFindEPackage(aClass);
        } else {
            this.resourcePath = fallback;
            this.nsUri = fallbackNsUri;
            this.packageNameValue = fallbackEPackageName;
            this.expression = null;
            this.expressionNsUri = null;
            this.expressionEPackageName = null;
            this.ePackage = null;
        }
    }

    /**
     * Determines whether the entity in question is a native CDO object or a standard EObject (if at all)
     *
     * @param rawType the class type of the entity
     */
    private void determineIfNativeCdoObject(Class<?> rawType) {
        //Check class itself whether it is a native CDO Object
        if (rawType.equals(InternalCDOObject.class) && !ClassUtils.isAssignable(CDOLegacyAdapter.class, rawType)) {
            isInheritedCdoObject = true;
            return;
        } else { // check all interfaces
            Class<?>[] allInterfacesForClass = ClassUtils.getAllInterfacesForClass(rawType);
            if (allInterfacesForClass.length > 0) {
                for (Class<?> each : allInterfacesForClass) {
                    if (each.equals(InternalCDOObject.class) && !ClassUtils.isAssignable(CDOLegacyAdapter.class, rawType)) {
                        isInheritedCdoObject = true;
                        break;
                    }
                }
            }
        }

        // if the entity is not a native CDO object, try to determine whether it is a standard EObject
        if (!isInheritedCdoObject && (ClassUtils.isAssignable(EObject.class, rawType) || ClassUtils.isAssignable(CDOLegacyAdapter.class, rawType))) {
            this.isLegacyObject = true;
        }
    }

    //Source: https://stackoverflow.com/a/49532492
    private void tryFindEPackage(Class ePackageClass) {
        if (ePackageClass.equals(EcorePackage.class)) {
            ePackage = EcorePackage.eINSTANCE;
        } else if (!ePackageClass.equals(Class.class)) {
            ePackage = EPackage.Registry.INSTANCE.getEPackage(this.nsUri);
            if (Objects.isNull(ePackage)) {
                try {
                    //TODO: works only with Java 8
                    Object o = Proxy.newProxyInstance(
                            Thread.currentThread().getContextClassLoader(),
                            new Class[]{ePackageClass},
                            (proxy, method, args) -> {
                                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                                        .getDeclaredConstructor(Class.class);
                                constructor.setAccessible(true);
                                constructor.newInstance(ePackageClass)
                                        .in(ePackageClass)
                                        .unreflectSpecial(method, ePackageClass)
                                        .bindTo(proxy)
                                        .invokeWithArguments();
                                return null;
                            }
                    );
                    ReflectionUtils.getField(ePackageClass.getField("eINSTANCE"), o);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Assert.notNull(ePackage, "Package with nsURI=" + nsUri + " couldn't be found in the EPackage Registry.");
    }

    @Override
    public EPackage getContext() {
        return ePackage;
    }

    @Override
    public boolean hasCDOAnnotation() {
        return isAnnotationPresent(CDO.class);
    }


    @Override
    public boolean hasIdProperty() {
        if (isNativeCDOObject() && getPersistentProperty("revision") != null) {//TODO string!
            return true;
        }
        return super.hasIdProperty();
    }

    @Override
    public CdoPersistentProperty getIdProperty() {
        if (isNativeCDOObject()) {
            return getPersistentProperty("revision"); //TODO string!
        } else if (isLegacyObject()) {
            return getPersistentProperty("idOrRevision"); //TODO string!
        } else {
            return super.getIdProperty();
        }
    }


    @Nullable
    private static Expression detectExpression(@Nullable String potentialExpression) {
        if (!StringUtils.hasText(potentialExpression)) {
            return null;
        }
        Expression expression = PARSER.parseExpression(potentialExpression, ParserContext.TEMPLATE_EXPRESSION);
        return expression instanceof LiteralExpression ? null : expression;
    }

    @Nullable
    @Override
    public String getResourcePath() {
        return expression == null //
                ? resourcePath //
                : expression.getValue(getEvaluationContext(null), String.class);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.model.BasicPersistentEntity#getEvaluationContext(java.lang.Object)
     */
    @Override
    public EvaluationContext getEvaluationContext(Object rootObject) {
        return super.getEvaluationContext(rootObject);
    }

    @Override
    public String getNsUri() {
        return expressionNsUri == null //
                ? nsUri //
                : expressionNsUri.getValue(getEvaluationContext(null), String.class);
    }

    @Override
    public String getPackageName() {
        return expressionEPackageName == null //
                ? packageNameValue //
                : expressionEPackageName.getValue(getEvaluationContext(null), String.class);
    }

    @Nullable
    @Override
    public CdoPersistentProperty getEObjectModelProperty() {
//        getRequiredAnnotation(CDO.class); //the annotation @EObjectModel should only be used with @CDO, //TODO
        return getPersistentProperty(EObjectModel.class);
    }

    @NonNull
    @Override
    public CdoPersistentProperty getRequiredEObjectModelProperty() {
        CdoPersistentProperty ePackageFieldProperty = getEObjectModelProperty();
        if (ePackageFieldProperty == null) {
            throw new IllegalStateException(String.format("Required property %s not found for %s!", EObjectModel.class,
                    this.getType()));
        }
        return ePackageFieldProperty;
    }

    @Override
    public boolean isNativeCDOObject() {
        return isInheritedCdoObject;
    }

    @Override
    public boolean isLegacyObject() {
        return isLegacyObject;
    }

    @Override
    public boolean hasEObjectModelProperty() {
        return Objects.nonNull(getEObjectModelProperty());
    }


}
