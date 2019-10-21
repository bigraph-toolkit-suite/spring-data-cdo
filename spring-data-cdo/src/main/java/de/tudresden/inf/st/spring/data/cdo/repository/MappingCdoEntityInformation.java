package de.tudresden.inf.st.spring.data.cdo.repository;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author Dominik Grzelak
 */
public class MappingCdoEntityInformation<T, ID> extends PersistentEntityInformation<T, ID> implements CdoEntityInformation<T, ID> {

    private final CdoPersistentEntity<T> persistentEntityMetadata;
    private final String customResourcePath;
    private final Class<ID> fallbackIdType;
    private final Function<Object, Object> valueLookup;
    private final @Nullable
    Class<?> valueType;

    public MappingCdoEntityInformation(CdoPersistentEntity<T> entity) {
        this(entity, null, null);
    }

    /**
     * Creates a new {@link MappingCdoEntityInformation} for the given {@link CdoPersistentEntity} and fallback
     * identifier type.
     *
     * @param entity         must not be {@literal null}.
     * @param fallbackIdType can be {@literal null}.
     */
    public MappingCdoEntityInformation(CdoPersistentEntity<T> entity, @Nullable Class<ID> fallbackIdType) {
        this(entity, null, fallbackIdType);
    }

    public MappingCdoEntityInformation(CdoPersistentEntity<T> entity, String customResourcePath, Class<ID> idType) {
        super(entity);

        this.persistentEntityMetadata = entity;
        this.customResourcePath = customResourcePath;
        this.fallbackIdType = idType != null ? idType : (Class<ID>) CDOID.class;

        boolean idOnly = true;
        this.valueLookup = entity.hasVersionProperty() && !idOnly //
                ? source -> entity.getPropertyAccessor(source).getProperty(entity.getRequiredVersionProperty())
                : source -> entity.getIdentifierAccessor(source).getIdentifier();

        this.valueType = entity.hasVersionProperty() && !idOnly //
                ? entity.getRequiredVersionProperty().getType() //
                : entity.hasIdProperty() ? entity.getRequiredIdProperty().getType() : null;
    }

    @Override
    public boolean isNew(T entity) {
        if (persistentEntityMetadata.isInheritedCDOObject() || persistentEntityMetadata.isInheritedLegacyObject()) {

            Object value = valueLookup.apply(entity);
            if (value == null) {
                return true;
            }

            if (valueType != null && !valueType.isPrimitive()) {
//                if(valueType instanceof StubCDORevision)
                if (ClassUtils.isAssignable(valueType, InternalCDORevision.class)) {
                    return Objects.isNull(((InternalCDORevision) value).getID());
                }
            }

            throw new IllegalArgumentException(
                    String.format("Could not determine whether %s is new! Unsupported identifier or version property!", entity)
            );
        }
//        persistentEntityMetadata.isNew(entity);
        return super.isNew(entity);
    }

    public String getPathValue() {
        return customResourcePath == null ? persistentEntityMetadata.getResourcePath() : customResourcePath;
    }

    public String getIdAttribute() {
        return persistentEntityMetadata.getRequiredIdProperty().getName();
    }

//    @Override
//    public EPackage getEPackage() {
//        return persistentEntityMetadata.getRequiredEPackageValue();
//    }

    @Override
    public Class<ID> getIdType() {
        if (this.persistentEntityMetadata.hasIdProperty()) {
            return super.getIdType();
        }
        return fallbackIdType;
    }
}
