package org.bigraphs.spring.data.cdo.repository.config;

import org.bigraphs.spring.data.cdo.annotation.CDO;
import org.bigraphs.spring.data.cdo.repository.CdoRepository;
import org.bigraphs.spring.data.cdo.repository.support.CdoRepositoryFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.w3c.dom.Element;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Dominik Grzelak
 */
public class CdoRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    private static final String CDO_TEMPLATE_REF = "cdo-template-ref";

    @Override
    public String getModuleName() {
        return "EclipseCDO";
    }

    @Override
    protected String getModulePrefix() {
        return "cdo";
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return CdoRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(CDO.class);
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(CdoRepository.class);
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

        Element element = config.getElement();

        ParsingUtils.setPropertyReference(builder, element, CDO_TEMPLATE_REF, "cdoOperations");
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

        AnnotationAttributes attributes = config.getAttributes();

        builder.addPropertyReference("cdoOperations", attributes.getString("cdoTemplateRef"));
//        builder.addPropertyReference(BeanNames.CDO_TEMPLATE_BEAN_NAME, attributes.getString("cdoTemplateRef"));
    }

    @Override
    protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
        return !metadata.isReactiveRepository();
    }
}
