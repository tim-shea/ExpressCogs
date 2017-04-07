package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;

/**
 * StdpSynapseGroup adds spike-timing dependent plasticity to the standard DelaySynapseGroup.
 *
 * Author: Tim
 */
public class StdpSynapseGroup extends FixedDelaySynapseGroup {
    public StdpSynapseGroup(String name, NeuronGroup source, NeuronGroup target, DoubleMatrix index, int maxDelay) {
        super(name, source, target, index, maxDelay);
    }

    @Override
    public void update(int step) {
        /*conductances.putColumn(step % getDelays().columns, conductances.getColumn(step % getDelays().columns).fill(0));
        int[] spikes = source.getSpikes().findIndices();
        for (int n : spikes) {
            int[] synapses = preIndex.eq(n).findIndices();
            DoubleMatrix w = weights.get(synapses);
            DoubleMatrix t = postIndex.get(synapses);
            DoubleMatrix d = getDelays().get(synapses).add(step);
            d.divi(getDelays().columns);
            d = d.sub(MatrixFunctions.floor(d)).mul(getDelays().columns);
            int[] indices = t.add(d.mul(conductances.rows)).toIntArray();
            w.addi(conductances.get(indices));
            conductances.put(indices, w);
        }*/
    }
}
