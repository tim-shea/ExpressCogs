package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;

import expresscogs.network.NeuronGroup;

/**
 * SynapseGroupTopology defines an interface for connectivity algorithms. Given source
 * and target NeuronGroups, SynapseGroupTopology can generate an index or a matrix of
 * connections.
 * 
 * @author Tim
 */
public interface SynapseGroupTopology {
    /** Flatten a connection matrix to generate pre- and post-synaptic indices. */
    public static DoubleMatrix flattenMatrix(DoubleMatrix matrix) {
        int count = 0;
        int numSynapses = (int)matrix.sum();
        DoubleMatrix index = DoubleMatrix.zeros(numSynapses, 2);
        for (int i : matrix.findIndices()) {
            index.put(count, 0, matrix.indexRows(i));
            index.put(count, 1, matrix.indexColumns(i));
            ++count;
        }
        return index;
    }
    
    /** Generate a connectivity matrix between the source and target NeuronGroups. */
    DoubleMatrix generateConnections(NeuronGroup source, NeuronGroup target);
}
