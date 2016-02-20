package expresscogs.network;

import java.util.ArrayList;
import java.util.List;

public class Network {
    public static Network createSimpleNetwork(int size, double connectivity) {
        Network network = new Network();
        
        NeuronGroup neurons = NeuronGroup.createInhibitory("N", size);
        network.addNeuronGroups(neurons);
        
        SynapseGroup synapses = SynapseGroup.connectNonLocalRandom(neurons, neurons, 0.5, 0.25);
        network.addSynapseGroups(synapses);
        
        return network;
    }
    
    public static Network createReservoirNetwork() {
        Network network = new Network();
        
        NeuronGroup input = NeuronGroup.createExcitatory("IN", 100);
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", 160);
        NeuronGroup inhibitory = NeuronGroup.createExcitatory("INH", 20);
        NeuronGroup output = NeuronGroup.createExcitatory("OUT", 100);
        network.addNeuronGroups(input, excitatory, inhibitory, output);
        
        SynapseGroup inputToExcitatory = SynapseGroup.connectSparseRandom(input, excitatory, 0.1);
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectSparseRandom(excitatory, excitatory, 0.1);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectSparseRandom(excitatory, inhibitory, 0.1);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectSparseRandom(inhibitory, excitatory, 0.25);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectSparseRandom(excitatory, output, 0.1);
        network.addSynapseGroups(inputToExcitatory, excitatoryToExcitatory, excitatoryToInhibitory,
                inhibitoryToExcitatory, excitatoryToOutput);
        
        return network;
    }
    
    public static Network createTopologicalNetwork() {
        Network network = new Network();
        
        NeuronGroup input = NeuronGroup.createExcitatory("IN", 100);
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC", 160);
        NeuronGroup inhibitory = NeuronGroup.createInhibitory("INH", 40);
        NeuronGroup output = NeuronGroup.createExcitatory("OUT", 100);
        network.addNeuronGroups(input, excitatory, inhibitory, output);
        
        SynapseGroup inputToExcitatory = SynapseGroup.connectLocalRandom(input, excitatory, 0.1, 0.1);
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectSparseRandom(excitatory, excitatory, 0.1);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectLocalRandom(excitatory, inhibitory, 0.5, 0.1);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectLocalRandom(inhibitory, excitatory, 0.5, 0.5);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectLocalRandom(excitatory, output, 0.1, 0.1);
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
