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

import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.common.model.CDOClassInfoImpl;
import org.eclipse.emf.ecore.EObject;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link IdentifierGenerator} to generate identifiers of types {@link UUID}, String,
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public enum DefaultIdentifierGenerator implements IdentifierGenerator {

    INSTANCE;

    private final AtomicReference<SecureRandom> secureRandom = new AtomicReference<>(null);

    /*
     * (non-Javadoc)
     * @see org.springframework.de.tudresden.inf.st.bifogtecture.data.keyvalue.core.IdentifierGenerator#newIdForType(org.springframework.de.tudresden.inf.st.bifogtecture.data.util.TypeInformation)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T generateIdentifierOfType(TypeInformation<T> identifierType, EObject source) {
//            CDOIDGenerator idGenerator = session.getDelegate().getIDGenerator();
//            CDOID cdoid = CDOIDGenerator.UUID.generateCDOID(internalValue);
//            CDOID cdoid = idGenerator.generateCDOID(internalValue);
//            System.out.println(cdoid);

        Class<?> type = identifierType.getType();
        if (ClassUtils.isAssignable(CDORevision.class, type)) {
            return (T) new CDOClassInfoImpl().getRevisionForID(CDOIDUtil.createUUID()); //CDOIDGenerator.UUID.generateCDOID(source));
        } else if (ClassUtils.isAssignable(UUID.class, type)) {
            return (T) UUID.randomUUID();
        } else if (ClassUtils.isAssignable(String.class, type)) {
            return (T) CDOIDUtil.createUUID().toString(); //CDOIDGenerator.UUID.generateCDOID(source).toString(); //UUID.randomUUID().toString();
        } else if (ClassUtils.isAssignable(Integer.class, type)) {
            return (T) Integer.valueOf(getSecureRandom().nextInt());
        } else if (ClassUtils.isAssignable(Long.class, type)) {
            return (T) Long.valueOf(getSecureRandom().nextLong());
        }

        throw new InvalidDataAccessApiUsageException("Non gereratable id type....");
    }

    private SecureRandom getSecureRandom() {

        SecureRandom secureRandom = this.secureRandom.get();
        if (secureRandom != null) {
            return secureRandom;
        }

        for (String algorithm : OsTools.secureRandomAlgorithmNames()) {
            try {
                secureRandom = SecureRandom.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                // ignore and try next.
            }
        }

        if (secureRandom == null) {
            throw new InvalidDataAccessApiUsageException(
                    String.format("Could not create SecureRandom instance for one of the algorithms '%s'.",
                            StringUtils.collectionToCommaDelimitedString(OsTools.secureRandomAlgorithmNames())));
        }

        this.secureRandom.compareAndSet(null, secureRandom);

        return secureRandom;
    }

    /**
     * @author Christoph Strobl
     * @since 1.1.2
     */
    private static class OsTools {

        private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name").toLowerCase();

        private static final List<String> SECURE_RANDOM_ALGORITHMS_LINUX_OSX_SOLARIS = Arrays.asList("NativePRNGBlocking",
                "NativePRNGNonBlocking", "NativePRNG", "SHA1PRNG");
        private static final List<String> SECURE_RANDOM_ALGORITHMS_WINDOWS = Arrays.asList("SHA1PRNG", "Windows-PRNG");

        static List<String> secureRandomAlgorithmNames() {
            return OPERATING_SYSTEM_NAME.contains("win") ? SECURE_RANDOM_ALGORITHMS_WINDOWS
                    : SECURE_RANDOM_ALGORITHMS_LINUX_OSX_SOLARIS;
        }
    }
}
