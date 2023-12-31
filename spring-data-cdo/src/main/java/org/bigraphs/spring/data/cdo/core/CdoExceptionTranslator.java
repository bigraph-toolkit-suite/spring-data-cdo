package org.bigraphs.spring.data.cdo.core;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Implementation of a {@link PersistenceExceptionTranslator} for CDO.
 * <p>
 * Translates CDO exceptions to exceptions in Spring's portable `DataAccessException` hierarchy for data access classes
 * annotated with the `@Repository` annotation. Translates the given CDO runtime exception to an appropriate exceptions
 * in Spring's portable {@link DataAccessException} hierarchy.
 * <p>
 * Returns {@code null} if no translation is appropriate: any other exception may have resulted from user code,
 * and should not be translated.
 *
 * @author Dominik Grzelak
 */
public class CdoExceptionTranslator implements PersistenceExceptionTranslator {

    //TODO: see mongodb reference implementation

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException e) {
        return null;
    }
}
