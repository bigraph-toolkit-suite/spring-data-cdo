package de.tudresden.inf.st.spring.data.cdo.core.listener;

import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.CdoListenerFilter;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    List<CdoSessionActionDelegate<?>> actions;

    public DefaultCdoSessionListener setAction(List<CdoSessionActionDelegate<?>> action) {
        this.actions = action;
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
                    if (actions != null && actions.size() > 0) {
                        dispatchActionDelegate(actions, event);
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

    void dispatchActionDelegate(List<CdoSessionActionDelegate<?>> delegates, IEvent event) {
        for (CdoSessionActionDelegate<?> delegate : delegates) {
            if (delegate instanceof CdoEventBasedActionDelegate) {
                ((CdoEventBasedActionDelegate) delegate).perform(event);
            } else if (delegate instanceof CdoChangedObjectsActionDelegate &&
                    event instanceof CDOSessionInvalidationEvent) {
                CDOSessionInvalidationEvent invalidationEvent = (CDOSessionInvalidationEvent) event;
                List<CDORevisionKey> changedObjects = invalidationEvent.getChangedObjects();
                ((CdoChangedObjectsActionDelegate) delegate).perform(changedObjects);
            } else if (delegate instanceof CdoNewObjectsActionDelegate &&
                    event instanceof CDOSessionInvalidationEvent) {
                CDOSessionInvalidationEvent invalidationEvent = (CDOSessionInvalidationEvent) event;
                List<CDOIDAndVersion> newObjects = invalidationEvent.getNewObjects();
                ((CdoNewObjectsActionDelegate) delegate).perform(newObjects);
            }
        }
    }
}
