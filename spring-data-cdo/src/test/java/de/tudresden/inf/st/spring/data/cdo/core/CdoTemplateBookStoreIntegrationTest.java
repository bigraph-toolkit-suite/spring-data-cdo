package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookStore;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelFactory;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookStoreImpl;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Requirements: CDO server must be running on localhost:2036
 * <p>
 * Integration test for {@link CdoTemplate} and {@link BookStore} model class.
 * This class is not converted to CDO yet. Thus, the legacy mode is tested here.
 * <p>
 * The required beans are configured using XML. See {@literal infrastructure.xml} inside the resource folder.
 *
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class CdoTemplateBookStoreIntegrationTest {
    @Autowired
    CdoTemplate template;
    @Autowired
    CdoDbFactory factory;

    ConfigurableApplicationContext context;

    static final String TEST_RESOURCE_PATH = "/junit/bookstore/";
    static final String FALLBACK_RESOURCE_PATH = "bookStoreImpl";

    @Autowired
    public void setApplicationContext(ConfigurableApplicationContext context) {

        this.context = context;

//        context.addApplicationListener(new AbstractCdoEventListener<WithObjectModelProperty>() {
//            @Override
//            public void onBeforeSaveEvent(BeforeSaveEvent<WithObjectModelProperty> event) {
//                super.onBeforeSaveEvent(event);
////                WithObjectModelProperty source = event.getSource();
////                System.out.println("ID of source=" + source.id);
//            }
//        });

        PersistentEntities entities = PersistentEntities.of(template.getConverter().getMappingContext());
    }

    @Before
    public void setUp() throws Exception {
        BookstoreDomainModelPackage eINSTANCE = BookstoreDomainModelPackage.eINSTANCE;
        this.template.setApplicationContext(context);
    }

    @After
    public void tearDown() throws Exception {
        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
//        Assertions.assertEquals(1, cdoDeleteResult.getDeletedCount());
//        Assertions.assertTrue(cdoDeleteResult.wasAcknowledged());
    }


    @Test
    public void add_single_bookstore_entity() {

        CdoTemplate template = new CdoTemplate(factory);
        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = factory.createBook();
        book.setIsbn("100-100");
        book.setName("Spring Data CDO - Book");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("Dresden");
        bookStore.setOwner("Dominik");
        bookStore.getBooks().add(book);

        BookStore insert = template.insert(bookStore, TEST_RESOURCE_PATH);
        System.out.println("Inserted=" + insert);
        Assertions.assertNotNull(CDOUtil.getCDOObject(insert).cdoID());
    }

    @Test
    public void add_single_bookstore_entity_use_fallback_resourcePath() {
        CdoTemplate template = new CdoTemplate(factory);
        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = factory.createBook();
        book.setIsbn("100-100");
        book.setName("Spring Data CDO - Book");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("Dresden");
        bookStore.setOwner("Dominik");
        bookStore.getBooks().add(book);

        Assertions.assertEquals(template.getResourcePathFrom(bookStore.getClass()), FALLBACK_RESOURCE_PATH);
        BookStore insert = template.insert(bookStore);
        System.out.println("Inserted=" + insert);
        Assertions.assertNotNull(CDOUtil.getCDOObject(insert).cdoID());
        List<BookStoreImpl> all = template.findAll(BookStoreImpl.class, FALLBACK_RESOURCE_PATH);
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.size() > 0);
//        System.out.println(all);
    }


}
