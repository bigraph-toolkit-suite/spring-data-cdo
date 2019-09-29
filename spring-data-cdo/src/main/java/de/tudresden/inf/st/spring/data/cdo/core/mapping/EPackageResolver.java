package de.tudresden.inf.st.spring.data.cdo.core.mapping;

import de.tudresden.inf.st.spring.data.cdo.CdoConverter;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import org.eclipse.emf.ecore.EObject;
import org.springframework.lang.Nullable;

/**
 * An {@link EPackageResolver} determines the {@literal epackage instance} a given type is assigned to.
 * A epackage in this context is a specific ecore model.
 *
 * @author Dominik Grzelak
 */
public interface EPackageResolver {

    /**
     * Determine the {@literal EPackage} to use for a given type.
     * <p>
     * //     * @param type must not be {@literal null}.
     *
     * @return
     */
    @Nullable
    <T> EObject resolveEPackageField(CdoPersistentEntity property, T source, CdoConverter converter);
}
