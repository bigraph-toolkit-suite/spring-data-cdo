package de.tudresden.inf.st.spring.data.cdo;

import org.eclipse.emf.cdo.common.revision.CDORevision;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Dominik Grzelak
 */
public class CDORevisionHolder<T> {

    final ConcurrentHashMap<RevisionSlot, T> revisionMap = new ConcurrentHashMap<>();

    private CDORevisionHolder() {
    }

    public static <T> CDORevisionHolder<T> create() {
        return new CDORevisionHolder<>();
    }

    public Optional<T> getByVersion(int version) {
        return revisionMap.entrySet().stream()
                .filter(e -> e.getKey().version.equals(version))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<CDORevision> getCDORevisionByVersion(int version) {
        return revisionMap.keySet().stream()
                .filter(t -> t.version.equals(version))
                .map(t -> t.cdoRevision)
                .findFirst();
    }

    public List<T> getByTimeStamp(Long timestamp) {
        return revisionMap.entrySet().stream()
                .filter(e -> e.getKey().timestamp.equals(timestamp))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    void add(T internalValue, CDORevision cdoRevision) {
        revisionMap.put(
                new RevisionSlot(cdoRevision.getTimeStamp(), cdoRevision.getVersion(), cdoRevision),
                internalValue
        );
    }

    public static class RevisionSlot {
        private final Long timestamp;
        private final Integer version;
        private final CDORevision cdoRevision;

        private RevisionSlot(Long timestamp, Integer version, CDORevision cdoRevision) {
            this.timestamp = timestamp;
            this.version = version;
            this.cdoRevision = cdoRevision;
        }
    }
}
