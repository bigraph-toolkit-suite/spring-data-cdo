package de.tudresden.inf.st.spring.data.cdo.repository;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Dominik Grzelak
 */
public class CdoEntityInformationSupport {
    private CdoEntityInformationSupport() {
    }

    /**
     * Factory method for creating {@link CdoEntityInformation}.
     *
     * @param entity must not be {@literal null}.
     * @param idType can be {@literal null}.
     * @return never {@literal null}.
     */
    public static <T, ID> CdoEntityInformation<T, ID> entityInformationFor(CdoPersistentEntity<?> entity,
                                                                    @Nullable Class<?> idType) {

        Assert.notNull(entity, "Entity must not be null!");

        return new MappingCdoEntityInformation<>((CdoPersistentEntity<T>) entity, (Class<ID>) idType);
    }
}
