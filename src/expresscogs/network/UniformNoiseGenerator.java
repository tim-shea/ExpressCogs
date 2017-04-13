package expresscogs.network;

import org.jblas.DoubleMatrix;

public class UniformNoiseGenerator implements InputGenerator {
    private int size;
    private double scale;
    
    public UniformNoiseGenerator(double scale) {
        this.scale = scale;
    }
    
    public void setNeuronGroup(NeuronGroup neurons) {
        size = neurons.getSize();
    }
    
    public DoubleMatrix generate() {
        return DoubleMatrix.rand(size).muli(scale);
    }
}