package org.bigraphs.spring.data.cdo.core.mapping;

import org.bigraphs.spring.data.cdo.repository.CdoPersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * @author Dominik Grzelak
 */
public class CachingCdoPersistentProperty extends BasicCdoPersistentProperty {

    public CachingCdoPersistentProperty(Property property, CdoPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder, FieldNamingStrategy fieldNamingStrategy) {
        super(property, owner, simpleTypeHolder, fieldNamingStrategy);
    }
}
