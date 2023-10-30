package org.bigraphs.spring.data.cdo.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Persistent
//@ReadOnlyProperty
public @interface EObjectModel {

//    @AliasFor("value")
    String name() default "";

    Class<?> ofClass();
}
