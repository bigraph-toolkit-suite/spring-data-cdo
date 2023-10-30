package org.bigraphs.spring.data.cdo;

import org.eclipse.emf.ecore.EObject;
import org.springframework.data.convert.EntityWriter;

/**
 * A {@link CdoWriter} is responsible for converting an object of type T to the native CDO representation CDOObject.
 *
 * @author Dominik Grzelak
 */
public interface CdoWriter<T> extends EntityWriter<T, EObject> {

    @Override
    void write(T t, EObject ePackage);
}
