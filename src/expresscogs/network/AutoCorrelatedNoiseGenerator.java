package expresscogs.network;

import org.jblas.DoubleMatrix;

public class AutoCorrelatedNoiseGenerator extends UniformNoiseGenerator {
    private DoubleMatrix i;
    
    public AutoCorrelatedNoiseGenerator(double scale) {
        super(scale);
    }
    
    public void setNeuronGroup(NeuronGroup neurons) {
        super.setNeuronGroup(neurons);
        i = DoubleMatrix.zeros(neurons.getSize());
    }
    
    public DoubleMatrix generate() {
        i.muli(0.9);
        i.addi(DoubleMatrix.rand(i.length).muli(0.1 * getScale()));
        i.put(i.lt(0), 0);
        i.put(i.gt(getScale()), getScale());
        return i;
    }
}