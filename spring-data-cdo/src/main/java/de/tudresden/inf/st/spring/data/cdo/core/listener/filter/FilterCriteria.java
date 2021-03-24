package de.tudresden.inf.st.spring.data.cdo.core.listener.filter;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;

/**
 * @author Dominik Grzelak
 */
public class FilterCriteria {
    private LinkedHashMap<String, Object> criteria = new LinkedHashMap<>();
    private @Nullable
    String key;

    public FilterCriteria() {
        this((String) null);
    }

    public FilterCriteria(@Nullable String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public FilterCriteria byRepositoryPath(String repositoryPath) {
        criteria.put(Key.REPOSITORY_PATH, repositoryPath);
        appendKey(Key.REPOSITORY_PATH);
        return this;
    }

    public String getRepositoryPath() {
        return (String) criteria.get(Key.REPOSITORY_PATH);
    }

    public FilterCriteria byCdoId(CDOID cdoid) {
        criteria.put(Key.CDOID, cdoid);
        appendKey(Key.CDOID);
        return this;
    }

    public CDOID getCDOID() {
        return (CDOID) criteria.get(Key.CDOID);
    }

    private void appendKey(String keyName) {
        if (this.key == null) {
            key = keyName;
            return;
        }
        key += keyName;
    }

    public interface Key {
        String REPOSITORY_PATH = "repositoryPath";
        String CDOID = "cdoId";
    }
}
