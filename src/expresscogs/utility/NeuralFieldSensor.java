package expresscogs.utility;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;

public class NeuralFieldSensor {
    private static final double decay = 0.7;
    
    private NeuronGroup neurons;
    private DoubleMatrix field;
    private double signalCenter;
    private double signalStrength;
    
    public NeuralFieldSensor(NeuronGroup neurons) {
        this.neurons = neurons;
        field = DoubleMatrix.zeros(neurons.getSize());
    }
    
    public DoubleMatrix getPosition() {
        return neurons.getXPosition();
    }
    
    public DoubleMatrix getActivity() {
        return field;
    }
    
    public double getSignalCenter() {
        return signalCenter;
    }
    
    public double getSignalStrength() {
        return signalStrength;
    }
    
    public void update(double t) {
        field.muli(decay);
        field.addi(neurons.getSpikes());
        signalCenter = neurons.getXPosition().get(field.argmax());
        DoubleMatrix signalIndex = MatrixFunctions.abs(neurons.getXPosition().sub(signalCenter)).lt(0.25);
        double fieldMean = field.sum();
        if (fieldMean > 0) {
            signalStrength = field.get(signalIndex).sum() / fieldMean;
        }
    }
}
