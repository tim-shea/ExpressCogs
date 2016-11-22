package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public class SynapseGroup {
    private String name;
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix preIndex;
    private DoubleMatrix postIndex;
    private DoubleMatrix weights;
    private DoubleMatrix delays;
    private DoubleMatrix conductances;
    
    public SynapseGroup(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix index, int maxDelay) {
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        preIndex = index.getColumn(0);
        postIndex = index.getColumn(1);
        weights = DoubleMatrix.zeros(index.length);
        delays = DoubleMatrix.zeros(index.length);
        conductances = DoubleMatrix.zeros(target.getSize(), maxDelay);
    }
    
    public String getName() {
        return name;
    }
    
    public void update(int step) {
        conductances.putColumn(step % delays.columns, conductances.getColumn(step % delays.columns).fill(0));
        int[] spikes = source.getSpikes().findIndices();
        for (int n : spikes) {
            int[] synapses = preIndex.eq(n).findIndices();
            DoubleMatrix w = weights.get(synapses);
            DoubleMatrix t = postIndex.get(synapses);
            DoubleMatrix d = delays.get(synapses).add(step);
            d.divi(delays.columns);
            d = d.sub(MatrixFunctions.floor(d)).mul(delays.columns);
            int[] indices = t.add(d.mul(conductances.rows)).toIntArray();
            w.addi(conductances.get(indices));
            conductances.put(indices, w);
        }
    }
    
    public NeuronGroup getSource() {
        return source;
    }
    
    public NeuronGroup getTarget() {
        return target;
    }
    
    public DoubleMatrix getPreIndex() {
        return preIndex;
    }
    
    public DoubleMatrix getPostIndex() {
        return postIndex;
    }
    
    public DoubleMatrix getWeights() {
        return weights;
    }
    
    public DoubleMatrix getDelays() {
        return delays;
    }
    
    public DoubleMatrix getConductances(int step) {
        return conductances.getColumn(step % delays.columns);
    }
}
