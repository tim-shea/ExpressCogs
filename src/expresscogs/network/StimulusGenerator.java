package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.InputGenerator;
import expresscogs.network.NeuronGroup;

/**
 * StimulusGenerator generates input to a neuron group from a random noise term
 * combined with position-based stimuli presented at fixed intervals. The stimuli
 * are centered at a randomly selected neuron position and stimulus intensity
 * falls off linearly to zero at the specified stimulus width.
 * 
 * @author Tim
 *
 */
public class StimulusGenerator implements InputGenerator {
    private DoubleMatrix stimuli;
    private int numberStimuli = 2;
    private double noise = 0.5e-3;
    private int duration = 500;
    private int interval = 500;
    private double intensity = 1e-3;
    private double width = 0.1;
    private int state = 0;
    private int step = 0;
    
    /** Return the scale of random noise added to the stimulus each update. */
    public double getNoise() {
        return noise;
    }
    
    /** Assign the scale of random noise added to the stimulus each update. */
    public void setNoise(double value) {
        noise = value;
    }
    
    /** Return the duration of the stimulus in steps. */
    public int getDuration() {
        return duration;
    }
    
    /** Assign the duration of the stimulus in steps. */
    public void setDuration(int value) {
        duration = value;
    }
    
    /** Return the interval between stimuli in steps. */
    public int getInterval() {
        return interval;
    }
    
    /** Assign the interval between stimuli in steps. */
    public void setInterval(int value) {
        interval = value;
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
        if (step % (duration + interval) == 0) {
            stimuli = DoubleMatrix.zeros(neurons.getSize());
            for (int i = 0; i < numberStimuli; ++i) {
                double x = width + (1 - 2 * width) * Math.random();
                DoubleMatrix stimulus = MatrixFunctions.absi(neurons.getXPosition().sub(x)).subi(width).negi();
                stimulus.put(stimulus.lt(0), 0);
                stimulus.muli((i + 1) * intensity / width);
                stimuli.addi(stimulus);
            }
            state = 1;
        } else if (step % (duration + interval) == duration) {
            stimuli.fill(intensity / neurons.getSize());
            state = 0;
        }
        ++step;
        return stimuli.add(DoubleMatrix.rand(neurons.getSize()).muli(noise));
    }
    
    public int getState() {
        return state;
    }
}