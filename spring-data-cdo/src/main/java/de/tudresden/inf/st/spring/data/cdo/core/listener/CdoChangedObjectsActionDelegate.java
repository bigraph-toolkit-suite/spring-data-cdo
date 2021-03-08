package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.common.revision.CDORevisionKey;

import java.util.List;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoChangedObjectsActionDelegate extends CdoSessionActionDelegate<List<CDORevisionKey>> {
    @Override
    void perform(List<CDORevisionKey> arg);
}
