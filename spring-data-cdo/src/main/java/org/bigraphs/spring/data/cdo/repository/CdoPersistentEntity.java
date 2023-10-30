package org.bigraphs.spring.data.cdo.repository;

import org.eclipse.emf.ecore.EPackage;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Dominik Grzelak
 */
public interface CdoPersistentEntity<T> extends PersistentEntity<T, CdoPersistentProperty> {

    @Nullable
    String getResourcePath();

    @Nullable
    String getNsUri();

    @Nullable
    String getPackageName();

    /**
     * Necessary for queries. A context (e.g., the EPackage of the entity) must be provided.
     *
     * @return the context/EPackage
     */
    EPackage getContext();

    @Nullable
    CdoPersistentProperty getEObjectModelProperty();

    @NonNull
    CdoPersistentProperty getRequiredEObjectModelProperty();

    /**
     * Indicates whether the object to be persisted is directly a CDOObject.
     * That means, that the supertype is a {@link org.eclipse.emf.cdo.CDOObject}.
     *
     * @return
     */
    boolean isNativeCDOObject();

    boolean isLegacyObject();

    /**
     * @return {@code true} if the entity is either a native CDO object or in legacy mode (but still a standard EObject),
     * otherwise {@code false} is returned
     */
    default boolean isNativeCdoOrLegacyMode() {
        return isNativeCDOObject() || isLegacyObject();
    }

    boolean hasEObjectModelProperty();

    boolean hasCDOAnnotation();

    @Override
    CdoPersistentProperty getIdProperty();
}
