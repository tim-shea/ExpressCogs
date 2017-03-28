package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;

/**
 * NoDelaySynapseGroup is a SynapseGroup with a standard conductance model and no conductance delays (network
 * update cycle may impose a fixed delay of 1 timestep).
 *
 * Author: Tim
 */
public class NoDelaySynapseGroup implements SynapseGroup {
    private String name;
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix weights;
    private DoubleMatrix conductances;
    private double weightScale = 1.0;
    
    public NoDelaySynapseGroup(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix weights) {
        this.name = name;
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        this.weights = weights;
        conductances = DoubleMatrix.zeros(target.getSize());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void update(int step) {
        conductances.fill(0);
        DoubleMatrix spikes = source.getSpikes();
        if (spikes.sum() > 0) {
            DoubleMatrix w = weights.getRows(spikes).mul(weightScale);
            conductances.addi(w.columnSums());
        }
    }
    
    @Override
    public NeuronGroup getSource() {
        return source;
    }
    
    @Override
    public NeuronGroup getTarget() {
        return target;
    }
    
    @Override
    public DoubleMatrix getWeights() {
        return weights;
    }
    
    @Override
    public DoubleMatrix getConductances(int step) {
        return conductances;
    }
    
    @Override
    public double getWeightScale() {
        return weightScale;
    }
    
    @Override
    public void setWeightScale(double value) {
        weightScale = value;
    }
}
