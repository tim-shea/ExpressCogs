package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import expresscogs.network.NeuronGroup;

/**
 * SynapseFactory creates SynapseGroups between NeuronGroups. 
 * @author Tim
 */
public final class SynapseFactory {
    public static double minWeight = 0.25;
    public static double maxWeight = 1.0;
    
    public static SynapseGroup connect(String name, NeuronGroup source, NeuronGroup target, SynapseGroupTopology topology, double weightScale) {
        DoubleMatrix connections = topology.generateConnections(source, target);
        NoDelaySynapseGroup synapses = new NoDelaySynapseGroup(name, source, target, connections);
        SynapseFactory.randomizeWeights(synapses, minWeight, maxWeight);
        synapses.setWeightScale(weightScale);
        return synapses;
    }
    
    public static SynapseGroup connect(NeuronGroup source, NeuronGroup target, SynapseGroupTopology topology, double weightScale) {
        String name = source.getName() + "_" + target.getName();
        return connect(name, source, target, topology, weightScale);
    }
    
    public static SynapseGroup connectWithDelay(String name, NeuronGroup source, NeuronGroup target, SynapseGroupTopology topology, double weightScale, int delay) {
        DoubleMatrix connections = topology.generateConnections(source, target);
        FixedDelaySynapseGroup synapses = new FixedDelaySynapseGroup(name, source, target, connections, delay);
        SynapseFactory.randomizeWeights(synapses, minWeight, maxWeight);
        synapses.setWeightScale(weightScale);
        return synapses;
    }
    
    public static SynapseGroup connectWithDelay(NeuronGroup source, NeuronGroup target, SynapseGroupTopology topology, double weightScale, int delay) {
        String name = source.getName() + "_" + target.getName();
        return connectWithDelay(name, source, target, topology, weightScale, delay);
    }
    
    private static void randomizeWeights(SynapseGroup synapses, double minWeight, double maxWeight) {
        DoubleMatrix weights = synapses.getWeights();
        DoubleMatrix scale = DoubleMatrix.rand(weights.rows, weights.columns);
        scale.muli(maxWeight - minWeight).addi(minWeight);
        weights.muli(scale);
    }
}
