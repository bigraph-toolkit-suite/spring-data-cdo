package org.bigraphs.spring.data.cdo.core;

import org.bigraphs.spring.data.cdo.CdoDbFactory;
import org.bigraphs.spring.data.cdo.CdoTemplate;
import org.bigraphs.spring.data.cdo.CreateResourceFailedException;
import org.bigraphs.spring.data.cdo.annotation.CDO;
import org.bigraphs.spring.data.cdo.annotation.EObjectModel;
import org.bigraphs.spring.data.cdo.core.CdoDeleteResult;
import org.bigraphs.spring.data.cdo.core.event.AbstractCdoEventListener;
import org.bigraphs.spring.data.cdo.core.event.BeforeSaveEvent;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

/**
 * Requirements: CDO server must be running on localhost:2036
 * <p>
 * Integration test for {@link CdoTemplate}.
 * <p>
 * The required beans are configured using XML. See {@literal infrastructure.xml} inside the resource folder.
 *
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class CdoTemplateBasicIntegrationTest {
    @Autowired
    CdoTemplate template;
    @Autowired
    CdoDbFactory factory;

    ConfigurableApplicationContext context;

    static final String TEST_RESOURCE_PATH = "/junit/test2/";

    @Autowired
    public void setApplicationContext(ConfigurableApplicationContext context) {

        this.context = context;

        context.addApplicationListener(new AbstractCdoEventListener<WithObjectModelProperty>() {
            @Override
            public void onBeforeSaveEvent(BeforeSaveEvent<WithObjectModelProperty> event) {
                super.onBeforeSaveEvent(event);
                WithObjectModelProperty source = event.getSource();
                System.out.println("ID of source=" + source.id);
            }
        });

        PersistentEntities entities = PersistentEntities.of(template.getConverter().getMappingContext());
    }

    @Before
    public void setUp() throws Exception {
        this.template.setApplicationContext(context);
    }

    @After
    public void tearDown() throws Exception {
        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
//        Assertions.assertEquals(1, cdoDeleteResult.getDeletedCount());
//        Assertions.assertTrue(cdoDeleteResult.wasAcknowledged());
    }

    @Test
    public void insert_simple_entity() {
        WithObjectModelProperty a = new WithObjectModelProperty();
        template.insert(a);

        WithObjectModelProperty withObjectModelProperty = template.find(a.id, a.getClass(), TEST_RESOURCE_PATH);
        Assertions.assertNotNull(withObjectModelProperty);
        Assertions.assertEquals(withObjectModelProperty.id, a.id);
    }
    //TODO: shall no fail
    @Test
    public void update_entity_not_triggering_exception() {
        Assertions.assertAll(() -> {
            EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;
            WithObjectModelProperty test = new WithObjectModelProperty();
            test = template.insert(test);
            EClass testCdoClass = theCoreFactory.createEClass();
            testCdoClass.setName(getClass().getSimpleName() + "Updated");
            test.value = testCdoClass;
            WithObjectModelProperty save = template.save(test);
            Assertions.assertEquals(save, test);
            WithObjectModelProperty save2 = template.save(test);
            Assertions.assertEquals(save2, save);
        });
    }


    @Test
    public void throwsException_duplicate_id() {
        CdoTemplate template = new CdoTemplate(factory);
        Person person = new Person();

        Person inserted = template.insert(person);
        Assertions.assertNotNull(inserted);
        Assertions.assertEquals(inserted, person);
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            template.insert(person);
        });
    }

    @Test
    public void throwsException_remove_single_resourcePath() {
        CdoTemplate template = new CdoTemplate(factory);
        final String sampleResourcePath = UUID.randomUUID().toString();
        final String folder = "/test2/xyz";
        final String folder2 = "/test2/xyz/" + sampleResourcePath;

        Assertions.assertAll(() -> {
            template.createResourcePath(folder);
        });
        Assertions.assertThrows(CreateResourceFailedException.class, () -> template.createResourcePath(folder2));

        Assertions.assertAll(() -> template.removeResourcePath(folder));

        //TODO test: will leave test2 folder intact
    }

    @Test
    public void remove_whole_resourcePath_recursively() {
        CdoTemplate template = new CdoTemplate(factory);
        final String sampleResourcePath = UUID.randomUUID().toString();
        final String folder = "/test1/abc/def/" + sampleResourcePath;

        Assertions.assertAll(() -> {
            template.createResourcePath(folder);
            template.removeResourcePath(folder, true);
        });

        //TODO: test: the whole path is deleted
    }

    @Test
    public void remove_single_resource_path_from_repository() {
        CdoTemplate template = new CdoTemplate(factory);

        final String sampleResourcePath = UUID.randomUUID().toString();
        Assertions.assertAll(() -> template.createResourcePath(sampleResourcePath));

        Assertions.assertThrows(EmptyResultDataAccessException.class, () -> template.removeResourcePath(UUID.randomUUID().toString()));

        Assertions.assertAll(() -> template.removeResourcePath(sampleResourcePath));

        //TODO: check: the resource node is deleted

    }

    @Test
    public void remove_simple_entity() {
        CdoTemplate template = new CdoTemplate(factory);
        Person person = new Person();

        template.insert(person);
        System.out.println("Inserted with cdoId=" + person.id.toURIFragment());
        CdoDeleteResult removed1 = template.remove(person, TEST_RESOURCE_PATH);
//        Assertions.assertEquals(1, removed1.getDeletedCount());
//        Assertions.assertTrue(removed1.wasAcknowledged());
//        Assertions.assertNull(person.id); // cdo-specific properties must be null
//
    }

    // *****
    // Data objects for the integration test
    // *****

    @CDO(path = TEST_RESOURCE_PATH, ePackage = EcorePackage.class)
    static class Person {

        @Id
        CDOID id;
        @EObjectModel(ofClass = EObject.class)
        EObject model;

        public Person() {
            EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;
            EClass testCdoClass = theCoreFactory.createEClass();
            testCdoClass.setName(getClass().getSimpleName());
            this.model = testCdoClass;
        }
    }

    @CDO(path = TEST_RESOURCE_PATH, ePackage = EcorePackage.class)
    static class WithObjectModelProperty {

        @Id
        CDOID id;
        @EObjectModel(ofClass = EObject.class)
        EObject value;

        public WithObjectModelProperty() {
            EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;
            EClass testCdoClass = theCoreFactory.createEClass();
            testCdoClass.setName(getClass().getSimpleName());
            this.value = testCdoClass;
        }
    }


}
