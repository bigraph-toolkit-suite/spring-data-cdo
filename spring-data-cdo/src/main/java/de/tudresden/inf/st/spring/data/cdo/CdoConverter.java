package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.core.ValueResolver;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.ecore.EObject;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

/**
 * @author Dominik Grzelak
 */
public interface CdoConverter
        extends CdoWriter<Object>,
        CdoReader<Object>,
        ValueResolver,
        EntityConverter<CdoPersistentEntity<?>, CdoPersistentProperty, Object, EObject> {

    @Override
    MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> getMappingContext();

    Method resolveEPackageFieldAnnotatedMethod(final Class<?> type);

    Method resolveCDOIDMethod(final Class<?> type);

    boolean checkIfLegacyEObject(@Nullable Class<?> o);
}
