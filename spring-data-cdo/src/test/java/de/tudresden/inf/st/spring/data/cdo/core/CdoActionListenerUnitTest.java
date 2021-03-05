package de.tudresden.inf.st.spring.data.cdo.core;

import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.spring.data.cdo.CDORevisionHolder;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.CdoListenerFilter;
import de.tudresden.inf.st.spring.data.cdo.core.listener.filter.FilterCriteria;
import de.tudresden.inf.st.spring.data.cdo.repositories.BookAnnotated;
import org.eclipse.emf.cdo.common.commit.CDOChangeKind;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class CdoActionListenerUnitTest {

    @Autowired
    private CdoDbFactory factory;
    private ConfigurableApplicationContext context;

    @Autowired
    public void setApplicationContext(ConfigurableApplicationContext context) {
        this.context = context;
//        PersistentEntities entities = PersistentEntities.of(template.getConverter().getMappingContext());
    }

    @Before
    public void setUp() throws Exception {
        CdoTemplate template = new CdoTemplate(factory);
        CDOPackageRegistry.INSTANCE.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        CDOPackageRegistry remoteRegistry = template.getCDOPackageRegistry();
        EPackage ePackage = remoteRegistry.getEPackage(BookstoreDomainModelPackage.eNS_URI);
        if (ePackage == null) {
            remoteRegistry.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        }
//        Thread.sleep(50);
//        CdoDeleteResult cdoDeleteResult = template.removeAll(TEST_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult2 = template.removeAll(FALLBACK_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult3 = template.removeAll(BOOK_TEST_RESOURCE_PATH);
//        CdoDeleteResult cdoDeleteResult4 = template.removeAll(BOOKSTORE_RESOURCE_PATH);
    }

    @Test
    public void add_listener_test() throws InterruptedException {
        CdoTemplate template = new CdoTemplate(factory);
        CDOPackageRegistry.INSTANCE.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        CDOPackageRegistry registry = template.getCDOPackageRegistry();
        EPackage ePackage = registry.getEPackage(BookstoreDomainModelPackage.eNS_URI);
        if (ePackage == null) {
            registry.put(BookstoreDomainModelPackage.eNS_URI, BookstoreDomainModelPackage.eINSTANCE);
        }

//
//        boolean run = true;
//        while (run) {
//            Thread.sleep(500);
//        }

        final BookAnnotated bookRev = new BookAnnotated("A book title");

        CdoListenerFilter filter = CdoListenerFilter
                .filter(
                        new FilterCriteria().byRepositoryPath("junit/test/books")
                )
                .restrict(CDOSessionInvalidationEvent.class);

        AtomicInteger cnt = new AtomicInteger(0);

        AtomicReference<CDOID> tmp = new AtomicReference<>();
        template.addListener(filter, event2 -> {
            System.out.println("Event received: " + event2);
            cnt.incrementAndGet();
            CDOSessionInvalidationEvent event = (CDOSessionInvalidationEvent) event2;
            Map<CDOID, CDOChangeKind> changeKinds = event.getChangeKinds();
//            for (Map.Entry<CDOID, CDOChangeKind> each : changeKinds.entrySet()) {
//                if (each.getKey() == tmp.get()) {
//                    System.out.println(each.getKey() + " - " + each.getValue());
//                    if (each.getValue() == CDOChangeKind.NEW || each.getValue() == CDOChangeKind.CHANGED) {
//                        System.out.println("\tNew object");
//                        EObject bookStoreImpl = template.find(each.getKey(), bookStore.getClass(), FALLBACK_RESOURCE_PATH);
//                        System.out.println("FOUND: " + bookStoreImpl);
//                    }
//                }
//            }

            List<CDORevisionKey> changedObjects = ((CDOSessionInvalidationEvent) event).getChangedObjects();
            for (CDORevisionKey each : changedObjects) {
//                if (each.getID() == tmp.get()) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("\tChanged object");
                    System.out.println("ID: " + each.getID() + "// " + each.getBranch().getPathName());
                    System.out.println("REVCOUNT: " + revisionById.getRevisionCount());
                } catch (Exception e) {
//                    e.printStackTrace();
                    continue;
                }
//                    System.out.println(each.getID());
//                    EObject bookStoreImpl = template.find(each.getID(), bookStore.getClass(), FALLBACK_RESOURCE_PATH);
//                    System.out.println("FOUND: " + bookStoreImpl);
//                }
            }
            List<CDOIDAndVersion> newObjects = event.getNewObjects();
            for (CDOIDAndVersion each : newObjects) {
//                if (each.getID() == tmp.get()) {
                try {
                    CDORevisionHolder<Object> revisionById = template.getRevisionById(each.getID(), "junit/test/books");
                    System.out.println("\tNew object");
                    System.out.println("ID: " + each.getID() + "// Version= " + each.getVersion());
                    System.out.println("REVCOUNT: " + revisionById.getRevisionCount());
                } catch (Exception e) {
//                    e.printStackTrace();
                    continue;
                }
            }

            if(cnt.get() >= 4) {
                bookRev.changeISBN("1337");
                template.save(bookRev);
                cnt.set(0);
            }

        });
        template.insert(bookRev);
        bookRev.changeISBN("1111");
        template.save(bookRev);
        bookRev.changeISBN("1112");
        template.save(bookRev);
        bookRev.changeISBN("1113");
        template.save(bookRev);
//        Thread.sleep(1000);
//        insert.setOwner("A");
//        insert = template.save(insert);
//        Thread.sleep(1000);
//        insert.setOwner("B");
//        insert = template.save(insert);
//        tmp.set(CDOUtil.getCDOObject(insert).cdoID());

//        System.out.println("Inserted=" + insert + " with ID= " + CDOUtil.getCDOObject(insert).cdoID());

        boolean run = true;
        while (run) {
            Thread.sleep(500);
        }
    }
}
