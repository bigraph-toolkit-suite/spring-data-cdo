package de.tudresden.inf.st.spring.data.cdo.core;

import com.github.javafaker.Faker;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookStore;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelFactory;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookImpl;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookStoreImpl;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.InvalidURIException;
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

import java.util.List;
import java.util.stream.IntStream;

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
    private CdoTemplate template;
    @Autowired
    private CdoDbFactory factory;

    private ConfigurableApplicationContext context;

    private static final String TEST_RESOURCE_PATH = "/junit/bookstore/";
    private static final String BOOK_TEST_RESOURCE_PATH = "/junit/books/";
    private static final String FALLBACK_RESOURCE_PATH = "bookStoreImpl";
    private static final Faker faker = new Faker();

    @Autowired
    public void setApplicationContext(ConfigurableApplicationContext context) {
        this.context = context;
        PersistentEntities entities = PersistentEntities.of(template.getConverter().getMappingContext());
    }

    @Before
    public void setUp() throws Exception {
        this.template.setApplicationContext(context);
    }

    @After
    public void tearDown() throws Exception {
        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
        CdoDeleteResult cdoDeleteResult2 = template.removeAll(FALLBACK_RESOURCE_PATH);
        Assertions.assertTrue(cdoDeleteResult.wasAcknowledged());
        Assertions.assertTrue(cdoDeleteResult2.wasAcknowledged());
    }


    @Test
    public void add_single_bookstore_entity() {

        CdoTemplate template = new CdoTemplate(factory);
        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = factory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Spring Data CDO - Book");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("Dresden");
        bookStore.setOwner("Bleda Isolda");
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
        book.setIsbn("9781234567897");
        book.setName("Haidee Aladdin Book");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("Berlin");
        bookStore.setOwner("Wenonah Bede");
        bookStore.getBooks().add(book);

        Assertions.assertEquals(template.getResourcePathFrom(bookStore.getClass()), FALLBACK_RESOURCE_PATH);
        BookStore insert = template.insert(bookStore);
        System.out.println("Inserted=" + insert);
        Assertions.assertNotNull(CDOUtil.getCDOObject(insert).cdoID());
        List<BookStoreImpl> all = template.findAll(BookStoreImpl.class, FALLBACK_RESOURCE_PATH);
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.size() > 0);
    }

    @Test
    public void find_bookstore_by_id() {
        CdoTemplate template = new CdoTemplate(factory);
        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = factory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Haidee Aladdin Book");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("Berlin");
        bookStore.setOwner("Wenonah Bede");
        bookStore.getBooks().add(book);

        Assertions.assertEquals(template.getResourcePathFrom(bookStore.getClass()), FALLBACK_RESOURCE_PATH);
        BookStore insert = template.insert(bookStore);
        System.out.println("Inserted=" + insert);

        CDOID idToSearch = CDOUtil.getCDOObject(insert).cdoID();
        BookStoreImpl bookStore1 = template.find(idToSearch, BookStoreImpl.class, FALLBACK_RESOURCE_PATH);
        Assertions.assertNotNull(bookStore1);
        Assertions.assertEquals(idToSearch, CDOUtil.getCDOObject(bookStore1).cdoID());
    }

    @Test
    public void remove_bookstore_entity_by_id() {
        CdoTemplate template = new CdoTemplate(factory);
        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = factory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Lucasta Mariamne");
        Book book2 = factory.createBook();
        book2.setIsbn("1711131517191");
        book2.setName("Mignon Geraint");

        BookStore bookStore = factory.createBookStore();
        bookStore.setLocation("France");
        bookStore.setOwner("Morgen Hasdrubal");
        bookStore.getBooks().add(book);
        bookStore.getBooks().add(book2);


        BookStore insert = template.insert(bookStore, TEST_RESOURCE_PATH);
        Assertions.assertNotNull(CDOUtil.getCDOObject(insert).cdoID());

        CdoDeleteResult remove = template.remove(insert, FALLBACK_RESOURCE_PATH);
        Assertions.assertTrue(remove.wasAcknowledged());
        Assertions.assertNull(CDOUtil.getCDOObject(insert).cdoID());

    }

    @Test
    public void create_and_remove_all_books_entities() {
        CdoTemplate template = new CdoTemplate(factory);

        int bookCount = 10;

        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;

        IntStream.range(0, bookCount)
                .mapToObj(ix -> generateRandomBook(factory))
                .forEach(eachBook -> template.insert(eachBook, BOOK_TEST_RESOURCE_PATH));

        System.out.println("Removing entities from the default computed resource path that doesn't exist should fail");
        CdoDeleteResult cdoDeleteResult = template.removeAll(BookImpl.class);
        Assertions.assertFalse(cdoDeleteResult.wasAcknowledged());
        Assertions.assertEquals(CdoDeleteResult.UnacknowledgedCdoDeleteResult.class, cdoDeleteResult.getClass());
        Assertions.assertThrows(InvalidURIException.class, () -> {
            throw ((CdoDeleteResult.UnacknowledgedCdoDeleteResult) cdoDeleteResult).getReason();
        });

        System.out.println("Removing entities from the correct resource path should succeed");
        CdoDeleteResult cdoDeleteResult1 = template.removeAll(BookImpl.class, BOOK_TEST_RESOURCE_PATH);
        Assertions.assertTrue(cdoDeleteResult1.wasAcknowledged());
        Assertions.assertTrue(cdoDeleteResult1.getDeletedCount() >= 10);
    }

    private static Book generateRandomBook(BookstoreDomainModelFactory factory) {
        String fakeISBN = String.format("%s-1-%s-%s-1", faker.number().digits(3), faker.number().digits(3), faker.number().digits(5));
        Book book = factory.createBook();
        book.setIsbn(fakeISBN);
        book.setName(faker.yoda().quote());
        return book;
    }
}
