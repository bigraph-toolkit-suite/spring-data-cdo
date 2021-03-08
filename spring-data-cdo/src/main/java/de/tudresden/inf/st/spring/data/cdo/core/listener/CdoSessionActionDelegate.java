package de.tudresden.inf.st.spring.data.cdo.core.listener;

/**
 * @author Dominik Grzelak
 * @see DefaultCdoSessionListener
 */
@FunctionalInterface
public interface CdoSessionActionDelegate<T> {
    void perform(T arg);
}
