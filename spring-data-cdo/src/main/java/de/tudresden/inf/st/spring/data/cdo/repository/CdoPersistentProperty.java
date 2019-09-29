package de.tudresden.inf.st.spring.data.cdo.repository;

import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import org.eclipse.emf.ecore.EPackage;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * CDO specific {@link org.springframework.data.mapping.PersistentProperty} extension.
 *
 * This is for a property of a class.
 *
 * @author Dominik Grzelak
 */
public interface CdoPersistentProperty extends PersistentProperty<CdoPersistentProperty> {

    @Override
    boolean usePropertyAccess();

    @Nullable
    Class<?> getClassFor();

    boolean isExplicitEPackageProperty();

    String getEPackageName();

    boolean isEPackageProperty();

    EObjectModel getEPackageField();

    /**
     * TODO WIP
     * @return
     */
    Class<?> getEPackageType();

    EPackage getEPackageValue();
}
