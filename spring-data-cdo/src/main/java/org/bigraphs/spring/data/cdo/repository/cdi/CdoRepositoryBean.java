package org.bigraphs.spring.data.cdo.repository.cdi;

import org.bigraphs.spring.data.cdo.CdoOperations;
import org.bigraphs.spring.data.cdo.repository.support.CdoRepositoryFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.util.Assert;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

/**
 * {@link CdiRepositoryBean} to create CDO repository instances.
 *
 * @author Dominik Grzelak
 */
public class CdoRepositoryBean<T> extends CdiRepositoryBean<T> {

    private final Bean<CdoOperations> cdoOperationsBean;

    /**
     * Creates a new {@link CdiRepositoryBean}.
     *
     * @param cdoOperationsBean must not be {@literal null}.
     * @param qualifiers        must not be {@literal null}.
     * @param repositoryType    must not be {@literal null}.
     * @param beanManager       must not be {@literal null}.
     * @param detector          detector for the custom {@link org.springframework.data.repository.Repository} implementations
     *                          {@link CustomRepositoryImplementationDetector}, can be {@literal null}.
     */
    public CdoRepositoryBean(Bean<CdoOperations> cdoOperationsBean, Set<Annotation> qualifiers, Class<T> repositoryType,
                             BeanManager beanManager, Optional<CustomRepositoryImplementationDetector> detector) {
        super(qualifiers, repositoryType, beanManager, detector);
        Assert.notNull(cdoOperationsBean, "CdoOperations bean must not be null!");
        this.cdoOperationsBean = cdoOperationsBean;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.cdi.CdiRepositoryBean#create(javax.enterprise.context.spi.CreationalContext, java.lang.Class)
     */
    @Override
    protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

        CdoOperations cdoOperations = getDependencyInstance(this.cdoOperationsBean, CdoOperations.class);

        return create(() -> new CdoRepositoryFactory(cdoOperations), repositoryType);
    }

}
