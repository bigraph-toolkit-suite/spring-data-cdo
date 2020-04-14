package de.tudresden.inf.st.spring.data.cdo.repositories.config;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookstoreDomainModelPackageImpl;
import de.tudresden.inf.st.spring.data.cdo.CdoServerConnectionString;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.SimpleCdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.repositories.BookAnnotatedRepository;
import de.tudresden.inf.st.spring.data.cdo.repository.config.EnableCdoRepositories;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

//import org.junit.jupiter.api.condition.DisabledIf;

/**
 * Integration tests for {@link de.tudresden.inf.st.spring.data.cdo.repository.config.CdoRepositoriesRegistrar}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
//@EnabledIf("'TRUE' == systemProperty.get('runCdoTests')")
//@EnabledOnOs({OS.WINDOWS, OS.MAC})
public class CdoRepositoriesRegistrarIntegrationTests {

    @BeforeClass
    public static void beforeClass() throws Exception {
        BookstoreDomainModelPackageImpl.init();
//        EPackage.Registry.INSTANCE.put("http://www.example.org/bookstoreDomainModel", BookstoreDomainModelPackage.eINSTANCE);
    }

    @Configuration
    @EnableCdoRepositories(basePackages = "de.tudresden.inf.st.spring.data.cdo.repositories")
    static class Config {

        @Bean
        public CdoTemplate cdoTemplate() throws Exception {
            return new CdoTemplate(new SimpleCdoDbFactory(new CdoServerConnectionString("cdo://localhost:2036/repo1")));
        }
    }

    @Autowired
    BookAnnotatedRepository bookRepository;
    @Autowired
    ApplicationContext context;

    @Test
    public void testConfiguration() {
        Assertions.assertNotNull(bookRepository);
    }
}