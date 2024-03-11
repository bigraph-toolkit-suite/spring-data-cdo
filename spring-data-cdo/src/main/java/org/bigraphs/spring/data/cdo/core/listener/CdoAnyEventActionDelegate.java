package org.bigraphs.spring.data.cdo.core.listener;

import org.eclipse.net4j.util.event.IEvent;

import java.util.Map;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoAnyEventActionDelegate extends CdoSessionActionDelegate<IEvent> {
    @Override
    default void perform(IEvent event) {
        this.perform(event, null);
    }

    /**
     * Similar to {@link #perform(IEvent)} but accepts additional properties to be evaluated later.
     *
     * @param arg        the objects that are <i>new</i>
     * @param properties additional properties to evaluate
     */
    void perform(IEvent arg, Map<String, Object> properties);
}
