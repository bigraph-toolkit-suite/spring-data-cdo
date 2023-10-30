package org.bigraphs.spring.data.cdo.core.event;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;

/**
 * @author Dominik Grzelak
 */
public class CdoMappingEvent<T> extends ApplicationEvent {
    @Nullable
    private final EObject entity;
    @Nullable
    private final String resourcePath;

    public CdoMappingEvent(T source) {
        this(source, null, null);
    }

    public CdoMappingEvent(T source, @Nullable EObject entity, @Nullable String resourcePath) {
        super(source);
        this.entity = entity;
        this.resourcePath = resourcePath;
    }

    @Nullable
    public EObject getEntity() {
        return entity;
    }

    @Nullable
    public String getResourcePath() {
        return resourcePath;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
