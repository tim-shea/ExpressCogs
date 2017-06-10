package expresscogs.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jblas.DoubleMatrix;

public class AdditiveInputGenerator implements InputGenerator {
    private List<InputGenerator> generators = new ArrayList<InputGenerator>();
    private DoubleMatrix stimulus;
    
    public AdditiveInputGenerator(InputGenerator... stimuli) {
        this.generators.addAll(Arrays.asList(stimuli));
    }
    
    @Override
    public void setNeuronGroup(NeuronGroup neurons) {
        for (InputGenerator generator : generators) {
            generator.setNeuronGroup(neurons);
        }
        stimulus = DoubleMatrix.zeros(neurons.getSize());
    }
    
    @Override
    public DoubleMatrix generate() {
        stimulus.fill(0);
        for (InputGenerator generator : generators) {
            stimulus.addi(generator.generate());
        }
        return stimulus;
    }
}
