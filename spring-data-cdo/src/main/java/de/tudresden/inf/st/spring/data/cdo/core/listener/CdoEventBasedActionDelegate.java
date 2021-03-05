package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.net4j.util.event.IEvent;

/**
 *
 * @author Dominik Grzelak
 */
public interface CdoEventBasedActionDelegate extends CdoSessionActionDelegate {
    void perform(IEvent event);
}
