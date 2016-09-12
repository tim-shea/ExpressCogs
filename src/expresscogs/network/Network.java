package expresscogs.network;

import java.util.ArrayList;
import java.util.List;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.PointRange;
import org.jblas.ranges.Range;

public class Network {
    public static Network createReservoirNetwork() {
        Network network = new Network();
        
        NeuronGroup input = NeuronGroup.createExcitatory("IN", 100);
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", 160);
        NeuronGroup inhibitory = NeuronGroup.createExcitatory("INH", 20);
        NeuronGroup output = NeuronGroup.createExcitatory("OUT", 100);
        network.addNeuronGroups(input, excitatory, inhibitory, output);
        
        SynapseGroup inputToExcitatory = SynapseGroup.connectUniformRandom(input, excitatory, 0.1, 1e-9);
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectUniformRandom(excitatory, excitatory, 0.1, 1e-9);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectUniformRandom(excitatory, inhibitory, 0.1, 1e-9);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectUniformRandom(inhibitory, excitatory, 0.25, 1e-9);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectUniformRandom(excitatory, output, 0.1, 1e-9);
        network.addSynapseGroups(inputToExcitatory, excitatoryToExcitatory, excitatoryToInhibitory,
                inhibitoryToExcitatory, excitatoryToOutput);
        
        return network;
    }
    
    public static Network createMotorNetwork(int motorGroups, int neuronsPerGroup) {
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", motorGroups * neuronsPerGroup, 1.5e-9);
        NeuronGroup inhibitory = NeuronGroup.createInhibitory("INH", motorGroups * neuronsPerGroup / 4,  new InputGenerator() {
            int step = 0;
            int group = 0;
            
            public DoubleMatrix generate(NeuronGroup neurons) {
                DoubleMatrix i = DoubleMatrix.rand(neurons.getSize()).muli(1.2e-9);
                if (step % 2000 == 0) {
                    group = (int)(Math.random() * motorGroups);
                    System.out.println("Step: " + step + " \tGroup: " + group);
                } else if (step % 2000 < 500) {
                    int a = group * neuronsPerGroup / 4;
                    int b = (group + 1) * neuronsPerGroup / 4;
                    Range r = new IntervalRange(a, b);
                    DoubleMatrix stimulus = i.getRange(a, b);
                    stimulus.addi(0.2e-9);
                    i.put(r, new PointRange(0), stimulus);
                }
                step++;
                return i;
            }
        });
        
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectGroups(excitatory, inhibitory, motorGroups, neuronsPerGroup, 0.25);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectOpponentGroups(inhibitory, excitatory, motorGroups, neuronsPerGroup, 0.25);
        
        Network network = new Network();
        network.addNeuronGroups(excitatory, inhibitory);
        network.addSynapseGroups(excitatoryToInhibitory, inhibitoryToExcitatory);
        return network;
    }
    
    public static Network createTopologicalNetwork(int numberNeurons) {
        Network network = new Network();
        
        NeuronGroup input = NeuronGroup.createExcitatory("IN", 200, new InputGenerator() {
            int step = 0;
            double x = 0;
            
            public DoubleMatrix generate(NeuronGroup neurons) {
                DoubleMatrix x = neurons.getXPosition();
                DoubleMatrix i = x.sub(0.5 + 0.3 * Math.sin(step / 600.0));
                MatrixFunctions.absi(i).subi(0.2).negi().muli(6e-9);
                i.put(i.lt(0), 0);
                step++;
                if (step % 10000 > 5000)
                    i = DoubleMatrix.zeros(neurons.getSize());
                return i;
            }
        });
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", 3 * numberNeurons / 4, 1.3e-9);
        NeuronGroup inhibitory = NeuronGroup.createInhibitory("INH", numberNeurons / 4, 1.22e-9);
        NeuronGroup output = NeuronGroup.createExcitatory("OUT", 200, 1.2e-9);
        network.addNeuronGroups(input, excitatory, inhibitory, output);
        
        SynapseGroup inputToExcitatory = SynapseGroup.connectNeighborhood(input, excitatory, 0.1, 0.05, 0.5e-9, 1e-9);
        //SynapseGroup inputToExcitatory = SynapseGroup.connectUniformRandom(input, excitatory, 0.1, 1e-9);
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectNeighborhood(excitatory, excitatory, 0.1, 0.025, 0e-9, 1e-9);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectNeighborhood(excitatory, inhibitory, 0.1, 0.01, 0e-9, 1e-9);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectNonNeighborhood(inhibitory, excitatory, 0.1, 0.025, 1e-9);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectNeighborhood(excitatory, output, 0.1, 0.01, 0e-9, 1e-9);
        network.addSynapseGroups(inputToExcitatory, excitatoryToExcitatory, excitatoryToInhibitory,
                inhibitoryToExcitatory, excitatoryToOutput);
        
        return network;
    }
    
    private List<NeuronGroup> neuronGroups = new ArrayList<NeuronGroup>();
    private List<SynapseGroup> synapseGroups = new ArrayList<SynapseGroup>();
    
    private Network() {}
    
    public void addNeuronGroups(NeuronGroup... groups) {
        for (NeuronGroup neurons : groups)
            neuronGroups.add(neurons);
    }
    
    public NeuronGroup getNeuronGroup(int index) {
        return neuronGroups.get(index);
    }
    
    public void addSynapseGroups(SynapseGroup... groups) {
        for (SynapseGroup synapses : groups)
            synapseGroups.add(synapses);
    }
    
    public void update(double dt) {
        for (NeuronGroup neurons : neuronGroups)
            neurons.update(dt);
        for (SynapseGroup synapses : synapseGroups)
            synapses.update(dt);
    }
}
