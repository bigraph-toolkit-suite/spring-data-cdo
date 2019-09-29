package de.tudresden.inf.st.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;

/**
 * @author Dominik Grzelak
 */
public class BeforeSaveEvent<T> extends CdoMappingEvent<T> {

    public BeforeSaveEvent(T source, EObject entity, String resourcePath) {
        super(source, entity, resourcePath);
    }
}
