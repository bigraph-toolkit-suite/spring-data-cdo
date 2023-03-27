package de.tudresden.inf.st.spring.data.cdo.core.listener;

/**
 * The base interface of any action delegate that is called when a CDO session registered a change.
 * <p>
 * The clas {@link DefaultCdoSessionListener} is responsible for the handling which delegate to call.
 *
 * @author Dominik Grzelak
 * @see DefaultCdoSessionListener
 */
@FunctionalInterface
public interface CdoSessionActionDelegate<T> {
    void perform(T arg);
}
