package org.bigraphs.spring.data.cdo.core.mapping;

import org.bigraphs.spring.data.cdo.annotation.EObjectModel;
import org.bigraphs.spring.data.cdo.repository.CdoPersistentEntity;
import org.bigraphs.spring.data.cdo.repository.CdoPersistentProperty;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.*;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * CDO specific {@link org.springframework.data.mapping.PersistentProperty} implementation.
 * <p>
 * Represents all annotation used for properties of a class
 *
 * @author Dominik Grzelak
 */
public class BasicCdoPersistentProperty extends AnnotationBasedPersistentProperty<CdoPersistentProperty>
        implements CdoPersistentProperty {

    private static final String ID_FIELD_NAME = "_id";
    private static final Set<Class<?>> SUPPORTED_ID_TYPES = new HashSet<>();
    private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<>();
    private Class<?> classFor;

    static {

//        SUPPORTED_ID_TYPES.add(ObjectId.class);
        SUPPORTED_ID_TYPES.add(String.class);
        SUPPORTED_ID_TYPES.add(BigInteger.class);

        SUPPORTED_ID_PROPERTY_NAMES.add("id");
        SUPPORTED_ID_PROPERTY_NAMES.add("_id");
    }

    private final FieldNamingStrategy fieldNamingStrategy;

    public BasicCdoPersistentProperty(Property property, CdoPersistentEntity<?> owner,
                                      SimpleTypeHolder simpleTypeHolder,
                                      @Nullable FieldNamingStrategy fieldNamingStrategy) {

        super(property, owner, simpleTypeHolder);
        this.fieldNamingStrategy = fieldNamingStrategy == null ? PropertyNameFieldNamingStrategy.INSTANCE
                : fieldNamingStrategy;

        if (!owner.isNativeCDOObject()) {

            EObjectModel field = findAnnotation(EObjectModel.class);
            if (Objects.nonNull(field)) {
                classFor = field.ofClass();
            }
        }
    }

    @Override
    public boolean isIdProperty() {
        return super.isIdProperty();
    }

    @Nullable
    @Override
    public Class<?> getClassFor() {
        return classFor;
    }

    @Override
    public boolean isExplicitEPackageProperty() {
        return isAnnotationPresent(EObjectModel.class);
    }

    public String getFieldName() {
        if (isIdProperty()) {
            if (getOwner().getIdProperty() == null) {
                return ID_FIELD_NAME;
            }

            if (getOwner().isIdProperty(this)) {
                return ID_FIELD_NAME;
            }
        }

        String fieldName = fieldNamingStrategy.getFieldName(this);

        if (!StringUtils.hasText(fieldName)) {
            throw new MappingException(String.format("Invalid (null or empty) field name returned for property %s by %s!",
                    this, fieldNamingStrategy.getClass()));
        }

        return fieldName;
    }

    @Override
    public String getEPackageName() {
        String fieldName = fieldNamingStrategy.getFieldName(this);

        if (!StringUtils.hasText(fieldName)) {
            throw new MappingException(String.format("Invalid (null or empty) field name returned for property %s by %s!",
                    this, fieldNamingStrategy.getClass()));
        }

        return fieldName;
    }

    @Override
    public boolean isEPackageProperty() {
        return isAnnotationPresent(EObjectModel.class);
    }

    @Override
    public EObjectModel getEPackageField() {
        return findAnnotation(EObjectModel.class);
    }

    @Override
    public Class<?> getEPackageType() {
        EObjectModel annotation = findAnnotation(EObjectModel.class);
        if (!isIdProperty()) {
            if (annotation == null) {
                return getType();
            }
            return annotation.getClass();
        }
        return null;
    }

    @Override
    protected Association<CdoPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }
}
