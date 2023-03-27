package de.tudresden.inf.st.spring.data.cdo.core.listener;

import org.eclipse.emf.cdo.CDOAdapter;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.INotifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Dominik Grzelak
 */
public class ResourceContentAdapter extends AdapterImpl implements CDOAdapter {
    List<CdoSessionActionDelegate<?>> delegates;
    HashMap<String, Object> properties = new LinkedHashMap<>();

    public ResourceContentAdapter(List<CdoSessionActionDelegate<?>> delegates) {
        this.delegates = delegates;
    }

    public HashMap<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void notifyChanged(Notification msg) {
        if (msg.getEventType() == 3) { //ADD, CDODeltaNotificationImpl
            if (msg.getNotifier() instanceof CDOResource) {
                String repoPath = ((CDOResource) msg.getNotifier()).getPath();
            }
            CDORevision cdoRevision = CDOUtil.getCDOObject((EObject) (msg).getNewValue()).cdoRevision(true);
            if (cdoRevision != null) {
                for (CdoSessionActionDelegate each : delegates) {
                    if (each instanceof CdoNewObjectsActionDelegate) {
                        ((CdoNewObjectsActionDelegate) each).perform(Collections.singletonList(cdoRevision), properties);
                    } else if (each instanceof CdoChangedObjectsActionDelegate) {
                        ((CdoChangedObjectsActionDelegate) each).perform(Collections.singletonList(cdoRevision), properties);
                    } else if (each instanceof CdoAnyEventActionDelegate) {
                        ((CdoAnyEventActionDelegate) each).perform(new IEvent() {
                            @Override
                            public INotifier getSource() {
                                return null;
                            }
                        });
                    }
                }
            }
        }
        if (msg.getEventType() == 4) { //REMOVE

        }

        //if a "model resource" in CDO is deleted
        if (msg.getEventType() == 112) { // CDOInvalidationNotificationImpl
        }
        if (msg.getEventType() == 111) { // CDODeltaNotification
        }
        if (msg.getEventType() == 1) { // ENotificationImpl, CDOResourceImpl
        }
    }
}