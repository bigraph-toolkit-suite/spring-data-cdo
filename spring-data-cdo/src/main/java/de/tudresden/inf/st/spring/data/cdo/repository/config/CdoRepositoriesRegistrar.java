package de.tudresden.inf.st.spring.data.cdo.repository.config;

import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * CDO-specific {@link ImportBeanDefinitionRegistrar}.
 *
 * @author Dominik Grzelak
 */
public class CdoRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    public CdoRepositoriesRegistrar() {
        System.out.println("CdoRepositoriesRegistrar");
    }

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableCdoRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new CdoRepositoryConfigurationExtension();
    }
}
