package org.bigraphs.spring.data.cdo.core;

import org.bigraphs.spring.data.cdo.CdoTemplate;
import org.bigraphs.spring.data.cdo.CdoClientSession;

/**
 * Generic callback interface for CDO 'low level' code that operates on a {@link CdoClientSession}.
 * "This is particularly useful for delegating code that needs to work closely on the underlying clientsession implementation."
 * <p>
 * Usage:
 * To be used with {@link CdoTemplate} execution methods, often as
 * anonymous classes within a method implementation. Usually, used for chaining several operations together (
 * {@code get/set/trim etc...}.
 *
 * @author Dominik Grzelak
 */
public interface CdoCallback<T> {
    /**
     * Gets called by {@code CdoTemplate#execute(CdoCallback)}. Allows for returning a result object created
     * within the callback, i.e. a domain object or a collection of domain objects.
     *
     * @param session the CDO session where the operation takes place
     * @return
     */
    T doInCdo(CdoClientSession session);
}
