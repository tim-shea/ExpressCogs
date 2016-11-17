package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public class SynapseGroup {
    public static SynapseGroup connect(NeuronGroup source, NeuronGroup target, DoubleMatrix connectivity, double minWeight, double maxWeight) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        generateSynapses(synapses, connectivity);
        randomizeWeights(synapses, minWeight, maxWeight);
        randomizeDelays(synapses, 10);
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
        randomizeWeights(synapses, minW, maxW);
        randomizeDelays(synapses, 10);
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
        /*
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
        */
        return synapses;
    }
    
    public static SynapseGroup connectOpponentGroups(NeuronGroup source, NeuronGroup target, int motorGroups, int neuronsPerGroup, double connectivity) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        /*
        generateSynapses(synapses, connectivity);
        DoubleMatrix sourceGroups = DoubleMatrix.linspace(0, source.getSize() - 1, source.getSize()).divi(source.getSize() / motorGroups);
        MatrixFunctions.floori(sourceGroups);
        DoubleMatrix targetGroups = DoubleMatrix.linspace(0, target.getSize() - 1, target.getSize()).divi(target.getSize() / motorGroups).transpose();
        MatrixFunctions.floori(targetGroups);
        synapses.s.muli(sourceGroups.repmat(1, target.getSize()).nei(targetGroups.repmat(source.getSize(), 1)));
        pruneSelfSynapses(synapses);
        randomizeWeights(synapses, 0e-9, 4e-9);
        */
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
        DoubleMatrix s = random.lt(probability);
        if (synapses.source == synapses.target) {
            s.put(DoubleMatrix.eye(synapses.source.getSize()), 0);
        }
        int count = 0;
        int numSynapses = (int)s.sum();
        synapses.preIndex = DoubleMatrix.zeros(numSynapses);
        synapses.postIndex = DoubleMatrix.zeros(numSynapses);
        for (int i : s.findIndices()) {
            synapses.preIndex.put(count, s.indexRows(i));
            synapses.postIndex.put(count, s.indexColumns(i));
            ++count;
        }
    }
    
    private static void randomizeWeights(SynapseGroup synapses, double minWeight, double maxWeight) {
        synapses.weights = DoubleMatrix.rand(synapses.preIndex.length);
        synapses.weights.muli(maxWeight - minWeight).addi(minWeight);
    }
    
    private static void randomizeDelays(SynapseGroup synapses, int maxDelay) {
        synapses.maxDelay = maxDelay;
        synapses.delays = DoubleMatrix.rand(synapses.preIndex.length);
        MatrixFunctions.floori(synapses.delays.muli(maxDelay));
        synapses.conductances = DoubleMatrix.zeros(synapses.target.getSize(), maxDelay);
    }
    
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix preIndex;
    private DoubleMatrix postIndex;
    private DoubleMatrix weights;
    private DoubleMatrix delays;
    private DoubleMatrix conductances;
    private int maxDelay;
    
    private SynapseGroup(NeuronGroup source, NeuronGroup target) {
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        preIndex = DoubleMatrix.zeros(0);
        postIndex = DoubleMatrix.zeros(0);
        weights = DoubleMatrix.zeros(0);
        delays = DoubleMatrix.zeros(0);
        conductances = DoubleMatrix.zeros(target.getSize(), 1);
    }
    
    public void update(int step) {
        // Check this math!!!
        conductances.putColumn(step % maxDelay, conductances.getColumn(step % maxDelay).fill(0));
        int[] spikes = source.getSpikes().findIndices();
        for (int n : spikes) {
            int[] synapses = preIndex.eq(n).findIndices();
            DoubleMatrix w = weights.get(synapses);
            DoubleMatrix t = postIndex.get(synapses);
            DoubleMatrix d = delays.get(synapses).add(step);
            d.divi(maxDelay);
            d = d.sub(MatrixFunctions.floor(d)).mul(maxDelay);
            int[] indices = t.add(d.mul(conductances.rows)).toIntArray();
            w.addi(conductances.get(indices));
            conductances.put(indices, w);
        }
    }
    
    public NeuronGroup getSource() {
        return source;
    }
    
    public NeuronGroup getTarget() {
        return target;
    }
    
    public DoubleMatrix getWeights() {
        return weights;
    }
    
    public DoubleMatrix getConductances(int step) {
        return conductances.getColumn(step % maxDelay);
    }
}
