package de.tudresden.inf.st.spring.data.cdo;

import org.eclipse.emf.ecore.EObject;
import org.springframework.data.convert.EntityReader;

/**
 * A {@link CdoReader} is responsible for converting an object of type T to the native CDO representation.
 *
 * @author Dominik Grzelak
 */
public interface CdoReader<T> extends EntityReader<T, EObject> {
}
