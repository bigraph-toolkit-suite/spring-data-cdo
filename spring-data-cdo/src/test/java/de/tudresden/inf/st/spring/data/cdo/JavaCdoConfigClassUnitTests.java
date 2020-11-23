package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.AbstractCdoClientConfiguration;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

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
