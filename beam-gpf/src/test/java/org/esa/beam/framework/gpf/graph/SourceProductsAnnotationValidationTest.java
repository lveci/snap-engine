package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class SourceProductsAnnotationValidationTest extends TestCase {

    private OperatorSpi someOpSpi;
    private OperatorSpi twoSourcesOpSpi;
    private OperatorSpi anySourcesOpSpi;
    private OptSourcesOp.Spi optSourcesOpSpi;

    @Override
    protected void setUp() throws Exception {
        someOpSpi = new InputOp.Spi();
        twoSourcesOpSpi = new TwoSourcesOp.Spi();
        anySourcesOpSpi = new AnySourcesOp.Spi();
        optSourcesOpSpi = new OptSourcesOp.Spi();

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(someOpSpi);
        registry.addOperatorSpi(twoSourcesOpSpi);
        registry.addOperatorSpi(anySourcesOpSpi);
        registry.addOperatorSpi(optSourcesOpSpi);
    }

    @Override
    protected void tearDown() {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.removeOperatorSpi(someOpSpi);
        registry.removeOperatorSpi(twoSourcesOpSpi);
        registry.removeOperatorSpi(anySourcesOpSpi);
        registry.removeOperatorSpi(optSourcesOpSpi);
    }

    public void testTwoSourcesOp() {
        final String opName = "TwoSourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        outputNode.addSource(new NodeSource("dummy", "input2"));
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException ge) {
            fail("GraphException not expected, exactly 2 sources given. Error: " + ge.getMessage());
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        outputNode.addSource(new NodeSource("dummy", "input2"));
        outputNode.addSource(new NodeSource("dummy", "input3"));
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, need exactly 2 sources");
        } catch (GraphException ge) {
        }
    }

    public void testAnySourcesOp() {
        final String opName = "AnySourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected, at least one source expected");
        } catch (GraphException ge) {
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        outputNode.addSource(new NodeSource("dummy", "input2"));
        outputNode.addSource(new NodeSource("dummy", "input3"));
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException ge) {
            fail("GraphException not expected, any number of sources allowed. Error: " + ge.getMessage());
        }
    }

    public void testOptSourcesOp() {
        final String opName = "OptSourcesOp";
        Graph graph;
        Node outputNode;

        graph = createTestGraph(opName);
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException ge) {
            fail("GraphException not expected, sources not checked. Error: " + ge.getMessage());
        }

        graph = createTestGraph(opName);
        outputNode = graph.getNode("output");
        outputNode.addSource(new NodeSource("dummy", "input1"));
        outputNode.addSource(new NodeSource("dummy", "input2"));
        outputNode.addSource(new NodeSource("dummy", "input3"));
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException ge) {
            fail("GraphException not expected, sources not checked. Error: " + ge.getMessage());
        }
    }


    private Graph createTestGraph(String opName) {
        Graph graph = new Graph("graph");
        Node input1Node = new Node("input1", "InputOp");
        Node input2Node = new Node("input2", "InputOp");
        Node input3Node = new Node("input3", "InputOp");
        Node outputNode = new Node("output", opName);
        graph.addNode(input1Node);
        graph.addNode(input2Node);
        graph.addNode(input3Node);
        graph.addNode(outputNode);
        return graph;
    }

    @OperatorMetadata(alias = "InputOp")
    public static class InputOp extends Operator {
        @TargetProduct
        private Product targetProduct;
        
        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("input", "inputType", 1, 1);
            targetProduct.addBand("a", ProductData.TYPE_INT8);
            targetProduct.addBand("b", ProductData.TYPE_INT8);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(InputOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "TwoSourcesOp")
    public static class TwoSourcesOp extends Operator {

        @SourceProducts(count = 2)
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {
            public Spi() {
                super(TwoSourcesOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "AnySourcesOp")
    public static class AnySourcesOp extends Operator {

        @SourceProducts(count = -1)
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(AnySourcesOp.class);
            }
        }
    }

    @OperatorMetadata(alias = "OptSourcesOp")
    public static class OptSourcesOp extends Operator {

        @SourceProducts
        // count=0
        Product[] inputs;

        @TargetProduct
        Product output;

        @Override
        public void initialize() throws OperatorException {
            output = new Product("output", "outputType", 12, 12);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(OptSourcesOp.class);
            }
        }
    }
}
