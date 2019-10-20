package de.tudresden.inf.st.spring.data.cdo.core.mapping;

import de.tudresden.inf.st.spring.data.cdo.MappingCdoConverter;
import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.ecore.InternalEObject;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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

    private MappingCdoConverter cdoConverter;
    private boolean isExplicitCdoObject = false;
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
        String fallbackNsUri = StringUtils.uncapitalize(rawType.getSimpleName());
        String fallbackEPackageName = ""; // empty string fallback 'cause this is rather an optional element
        determineIfCdoObject(rawType);

        //TODO valid resourcepath also: cdo://repo1/sample
        //add also a boolean hasRepoInPath() to be checked from cdotemplate later: hat vorrang!
        //find the resource path of the object
        if (this.isAnnotationPresent(CDO.class)) {
            CDO document = this.getRequiredAnnotation(CDO.class);
            this.resourcePath = StringUtils.hasText(document.path()) ? document.path() : fallback;
            this.expression = detectExpression(document.path());
            this.expressionNsUri = detectExpression(document.nsUri());
            this.expressionEPackageName = detectExpression(document.packageName());
            this.nsUri = StringUtils.hasText(document.nsUri()) ? document.nsUri() : fallbackNsUri;
            this.packageNameValue = StringUtils.hasText(document.packageName()) ? document.packageName() : fallbackEPackageName;
        } else {
            this.resourcePath = fallback;
            this.nsUri = fallbackNsUri;
            this.packageNameValue = fallbackEPackageName;
            this.expression = null;
            this.expressionNsUri = null;
            this.expressionEPackageName = null;
            if (!isExplicitCDOObject() && (ClassUtils.isAssignable(InternalEObject.class, rawType) || ClassUtils.isAssignable(CDOLegacyAdapter.class, rawType))) {
                this.isLegacyObject = true;
            }
        }
    }

    private void determineIfCdoObject(Class<?> rawType) {
        Class<?>[] allInterfacesForClass = ClassUtils.getAllInterfacesForClass(rawType);
        if (allInterfacesForClass.length > 0) {
            for (Class<?> each : allInterfacesForClass) {
                if (each.equals(InternalCDOObject.class) && !ClassUtils.isAssignable(CDOLegacyAdapter.class, rawType)) {
                    isExplicitCdoObject = true;
                    break;
                }
            }
        }
    }

    @Override
    public boolean hasIdProperty() {
        if (isExplicitCDOObject() && getPersistentProperty("revision") != null) {//TODO string!
            return true;
        }
        return super.hasIdProperty();
    }

    @Override
    public CdoPersistentProperty getIdProperty() {
        if (isExplicitCDOObject()) {
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
        getRequiredAnnotation(CDO.class); //the annotation @EObjectModel should only be used with @CDO
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
    public boolean isExplicitCDOObject() {
        return isExplicitCdoObject;
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
