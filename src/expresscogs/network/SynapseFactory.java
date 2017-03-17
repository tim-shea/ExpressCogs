package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public final class SynapseFactory {
    public static SynapseGroup connect(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix connectivity, double minWeight, double maxWeight) {
        DoubleMatrix index = generateConnections(connectivity, source != target);
        DelaySynapseGroup synapses = new DelaySynapseGroup(name, source, target, index, 10);
        randomizeWeights(synapses, minWeight, maxWeight);
        randomizeDelays(synapses, 10);
        return synapses;
    }
    
    public static SynapseGroup connectUniformRandom(NeuronGroup source, NeuronGroup target, double connectivity, double minWeight, double maxWeight) {
        return connectUniformRandom(source.getName() + "_" + target.getName(), source, target, connectivity, minWeight, maxWeight);
    }
    
    public static SynapseGroup connectUniformRandom(String name, NeuronGroup source, NeuronGroup target, double connectivity, double minWeight, double maxWeight) {
        DoubleMatrix p = DoubleMatrix.ones(source.getSize(), target.getSize()).muli(connectivity);
        return connect(name, source, target, p, minWeight, maxWeight);
    }
    
    public static SynapseGroup connectNeighborhood(NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood, double minWeight, double maxWeight) {
        return connectNeighborhood(source.getName() + "_" + target.getName(), source, target, connectivity, neighborhood,
                minWeight, maxWeight);
    }
    
    public static SynapseGroup connectNeighborhood(String name, NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood, double minWeight, double maxWeight) {
        DoubleMatrix d = source.getXPosition().repmat(1, target.getSize());
        d.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(d);
        DoubleMatrix p = normalPdf(d, 0.0, neighborhood);
        p.diviColumnVector(p.rowMeans()).muli(connectivity);
        return connect(name, source, target, p, minWeight, maxWeight);
    }
    
    public static SynapseGroup connectNonNeighborhood(String name, NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood, double minW, double maxW) {
        DoubleMatrix d = source.getXPosition().repmat(1, target.getSize());
        d.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(d);
        DoubleMatrix p = normalPdf(d, 0.0, 10 * neighborhood);
        p.diviColumnVector(p.rowMeans().div(2));
        DoubleMatrix p2 = normalPdf(d, 0.0, neighborhood);
        p2.diviColumnVector(p2.rowMeans());
        p.subi(p2).addi(p2.rowMaxs().repmat(1, p.columns));
        p.diviColumnVector(p.rowMeans()).muli(connectivity);
        DoubleMatrix index = generateConnections(p, source != target);
        DelaySynapseGroup synapses = new DelaySynapseGroup(name, source, target, index, 10);
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
    
    private static DoubleMatrix generateConnections(DoubleMatrix probability, boolean selfSynapses) {
        DoubleMatrix random = DoubleMatrix.rand(probability.rows, probability.columns);
        DoubleMatrix c = random.lt(probability);
        if (!selfSynapses) {
            c.put(DoubleMatrix.eye(probability.rows), 0);
        }
        return flattenConnectionMatrix(c);
    }
    
    private static DoubleMatrix flattenConnectionMatrix(DoubleMatrix c) {
        int count = 0;
        int numSynapses = (int)c.sum();
        DoubleMatrix index = DoubleMatrix.zeros(numSynapses, 2);
        for (int i : c.findIndices()) {
            index.put(count, 0, c.indexRows(i));
            index.put(count, 1, c.indexColumns(i));
            ++count;
        }
        return index;
    }
    
    private static void randomizeWeights(SynapseGroup synapses, double minWeight, double maxWeight) {
        synapses.getWeights().copy(DoubleMatrix.rand(synapses.getPreIndex().length));
        synapses.getWeights().muli(maxWeight - minWeight).addi(minWeight);
    }
    
    private static void randomizeDelays(DelaySynapseGroup synapses, int maxDelay) {
        synapses.getDelays().copy(DoubleMatrix.rand(synapses.getPreIndex().length));
        MatrixFunctions.floori(synapses.getDelays().muli(maxDelay));
    }
    
    private static void fixDelays(DelaySynapseGroup synapses, int delay) {
        synapses.getDelays().copy(DoubleMatrix.ones(synapses.getPreIndex().length));
        synapses.getDelays().muli(delay);
    }
}
