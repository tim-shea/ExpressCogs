package expresscogs.network;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private List<NeuronGroup> neuronGroups = new ArrayList<NeuronGroup>();
    private List<SynapseGroup> synapseGroups = new ArrayList<SynapseGroup>();
    
    public void addNeuronGroups(NeuronGroup... groups) {
        for (NeuronGroup neurons : groups) {
            neuronGroups.add(neurons);
        }
    }
    
    public List<NeuronGroup> getNeuronGroups() {
        return neuronGroups;
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
    
    public void update(int step) {
        for (NeuronGroup neurons : neuronGroups) {
            neurons.update(step);
        }
        for (SynapseGroup synapses : synapseGroups) {
            synapses.update(step);
        }
    }
}
