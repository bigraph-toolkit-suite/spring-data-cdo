package de.tudresden.inf.st.spring.data.cdo.core.listener;

import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.CdoListenerFilter;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;

import java.util.*;

/**
 * @author Dominik Grzelak
 */
final public class DefaultCdoSessionListener implements IListener {
    public static final int NO_TIME_STAMP = 0;
    CdoListenerFilter filter;
    private final Map<IEvent, Long> events = new LinkedHashMap<IEvent, Long>();
    private static final long DEFAULT_TIMEOUT = 3000; // 3 seconds
    private long timeout;
    private String name;
    CdoEventBasedActionDelegate action;

    public DefaultCdoSessionListener setAction(CdoEventBasedActionDelegate action) {
        this.action = action;
        return this;
    }

    public DefaultCdoSessionListener(String name) {
        this(new CdoListenerFilter(), name);
    }

    public DefaultCdoSessionListener(CdoListenerFilter filter, String name) {
        this.name = name;
        this.timeout = DEFAULT_TIMEOUT;
        this.filter = filter;
    }

    @Override
    public void notifyEvent(IEvent event) {
//        System.out.println("Event received: " + event);
        if (isApplicable(event)) {
            long timeStamp = System.currentTimeMillis();
            if (timeStamp == NO_TIME_STAMP) {
                throw new IllegalStateException("Regular time stamp is equal to NO_TIME_STAMP");
            }

            synchronized (this) {
                events.put(event, timeStamp);
                if (filter.getNotifyThreshold() == -1) {
                    if (action != null) {
                        action.perform((CDOSessionInvalidationEvent) event);
                    }
                } else {
                    throw new RuntimeException("Not yet implemented");
                }
            }
        }
    }

    public void clearEvents() {
        synchronized (this) {
            events.clear();
        }
    }

    public List<IEvent> getEvents() {
        synchronized (this) {
            return new ArrayList<IEvent>(events.keySet());
        }
    }

    public boolean isApplicable(IEvent event) {
        if (filter.getEventClasses().isEmpty()) {
            return true;
        }

        Class<? extends IEvent> theClass = event.getClass();
        for (Class<? extends IEvent> eventClass : filter.getEventClasses()) {
            if (eventClass.isAssignableFrom(theClass)) {
                return true;
            }
        }
        return false;
    }
}
