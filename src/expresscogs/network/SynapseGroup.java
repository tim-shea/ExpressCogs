package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public class SynapseGroup {
    public static SynapseGroup connect(NeuronGroup source, NeuronGroup target, DoubleMatrix connectivity, double minWeight, double maxWeight) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        generateSynapses(synapses, connectivity);
        pruneSelfSynapses(synapses);
        randomizeWeights(synapses, minWeight, maxWeight);
        return synapses;
    }
    
    public static SynapseGroup connectUniformRandom(NeuronGroup source, NeuronGroup target, double connectivity, double minWeight, double maxWeight) {
        DoubleMatrix p = DoubleMatrix.ones(source.getSize(), target.getSize()).muli(connectivity);
        return connect(source, target, p, minWeight, maxWeight);
    }
    
    public static SynapseGroup connectNeighborhood(NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood, double minWeight, double maxWeight) {
        DoubleMatrix d = source.getXPosition().repmat(1, target.getSize());
        d.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(d);
        DoubleMatrix p = normalPdf(d, 0.0, neighborhood);
        p.diviColumnVector(p.rowMeans()).muli(connectivity);
        return connect(source, target, p, minWeight, maxWeight);
    }
    
    public static SynapseGroup connectNonNeighborhood(NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood, double minW, double maxW) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        DoubleMatrix d = source.getXPosition().repmat(1, target.getSize());
        d.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(d);
        DoubleMatrix p = normalPdf(d, 0.0, 10 * neighborhood);
        p.diviColumnVector(p.rowMeans().div(2));
        DoubleMatrix p2 = normalPdf(d, 0.0, neighborhood);
        p2.diviColumnVector(p2.rowMeans());
        p.subi(p2).addi(p2.rowMaxs().repmat(1, p.columns));
        p.diviColumnVector(p.rowMeans()).muli(connectivity);
        generateSynapses(synapses, p);
        pruneSelfSynapses(synapses);
        randomizeWeights(synapses, minW, maxW);
        return synapses;
    }
    
    private static DoubleMatrix normalPdf(DoubleMatrix x, double mean, double std) {
        double scale = (1 / (std * Math.sqrt(2 * Math.PI)));
        double twoSigmaSqr = (2 * std * std);
        DoubleMatrix shiftedX = x.sub(mean);
        return MatrixFunctions.expi(shiftedX.mul(shiftedX).divi(-twoSigmaSqr)).muli(scale);
    }
    
    public static SynapseGroup connectGroups(NeuronGroup source, NeuronGroup target, int motorGroups, int neuronsPerGroup, double connectivity) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        generateSynapses(synapses, connectivity);
        DoubleMatrix sourceGroups = DoubleMatrix.linspace(0, source.getSize() - 1,
                source.getSize()).divi(source.getSize() / motorGroups);
        MatrixFunctions.floori(sourceGroups);
        DoubleMatrix targetGroups = DoubleMatrix.linspace(0, target.getSize() - 1,
                target.getSize()).divi(target.getSize() / motorGroups).transpose();
        MatrixFunctions.floori(targetGroups);
        synapses.s.muli(sourceGroups.repmat(1, target.getSize()).eqi(targetGroups.repmat(source.getSize(), 1)));
        pruneSelfSynapses(synapses);
        randomizeWeights(synapses, 0e-9, 4e-9);
        return synapses;
    }
    
    public static SynapseGroup connectOpponentGroups(NeuronGroup source, NeuronGroup target, int motorGroups, int neuronsPerGroup, double connectivity) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        generateSynapses(synapses, connectivity);
        DoubleMatrix sourceGroups = DoubleMatrix.linspace(0, source.getSize() - 1, source.getSize()).divi(source.getSize() / motorGroups);
        MatrixFunctions.floori(sourceGroups);
        DoubleMatrix targetGroups = DoubleMatrix.linspace(0, target.getSize() - 1, target.getSize()).divi(target.getSize() / motorGroups).transpose();
        MatrixFunctions.floori(targetGroups);
        synapses.s.muli(sourceGroups.repmat(1, target.getSize()).nei(targetGroups.repmat(source.getSize(), 1)));
        pruneSelfSynapses(synapses);
        randomizeWeights(synapses, 0e-9, 4e-9);
        return synapses;
    }
    
    private static void generateSynapses(SynapseGroup synapses, double connectivity) {
        int rows = synapses.source.getSize();
        int cols = synapses.target.getSize();
        DoubleMatrix probability = DoubleMatrix.ones(rows, cols).muli(connectivity);
        generateSynapses(synapses, probability);
    }
    
    private static void generateSynapses(SynapseGroup synapses, DoubleMatrix probability) {
        DoubleMatrix random = DoubleMatrix.rand(probability.rows, probability.columns);
        synapses.s = random.lt(probability);
    }
    
    private static void pruneSelfSynapses(SynapseGroup synapses) {
        if (synapses.source == synapses.target) {
            synapses.s.put(DoubleMatrix.eye(synapses.source.getSize()), 0);
        }
    }
    
    private static void randomizeWeights(SynapseGroup synapses, double minWeight, double maxWeight) {
        synapses.w = DoubleMatrix.rand(synapses.source.getSize(), synapses.target.getSize());
        synapses.w.muli(maxWeight - minWeight).addi(minWeight);
        synapses.w.muli(synapses.s);
    }
    
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix s;
    private DoubleMatrix w;
    private DoubleMatrix g;
    
    private SynapseGroup(NeuronGroup source, NeuronGroup target) {
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        s = DoubleMatrix.zeros(source.getSize(), target.getSize());
        w = DoubleMatrix.zeros(source.getSize(), target.getSize());
        g = DoubleMatrix.zeros(target.getSize());
    }
    
    public void update(double dt) {
        g.fill(0);
        g.addi(w.getRows(source.getSpikes()).columnSums());
    }
    
    public NeuronGroup getSource() {
        return source;
    }
    
    public NeuronGroup getTarget() {
        return target;
    }
    
    public DoubleMatrix getStates() {
        return s;
    }
    
    public DoubleMatrix getWeights() {
        return w;
    }
    
    public DoubleMatrix getConductances() {
        return g;
    }
}
