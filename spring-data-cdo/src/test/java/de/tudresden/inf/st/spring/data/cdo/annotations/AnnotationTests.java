package de.tudresden.inf.st.spring.data.cdo.annotations;

import de.tudresden.inf.st.spring.data.cdo.CdoClient;
import de.tudresden.inf.st.spring.data.cdo.CdoOperations;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.config.AbstractCdoClientConfiguration;
import lombok.RequiredArgsConstructor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Requirements: CDO server must be running on localhost:2036
 * <p>
 * Java configuration is used to create the necessary beans (e.g., {@link CdoOperations}). Then, the beans are registered
 * manually via {@link AnnotationConfigApplicationContext}.
 * The required beans are then acquired by the current context via the {@link GenericApplicationContext#getBean(Class)} method.
 *
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:test.properties") // load test properties
public class AnnotationTests {

    private static GenericApplicationContext context;
    private CdoOperations operations;
    private TestCdoService cdoService;

    @Value("${path}")
    private String pathProperty;

    @Value("${packageName}")
    private String packageNameValue;

    @BeforeClass
    public static void init() {
        context = new AnnotationConfigApplicationContext(TestCdoConfig.class, TestCdoService.class);
    }

    @AfterClass
    public static void after() {
        context.close();
    }

    @Before
    public void setUp() throws Exception {
        cdoService = context.getBean(TestCdoService.class);
        operations = context.getBean(CdoOperations.class);

        Assertions.assertNotNull(cdoService);
        Assertions.assertNotNull(operations);
    }


    @Test
    public void evaluate_spel_expression_of_cdoPath() {
        System.out.println("Injected path value from test.property=" + pathProperty);
        System.out.println("Injected packageNameValue from test.property=" + packageNameValue);
        TestCdo test = new TestCdo();
        System.out.println(test);
        String resourcePathFrom = operations.getResourcePathFrom(test.getClass());
        Assertions.assertEquals(resourcePathFrom, pathProperty);
        String packageNameFrom = operations.getPackageNameFrom(test.getClass());
        Assertions.assertEquals(packageNameFrom, packageNameValue);
    }

    // basic java configuration for CDO
    @Configuration
    static class TestCdoConfig extends AbstractCdoClientConfiguration {

        @Override
        public CdoClient cdoClient() {
            return new CdoClient();
        }

        @NonNull
        @Override
        protected String getRepositoryName() {
            return "repo1";
        }

        // must be provided as bean before. TestCdoService is accessing it.
        @Bean
        public CdoTemplate cdoTemplate() {
            CdoTemplate cdoTemplate = new CdoTemplate(cdoClient(), getRepositoryName());
            return cdoTemplate;
        }
    }

    @CDO(path = "#{'test' + '/' + 'samples'}", packageName = "#{'customSamplePackage'}")
    public class TestCdo {
    }

    // examplary test service with injected CdoOperations
    @RequiredArgsConstructor
    static class TestCdoService {

        final CdoOperations operations;

    }
}
