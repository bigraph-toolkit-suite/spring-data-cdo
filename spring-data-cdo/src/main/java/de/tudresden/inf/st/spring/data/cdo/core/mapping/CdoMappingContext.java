package de.tudresden.inf.st.spring.data.cdo.core.mapping;

import de.tudresden.inf.st.spring.data.cdo.MappingCdoConverter;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import java.util.AbstractMap;

/**
 * @author Dominik Grzelak
 */
public class CdoMappingContext extends AbstractMappingContext<BasicCdoPersistentEntity<?>, CdoPersistentProperty>
        implements ApplicationContextAware, InitializingBean {

    private static final FieldNamingStrategy DEFAULT_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;
    private FieldNamingStrategy fieldNamingStrategy = DEFAULT_NAMING_STRATEGY;
    @Nullable
    private ApplicationContext context;

    /**
     * Creates a new {@link CdoMappingContext}.
     */
    public CdoMappingContext() {
        setSimpleTypeHolder(SimpleTypeHolder.DEFAULT);
    }

    public void setFieldNamingStrategy(@Nullable FieldNamingStrategy fieldNamingStrategy) {
        this.fieldNamingStrategy = fieldNamingStrategy == null ? DEFAULT_NAMING_STRATEGY : fieldNamingStrategy;
    }

    public void afterPropertiesSet() {

    }

    @Override
    protected <T> BasicCdoPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicCdoPersistentEntity<T>(typeInformation, new MappingCdoConverter(this));
    }

    @Override
    protected CdoPersistentProperty createPersistentProperty(Property property, BasicCdoPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new BasicCdoPersistentProperty(property, owner, simpleTypeHolder, fieldNamingStrategy);
    }

    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
        return !SimpleTypeHolder.DEFAULT.isSimpleType(type.getType()) && !AbstractMap.class.isAssignableFrom(type.getType());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.context = applicationContext;
    }
}
