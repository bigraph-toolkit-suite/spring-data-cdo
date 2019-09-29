package de.tudresden.inf.st.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class BeforeDeleteEvent<T> extends CdoMappingEvent<T> {

    public BeforeDeleteEvent(T source) {
        super(source);
    }

    public BeforeDeleteEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
