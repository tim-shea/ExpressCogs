package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.InputGenerator;
import expresscogs.network.NeuronGroup;

/**
 * ContinuousStimulusGenerator generates input to a neuron group from a random noise
 * term combined with position-based stimuli. The stimulus falls off linearly to zero
 * at the specified stimulus width.
 */
public class TopologicalStimulusGenerator implements InputGenerator {
    public enum Shape {
        TRIANGLE {
            @Override
            public DoubleMatrix generate(DoubleMatrix x, double center, double width, double height) {
                DoubleMatrix stimulus = MatrixFunctions.absi(x.sub(center));
                stimulus.negi().addi(width);
                stimulus.put(stimulus.lt(0), 0);
                stimulus.muli(height / width);
                return stimulus;
            }
        },
        
        NOTCH {
            @Override
            public DoubleMatrix generate(DoubleMatrix x, double center, double width, double height) {
                DoubleMatrix stimulus = MatrixFunctions.absi(x.sub(center));
                stimulus.lti(width / 2);
                stimulus.muli(height);
                return stimulus;
            }
        },
        
        GAUSSIAN {
            @Override
            public DoubleMatrix generate(DoubleMatrix x, double center, double width, double height) {
                double quarterWidthSqr = (0.25 * width * width);
                DoubleMatrix stimulus = x.sub(center);
                stimulus.muli(stimulus).divi(-quarterWidthSqr);
                MatrixFunctions.expi(stimulus);
                stimulus.muli(height);
                return stimulus;
            }
        };
        
        public abstract DoubleMatrix generate(DoubleMatrix x, double center, double width, double height);
    }
    
    private DoubleMatrix neuronPositions;
    private DoubleMatrix stimulus;
    private Shape shape = Shape.GAUSSIAN;
    private double position = 0.5;
    private double noise = 1e-3;
    private double intensity = 0.5e-3;
    private double width = 0.125;
    private boolean randomize = true;
    private int interval = 2000;
    private int step;
    
    @Override
    public void setNeuronGroup(NeuronGroup neurons) {
        neuronPositions = neurons.getXPosition();
        generateStimulus();
    }
    
    /** Return the shape of the stimulus. */
    public Shape getShape() {
        return shape;
    }
    
    /** Assign the shape of the stimulus. */
    public void setShape(Shape value) {
        shape = value;
        generateStimulus();
    }
    
    /** Return the scale of random noise added to the stimulus each update. */
    public double getNoise() {
        return noise;
    }
    
    /** Assign the scale of random noise added to the stimulus each update. */
    public void setNoise(double value) {
        noise = value;
        generateStimulus();
    }
    
    /** Return the position of the stimulus. */
    public double getPosition() {
        return position;
    }
    
    /** Assign the position of the stimulus. */
    public void setPosition(double value) {
        position = value;
        generateStimulus();
    }
    
    /** Return the scale of activation at the center of the stimulus. */
    public double getIntensity() {
        return intensity;
    }
    
    /** Assign the scale of activation at the center of the stimulus. */
    public void setIntensity(double value) {
        intensity = value;
        generateStimulus();
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
        generateStimulus();
    }
    
    /** Return the signal-to-noise ratio of the generated input values. */
    public double getSignalToNoiseRatio() {
        if (noise == 0) {
            return 0;
        } else {
            return intensity / noise;
        }
    }
    
    /** Assign the signal-to-noise ratio of the generated input values by adjusting the stimulus
     * scale according to the magnitude of the noise. */
    public void setSignalToNoiseRatio(double value) {
        intensity = value * noise;
        generateStimulus();
    }
    
    /** Return whether the stimulus will be randomized. If true, the position and signal-to-noise
     *  ratio of the stimulus generator will be drawn from a uniform random distribution after an
     *  interval of timesteps. */
    public boolean getRandomize() {
        return randomize;
    }
    
    /** Assign whether the stimulus will be randomized. If true, the position and signal-to-noise
     *  ratio of the stimulus generator will be drawn from a uniform random distribution after an
     *  interval of timesteps. */
    public void setRandomize(boolean value) {
        randomize = value;
    }
    
    /** Return the interval after which the stimulus generator will randomize if randomize is true. */
    public int getInterval() {
        return interval;
    }
    
    /** Assign the interval after which the stimulus generator will randomize if randomize is true. */
    public void setInterval(int value) {
        interval = value;
    }
    
    @Override
    public DoubleMatrix generate() {
        if (randomize) {
            if (step % interval == 0) {
                setPosition(Math.random() * 0.9 + 0.05);
                setSignalToNoiseRatio(Math.random() * 2);
            }
            ++step;
        }
        return DoubleMatrix.rand(neuronPositions.length).muli(stimulus);
    }
    
    private void generateStimulus() {
        if (neuronPositions == null) {
            return;
        }
        stimulus = shape.generate(neuronPositions, position, width, intensity);
        stimulus.addi(noise);
        stimulus.put(neuronPositions.lt(0.05), 0);
        stimulus.put(neuronPositions.gt(0.95), 0);
        //DoubleMatrix leftEdge = neuronPositions.lt(0.2);
        //DoubleMatrix leftFalloff = neuronPositions.get(leftEdge).add(0.8);
        //stimulus.put(leftEdge, stimulus.get(leftEdge).mul(leftFalloff));
        //DoubleMatrix rightEdge = neuronPositions.gt(0.8);
        //DoubleMatrix rightFalloff = neuronPositions.get(rightEdge).rsub(1.0).add(0.8);
        //stimulus.put(rightEdge, stimulus.get(rightEdge).mul(rightFalloff));
    }
}
