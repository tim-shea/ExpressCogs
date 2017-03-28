package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;

import expresscogs.network.NeuronGroup;

/**
 * SynapseGroup is an interface representing a synaptic pathway between two neuron groups. A synapse group is updated
 * each frame and generates a vector of post synaptic conductances which are subsequently applied to the post neuron
 * group.
 *
 * Author: Tim
 */
public interface SynapseGroup {
    /** Get the name of the synapse group. */
    String getName();

    /** Update the synapse group. */
    void update(int step);

    /** Get the source of the synaptic pathway. */
    NeuronGroup getSource();

    /** Get the target of the synaptic pathway. */
    NeuronGroup getTarget();

    /** Get a vector of synaptic weights. */
    DoubleMatrix getWeights();

    /** Get a vector of post synaptic conductances. */
    DoubleMatrix getConductances(int step);
    
    /** Get the scale of the synaptic weights. */
    double getWeightScale();
    
    /** Set the scale of the synaptic weights. */
    void setWeightScale(double value);
}
