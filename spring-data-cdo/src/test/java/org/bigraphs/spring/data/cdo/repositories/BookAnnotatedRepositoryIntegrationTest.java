package org.bigraphs.spring.data.cdo.repositories;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookstoreDomainModelPackageImpl;
import org.bigraphs.spring.data.cdo.*;
import org.bigraphs.spring.data.cdo.repository.config.EnableCdoRepositories;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BookAnnotatedRepositoryIntegrationTest {
    @BeforeClass
    public static void init() throws Exception {
        System.out.println("Register EPackage");
        // The package must be registered as it is later retrieved. Necessary for the CDO query.
//        BookstoreDomainModelPackage eINSTANCE = BookstoreDomainModelPackage.eINSTANCE;
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

    BookAnnotated bookA, bookB, bookC, bookD;
    BookAnnotated bookRev;

    @Autowired
    protected BookAnnotatedRepository bookRepository;
    //
    @Autowired
    CdoOperations operations;


    @Before
    public void setUp() throws Exception {

        try {
            bookRepository.deleteAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(0, bookRepository.count());

        bookA = new BookAnnotated("EMF");
        bookB = new BookAnnotated("CDO");
        bookC = new BookAnnotated("ECORE");
        bookD = new BookAnnotated("Eclipse");
        bookRev = new BookAnnotated("Book of Samples");

        bookRepository.saveAll(Arrays.asList(bookA, bookB, bookC, bookD));
        bookRepository.saveAll(Arrays.asList(bookA, bookB, bookC, bookD));
        bookRepository.save(bookRev);
    }

    @Test
    public void get_revisions_of_book() {
        bookRepository.save(bookRev);
        bookRev.changeISBN("12345");
        bookRepository.save(bookRev);

        CDORevisionHolder<BookAnnotated> revision = bookRepository.getRevision(bookRev);
        System.out.println(revision);
        assertEquals(2, revision.getRevisionCount());
    }

    @Test
    public void find_book_by_id() {
        Optional<BookAnnotated> byId = bookRepository.findById(bookD.getId());
        Assertions.assertTrue(byId.isPresent());
        assertEquals(((Book) byId.get().model).getName(), ((Book) bookD.model).getName());
    }

    //TODO: this causes a stackoverflow error when removing
    @Test
    public void save_multiple_times_has_no_effect() {
        String changeTitleTo = "Maven - HowTo";
        System.out.println(CDOUtil.getCDOObject(bookA.model).cdoID() + "<->" + bookA.id);
        assertEquals(CDOUtil.getCDOObject(bookA.model).cdoID(), bookA.id);
        ((Book) bookA.model).setName(changeTitleTo);
        bookRepository.save(bookA);
        System.out.println(CDOUtil.getCDOObject(bookA.model).cdoID() + "<->" + bookA.id);
        assertEquals(CDOUtil.getCDOObject(bookA.model).cdoID(), bookA.id);
        assertEquals(changeTitleTo, ((Book) bookA.model).getName());
        bookRepository.save(bookA);
        System.out.println(CDOUtil.getCDOObject(bookA.model).cdoID() + "<->" + bookA.id);
        assertEquals(CDOUtil.getCDOObject(bookA.model).cdoID(), bookA.id);
        assertEquals(changeTitleTo, ((Book) bookA.model).getName());
    }

    @Test
    public void count_entities() {
        long count = bookRepository.count();
        assertEquals(5, count);
    }

}
