package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookstoreDomainModelPackageImpl;
import de.tudresden.inf.st.spring.data.cdo.CdoClient;
import de.tudresden.inf.st.spring.data.cdo.CdoOperations;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import de.tudresden.inf.st.spring.data.cdo.config.AbstractCdoClientConfiguration;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class CdoBasicOperationsUnitTest {
    static GenericApplicationContext context;
    CdoOperations operations;

    @BeforeClass
    public static void init() {
        context = new AnnotationConfigApplicationContext(TestCdoConfig.class);
    }

    @AfterClass
    public static void after() {
        context.close();
    }

    @Before
    public void setUp() throws Exception {
        BookstoreDomainModelPackageImpl.init();
        operations = context.getBean(CdoOperations.class);
        Assertions.assertNotNull(operations);
    }

    @Test
    public void operations_update_test() {
        TestCdoNoIdProperty test2 = new TestCdoNoIdProperty();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            operations.insert(test2);
        });

        TestCdo test = new TestCdo();
        Assertions.assertThrows(IllegalStateException.class, () -> {
            operations.save(test);
        });

        operations.insert(test);

        TestCdo testCdo = operations.find(test.id, test.getClass(), "test/samples");
        Assertions.assertNotNull(testCdo);
    }

    // basic java configuration for CDO
    @Configuration
    static class TestCdoConfig extends AbstractCdoClientConfiguration {

        @Override
        public CdoClient cdoClient() {
            return new CdoClient();
        }

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

        @Id
        public CDOID id;

        @EObjectModel(ofClass = EObject.class)
        public EObject model;

        public TestCdo() {
            EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;
            EClass testCdoClass = theCoreFactory.createEClass();
            testCdoClass.setName("TestCdoClass");
            this.model = testCdoClass;
        }
    }

    // class missing the id attribute
    @CDO(path = "#{'test' + '/' + 'samples'}", packageName = "#{'customSamplePackage'}")
    public class TestCdoNoIdProperty {

        @EObjectModel(ofClass = EObject.class) //BOuterName.class, BInnerName.class
        public EObject model;

        public TestCdoNoIdProperty() {
            EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;
            EClass testCdoClass = theCoreFactory.createEClass();
            testCdoClass.setName("TestCdoClass");
            this.model = testCdoClass;
        }
    }
}
