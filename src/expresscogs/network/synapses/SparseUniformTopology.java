package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import expresscogs.network.NeuronGroup;

/**
 * SparseUniformTopology generates SynapseGroup connections based on a uniform probability
 * of connection between source and target neurons.
 * 
 * @author Tim
 */
public class SparseUniformTopology implements SynapseGroupTopology {
    private double connectivity = 0.1;
    private boolean selfSynapses = false;
    
    /** Get the probability of generating a connection between each pair of source and target neurons. */
    public double getConnctivity() {
        return connectivity;
    }
    
    /** Set the probability of generating a connection between each pair of source and target neurons. */
    public void setConnectivity(double value) {
        connectivity = value;
    }
    
    /** Get whether synapses are allowed from a neuron to itself in recurrent (source == target) SynapseGroups. */
    public boolean getSelfSynapses() {
        return selfSynapses;
    }
    
    /** Set whether synapses are allowed from a neuron to itself in recurrent (source == target) SynapseGroups. */
    public void setSelfSynapses(boolean value) {
        selfSynapses = value;
    }
    
    @Override
    public DoubleMatrix generateConnections(NeuronGroup source, NeuronGroup target) {
        DoubleMatrix connections = DoubleMatrix.rand(source.getSize(), target.getSize());
        connections.lti(connectivity);
        if (source == target && !selfSynapses) {
            connections.put(DoubleMatrix.eye(connections.rows), 0);
        }
        return connections;
    }
}
