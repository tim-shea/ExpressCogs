package expresscogs.network;

public class TopologicalLayerFactory {
    public int size = 1000;
    public double excitatoryRatio = 0.75;
    public double excitatoryNoise = 1.2e-9;
    public double inhibitoryNoise = 1.2e-9;
    public double connectivity = 0.1;
    public double neighborhood = 0.02;
    public double minimumWeight = 5e-8;
    public double maximumWeight = 10e-8;
    
    public void createRecurrent(Network network) {
        NeuronGroup excitatory = NeuronGroup.createExcitatory("EXC",
                (int)(size * excitatoryRatio), excitatoryNoise);
        NeuronGroup inhibitory = NeuronGroup.createInhibitory("INH",
                (int)(size * (1 - excitatoryRatio)), inhibitoryNoise);
        network.addNeuronGroups(excitatory, inhibitory);
        connectRecurrent(network, excitatory, inhibitory);
    }
    
    public void connectRecurrent(Network network, NeuronGroup excitatory, NeuronGroup inhibitory) {
        SynapseGroup excitatoryToExcitatory = SynapseGroup.connectNeighborhood(excitatory, excitatory,
                connectivity, neighborhood, minimumWeight, maximumWeight);
        SynapseGroup excitatoryToInhibitory = SynapseGroup.connectNeighborhood(excitatory, inhibitory,
                connectivity, neighborhood / 2, minimumWeight, maximumWeight);
        SynapseGroup inhibitoryToExcitatory = SynapseGroup.connectNonNeighborhood(inhibitory, excitatory,
                connectivity, neighborhood, minimumWeight, maximumWeight);
        network.addSynapseGroups(excitatoryToExcitatory, excitatoryToInhibitory, inhibitoryToExcitatory);
    }
    
    public void createFeedForward(Network network) {
        //NeuronGroup 
    }
}
