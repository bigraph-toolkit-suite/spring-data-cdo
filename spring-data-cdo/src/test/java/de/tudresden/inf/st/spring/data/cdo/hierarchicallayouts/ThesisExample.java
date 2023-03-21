package de.tudresden.inf.st.spring.data.cdo.hierarchicallayouts;

import de.tudresden.inf.st.bigraphs.core.Bigraph;
import de.tudresden.inf.st.bigraphs.core.BigraphComposite;
import de.tudresden.inf.st.bigraphs.core.BigraphFileModelManagement;
import de.tudresden.inf.st.bigraphs.core.EcoreBigraph;
import de.tudresden.inf.st.bigraphs.core.datatypes.StringTypedName;
import de.tudresden.inf.st.bigraphs.core.exceptions.IncompatibleSignatureException;
import de.tudresden.inf.st.bigraphs.core.exceptions.InvalidConnectionException;
import de.tudresden.inf.st.bigraphs.core.exceptions.operations.IncompatibleInterfaceException;
import de.tudresden.inf.st.bigraphs.core.factory.BigraphFactory;
import de.tudresden.inf.st.bigraphs.core.impl.pure.PureBigraph;
import de.tudresden.inf.st.bigraphs.core.impl.pure.PureBigraphBuilder;
import de.tudresden.inf.st.bigraphs.core.impl.signature.DefaultDynamicSignature;
import de.tudresden.inf.st.bigraphs.models.bigraphBaseModel.BBigraph;
import de.tudresden.inf.st.bigraphs.models.bigraphBaseModel.BigraphBaseModelPackage;
import de.tudresden.inf.st.bigraphs.models.bigraphBaseModel.impl.BigraphBaseModelPackageImpl;
import de.tudresden.inf.st.ecore.models.bookstoreDomainModel.BookstoreDomainModelPackage;
import de.tudresden.inf.st.spring.data.cdo.CdoDbFactory;
import de.tudresden.inf.st.spring.data.cdo.CdoTemplate;
import de.tudresden.inf.st.spring.data.cdo.core.CdoTemplateBasicIntegrationTest;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static de.tudresden.inf.st.bigraphs.core.factory.BigraphFactory.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class ThesisExample {

    @Autowired
    private CdoDbFactory factory;
    @Autowired
    CdoTemplate template;


    @Before
    public void setUp() throws Exception {
        BigraphBaseModelPackageImpl.init();
//        CDOPackageRegistry.INSTANCE.put(BigraphBaseModelPackage.eNS_URI, BigraphBaseModelPackage.eINSTANCE);
//        CDOPackageRegistry remoteRegistry = template.getCDOPackageRegistry();
//        EPackage ePackage = remoteRegistry.getEPackage(BigraphBaseModelPackage.eNS_URI);
//        if (ePackage == null) {
//            remoteRegistry.put(BigraphBaseModelPackage.eNS_URI, BigraphBaseModelPackage.eINSTANCE);
//        }
        registerPackages(BigraphBaseModelPackage.eNS_URI, BigraphBaseModelPackage.eINSTANCE, template);
    }

    static private void registerPackages(String uri, EPackage ePackage, CdoTemplate template) {
        CDOPackageRegistry.INSTANCE.put(uri, ePackage);
        CDOPackageRegistry remoteRegistry = template.getCDOPackageRegistry();
        EPackage ePackage0 = remoteRegistry.getEPackage(uri);
        if (ePackage0 == null) {
            remoteRegistry.put(uri, ePackage);
        }
    }

    @Test
    public void name() throws IOException, InvalidConnectionException, CloneNotSupportedException, IncompatibleSignatureException, IncompatibleInterfaceException {
        DefaultDynamicSignature sig = pureSignatureBuilder()
                .addControl("Basket", 1)
                .addControl("Choc", 1)
                .addControl("Nuts", 1)
                .addControl("Apple", 1)
                .addControl("Banana", 1)
                .addControl("Citron", 1)
                .create();

        PureBigraph bigraph = pureBuilder(sig).createRoot()
                .addChild("Basket", "x").down()
                .addChild("Apple", "x")
                .addChild("Banana", "x")
                .addChild("Citron", "x").up()
                .addChild("Basket", "y").down()
                .addChild("Choc", "y")
                .addChild("Nuts", "y").top().createBigraph();


        BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        registerPackages(bigraph.getModelPackage().getNsURI(), bigraph.getModelPackage(), template);

        // Decompose bigraph
        PureBigraph p1 = pureBuilder(sig).createRoot().addChild("Basket", "x").down().addSite().createBigraph();
        PureBigraph p2 = pureBuilder(sig).createRoot().addChild("Basket", "y").down().addSite().createBigraph();
        List<PureBigraph> children_p1 = Arrays.asList(
                pureBuilder(sig).createRoot().addChild("Apple", "x").createBigraph(),
                pureBuilder(sig).createRoot().addChild("Banana", "x").createBigraph(),
                pureBuilder(sig).createRoot().addChild("Citron", "x").createBigraph()
        );
        List<PureBigraph> children_p2 = Arrays.asList(
                pureBuilder(sig).createRoot().addChild("Choc", "y").createBigraph(),
                pureBuilder(sig).createRoot().addChild("Nuts", "y").createBigraph()
        );
//        PureBigraph b11 = pureBuilder(sig).createRoot().addChild("Apple", "x").createBigraph();
//        PureBigraph b12 = pureBuilder(sig).createRoot().addChild("Banana", "x").createBigraph();
//        PureBigraph b13 = pureBuilder(sig).createRoot().addChild("Citron", "x").createBigraph();
//        PureBigraph b21 = pureBuilder(sig).createRoot().addChild("Choc", "y").createBigraph();
//        PureBigraph b22 = pureBuilder(sig).createRoot().addChild("Nuts", "y").createBigraph();

//        template.removeAll("/parent1/children");
//        template.removeAll("/parent2/children");
        template.removeAll("/parent1");
        template.removeAll("/parent2");

        template.createResourceFolder("/parent1/children");
        template.createResourceFolder("/parent2/children");
        EObject inserted_p1 = template.insert(p1.getModel(), "/parent1");//TODO getInstanceModel:BigraphExt
        EObject inserted_p2 = template.insert(p2.getModel(), "/parent2");
        template.insertAll(children_p1.stream().map(x -> x.getModel()).collect(Collectors.toList()), "/parent1/children");
        template.insertAll(children_p2.stream().map(x -> x.getModel()).collect(Collectors.toList()), "/parent2/children");

//        template.insert(b11.getModel(), "/parent1/children");
//        template.insert(b12.getModel(), "/parent1/children");
//        template.insert(b13.getModel(), "/parent1/children");
//        template.insert(b21.getModel(), "/parent2/children");
//        template.insert(b22.getModel(), "/parent2/children");

        EPackage bigraphMetaModel = createOrGetBigraphMetaModel(sig);
        Bigraph children1 = template.findAll(EObject.class, "/parent1/children/bigraphBaseModel")
                .stream()
                .map(x -> (Bigraph) PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, x).createBigraph())
                .reduce(pureLinkings(sig).identity_e(), accumulator::apply);
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) children1, System.out);
        Bigraph children2 = template.findAll(EObject.class, "/parent2/children/bigraphBaseModel")
                .stream()
                .map(x -> (Bigraph) PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, x).createBigraph())
                .reduce(pureLinkings(sig).identity_e(), accumulator::apply);
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) children2, System.out);

        EObject eObj_p1 = template.find(CDOUtil.getCDOObject(inserted_p1).cdoID(), EObject.class, "/parent1");
        Bigraph _p1 = PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, eObj_p1).createBigraph();
        EObject eObj_p2 = template.find(CDOUtil.getCDOObject(inserted_p2).cdoID(), EObject.class, "/parent2");
        Bigraph _p2 = PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, eObj_p2).createBigraph();

        Bigraph result = ops(purePlacings(sig).merge(2))
                .nesting(ops(_p1).parallelProduct(_p2))
                .merge(pureLinkings(sig).identity(StringTypedName.of("x"), StringTypedName.of("y")))
                .compose(
                        ops(purePlacings(sig).merge(children1.getRoots().size()))
                                .nesting(children1)
                                .parallelProduct(
                                        ops(purePlacings(sig).merge(children2.getRoots().size())).nesting(children2)
                                )
                )
                .getOuterBigraph();
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) result, System.out);


    }

    static BinaryOperator<Bigraph<DefaultDynamicSignature>> accumulator = (partial, element) -> {
        try {
            return ops(partial).parallelProduct(element).getOuterBigraph();
        } catch (IncompatibleSignatureException | IncompatibleInterfaceException e) {
            return pureLinkings(partial.getSignature()).identity_e();
        }
    };
}
