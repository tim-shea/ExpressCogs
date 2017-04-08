package expresscogs.utility;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import expresscogs.network.NeuronGroup;

public class LocalFieldPotentialSensor {
    private NeuronGroup neurons;
    private DoubleMatrix distanceToElectrode;
    private double lfp;
    
    public LocalFieldPotentialSensor(NeuronGroup neurons) {
        this.neurons = neurons;
        setElectrodePosition(0.5, 0.5);
    }
    
    private void setElectrodePosition(double x, double y) {
        DoubleMatrix dx = neurons.getXPosition().sub(0.5);
        dx.muli(dx);
        DoubleMatrix dy = neurons.getYPosition().sub(0.5);
        dy.muli(dy);
        distanceToElectrode = MatrixFunctions.sqrt(dx.add(dy));
    }
    
    public double getLfp() {
        return lfp;
    }
    
    public void update(double t) {
        DoubleMatrix c = neurons.getLeakConductance()
                .add(neurons.getExcitatoryConductance())
                .sub(neurons.getInhibitoryConductance());
        lfp = c.divi(distanceToElectrode).sum();
    }
}
