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
    private DoubleMatrix stimulus;
    private double noise = 0.5e-3;
    private int duration = 500;
    private int interval = 1000;
    private double scale = 3e-3;
    private double width = 0.05;
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
    public double getScale() {
        return scale;
    }
    
    /** Assign the scale of activation at the center of the stimulus. */
    public void setScale(double value) {
        scale = value;
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
        return scale / noise;
    }
    
    /** Assign the signal-to-noise ratio of the generated input values by adjusting the stimulus
     * scale according to the magnitude of the noise. */
    public void setSignalToNoiseRatio(double value) {
        scale = value * noise;
    }
    
    @Override
    public DoubleMatrix generate(NeuronGroup neurons) {
        if (step % (duration + interval) == 0) {
            double x1 = Math.random();
            DoubleMatrix stimulus1 = MatrixFunctions.absi(neurons.getXPosition().sub(x1)).subi(width).negi();
            stimulus1.put(stimulus1.lt(0), 0);
            stimulus1.muli(scale / width);
            stimulus = stimulus1;
            //double x2 = Math.random();
            //DoubleMatrix stimulus2 = MatrixFunctions.absi(neurons.getXPosition().sub(x2)).subi(width).negi();
            //stimulus2.put(stimulus2.lt(0), 0);
            //stimulus2.muli(0.5 * scale / width);
            //stimulus = stimulus1.addi(stimulus2);
            state = 1;
        } else if (step % (duration + interval) == duration) {
            stimulus.fill(0);
            state = 0;
        }
        ++step;
        return stimulus.add(DoubleMatrix.rand(neurons.getSize()).muli(noise));
    }
    
    public int getState() {
        return state;
    }
}