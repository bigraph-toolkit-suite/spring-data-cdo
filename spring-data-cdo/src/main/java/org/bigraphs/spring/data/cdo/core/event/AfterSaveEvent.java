package org.bigraphs.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class AfterSaveEvent<T> extends CdoMappingEvent<T> {

    public AfterSaveEvent(T source) {
        super(source);
    }

    public AfterSaveEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
