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

        SynapseGroup inputToExcitatory = SynapseGroup.connectUniformRandom(input, excitatory, 0.1, 0, 1e-9);
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectUniformRandom(excitatory, excitatory, 0.1, 0, 1e-9);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectUniformRandom(excitatory, inhibitory, 0.1, 0, 1e-9);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectUniformRandom(inhibitory, excitatory, 0.25, 0, 1e-9);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectUniformRandom(excitatory, output, 0.1, 0, 1e-9);
        network.addSynapseGroups(inputToExcitatory, excitatoryToExcitatory, excitatoryToInhibitory,
                inhibitoryToExcitatory, excitatoryToOutput);

        return network;
    }

    public static Network createMotorNetwork(int motorGroups, int neuronsPerGroup) {
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", motorGroups * neuronsPerGroup, 1.5e-9);
        NeuronGroup inhibitory = NeuronGroup.createInhibitory("INH", motorGroups * neuronsPerGroup / 4,
                new InputGenerator() {
                    int step = 0;
                    int group = 0;

                    public DoubleMatrix generate(NeuronGroup neurons) {
                        DoubleMatrix i = DoubleMatrix.rand(neurons.getSize()).muli(1.2e-9);
                        if (step % 2000 == 0) {
                            group = (int) (Math.random() * motorGroups);
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

        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectGroups(excitatory, inhibitory, motorGroups,
                neuronsPerGroup, 0.25);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectOpponentGroups(inhibitory, excitatory, motorGroups,
                neuronsPerGroup, 0.25);

        Network network = new Network();
        network.addNeuronGroups(excitatory, inhibitory);
        network.addSynapseGroups(excitatoryToInhibitory, inhibitoryToExcitatory);
        return network;
    }
    
    private List<NeuronGroup> neuronGroups = new ArrayList<NeuronGroup>();
    private List<SynapseGroup> synapseGroups = new ArrayList<SynapseGroup>();
    
    public void addNeuronGroups(NeuronGroup... groups) {
        for (NeuronGroup neurons : groups) {
            neuronGroups.add(neurons);
        }
    }
    
    public NeuronGroup getNeuronGroup(int index) {
        return neuronGroups.get(index);
    }
    
    public NeuronGroup getNeuronGroup(String name) {
        for (NeuronGroup neurons : neuronGroups) {
            if (neurons.getName().equals(name)) {
                return neurons;
            }
        }
        return null;
    }
    
    public void addSynapseGroups(SynapseGroup... groups) {
        for (SynapseGroup synapses : groups) {
            synapseGroups.add(synapses);
        }
    }
    
    public void update(double dt) {
        for (NeuronGroup neurons : neuronGroups) {
            neurons.update(dt);
        }
        for (SynapseGroup synapses : synapseGroups) {
            synapses.update(dt);
        }
    }
}
