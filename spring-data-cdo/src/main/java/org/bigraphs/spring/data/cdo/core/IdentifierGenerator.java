package org.bigraphs.spring.data.cdo.core;

import org.eclipse.emf.ecore.EObject;
import org.springframework.data.util.TypeInformation;

public interface IdentifierGenerator {

	/**
	 * Creates an identifier of the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return an identifier of the given type.
	 */
	<T> T generateIdentifierOfType(TypeInformation<T> type, EObject source);
}
