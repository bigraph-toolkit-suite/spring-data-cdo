package de.tudresden.inf.st.spring.data.cdo.repository.support;

import de.tudresden.inf.st.spring.data.cdo.CdoOperations;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoEntityInformation;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoEntityInformationSupport;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * "The very core of the repository abstraction is the factory to create repository instances."
 *
 * @author Dominik Grzelak
 */
public class CdoRepositoryFactory extends RepositoryFactorySupport {
    private final CdoOperations operations;
    private final MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> mappingContext;

    /**
     * Creates a new {@link CdoRepositoryFactory} with the given {@link CdoOperations}.
     *
     * @param cdoOperations must not be {@literal null}.
     */
    public CdoRepositoryFactory(CdoOperations cdoOperations) {

        Assert.notNull(cdoOperations, "CdoOperations must not be null!");
        Assert.notNull(cdoOperations.getMappingContext(), "cdoMappingContext must not be null!");

        this.operations = cdoOperations;
        this.mappingContext = cdoOperations.getMappingContext();
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        return SimpleCdoRepository.class;
    }

    /**
     * "returns the EntityInformation which encapsulates ways to determine whether an entity is new,
     * lookup the identifier of the entity as well as the type of the id."
     *
     * @param domainClass
     * @param <T>
     * @param <ID>
     * @return
     */
    @Override
    public <T, ID> CdoEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return getEntityInformation(domainClass, null);
    }

    //information.getDomainType() is the <T> type of a Repository subclass
    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        CdoEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType(),
                information);
        return getTargetRepositoryViaReflection(information, entityInformation, operations);
    }

    private <T, ID> CdoEntityInformation<T, ID> getEntityInformation(Class<T> domainClass,
                                                                     @Nullable RepositoryMetadata metadata) {

        CdoPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(domainClass);
        return CdoEntityInformationSupport.<T, ID>entityInformationFor(
                entity,
                metadata != null ? metadata.getIdType() : null
        );
    }
}
