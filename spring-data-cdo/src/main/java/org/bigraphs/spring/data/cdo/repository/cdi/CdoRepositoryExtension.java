package org.bigraphs.spring.data.cdo.repository.cdi;

import org.bigraphs.spring.data.cdo.CdoOperations;
import org.bigraphs.spring.data.cdo.CdoTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * CDI extension to export CDO repositories. This extension enables CDO
 * {@link org.springframework.data.repository.Repository} support. It requires either a {@link CdoTemplate} or a
 * {@link CdoOperations} bean. If no {@link CdoTemplate}
 * is provided by the user, the extension creates own managed beans.
 *
 * @author Dominik Grzelak
 */
public class CdoRepositoryExtension extends CdiRepositoryExtensionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CdoRepositoryExtension.class);

    private final Map<Set<Annotation>, Bean<CdoOperations>> cdoOperations = new HashMap<>();

    public CdoRepositoryExtension() {
        LOG.info("Activating CDI extension for Spring Data CDO repositories.");
    }

    /**
     * Pick up existing bean definitions that are required for a CDO Repository to work.
     *
     * @param processBean
     * @param <X>
     */
    @SuppressWarnings("unchecked")
    <X> void processBean(@Observes ProcessBean<X> processBean) {

        Bean<X> bean = processBean.getBean();

        for (Type type : bean.getTypes()) {
            if (type instanceof Class<?> && CdoOperations.class.isAssignableFrom((Class<?>) type)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Discovered %s with qualifiers %s.", CdoOperations.class.getName(),
                            bean.getQualifiers()));
                }

                // Store the EntityManager bean using its qualifiers.
                cdoOperations.put(new HashSet<>(bean.getQualifiers()), (Bean<CdoOperations>) bean);
            }
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        for (Map.Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

            Class<?> repositoryType = entry.getKey();
            Set<Annotation> qualifiers = entry.getValue();

            // Create the bean representing the repository.
            CdiRepositoryBean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);

            if (LOG.isInfoEnabled()) {
                LOG.info(String.format("Registering bean for %s with qualifiers %s.", repositoryType.getName(), qualifiers));
            }

            // Register the bean to the container.
            registerBean(repositoryBean);
            afterBeanDiscovery.addBean(repositoryBean);
        }
    }

    private <T> CdiRepositoryBean<T> createRepositoryBean(Class<T> repositoryType, Set<Annotation> qualifiers,
                                                          BeanManager beanManager) {

        // Determine the CdoOperations bean which matches the qualifiers of the repository.
        Bean<CdoOperations> cdoOperations = this.cdoOperations.get(qualifiers);

        if (cdoOperations == null) {
            throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s.",
                    CdoOperations.class.getName(), qualifiers));
        }

        // Construct and return the repository bean.
        return new CdoRepositoryBean<T>(cdoOperations, qualifiers, repositoryType, beanManager,
                Optional.of(getCustomImplementationDetector()));
    }
}
