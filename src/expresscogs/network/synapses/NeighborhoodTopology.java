package expresscogs.network.synapses;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;

/**
 * NeighborhoodTopology generates SynapseGroup connections based on a normal probability
 * density function of x distance. Approximately two thirds of a given neuron's synapses
 * will be made with neurons that are less than a distance of neighborhood away.
 */
public class NeighborhoodTopology implements SynapseGroupTopology {
    private static DoubleMatrix normalPdf(DoubleMatrix x, double mean, double std) {
        double scale = (1 / (std * Math.sqrt(2 * Math.PI)));
        double twoSigmaSqr = (2 * std * std);
        DoubleMatrix shiftedX = x.sub(mean);
        return MatrixFunctions.expi(shiftedX.mul(shiftedX).divi(-twoSigmaSqr)).muli(scale);
    }
    
    private double connectivity = 0.1;
    private double neighborhood = 0.05;
    private boolean selfSynapses = false;
    
    public NeighborhoodTopology() {}
    
    public NeighborhoodTopology(double connectivity, double neighborhood) {
        this.connectivity = connectivity;
        this.neighborhood = neighborhood;
    }
    
    /** Get the mean probability of generating a connection between each pair of source and target neurons. */
    public double getConnectivity() {
        return connectivity;
    }
    
    /** Set the mean probability of generating a connection between each pair of source and target neurons. */
    public void setConnectivity(double value) {
        connectivity = value;
    }
    
    /** Get the width of the connectivity function. */
    public double getNeighborhood() {
        return neighborhood;
    }
    
    /** Set the width of the connectivity function. */
    public void setNeighborhood(double value) {
        neighborhood = value;
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
        DoubleMatrix distance = getDistanceMatrix(source, target);
        DoubleMatrix probability = normalPdf(distance, 0.0, neighborhood);
        // Scale overall probability matrix to connectivity (edges receive fewer connections)
        //probability.divi(probability.mean()).muli(connectivity);
        // Scale probability matrix rows to connectivity
        probability.diviColumnVector(probability.rowMeans()).muli(connectivity);
        DoubleMatrix connections = DoubleMatrix.rand(source.getSize(), target.getSize());
        connections.lti(probability);
        if (source == target && !selfSynapses) {
            connections.put(DoubleMatrix.eye(connections.rows), 0);
        }
        return connections;
    }
    
    /* Code for non-neighborhood (inverted mexican hat) connectivity function
        DoubleMatrix d = source.getXPosition().repmat(1, target.getSize());
        d.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(d);
        DoubleMatrix p = normalPdf(d, 0.0, 10 * neighborhood);
        p.diviColumnVector(p.rowMeans().div(2));
        DoubleMatrix p2 = normalPdf(d, 0.0, neighborhood);
        p2.diviColumnVector(p2.rowMeans());
        p.subi(p2).addi(p2.rowMaxs().repmat(1, p.columns));
        p.diviColumnVector(p.rowMeans()).muli(connectivity);
    */
    
    private DoubleMatrix getDistanceMatrix(NeuronGroup source, NeuronGroup target) {
        DoubleMatrix distance = source.getXPosition().repmat(1, target.getSize());
        distance.subi(target.getXPosition().transpose().repmat(source.getSize(), 1));
        MatrixFunctions.absi(distance);
        return distance;
    }
}
