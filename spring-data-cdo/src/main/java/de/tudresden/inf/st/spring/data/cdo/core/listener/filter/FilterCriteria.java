package de.tudresden.inf.st.spring.data.cdo.core.listener.filter;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;

/**
 *
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
        criteria.put("repositoryPath", repositoryPath);
        return this;
    }

    public FilterCriteria byCdoId(CDOID cdoid) {
        criteria.put("cdoId", cdoid);
        return this;
    }
}
