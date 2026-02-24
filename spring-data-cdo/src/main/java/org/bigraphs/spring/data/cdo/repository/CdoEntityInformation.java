package org.bigraphs.spring.data.cdo.repository;

import org.eclipse.emf.ecore.EPackage;
import org.springframework.data.repository.core.EntityInformation;

/**
 * @author Dominik Grzelak
 */
public interface CdoEntityInformation<T, ID> extends EntityInformation<T, ID> {

    String getPathValue();

    @Override
    boolean isNew(T entity);

    EPackage getContext();

    /**
     * Returns whether the entity uses optimistic locking.
     *
     * @return true if the entity defines a {@link org.springframework.data.annotation.Version} property.
     */
    default boolean isVersioned() {
        return false;
    }

    String getIdAttribute();
}
