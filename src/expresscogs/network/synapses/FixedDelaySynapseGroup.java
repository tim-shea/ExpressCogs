package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import expresscogs.network.NeuronGroup;

/**
 * DelaySynapseGroup is a SynapseGroup with a standard conductance model and fixed integer conductance delays.
 *
 * Author: Tim
 */
public class FixedDelaySynapseGroup implements SynapseGroup {
    private String name;
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix weights;
    private DoubleMatrix conductances;
    private int delay;
    private double weightScale = 1.0;
    
    public FixedDelaySynapseGroup(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix weights, int delay) {
        this.name = name;
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        this.weights = weights;
        conductances = DoubleMatrix.zeros(target.getSize(), delay);
        this.delay = delay;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void update(int step) {
        DoubleMatrix spikes = source.getSpikes();
        if (spikes.sum() > 0) {
            DoubleMatrix w = weights.getRows(spikes).mul(weightScale);
            int i = (step + delay) % delay;
            conductances.putColumn(i, w.columnSums());
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
        return conductances.getColumn(step % delay);
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
