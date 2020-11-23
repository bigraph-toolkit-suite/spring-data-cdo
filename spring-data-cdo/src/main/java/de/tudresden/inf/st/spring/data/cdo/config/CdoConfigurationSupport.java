package de.tudresden.inf.st.spring.data.cdo.config;

import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.core.mapping.CdoMappingContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Dominik Grzelak
 */
public abstract class CdoConfigurationSupport {

    protected abstract String getRepositoryName();

    @Nullable
    protected Collection<String> getMappingBasePackages() {
        Package mappingBasePackage = getClass().getPackage();
        return Collections.singleton(mappingBasePackage == null ? null : mappingBasePackage.getName());
    }

    @Bean
    public CdoMappingContext cdoMappingContext() throws ClassNotFoundException {

        CdoMappingContext mappingContext = new CdoMappingContext();
        mappingContext.setInitialEntitySet(getInitialEntitySet());
//        mappingContext.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
        mappingContext.setFieldNamingStrategy(fieldNamingStrategy());
//        mappingContext.setAutoIndexCreation(autoIndexCreation());

        return mappingContext;
    }

    protected boolean abbreviateFieldNames() {
        return false;
    }

    protected FieldNamingStrategy fieldNamingStrategy() {
        return abbreviateFieldNames() ? new CamelCaseAbbreviatingFieldNamingStrategy()
                : PropertyNameFieldNamingStrategy.INSTANCE;
    }

    /**
     * Scans the mapping base package for classes annotated with {@link CDO}. By default, it scans for entities in
     * all packages returned by {@link #getMappingBasePackages()}.
     *
     * @return set of all CDO entities
     * @throws ClassNotFoundException
     * @see #getMappingBasePackages()
     */
    protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {

        Set<Class<?>> initialEntitySet = new HashSet<>();
        if (Objects.nonNull(getMappingBasePackages())) {
            for (String basePackage : getMappingBasePackages()) {
                initialEntitySet.addAll(scanForEntities(basePackage));
            }
        }
        return initialEntitySet;
    }

    /**
     * Scans the given base package for entities, i.e. CDO specific types annotated with
     * and
     * {@link Persistent}.
     *
     * @param basePackage must not be {@literal null}.
     * @return
     * @throws ClassNotFoundException
     * @since 1.10
     */
    protected Set<Class<?>> scanForEntities(String basePackage) throws ClassNotFoundException {

        if (!StringUtils.hasText(basePackage)) {
            return Collections.emptySet();
        }

        Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

        if (StringUtils.hasText(basePackage)) {

            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
                    false);
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(CDO.class));
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

            for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {

                initialEntitySet
                        .add(ClassUtils.forName(candidate.getBeanClassName(), CdoConfigurationSupport.class.getClassLoader()));
            }
        }

        return initialEntitySet;
    }
}
