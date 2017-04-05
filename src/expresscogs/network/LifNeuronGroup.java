package expresscogs.network;

import java.util.ArrayList;
import java.util.List;

import org.jblas.DoubleMatrix;

import expresscogs.network.synapses.SynapseGroup;

/**
 * LifNeuronGroup represents a nucleus of leaky integrate-and-fire neurons with an exponentially
 * decaying membrane potential and a fixed spiking threshold.
 * 
 * @author Tim
 *
 */
public class LifNeuronGroup implements NeuronGroup {
    private List<SynapseGroup> dendriticSynapseGroups = new ArrayList<SynapseGroup>();
    private List<SynapseGroup> axonalSynapseGroups = new ArrayList<SynapseGroup>();
    private String name;
    private int size;
    private boolean excitatory;
    private DoubleMatrix x;
    private DoubleMatrix y;
    private DoubleMatrix i;
    private DoubleMatrix gE;
    private DoubleMatrix gI;
    private DoubleMatrix v;
    private DoubleMatrix dv;
    private DoubleMatrix spk;
    private double vDecay = 0.01;
    private double vRest = -70e-3;
    private double vThresh = -50e-3;
    private double gEDecay = 0.1;
    private double gIDecay = 0.1;
    private double gEMax = 4e-3;
    private double gIMax = 4e-3;
    private InputGenerator generator;

    public LifNeuronGroup(String name, int size, boolean excitatory, InputGenerator generator) {
        this.name = name;
        this.size = size;
        this.excitatory = excitatory;
        x = DoubleMatrix.linspace(0, 1, size);
        y = DoubleMatrix.linspace(0, 1, size);
        i = DoubleMatrix.zeros(size);
        gE = DoubleMatrix.zeros(size);
        gI = DoubleMatrix.zeros(size);
        v = DoubleMatrix.ones(size).muli(vRest).addi(DoubleMatrix.rand(size).muli(vThresh - vRest));
        dv = DoubleMatrix.zeros(size);
        spk = DoubleMatrix.zeros(size);
        this.generator = generator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addDendriticSynapseGroup(SynapseGroup group) {
        dendriticSynapseGroups.add(group);
    }

    @Override
    public void addAxonalSynapseGroup(SynapseGroup group) {
        axonalSynapseGroups.add(group);
    }

    @Override
    public void update(int step) {
        v.gti(vThresh, spk);
        v.put(spk, vRest);
        gE.put(spk, 0);
        gI.put(spk, 0);
        i = generator.generate(this);
        gE.muli(1 - gEDecay);
        gI.muli(1 - gIDecay);
        for (SynapseGroup synapses : dendriticSynapseGroups) {
            if (synapses.getSource().isExcitatory()) {
                gE.addi(synapses.getConductances(step));
            } else {
                gI.addi(synapses.getConductances(step));
            }
        }
        // Apply a ceiling to the input currents?
        gE.put(gE.gt(gEMax), gEMax);
        gI.put(gI.gt(gIMax), gIMax);
        i.addi(gE).subi(gI);
        // dv = -vDecay * (v - eL) + i
        v.subi(vRest, dv).muli(-vDecay).addi(i);
        v.addi(dv);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isExcitatory() {
        return excitatory;
    }

    @Override
    public DoubleMatrix getXPosition() {
        return x;
    }

    @Override
    public DoubleMatrix getYPosition() {
        return y;
    }
    
    @Override
    public DoubleMatrix getExcitatoryConductance() {
        return gE;
    }
    
    @Override
    public DoubleMatrix getInhibitoryConductance() {
        return gI;
    }

    @Override
    public DoubleMatrix getInputs() {
        return i;
    }

    @Override
    public DoubleMatrix getPotentials() {
        return v;
    }

    @Override
    public DoubleMatrix getSpikes() {
        return spk;
    }
}
