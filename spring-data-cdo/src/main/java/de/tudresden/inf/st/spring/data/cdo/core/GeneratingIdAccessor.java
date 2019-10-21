/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.spring.data.cdo.CdoConverter;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.spi.common.revision.StubCDORevision;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * // * {@link IdentifierAccessor} adding a {} to automatically generate an identifier and
 * set it on the underling bean instance.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class GeneratingIdAccessor implements IdentifierAccessor {

    private final PersistentPropertyAccessor<?> accessor;
    private final PersistentProperty<?> identifierProperty;
    private final IdentifierGenerator generator;
    private final CdoConverter cdoConverter;
    private final CdoPersistentEntity persistentEntity;
    private final Object source;

    /**
     * Creates a new {@link GeneratingIdAccessor} using the given {@link PersistentPropertyAccessor}, identifier property
     * and {@link IdentifierGenerator}.
     */
    public GeneratingIdAccessor(Object source, CdoPersistentEntity persistentEntity,
                                IdentifierGenerator generator, CdoConverter cdoConverter) {
        Assert.notNull(source, "source object must not be null!");
        this.identifierProperty = persistentEntity.getIdProperty();
        if (!persistentEntity.isInheritedCDOObject() && !persistentEntity.isInheritedLegacyObject())
            Assert.notNull(identifierProperty, "Identifier property must not be null!");
        this.source = source;
        this.cdoConverter = cdoConverter;
        this.accessor = persistentEntity.getPropertyAccessor(source);
        this.persistentEntity = persistentEntity;
        this.generator = generator;

        Assert.notNull(generator, "IdentifierGenerator must not be null!");
        Assert.notNull(accessor, "PersistentPropertyAccessor must not be null!");
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.de.tudresden.inf.st.bifogtecture.data.keyvalue.core.IdentifierAccessor#getIdentifier()
     */
    @Override
    public Object getIdentifier() {
        return accessor.getProperty(identifierProperty);
    }

    /**
     * Returns the identifier value of the backing bean or replace a new one.
     *
     * @return
     */
//    using the configured
//    {@link IdentifierGenerator}.
    public Object getOrSetProvidedIdentifier(CDOID id) {
        Object existingIdentifier = getIdentifier();
        if (existingIdentifier instanceof CDORevision) {
            existingIdentifier = ((StubCDORevision) existingIdentifier).getID();
        }


        if (Objects.nonNull(existingIdentifier)) {
            return existingIdentifier;
        }

//        Object generatedIdentifier = generator.generateIdentifierOfType(identifierProperty.getTypeInformation(), source);
        accessor.setProperty(identifierProperty, id);

        return id;
    }
}
