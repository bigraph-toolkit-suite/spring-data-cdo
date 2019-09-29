package de.tudresden.inf.st.spring.data.cdo.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

/**
 * @author Dominik Grzelak
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface CDO { //TODO

    @AliasFor("path")
    String value() default "";

    /**
     * the unique path id, resource for each object model
     * like a collection name
     *
     * @return
     */
    @AliasFor("value")
    String path() default "";

    String packageName() default "";

//    @AliasFor("nsUri")
    String nsUri() default "";

}
