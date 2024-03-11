package org.bigraphs.spring.data.cdo.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.GenericTypeResolver;

/**
 * A subclass can override the appropriate methods for listening on specific events.
 * <p>
 * All available events are contained in this package. For example:
 * <ul>
 *     <li>{@link AfterDeleteEvent}</li>
 *     <li>{@link BeforeSaveEvent}</li>
 *     <li>etc.</li>
 * </ul>
 *
 * @author Dominik Grzelak
 */
public abstract class AbstractCdoEventListener<E> implements ApplicationListener<CdoMappingEvent<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCdoEventListener.class);
    private final Class<?> domainClass;

    public AbstractCdoEventListener() {
        Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(this.getClass(), AbstractCdoEventListener.class);
        this.domainClass = typeArgument == null ? Object.class : typeArgument;
    }

    @Override
    public void onApplicationEvent(CdoMappingEvent<?> cdoMappingEvent) {

//        Object source = cdoMappingEvent.getSource();
//        // Check for matching domain type and invoke callbacks
//        if (source != null && !domainClass.isAssignableFrom(source.getClass())) {
//            return;
//        }

        if (cdoMappingEvent instanceof BeforeDeleteEvent) {
            onBeforeDeleteEvent((BeforeDeleteEvent<E>) cdoMappingEvent);
        } else if (cdoMappingEvent instanceof AfterFindEvent) {
            onAfterFindEvent((AfterFindEvent<E>) cdoMappingEvent);
        } else if (cdoMappingEvent instanceof AfterSaveEvent) {
            onAfterSaveEvent((AfterSaveEvent<E>) cdoMappingEvent);
        } else if (cdoMappingEvent instanceof AfterDeleteEvent) {
            onAfterDeleteEvent((AfterDeleteEvent<E>) cdoMappingEvent);
        } else if (cdoMappingEvent instanceof BeforeFindEvent) {
            onBeforeFindEvent((BeforeFindEvent<E>) cdoMappingEvent);
        } else if (cdoMappingEvent instanceof BeforeSaveEvent) {
            onBeforeSaveEvent((BeforeSaveEvent<E>) cdoMappingEvent);
        }
    }

    public void onAfterSaveEvent(AfterSaveEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onAfterSaveEvent({})", event.getSource());
        }
    }

    public void onAfterDeleteEvent(AfterDeleteEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onAfterDeleteEvent({})", event.getSource());
        }
    }

    public void onAfterFindEvent(AfterFindEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onAfterFindEvent({})", event.getSource());
        }
    }

    public void onBeforeDeleteEvent(BeforeDeleteEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onBeforeDeleteEvent({})", event.getSource());
        }
    }

    public void onBeforeFindEvent(BeforeFindEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onBeforeFindEvent({})", event.getSource());
        }
    }

    public void onBeforeSaveEvent(BeforeSaveEvent<E> event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onBeforeSaveEvent({})", event.getSource());
        }
    }
}
