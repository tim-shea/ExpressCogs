package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.InputGenerator;
import expresscogs.network.NeuronGroup;

/**
 * ContinuousStimulusGenerator generates input to a neuron group from a random noise
 * term combined with position-based stimuli. The stimulus falls off linearly to zero
 * at the specified stimulus width.
 * 
 * @author Tim
 *
 */
public class ContinuousStimulusGenerator implements InputGenerator {
    private DoubleMatrix stimulus;
    private double position = 0.5;
    private double noise = 0.5e-3;
    private double intensity = 3e-3;
    private double width = 0.1;
    
    /** Return the scale of random noise added to the stimulus each update. */
    public double getNoise() {
        return noise;
    }
    
    /** Assign the scale of random noise added to the stimulus each update. */
    public void setNoise(double value) {
        noise = value;
    }
    
    /** Return the position of the stimulus. */
    public double getPosition() {
        return position;
    }
    
    /** Assign the position of the stimulus. */
    public void setPosition(double value) {
        position = value;
    }
    
    /** Return the scale of activation at the center of the stimulus. */
    public double getIntensity() {
        return intensity;
    }
    
    /** Assign the scale of activation at the center of the stimulus. */
    public void setIntensity(double value) {
        intensity = value;
    }
    
    /** Return the width of the stimulus in neuronal coordinates i.e. the distance from the
     * center of the stimulus at which activation is zero. */
    public double getWidth() {
        return width;
    }
    
    /** Assign the width of the stimulus in neuronal coordinates i.e. the distance from the
     * center of the stimulus at which activation is zero. */
    public void setWidth(double value) {
        width = value;
    }
    
    /** Return the signal-to-noise ratio of the generated input values. */
    public double getSignalToNoiseRatio() {
        return intensity / noise;
    }
    
    /** Assign the signal-to-noise ratio of the generated input values by adjusting the stimulus
     * scale according to the magnitude of the noise. */
    public void setSignalToNoiseRatio(double value) {
        intensity = value * noise;
    }
    
    @Override
    public DoubleMatrix generate(NeuronGroup neurons) {
        stimulus = MatrixFunctions.absi(neurons.getXPosition().sub(position)).subi(width).negi();
        stimulus.put(stimulus.lt(0), 0);
        stimulus.muli(intensity / width);
        stimulus.addi(noise);
        return DoubleMatrix.rand(neurons.getSize()).muli(stimulus);
    }
}