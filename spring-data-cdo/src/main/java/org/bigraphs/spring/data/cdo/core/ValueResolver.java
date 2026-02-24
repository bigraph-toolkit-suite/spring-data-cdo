package org.bigraphs.spring.data.cdo.core;

import org.springframework.data.mapping.PersistentEntity;

import java.lang.annotation.Annotation;

/**
 * @author Dominik Grzelak
 */
public interface ValueResolver {

    Object getInternalValue(PersistentEntity<?,?> owner, Object source, Class<? extends Annotation> annotation);
}
