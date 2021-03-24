package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.CDOAdapter;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;

import java.util.Collections;
import java.util.List;

/**
 * @author Dominik Grzelak
 */
public class ResourceContentAdapter extends AdapterImpl implements CDOAdapter {
    List<CdoNewObjectsActionDelegate> delegates;

    public ResourceContentAdapter(List<CdoNewObjectsActionDelegate> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void notifyChanged(Notification msg) {
        if (msg.getEventType() == 3) { //ADD
            CDORevision cdoRevision = CDOUtil.getCDOObject((EObject) (msg).getNewValue()).cdoRevision(true);
            if (cdoRevision != null) {
                for (CdoNewObjectsActionDelegate each : delegates) {
                    each.perform(Collections.singletonList(cdoRevision));
                }
            }
        }
        if (msg.getEventType() == 4) { //REMOVE

        }
    }
}