package de.tudresden.inf.st.spring.data.cdo.repositories;

import com.github.javafaker.Faker;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.Book;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelFactory;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookImpl;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.impl.BookstoreDomainModelPackageImpl;
import de.tudresden.inf.st.spring.data.cdo.annotation.CDO;
import de.tudresden.inf.st.spring.data.cdo.annotation.EObjectModel;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.ecore.EObject;
import org.springframework.data.annotation.Id;

import java.util.Objects;

@CDO(path = "junit/test/books",
        nsUri = "http://www.example.org/bookstoreDomainModel",
        ePackage = BookstoreDomainModelPackage.class,
        ePackageBaseClass = "de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage")
public class BookAnnotated {

    @Id
    CDOID id;
    @EObjectModel(ofClass = Book.class)
    EObject model;

    public CDOID getId() {
        return id;
    }

    public BookAnnotated() {
        this.model = generateRandomBook(null);
    }

    public BookAnnotated(String title) {
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