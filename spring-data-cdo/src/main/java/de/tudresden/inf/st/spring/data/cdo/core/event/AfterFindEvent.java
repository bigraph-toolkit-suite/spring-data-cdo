package de.tudresden.inf.st.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class AfterFindEvent<T> extends CdoMappingEvent<T> {

    public AfterFindEvent(T source) {
        super(source);
    }

    public AfterFindEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
