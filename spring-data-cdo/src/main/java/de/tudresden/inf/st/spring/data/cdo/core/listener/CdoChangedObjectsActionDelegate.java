package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import java.util.List;
import java.util.Map;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoChangedObjectsActionDelegate extends CdoSessionActionDelegate<List<CDORevisionKey>> {
    @Override
    default void perform(List<CDORevisionKey> arg) {
        this.perform(arg, null);
    }

    /**
     * Similar to {@link #perform(List)} but accepts additional properties to be evaluated later.
     *
     * @param arg        the objects that are <i>new</i>
     * @param properties additional properties to evaluate
     */
    void perform(List<CDORevisionKey> arg, Map<String, Object> properties);
}
