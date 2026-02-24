package org.bigraphs.spring.data.cdo.repositories;

import com.github.javafaker.Faker;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelFactory;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookstoreDomainModelPackageImpl;
import org.bigraphs.spring.data.cdo.CdoOperations;
import org.bigraphs.spring.data.cdo.CdoServerConnectionString;
import org.bigraphs.spring.data.cdo.CdoTemplate;
import org.bigraphs.spring.data.cdo.SimpleCdoDbFactory;
import org.bigraphs.spring.data.cdo.annotation.EObjectModel;
import org.bigraphs.spring.data.cdo.repository.config.EnableCdoRepositories;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.ecore.EObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * If the class is not annotated correctly and is not a native EObject or CDOObject, an {@link IllegalArgumentException}
 * will be thrown.
 *
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BookNotAnnotatedRepositoryIntegrationTest {

    @BeforeClass
    public static void init() throws Exception {
        System.out.println("Register EPackage");
        // The package must be registered as it is later retrieved. Necessary for the CDO query.
        BookstoreDomainModelPackageImpl.init();
    }

    @Configuration
    @EnableCdoRepositories(basePackages = "org.bigraphs.spring.data.cdo.repositories")
    static class Config {

        @Bean
        public CdoTemplate cdoTemplate() throws Exception {
            return new CdoTemplate(new SimpleCdoDbFactory(new CdoServerConnectionString("cdo://localhost:2036/repo1")));
        }
    }

    BookNotAnnotated bookA, bookB, bookC, bookD;

    @Autowired
    protected BookNotAnnotatedRepository bookRepository;
    //
    @Autowired
    CdoOperations operations;

    @Before
    public void setUp() throws Exception {

        bookRepository.deleteAll();

        bookA = new BookNotAnnotated("EMF");
        bookB = new BookNotAnnotated("CDO");
        bookC = new BookNotAnnotated("ECORE");
        bookD = new BookNotAnnotated("Eclipse");
        assertThrows(IllegalArgumentException.class, () -> {
            bookRepository.save(bookA);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            bookRepository.save(bookB);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            bookRepository.save(bookC);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            bookRepository.save(bookD);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            bookRepository.saveAll(Arrays.asList(bookA, bookB, bookC, bookD));
        });
    }

    @Test
    public void find_book_by_id() {
//        Optional<BookNotAnnotated> byId = bookRepository.findById(bookD.getId());
//        Assertions.assertFalse(byId.isPresent());
//        Assertions.assertEquals(((Book) byId.get().model).getName(), ((Book) bookD.model).getName());
    }

    @Test
    public void save_multiple_times_has_no_effect() {
//        bookRepository.save(bookA);
//        bookRepository.save(bookA);
    }

    @Test
    public void count_entities() {
//        bookRepository.count();
    }

    public static class BookNotAnnotated {

        @Id
        CDOID id;
        @EObjectModel(ofClass = Book.class)
        EObject model;

        public CDOID getId() {
            return id;
        }

        public BookNotAnnotated() {
            this.model = generateRandomBook(null);
        }

        public BookNotAnnotated(String title) {
            this.model = generateRandomBook(title);
        }

        private static Book generateRandomBook(String title) {
            BookstoreDomainModelFactory factory = BookstoreDomainModelFactory.eINSTANCE;
            Faker faker = new Faker();
            String fakeISBN = String.format("%s-1-%s-%s-1", faker.number().digits(3), faker.number().digits(3), faker.number().digits(5));
            Book book = factory.createBook();
            book.setIsbn(fakeISBN);
            if (Objects.isNull(title))
                book.setName(faker.backToTheFuture().quote());
            else
                book.setName(title);
            return book;
        }
    }
}
