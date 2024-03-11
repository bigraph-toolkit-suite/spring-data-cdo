package org.bigraphs.spring.data.cdo;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Unit test for {@link JavaCdoConfigClass}.
 *
 * @author Dominik Grzelak
 */
public class JavaCdoConfigClassUnitTests extends AbstractCdoUnitTestSupport{

	@Test
	public void loadsConfigClassFromDefaultPackage() {
		new AnnotationConfigApplicationContext(JavaCdoConfigClass.class).close();
	}

}
