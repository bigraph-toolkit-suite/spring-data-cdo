package de.tudresden.inf.st.spring.data.cdo.repositories;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.spring.data.cdo.CdoOperations;
import de.tudresden.inf.st.spring.data.cdo.CdoServerConnectionString;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.SimpleCdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.repository.config.EnableCdoRepositories;
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

import java.util.Optional;

/**
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BookAnnotatedRepositoryIntegrationTest {

    @Configuration
    @EnableCdoRepositories(basePackages = "de.tudresden.inf.st.spring.data.cdo.repositories")
    static class Config {

        @Bean
        public CdoTemplate cdoTemplate() throws Exception {
            return new CdoTemplate(new SimpleCdoDbFactory(new CdoServerConnectionString("cdo://localhost:2036/repo1")));
        }
    }

    BookAnnotated bookA, bookB, bookC, bookD;

    @Autowired
    protected BookAnnotatedRepository bookRepository;
    //
    @Autowired
    CdoOperations operations;

    @BeforeClass
    public static void setU2p() throws Exception {
        System.out.println("Register EPackage");
        // The package must be registered as it is later retrieved. Necessary for the CDO query.
        BookstoreDomainModelPackage eINSTANCE = BookstoreDomainModelPackage.eINSTANCE;
    }

    @Before
    public void setUp() throws Exception {

        bookRepository.deleteAll();

        bookA = new BookAnnotated("EMF");
        bookB = new BookAnnotated("CDO");
        bookC = new BookAnnotated("ECORE");
        bookD = new BookAnnotated("Eclipse");

        bookRepository.save(bookA);
        bookRepository.save(bookB);
        bookRepository.save(bookC);
        bookRepository.save(bookD);
        //TODO:
//        bookRepository.saveAll(Arrays.asList(bookA, bookB, bookC, bookD));
    }

    @Test
    public void find_book_by_id() {
        Optional<BookAnnotated> byId = bookRepository.findById(bookD.getId());
        Assertions.assertTrue(byId.isPresent());
        Assertions.assertEquals(((Book) byId.get().model).getName(), ((Book) bookD.model).getName());
    }

    @Test
    public void save_multiple_times_has_no_effect() {
        System.out.println(CDOUtil.getCDOObject(bookA.model).cdoID() + " // " + bookA.id);
        bookRepository.save(bookA);
        System.out.println(CDOUtil.getCDOObject(bookA.model).cdoID() + " // " + bookA.id);
//        bookRepository.save(bookA);
    }

    @Test
    public void count_entities() {
        //TODO
        bookRepository.count();
    }

}
