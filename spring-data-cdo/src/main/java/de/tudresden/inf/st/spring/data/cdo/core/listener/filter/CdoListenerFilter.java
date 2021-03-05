package de.tudresden.inf.st.spring.data.cdo.core.listener.filter;

import de.tudresden.inf.st.spring.data.cdo.InvalidCdoApiUsageException;
import org.eclipse.net4j.util.event.IEvent;
import org.springframework.util.Assert;

import java.util.*;

/**
 *
 * @author Dominik Grzelak
 */
public class CdoListenerFilter {
    private final Map<String, FilterCriteria> criteria = new LinkedHashMap<>();
    private Collection<Class<? extends IEvent>> eventClasses;
    private static final Set<Class<? extends IEvent>> NO_EVENT_CLASSES = Collections.emptySet();
    private int notifyThreshold = -1;

    public static CdoListenerFilter filter(FilterCriteria filterCriteriaDef) {
        return new CdoListenerFilter(filterCriteriaDef);
    }

    public CdoListenerFilter() {
    }

    public CdoListenerFilter(FilterCriteria filterCriteriaDef) {
        addCriteria(filterCriteriaDef);
    }

    public CdoListenerFilter restrict(Collection<Class<? extends IEvent>> eventClasses) {
        if (this.eventClasses == null) {
            this.eventClasses = new LinkedList<>();
        }
        if (eventClasses != null) {
            this.eventClasses.addAll(eventClasses);
        }
        return this;
    }

    public CdoListenerFilter restrict(Class<? extends IEvent> eventClass, Class<? extends IEvent>... additionalClasses) {
        Assert.notNull(eventClass, "eventClass must not be null");
        Assert.notNull(additionalClasses, "additionalClasses must not be null");

        restrict(Collections.singleton(eventClass));
        if (additionalClasses.length != 0)
            restrict(Arrays.asList(additionalClasses));
        return this;
    }


    /**
     * Sets the number of events to {@code notifyThreshold} that must occur before the action is called.
     *
     * @param notifyThreshold
     * @return
     */
    public CdoListenerFilter notifyThreshold(int notifyThreshold) {
        this.notifyThreshold = notifyThreshold;
        return this;
    }

    public int getNotifyThreshold() {
        return notifyThreshold;
    }

    public Collection<Class<? extends IEvent>> getEventClasses() {
        return eventClasses != null ? eventClasses : NO_EVENT_CLASSES;
    }

    public CdoListenerFilter addCriteria(FilterCriteria criteriaDefinition) {
        FilterCriteria existing = this.criteria.get(criteriaDefinition.getKey());
        String key = criteriaDefinition.getKey();

        if (existing == null) {
            this.criteria.put(key, criteriaDefinition);
        } else {
            throw new InvalidCdoApiUsageException(
                    String.format("You can't add a second '%s' criteria because the CdoListenerFilter already contains '%s'", key, existing));
        }
        return this;
    }
}
