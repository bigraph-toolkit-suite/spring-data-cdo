package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;

import java.util.List;

/**
 * @author Dominik Grzelak
 */
@FunctionalInterface
public interface CdoNewObjectsActionDelegate extends CdoSessionActionDelegate<List<CDOIDAndVersion>> {
    @Override
    void perform(List<CDOIDAndVersion> arg);
}
