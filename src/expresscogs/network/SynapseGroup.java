package expresscogs.network;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public class SynapseGroup {
    public static SynapseGroup connectSparseRandom(NeuronGroup source, NeuronGroup target, double connectivity) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        synapses.s = DoubleMatrix.rand(source.getSize(), target.getSize()).lti(connectivity);
        synapses.s.muli(source.getExcitatory() ? 1 : -1);
        if (source == target)
            synapses.s.muli(DoubleMatrix.ones(source.getSize(), source.getSize()).subi(DoubleMatrix.eye(source.getSize())));
        synapses.w = DoubleMatrix.rand(source.getSize(), target.getSize()).muli(synapses.s).muli(1e-9);
        return synapses;
    }
    
    public static SynapseGroup connectLocalRandom(NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        synapses.s = DoubleMatrix.rand(source.getSize(), target.getSize());
        for (int r = 0; r < source.getSize(); ++r) {
            DoubleMatrix x = DoubleMatrix.linspace(0, 1, target.getSize());
            DoubleMatrix p = normalPdf(x, r / (double)source.getSize(), neighborhood);
            p.muli(connectivity / p.mean());
            synapses.s.putRow(r, synapses.s.getRow(r).le(p));
        }
        synapses.s.muli(source.getExcitatory() ? 1 : -1);
        if (source == target)
            synapses.s.muli(DoubleMatrix.ones(source.getSize(), source.getSize()).subi(DoubleMatrix.eye(source.getSize())));
        synapses.w = DoubleMatrix.rand(source.getSize(), target.getSize()).muli(synapses.s).muli(5e-9);
        return synapses;
    }
    
    public static SynapseGroup connectNonLocalRandom(NeuronGroup source, NeuronGroup target, double connectivity, double neighborhood) {
        SynapseGroup synapses = new SynapseGroup(source, target);
        synapses.s = DoubleMatrix.rand(source.getSize(), target.getSize());
        for (int r = 0; r < source.getSize(); ++r) {
            DoubleMatrix x = DoubleMatrix.linspace(0, 1, target.getSize());
            DoubleMatrix p = normalPdf(x, r / (double)source.getSize(), neighborhood);
            p = DoubleMatrix.ones(target.getSize()).muli(p.max()).subi(p);
            p.muli(connectivity / p.mean());
            synapses.s.putRow(r, synapses.s.getRow(r).le(p));
        }
        synapses.s.muli(source.getExcitatory() ? 1 : -1);
        if (source == target)
            synapses.s.muli(DoubleMatrix.ones(source.getSize(), source.getSize()).subi(DoubleMatrix.eye(source.getSize())));
        synapses.w = DoubleMatrix.rand(source.getSize(), target.getSize()).muli(synapses.s).muli(5e-9);
        return synapses;
    }
    
    private static DoubleMatrix normalPdf(DoubleMatrix x, double mean, double std) {
        double scale = (1 / (std * Math.sqrt(2 * Math.PI)));
        double twoSigmaSqr = (2 * std * std);
        DoubleMatrix shiftedX = x.sub(mean);
        return MatrixFunctions.expi(shiftedX.mul(shiftedX).divi(-twoSigmaSqr)).muli(scale);
    }
    
    private NeuronGroup source;
    private NeuronGroup target;
    private DoubleMatrix s;
    private DoubleMatrix w;
    private DoubleMatrix g;
    
    private SynapseGroup(NeuronGroup source, NeuronGroup target) {
        this.source = source;
        source.addAxonalSynapseGroup(this);
        this.target = target;
        target.addDendriticSynapseGroup(this);
        s = DoubleMatrix.zeros(source.getSize(), target.getSize());
        w = DoubleMatrix.zeros(source.getSize(), target.getSize());
        g = DoubleMatrix.zeros(target.getSize());
    }
    
    public void update(double dt) {
        g.fill(0);
        g.addi(w.getRows(source.getSpikes()).columnSums());
    }
    
    public NeuronGroup getSource() {
        return source;
    }
    
    public NeuronGroup getTarget() {
        return target;
    }
    
    public DoubleMatrix getStates() {
        return s;
    }
    
    public DoubleMatrix getWeights() {
        return w;
    }
    
    public DoubleMatrix getConductances() {
        return g;
    }
}
