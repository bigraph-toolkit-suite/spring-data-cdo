package de.tudresden.inf.st.spring.data.cdo.annotation;

import org.eclipse.emf.ecore.EcorePackage;
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
public @interface CDO {

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

    String nsUri() default "http://www.eclipse.org/emf/2002/Ecore";

    String nsPrefix() default "";

    /**
     * Default setting suited for dynamic generated Ecore objects
     *
     * @return
     */
    String ePackageBaseClass() default "org.eclipse.emf.ecore.EcorePackage";

    /**
     * Default setting suited for dynamic generated Ecore objects
     *
     * @return
     */
    Class ePackage() default EcorePackage.class;

}
