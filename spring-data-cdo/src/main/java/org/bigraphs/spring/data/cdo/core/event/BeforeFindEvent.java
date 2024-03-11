package org.bigraphs.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class BeforeFindEvent<T> extends CdoMappingEvent<T> {

    public BeforeFindEvent(T source) {
        super(source);
    }

    public BeforeFindEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
