package de.tudresden.inf.st.spring.data.cdo.repository;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Dominik Grzelak
 */
public interface CdoPersistentEntity<T> extends PersistentEntity<T, CdoPersistentProperty> {

    @Nullable
    String getResourcePath();

    String getNsUri();

    String getPackageName();

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
    boolean isExplicitCDOObject();

    boolean hasEObjectModelProperty();

    @Override
    CdoPersistentProperty getIdProperty();
}
