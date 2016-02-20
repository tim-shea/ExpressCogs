package expresscogs.network;

import java.util.ArrayList;
import java.util.List;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IndicesRange;
import org.jblas.ranges.IntervalRange;

public class NeuronGroup {
    public static NeuronGroup createExcitatory(String name, int size) {
        return new NeuronGroup(name, size, true);
    }
    
    public static NeuronGroup createInhibitory(String name, int size) {
        return new NeuronGroup(name, size, false);
    }
    
    private List<SynapseGroup> dendriticSynapseGroups = new ArrayList<SynapseGroup>();
    private List<SynapseGroup> axonalSynapseGroups = new ArrayList<SynapseGroup>();
    private String name;
    private int size;
    private boolean excitatory;
    private DoubleMatrix i;
    private DoubleMatrix v;
    private DoubleMatrix dv;
    private DoubleMatrix w;
    private DoubleMatrix dw;
    private DoubleMatrix spk;
    private double c = 2.81e-9;
    private double gL = 3.0e-8;
    private double eL = -70.6e-3;
    private double vT = -50.4e-3;
    private double deltaT = 2e-3;
    private double tauW = 144e-3, a = 4e-9, b = 0.08e-9, vR = eL; // regular spiking
    //private double tauW = 20e-3, a = 4e-9, b = 0.5e-9, vR = vT + 5e-3; // bursting
    //private double tauW = 144e-3, a = 2 * c / 144e-3, b = 0, vR = eL // fast spiking
    private double vCut = vT + 5 * deltaT;
    private double iScale = 1.5e-9;
    
    private NeuronGroup(String name, int size, boolean excitatory) {
        this.name = name;
        this.size = size;
        this.excitatory = excitatory;
        i = DoubleMatrix.zeros(size);
        v = DoubleMatrix.ones(size).muli(eL);
        dv = DoubleMatrix.zeros(size);
        w = DoubleMatrix.zeros(size);
        dw = DoubleMatrix.zeros(size);
        spk = DoubleMatrix.zeros(size);
    }
    
    public void addDendriticSynapseGroup(SynapseGroup group) {
        dendriticSynapseGroups.add(group);
    }
    
    public void addAxonalSynapseGroup(SynapseGroup group) {
        axonalSynapseGroups.add(group);
    }
    
    public void update(double dt) {
        v.gti(vCut, spk);
        v.put(spk, vR);
        w.put(spk, w.get(spk).add(b));
        i = DoubleMatrix.rand(size).muli(iScale);
        for (SynapseGroup synapses : dendriticSynapseGroups)
            i.addi(synapses.getConductances());
        // dv = (dt / c) * (gL * deltaT * exp((v - vT) / deltaT) - gL * (v - eL) - w + i);
        MatrixFunctions.expi(v.subi(vT, dv).divi(deltaT)).muli(gL * deltaT).subi(v.sub(eL).muli(gL)).subi(w).addi(i).muli(dt / c);
        // dw = (dt / tauW) * (a * (v - eL) - w);
        v.subi(eL, dw).muli(a).subi(w).muli(dt / tauW);
        v.addi(dv);
        w.addi(dw);
    }
    
    public int getSize() {
        return size;
    }
    
    public boolean getExcitatory() {
        return excitatory;
    }
    
    public DoubleMatrix getInputs() {
        return i;
    }
    
    public DoubleMatrix getPotentials() {
        return v;
    }
    
    public DoubleMatrix getSpikes() {
        return spk;
    }
}
