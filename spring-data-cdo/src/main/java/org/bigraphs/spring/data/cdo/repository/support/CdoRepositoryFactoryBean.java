package org.bigraphs.spring.data.cdo.repository.support;

import org.bigraphs.spring.data.cdo.CdoOperations;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @author Dominik Grzelak
 */
public class CdoRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends RepositoryFactoryBeanSupport<T, S, ID> {

    private boolean mappingContextConfigured = false;

    @Nullable
    private CdoOperations operations;

    /**
     * Creates a new {@link CdoRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    protected CdoRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        RepositoryFactorySupport factory = getFactoryInstance(operations);
        return factory;
    }

    /**
     * Creates and initializes a {@link RepositoryFactorySupport} instance.
     *
     * @param operations
     * @return
     */
    protected RepositoryFactorySupport getFactoryInstance(CdoOperations operations) {
        return new CdoRepositoryFactory(operations);
    }

    /**
     * Configures the {@link CdoOperations} to be used.
     *
     * @param operations the operations to set
     */
    public void setCdoOperations(CdoOperations operations) {
        this.operations = operations;
    }

    @Override
    protected void setMappingContext(MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
        this.mappingContextConfigured = true;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        Assert.state(operations != null, "cdoTemplate must not be null!");

        if (!mappingContextConfigured) {
            setMappingContext(operations.getMappingContext());
        }
    }


}
