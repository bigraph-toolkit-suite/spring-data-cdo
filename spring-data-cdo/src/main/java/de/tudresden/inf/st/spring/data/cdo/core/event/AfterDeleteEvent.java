package de.tudresden.inf.st.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class AfterDeleteEvent<T> extends CdoMappingEvent<T> {

    public AfterDeleteEvent(T source) {
        super(source);
    }

    public AfterDeleteEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
