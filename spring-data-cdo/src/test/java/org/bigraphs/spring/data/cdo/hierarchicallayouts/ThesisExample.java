package org.bigraphs.spring.data.cdo.hierarchicallayouts;

import org.bigraphs.framework.core.AbstractEcoreSignature;
import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.EcoreBigraph;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.exceptions.IncompatibleSignatureException;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.operations.IncompatibleInterfaceException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.utils.BigraphUtil;
import org.bigraphs.model.bigraphBaseModel.BigraphBaseModelPackage;
import org.bigraphs.model.bigraphBaseModel.impl.BigraphBaseModelPackageImpl;
import org.bigraphs.spring.data.cdo.CdoDbFactory;
import org.bigraphs.spring.data.cdo.CdoTemplate;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.After;
import org.junit.AfterClass;
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

import static org.bigraphs.framework.core.factory.BigraphFactory.*;
import static org.bigraphs.framework.core.utils.BigraphUtil.ACCUMULATOR_PARALLEL_PRODUCT;

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
        registerPackages(BigraphBaseModelPackage.eNS_URI, BigraphBaseModelPackage.eINSTANCE, template);
    }

    @After
    public void after() throws Exception {
        template.removeAll("/parent1");
        template.removeAll("/parent2");
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
    public void test_store_and_retrieve() throws IOException, InvalidConnectionException, CloneNotSupportedException, IncompatibleSignatureException, IncompatibleInterfaceException {
        DynamicSignature sig = pureSignatureBuilder()
                .add("Basket", 1)
                .add("Choc", 1)
                .add("Nuts", 1)
                .add("Apple", 1)
                .add("Banana", 1)
                .add("Citron", 1)
                .create();

        PureBigraph bigraph = pureBuilder(sig).root()
                .child("Basket", "x").down()
                .child("Apple", "x")
                .child("Banana", "x")
                .child("Citron", "x").up()
                .child("Basket", "y").down()
                .child("Choc", "y")
                .child("Nuts", "y").top().create();


        BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        registerPackages(bigraph.getMetaModel().getNsURI(), bigraph.getMetaModel(), template);

        // Decompose bigraph
        PureBigraph p1 = pureBuilder(sig).root().child("Basket", "x").down().site().create();
        PureBigraph p2 = pureBuilder(sig).root().child("Basket", "y").down().site().create();
        List<PureBigraph> children_p1 = Arrays.asList(
                pureBuilder(sig).root().child("Apple", "x").create(),
                pureBuilder(sig).root().child("Banana", "x").create(),
                pureBuilder(sig).root().child("Citron", "x").create()
        );
        List<PureBigraph> children_p2 = Arrays.asList(
                pureBuilder(sig).root().child("Choc", "y").create(),
                pureBuilder(sig).root().child("Nuts", "y").create()
        );
//        PureBigraph b11 = pureBuilder(sig).createRoot().addChild("Apple", "x").createBigraph();
//        PureBigraph b12 = pureBuilder(sig).createRoot().addChild("Banana", "x").createBigraph();
//        PureBigraph b13 = pureBuilder(sig).createRoot().addChild("Citron", "x").createBigraph();
//        PureBigraph b21 = pureBuilder(sig).createRoot().addChild("Choc", "y").createBigraph();
//        PureBigraph b22 = pureBuilder(sig).createRoot().addChild("Nuts", "y").createBigraph();

//        template.removeAll("/parent1/children");
//        template.removeAll("/parent2/children");

        template.createResourceFolder("/parent1/children");
        template.createResourceFolder("/parent2/children");
        EObject inserted_p1 = template.insert(p1.getInstanceModel(), "/parent1");
        EObject inserted_p2 = template.insert(p2.getInstanceModel(), "/parent2");
        template.insertAll(children_p1.stream().map(PureBigraph::getInstanceModel).collect(Collectors.toList()), "/parent1/children");
        template.insertAll(children_p2.stream().map(PureBigraph::getInstanceModel).collect(Collectors.toList()), "/parent2/children");

//        template.insert(b11.getModel(), "/parent1/children");
//        template.insert(b12.getModel(), "/parent1/children");
//        template.insert(b13.getModel(), "/parent1/children");
//        template.insert(b21.getModel(), "/parent2/children");
//        template.insert(b22.getModel(), "/parent2/children");

        EPackage bigraphMetaModel = createOrGetBigraphMetaModel(sig);
        Bigraph children1 = template.findAll(EObject.class, "/parent1/children/bigraphBaseModel")
                .stream()
                .map(x -> (Bigraph) PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, x).create())
                .reduce(pureLinkings(sig).identity_e(), BigraphUtil.ACCUMULATOR_PARALLEL_PRODUCT::apply);
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) children1, System.out);
        Bigraph children2 = template.findAll(EObject.class, "/parent2/children/bigraphBaseModel")
                .stream()
                .map(x -> (Bigraph) PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, x).create())
                .reduce(pureLinkings(sig).identity_e(), BigraphUtil.ACCUMULATOR_PARALLEL_PRODUCT::apply);
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) children2, System.out);

        EObject eObj_p1 = template.find(CDOUtil.getCDOObject(inserted_p1).cdoID(), EObject.class, "/parent1");
        Bigraph _p1 = PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, eObj_p1).create();
        EObject eObj_p2 = template.find(CDOUtil.getCDOObject(inserted_p2).cdoID(), EObject.class, "/parent2");
        Bigraph _p2 = PureBigraphBuilder.create(sig.getInstanceModel(), bigraphMetaModel, eObj_p2).create();

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

        System.out.println("Result: ");
        BigraphFileModelManagement.Store.exportAsInstanceModel((EcoreBigraph) result, System.out);
    }
}
