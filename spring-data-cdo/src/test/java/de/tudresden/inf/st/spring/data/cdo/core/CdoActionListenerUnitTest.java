package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.spring.data.cdo.CDORevisionHolder;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.CdoChangedObjectsActionDelegate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.CdoEventBasedActionDelegate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.CdoNewObjectsActionDelegate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.CdoListenerFilter;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.FilterCriteria;
import de.tudresden.inf.st.spring.data.cdo.repositories.BookAnnotated;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class CdoActionListenerUnitTest {

    @Autowired
    private CdoDbFactory factory;

    @Before
    public void setUp() throws Exception {
        CdoTemplate template = new CdoTemplate(factory);
        CDOPackageRegistry.INSTANCE.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        CDOPackageRegistry remoteRegistry = template.getCDOPackageRegistry();
        EPackage ePackage = remoteRegistry.getEPackage(BookstoreDomainModelPackage.eNS_URI);
        if (ePackage == null) {
            remoteRegistry.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        }

//        CdoDeleteResult cdoDeleteResult2 = template.removeAll(FALLBACK_RESOURCE_PATH);
    }

    @Test
    public void add_single_listener_test() throws InterruptedException {
        CdoTemplate template = new CdoTemplate(factory);
        CDOPackageRegistry.INSTANCE.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        CDOPackageRegistry registry = template.getCDOPackageRegistry();
        EPackage ePackage = registry.getEPackage(BookstoreDomainModelPackage.eNS_URI);
        if (ePackage == null) {
            registry.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        }

        final BookAnnotated bookRev = new BookAnnotated("A book title");

        CdoListenerFilter filter = CdoListenerFilter
                .filter(
                        new FilterCriteria().byRepositoryPath("junit/test/books")
                )
                .restrict(CDOSessionInvalidationEvent.class);

        AtomicInteger cnt = new AtomicInteger(0);
        AtomicInteger changeCounter = new AtomicInteger(0);

        CdoEventBasedActionDelegate f = event2 -> {
            System.out.println("Event received: " + event2);
            cnt.incrementAndGet();
            changeCounter.incrementAndGet();
            CDOSessionInvalidationEvent event = (CDOSessionInvalidationEvent) event2;
            List<CDORevisionKey> changedObjects = ((CDOSessionInvalidationEvent) event).getChangedObjects();
            for (CDORevisionKey each : changedObjects) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("\tChanged object");
                    System.out.println("ID: " + each.getID() + "// " + each.getBranch().getPathName());
                    System.out.println("REVCOUNT: " + revisionById.getRevisionCount());
                } catch (Exception e) {
                    continue;
                }
            }
            List<CDOIDAndVersion> newObjects = event.getNewObjects();
            for (CDOIDAndVersion each : newObjects) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("\tNew object");
                    System.out.println("ID: " + each.getID() + "// Version= " + each.getVersion());
                    System.out.println("REVCOUNT: " + revisionById.getRevisionCount());
                } catch (Exception e) {
                    continue;
                }
            }

            if (cnt.get() >= 4) {
                bookRev.changeISBN("1337");
                template.save(bookRev);
                cnt.set(0);
            }
        };
        template.addListener(filter, f);
        template.insert(bookRev);
        bookRev.changeISBN("1111");
        template.save(bookRev);
        bookRev.changeISBN("1112");
        template.save(bookRev);
        bookRev.changeISBN("1113");
        template.save(bookRev);

        System.out.println("Inserted=" + bookRev + " with ID= " + CDOUtil.getCDOObject(bookRev.getModel()).cdoID());
        assert bookRev.getId() == CDOUtil.getCDOObject(bookRev.getModel()).cdoID();

        Thread.sleep(2500);
        System.out.println(changeCounter.get());
        assert changeCounter.get() == 5;
    }

    @Test
    public void add_two_listener_test() throws InterruptedException {
        CdoTemplate template = new CdoTemplate(factory);
        CDOPackageRegistry.INSTANCE.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        CDOPackageRegistry registry = template.getCDOPackageRegistry();
        EPackage ePackage = registry.getEPackage(BookstoreDomainModelPackage.eNS_URI);
        if (ePackage == null) {
            registry.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        }

        final BookAnnotated bookRev = new BookAnnotated("A book title");

        CdoListenerFilter filter = CdoListenerFilter
                .filter(
                        new FilterCriteria().byRepositoryPath("junit/test/books")
                )
                .restrict(CDOSessionInvalidationEvent.class);

        AtomicInteger cnt = new AtomicInteger(0);

        CdoChangedObjectsActionDelegate actionDelegate1 = changedObjects -> {
            for (CDORevisionKey each : changedObjects) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("Changed object");
                    System.out.println("\t CO: ID: " + each.getID() + " // Version: " + each.getVersion());
                    System.out.println("\t CO: RevisionCount: " + revisionById.getRevisionCount());
                    System.out.println("---------------");
                    cnt.incrementAndGet();
                } catch (Exception e) {
                    continue;
                }
            }
        };

        CdoNewObjectsActionDelegate actionDelegate2 = newObjects -> {
            for (CDOIDAndVersion each : newObjects) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("New object");
                    System.out.println("\t NO: ID: " + each.getID() + " // Version: " + each.getVersion());
                    System.out.println("\t NO: RevisionCount: " + revisionById.getRevisionCount());
                    System.out.println("---------------");
                    cnt.incrementAndGet();
                } catch (Exception e) {
                    continue;
                }
            }
        };

//         When order is important: add all at once for a session:
        template.addListeners(filter, actionDelegate2, actionDelegate1);
//         if order is not important: add them individually for two CDO sessions
//        template.addListener(filter, actionDelegate1);
//        template.addListener(filter, actionDelegate2);
        BookAnnotated inserted = template.insert(bookRev);
        inserted.changeISBN("1111");
        inserted = template.save(inserted);
        inserted.changeISBN("1112");
        inserted = template.save(inserted);
        inserted.changeISBN("1113");
        inserted = template.save(inserted);
        System.out.println("Inserted=" + inserted + " with ID= " + CDOUtil.getCDOObject(inserted.getModel()).cdoID());
        assert inserted.getId() == CDOUtil.getCDOObject(inserted.getModel()).cdoID();
        Thread.sleep(2500);
        assert cnt.get() == 4;
    }
}