package expresscogs.network;

import org.jblas.DoubleMatrix;

public class UniformNoiseGenerator implements InputGenerator {
    private int size;
    private double scale;
    private double constant;
    
    public UniformNoiseGenerator(double scale) {
        this.scale = scale;
        constant = 0;
    }
    
    public UniformNoiseGenerator(double scale, double constant) {
        this.scale = scale;
        this.constant = constant;
    }
    
    public void setNeuronGroup(NeuronGroup neurons) {
        size = neurons.getSize();
    }
    
    public DoubleMatrix generate() {
        return DoubleMatrix.rand(size).muli(scale).addi(constant);
    }
    
    public double getScale() {
        return scale;
    }
    
    public void setScale(double value) {
        scale = value;
    }
}
