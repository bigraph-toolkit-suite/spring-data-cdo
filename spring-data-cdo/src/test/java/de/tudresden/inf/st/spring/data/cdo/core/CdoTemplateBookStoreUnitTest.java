package de.tudresden.inf.st.spring.data.cdo.core;

import com.github.javafaker.Faker;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookStore;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelFactory;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookImpl;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookStoreImpl;
import de.tudresden.inf.st.spring.data.cdo.CDORevisionHolder;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.DataNotFoundException;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
public class CdoTemplateBookStoreUnitTest {
    @Autowired
    private CdoTemplate template;

    @Autowired
    private CdoDbFactory factory;

    private ConfigurableApplicationContext context;
    private BookstoreDomainModelFactory bookStorefactory = BookstoreDomainModelFactory.eINSTANCE;

    private static final String TEST_RESOURCE_PATH = "/junit/bookstore/";
    private static final String BOOKSTORE_RESOURCE_PATH = "/junit/bookstore2/";
    private static final String BOOK_TEST_RESOURCE_PATH = "/junit/books/";
    private static final String FALLBACK_RESOURCE_PATH = "bookStoreImpl";
    private static final Faker faker = new Faker();

    @Autowired
    public void setApplicationContext(ConfigurableApplicationContext context) {
        this.context = context;
//        PersistentEntities entities = PersistentEntities.of(template.getConverter().getMappingContext());
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(50);
        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
        CdoDeleteResult cdoDeleteResult2 = template.removeAll(FALLBACK_RESOURCE_PATH);
        CdoDeleteResult cdoDeleteResult3 = template.removeAll(BOOK_TEST_RESOURCE_PATH);
        CdoDeleteResult cdoDeleteResult4 = template.removeAll(BOOKSTORE_RESOURCE_PATH);
    }


    @Before
    public void setUp() throws Exception {
        this.template.setApplicationContext(context);
        bookStorefactory = BookstoreDomainModelFactory.eINSTANCE;
//        Thread.sleep(50);
//        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult2 = template.removeAll(FALLBACK_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult3 = template.removeAll(BOOK_TEST_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult4 = template.removeAll(BOOKSTORE_RESOURCE_PATH);
    }

    @Test
    public void add_and_retrieve_book_revisions() {
        CdoTemplate template = new CdoTemplate(factory);
        Book book = bookStorefactory.createBook();
        book.setIsbn("123456789");
        book.setName("Book of Examples");

        Book inserted = template.insert(book, BOOK_TEST_RESOURCE_PATH);
        inserted.setIsbn("12345678");
        inserted = template.save(inserted, BOOK_TEST_RESOURCE_PATH);
        inserted.setIsbn("1234567");
        inserted = template.save(inserted, BOOK_TEST_RESOURCE_PATH);
        CDORevisionHolder<Book> revisions = template.getRevisionById(CDOUtil.getCDOObject(inserted).cdoID(), BOOK_TEST_RESOURCE_PATH);

        List<Book> byTimeStamp = revisions.getByTimeStamp(678L);
        assertEquals(0, byTimeStamp.size());

        Optional<Book> byVersion1 = revisions.getByVersion(1);
        Optional<CDORevision> cdoRevisionByVersion = revisions.getCDORevisionByVersion(1);
        assertTrue(byVersion1.isPresent());
        assertEquals("123456789", byVersion1.get().getIsbn());
        assertTrue(cdoRevisionByVersion.isPresent());
        assertEquals(1, cdoRevisionByVersion.get().getVersion());
        Optional<Book> byVersion2 = revisions.getByVersion(2);
        assertTrue(byVersion2.isPresent());
        assertEquals("12345678", byVersion2.get().getIsbn());
        Optional<Book> byVersion3 = revisions.getByVersion(3);
        assertTrue(byVersion3.isPresent());
        assertEquals("1234567", byVersion3.get().getIsbn());
        Optional<Book> byVersion4 = revisions.getByVersion(4);
        assertFalse(byVersion4.isPresent());

    }

    @Test
    public void add_single_bookstore_entity() {

        CdoTemplate template = new CdoTemplate(factory);
//        BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
        Book book = bookStorefactory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Spring Data CDO - Book");

        BookStore bookStore = bookStorefactory.createBookStore();
        bookStore.setLocation("Dresden");
        bookStore.setOwner("Bleda Isolda");
        bookStore.getBooks().add(book);

        BookStore insert = template.insert(bookStore, TEST_RESOURCE_PATH);
        System.out.println("Inserted=" + insert);
        assertNotNull(CDOUtil.getCDOObject(insert).cdoID());
    }

    @Test
    public void update_entity_will_not_fail() {
        CdoTemplate template = new CdoTemplate(factory);
        Book book = generateRandomBook(bookStorefactory);

        System.out.println("Save entity the first time throws exception because the ID cannot be obtained.");
        Assertions.assertThrows(IllegalStateException.class, () -> {
            template.save(book, TEST_RESOURCE_PATH);
        });

        String changeTitleTo = "New Title";
        String changeIsbnTo = "0123456789";
        System.out.println("Insert entity first, then update entity using save operation should not fail");
        template.insert(book, TEST_RESOURCE_PATH);
        CDOID cdoid = CDOUtil.getCDOObject(book).cdoID();

        System.out.println("Update the title of the book");
        book.setName(changeTitleTo);
        template.save(book, TEST_RESOURCE_PATH);
        Assertions.assertEquals(cdoid, CDOUtil.getCDOObject(book).cdoID());
        Assertions.assertEquals(changeTitleTo, book.getName());

        System.out.println("Update the ISBN of the book");
        book.setIsbn(changeIsbnTo);
        template.save(book, TEST_RESOURCE_PATH);
        Assertions.assertEquals(cdoid, CDOUtil.getCDOObject(book).cdoID());
        Assertions.assertEquals(changeIsbnTo, book.getIsbn());
    }

    @Test
    public void add_single_bookstore_entity_use_fallback_resourcePath() {
        CdoTemplate template = new CdoTemplate(factory);

        Book book = bookStorefactory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Haidee Aladdin Book");

        BookStore bookStore = bookStorefactory.createBookStore();
        bookStore.setLocation("Berlin");
        bookStore.setOwner("Wenonah Bede");
        bookStore.getBooks().add(book);

        Assertions.assertEquals(template.getResourcePathFrom(bookStore.getClass()), FALLBACK_RESOURCE_PATH);
        BookStore insert = template.insert(bookStore);
        System.out.println("Inserted=" + insert);
        assertNotNull(CDOUtil.getCDOObject(insert).cdoID());
        List<BookStoreImpl> all = template.findAll(BookStoreImpl.class, FALLBACK_RESOURCE_PATH);
        assertNotNull(all);
        Assertions.assertTrue(all.size() > 0);
    }

    @Test
    public void find_bookstore_by_id() {
        CdoTemplate template = new CdoTemplate(factory);

        Book book = bookStorefactory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Haidee Aladdin Book");

        BookStore bookStore = bookStorefactory.createBookStore();
        bookStore.setLocation("Berlin");
        bookStore.setOwner("Wenonah Bede");
        bookStore.getBooks().add(book);

        Assertions.assertEquals(template.getResourcePathFrom(bookStore.getClass()), FALLBACK_RESOURCE_PATH);
        BookStore insert = template.insert(bookStore);
        System.out.println("Inserted=" + insert);

        CDOID idToSearch = CDOUtil.getCDOObject(insert).cdoID();
        BookStoreImpl bookStore1 = template.find(idToSearch, BookStoreImpl.class, FALLBACK_RESOURCE_PATH);
        assertNotNull(bookStore1);
        Assertions.assertEquals(idToSearch, CDOUtil.getCDOObject(bookStore1).cdoID());
    }

    @Test
    public void remove_bookstore_entity_by_id() {
        CdoTemplate template = new CdoTemplate(factory);

        Book book = bookStorefactory.createBook();
        book.setIsbn("9781234567897");
        book.setName("Lucasta Mariamne");
        Book book2 = bookStorefactory.createBook();
        book2.setIsbn("1711131517191");
        book2.setName("Mignon Geraint");

        BookStore bookStore = bookStorefactory.createBookStore();
        bookStore.setLocation("France");
        bookStore.setOwner("Morgen Hasdrubal");
        bookStore.getBooks().add(book);
        bookStore.getBooks().add(book2);


        BookStore insert = template.insert(bookStore, TEST_RESOURCE_PATH);
        CDOID cdoid = CDOUtil.getCDOObject(insert).cdoID();
        assertNotNull(cdoid);
        System.out.println("ID is=" + cdoid);

        Assertions.assertThrows(DataNotFoundException.class, () -> template.remove(insert, FALLBACK_RESOURCE_PATH));

        template.remove(insert, TEST_RESOURCE_PATH);

    }

    @Test
    public void create_and_remove_all_books_entities() {
        CdoTemplate template = new CdoTemplate(factory);

        int bookCount = 10;
        IntStream.range(0, bookCount)
                .mapToObj(ix -> generateRandomBook(bookStorefactory))
                .forEach(eachBook -> template.insert(eachBook, BOOK_TEST_RESOURCE_PATH));

        System.out.println("Should fail: Removing entities from the default computed resource path that doesn't exist");
        CdoDeleteResult cdoDeleteResult = template.removeAll(BookImpl.class);
        assertFalse(cdoDeleteResult.wasAcknowledged());
        Assertions.assertEquals(CdoDeleteResult.UnacknowledgedCdoDeleteResult.class, cdoDeleteResult.getClass());
        Assertions.assertThrows(InvalidURIException.class, () -> {
            throw ((CdoDeleteResult.UnacknowledgedCdoDeleteResult) cdoDeleteResult).getReason();
        });

        System.out.println("Should succeed: Removing entities from the correct resource path");
        CdoDeleteResult cdoDeleteResult1 = template.removeAll(BookImpl.class, BOOK_TEST_RESOURCE_PATH);
        Assertions.assertTrue(cdoDeleteResult1.wasAcknowledged());
        Assertions.assertTrue(cdoDeleteResult1.getDeletedCount() >= 10);
    }

    @Test
    public void count_book_entities() {
        CdoTemplate template = new CdoTemplate(factory);

        int bookCount = 10;
        IntStream.range(0, bookCount)
                .mapToObj(ix -> generateRandomBook(bookStorefactory))
                .forEach(eachBook -> template.insert(eachBook, BOOK_TEST_RESOURCE_PATH));

        long l = template.countAll(Book.class, BookstoreDomainModelPackage.eINSTANCE, BOOK_TEST_RESOURCE_PATH);
        Assertions.assertTrue(l >= 10);
    }

    @Test
    public void count_bookstore_entities() {
        CdoTemplate template = new CdoTemplate(factory);

        BookStore bookStore = bookStorefactory.createBookStore();
        bookStore.setLocation("Poland");
        bookStore.setOwner("Hasdrubal Morganowitsch");
        BookStore bookStore2 = bookStorefactory.createBookStore();
        bookStore.setLocation("France");
        bookStore.setOwner("Morgen Hasdrubalou");

        int bookCount = 4;
        IntStream.range(0, bookCount)
                .peek(ix -> bookStore.getBooks().add(generateRandomBook(bookStorefactory)))
                .forEach(ix -> bookStore2.getBooks().add(generateRandomBook(bookStorefactory)));

        template.insert(bookStore, BOOKSTORE_RESOURCE_PATH);
        template.insert(bookStore2, BOOKSTORE_RESOURCE_PATH);
        assertNotNull(CDOUtil.getCDOObject(bookStore).cdoID());
        assertNotNull(CDOUtil.getCDOObject(bookStore2).cdoID());

        Assertions.assertThrows(Exception.class, () -> template.countAll(BookStoreImpl.class, BookstoreDomainModelPackage.eINSTANCE, BOOKSTORE_RESOURCE_PATH));
        long l = template.countAll(BookStore.class, BookstoreDomainModelPackage.eINSTANCE, BOOKSTORE_RESOURCE_PATH);
        Assertions.assertTrue(l >= 2);
    }

    private static Book generateRandomBook(BookstoreDomainModelFactory factory) {
        String fakeISBN = String.format("%s-1-%s-%s-1", faker.number().digits(3), faker.number().digits(3), faker.number().digits(5));
        Book book = factory.createBook();
        book.setIsbn(fakeISBN);
        book.setName(faker.yoda().quote());
        return book;
    }
}
