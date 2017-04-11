package expresscogs.utility;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;

import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;

public class SpikeRasterPlot extends BufferedPlot {
    private Network network;
    private HashMap<String, BufferedDataSeries> data = new HashMap<String, BufferedDataSeries>();
    private double windowSize = 1;
    private List<DoubleMatrix> sampleIndices = new LinkedList<DoubleMatrix>();
    private boolean enabled = true;
    
    public SpikeRasterPlot(Network network, int sampleSize) {
        createScatter();
        this.network = network;
        for (NeuronGroup group : network.getNeuronGroups()) {
            BufferedDataSeries series = addSeries(group.getName());
            series.setMaxLength(0);
            data.put(group.getName(), series);
            double p = (double)sampleSize / group.getSize();
            sampleIndices.add(DoubleMatrix.rand(group.getSize()).lti(p));
        }
    }
    
    @Override
    public void updateBuffers(double t) {
        if (!isEnabled()) {
            return;
        }
        double offset = 0;
        int i = 0;
        for (NeuronGroup group : network.getNeuronGroups()) {
            DoubleMatrix sampledSpikes = group.getSpikes().and(sampleIndices.get(i++));
            double[] points = group.getXPosition().get(sampledSpikes).add(offset).data;
            data.get(group.getName()).bufferPoints(t, points);
            offset += 1;
        }
    }
    
    @Override
    public void updatePlot(double t) {
        if (!enabled) {
            return;
        }
        for (BufferedDataSeries series : data.values()) {
            series.setMinXValue(t - windowSize);
            series.addBuffered();
        }
        setLimits(t - windowSize, t, 0, network.getNeuronGroups().size());
    }
    
    public double getWindowSize() {
        return windowSize;
    }
    
    public void setWindowSize(double value) {
        windowSize = value;
    }
}
