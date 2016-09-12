package expresscogs.network;

import org.jblas.DoubleMatrix;

public class UniformNoiseGenerator implements InputGenerator {
    private double scale;
    
    public UniformNoiseGenerator(double scale) {
        this.scale = scale;
    }
    
    public DoubleMatrix generate(NeuronGroup neurons) {
        return DoubleMatrix.rand(neurons.getSize()).muli(scale);
    }
}