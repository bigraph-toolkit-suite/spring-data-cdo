package org.bigraphs.spring.data.cdo.core.mapping;

import org.bigraphs.spring.data.cdo.CdoConverter;
import org.bigraphs.spring.data.cdo.annotation.EObjectModel;
import org.bigraphs.spring.data.cdo.repository.CdoPersistentEntity;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author Dominik Grzelak
 */
public class AnnotationBasedEPackageResolver implements EPackageResolver {
    public static AnnotationBasedEPackageResolver INSTANCE = new AnnotationBasedEPackageResolver();
//    private final ObjenesisStd objenesis;

    private AnnotationBasedEPackageResolver() {
//        this.objenesis = new ObjenesisStd(true);
    }

    @Override
    public <T> org.eclipse.emf.ecore.EObject resolveEPackageField(CdoPersistentEntity owner, T source, CdoConverter converter) {
        Assert.notNull(owner, "CdoPersistentEntity for EPackageField must not be null!");
        Assert.notNull(source, "source must not be null!");
        TypeInformation<?> typeInformation = owner.getTypeInformation();
        Class<?> type = typeInformation.getType();

        PersistentProperty persistentProperty = owner.getPersistentProperty(EObjectModel.class);
        if (Objects.nonNull(persistentProperty)) {
            return (org.eclipse.emf.ecore.EObject) owner.getPropertyAccessor(source).getProperty(persistentProperty);
        }

        Method method = converter.resolveEPackageFieldAnnotatedMethod(type);
        if (Objects.nonNull(method)) {
            EObjectModel ttl = (EObjectModel) AnnotationUtils.findAnnotation(method, EObjectModel.class);
            return (org.eclipse.emf.ecore.EObject) ReflectionUtils.invokeMethod(method, source, (Object[]) null);
        }
        return null;
    }
}
