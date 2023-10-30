package org.bigraphs.spring.data.cdo.config;

import org.bigraphs.spring.data.cdo.CdoClient;
import org.bigraphs.spring.data.cdo.CdoDbFactory;
import org.bigraphs.spring.data.cdo.SimpleCdoDbFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Base class for Spring Data CDO configuration to be extended by a custom user class for Java-based configuration with {@link CdoClient}.
 *
 * @author Dominik Grzelak
 */
@Configuration
public abstract class AbstractCdoClientConfiguration extends CdoConfigurationSupport {

    public abstract CdoClient cdoClient();

    //TODO cdoTemplate() erlaubt queries (müssen erstmal erstellt werden)

    /**
     * Return the name of the database to connect to.
     *
     * @return the repository name; must not be {@literal null}.
     */
    @NonNull
    protected abstract String getRepositoryName();

    @Bean
    public CdoDbFactory cdoDbFactory() {
        return new SimpleCdoDbFactory(cdoClient(), getRepositoryName());
    }

    /**
     * Returns the base packages to scan for CDO mapped entities at startup. Will return the package name of the
     * configuration class' (the concrete class, not this one here) by default. So if you have a
     * {@code com.acme.AppConfig} extending {@link } the base package will be considered
     * {@code com.acme} unless the method is overridden to implement alternate behavior.
     *
     * @return the base packages to scan for mapped {@link } classes or an empty collection to not enable scanning
     * for entities.
     * @since 1.10
     */
    @Nullable
    protected Collection<String> getMappingBasePackages() {
        Package mappingBasePackage = getClass().getPackage();
        return Collections.singleton(mappingBasePackage == null ? null : mappingBasePackage.getName());
    }
}
