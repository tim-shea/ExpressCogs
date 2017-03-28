package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;

/**
 * DelaySynapseGroup is a SynapseGroup with a standard conductance model and fixed integer conductance delays.
 *
 * Author: Tim
 */
public class DelaySynapseGroup implements SynapseGroup {
    private String name;
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix preIndex;
    private DoubleMatrix postIndex;
    private DoubleMatrix weights;
    private DoubleMatrix delays;
    private DoubleMatrix conductances;
    private int maxDelay;
    private int targetSize;
    private double weightScale = 1.0;
    
    private DoubleMatrix s;
    
    public DelaySynapseGroup(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix index, int maxDelay) {
        this.name = name;
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        preIndex = index.getColumn(0);
        postIndex = index.getColumn(1);
        weights = DoubleMatrix.zeros(index.length);
        delays = DoubleMatrix.zeros(index.length);
        conductances = DoubleMatrix.zeros(target.getSize(), maxDelay);
        this.maxDelay = maxDelay;
        targetSize = target.getSize();
        s = DoubleMatrix.zeros(index.length);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void update(int step) {
        conductances.putColumn(step % delays.columns, conductances.getColumn(step % delays.columns).fill(0));
        int[] spikes = source.getSpikes().findIndices();
        for (int n : spikes) {
            /*
            //int[] synapses = preIndex.eq(n).findIndices();
            preIndex.eqi(n, s);
            DoubleMatrix w = weights.get(s).muli(weightScale);
            DoubleMatrix t = postIndex.get(s);
            DoubleMatrix d = delays.get(s);
            // d = ((d + step) / D - floor(d)) * (D * N)
            d.addi(step).divi(maxDelay);
            d.subi(MatrixFunctions.floor(d)).muli(maxDelay * targetSize);
            int[] indices = t.add(d).toIntArray();
            w.addi(conductances.get(indices));
            conductances.put(indices, w);
            */
            int[] s = preIndex.eq(n).findIndices();
            DoubleMatrix w = weights.get(s).muli(weightScale);
            DoubleMatrix t = postIndex.get(s);
            DoubleMatrix d = delays.get(s).addi(step);
            d.divi(delays.columns);
            d.subi(MatrixFunctions.floor(d)).muli(delays.columns);
            d.muli(conductances.rows);
            int[] indices = t.add(d).toIntArray();
            w.addi(conductances.get(indices));
            conductances.put(indices, w);
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
    
    public DoubleMatrix getPreIndex() {
        return preIndex;
    }
    
    public DoubleMatrix getPostIndex() {
        return postIndex;
    }
    
    @Override
    public DoubleMatrix getWeights() {
        return weights;
    }
    
    public DoubleMatrix getDelays() {
        return delays;
    }
    
    @Override
    public DoubleMatrix getConductances(int step) {
        return conductances.getColumn(step % delays.columns);
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
