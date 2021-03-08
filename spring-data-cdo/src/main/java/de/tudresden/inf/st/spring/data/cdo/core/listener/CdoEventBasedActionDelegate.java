package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.net4j.util.event.IEvent;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoEventBasedActionDelegate extends CdoSessionActionDelegate<IEvent> {
    void perform(IEvent event);
}
