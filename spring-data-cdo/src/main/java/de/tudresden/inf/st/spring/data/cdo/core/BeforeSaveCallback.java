package de.tudresden.inf.st.spring.data.cdo.core;

import org.springframework.data.mapping.callback.EntityCallback;

/**
 * Callback being invoked before a domain object is persisted.
 *
 * @author Dominik Grzelak
 */
//@FunctionalInterface
public interface BeforeSaveCallback<T> extends EntityCallback<T> {

    /**
     * Entity callback method invoked before a domain object is persisted.
     * Can return either the same or a modified instance of the domain object.
     *
     * @param entity the domain object to save.
     * @return the domain object to be persisted.
     */
    T onBeforeSave(T entity);
}
