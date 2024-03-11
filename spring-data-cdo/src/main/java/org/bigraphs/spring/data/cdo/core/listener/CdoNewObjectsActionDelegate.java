package org.bigraphs.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;

import java.util.List;
import java.util.Map;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoNewObjectsActionDelegate extends CdoSessionActionDelegate<List<CDOIDAndVersion>> {
    /**
     * Default implementation of {@link CdoSessionActionDelegate#perform(Object)} that calls {@link #perform(List, Map)} here
     * where no additional properties are passed, i.e., this object is always {@code null} later.
     *
     * @param arg the objects that are <i>new</i>
     */
    @Override
    default void perform(List<CDOIDAndVersion> arg) {
        this.perform(arg, null);
    }

    /**
     * Similar to {@link #perform(List)} but accepts additional properties to be evaluated later.
     *
     * @param arg        the objects that are <i>new</i>
     * @param properties additional properties to evaluate
     */
    void perform(List<CDOIDAndVersion> arg, Map<String, Object> properties);
}
