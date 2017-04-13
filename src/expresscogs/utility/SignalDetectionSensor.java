package expresscogs.utility;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.TopologicalStimulusGenerator;
import expresscogs.network.NeuronGroup;

public class SignalDetectionSensor {
    private NeuronGroup neurons;
    private TopologicalStimulusGenerator generator;
    private double signalStrength;
    private double noiseStrength;
    private double frequency = 1000;
    
    public SignalDetectionSensor(NeuronGroup neurons, TopologicalStimulusGenerator generator) {
        this.neurons = neurons;
        this.generator = generator;
    }
    
    public double getSignalStrength() {
        return signalStrength;
    }
    
    public double getNoiseStrength() {
        return noiseStrength;
    }
    
    public void update(double t) {
        double signal = generator.getPosition();
        double width = generator.getWidth();
        DoubleMatrix signalIndex = MatrixFunctions.abs(neurons.getXPosition().sub(signal)).lt(width);
        signalStrength = neurons.getSpikes().get(signalIndex).mean() * frequency;
        noiseStrength = neurons.getSpikes().get(signalIndex.not()).mean() * frequency;
    }
}
