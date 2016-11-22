package expresscogs.network;

import org.jblas.DoubleMatrix;

public interface NeuronGroup {

    String getName();

    void addDendriticSynapseGroup(SynapseGroup group);

    void addAxonalSynapseGroup(SynapseGroup group);

    void update(int step);

    int getSize();

    boolean isExcitatory();

    DoubleMatrix getXPosition();

    DoubleMatrix getYPosition();

    DoubleMatrix getExcitatoryConductance();

    DoubleMatrix getInhibitoryConductance();

    DoubleMatrix getInputs();

    DoubleMatrix getPotentials();

    DoubleMatrix getSpikes();

}