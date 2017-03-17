package expresscogs.utility;

import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;

import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;

public class SpikeRasterPlot {
    private TimeSeriesPlot plot = TimeSeriesPlot.scatter();
    private Network network;
    private List<BufferedDataSeries> data = new LinkedList<BufferedDataSeries>();
    private double windowSize = 0.25;
    
    public SpikeRasterPlot(Network network) {
        this.network = network;
        for (NeuronGroup group : network.getNeuronGroups()) {
            BufferedDataSeries series = plot.addSeries(group.getName());
            series.setMaxLength(0);
            data.add(series);
            //subsample = DoubleMatrix.rand(group.getSize()).lti(0.2);
        }
    }
    
    public void bufferSpikes(double t) {
        double offset = 0;
        for (NeuronGroup group : network.getNeuronGroups()) {
            DoubleMatrix spikes = group.getSpikes();
            double[] points = group.getXPosition().get(spikes).add(offset).data;
            plot.bufferPoints(group.getName(), t, points);
            offset += 1;
        }
    }
    
    public void updatePlot(double t) {
        for (BufferedDataSeries series : data) {
            series.setMinXValue(t - windowSize);
        }
        plot.addPoints();
        plot.setLimits(t - windowSize, t, 0, network.getNeuronGroups().size());
    }
    
    public double getWindowSize() {
        return windowSize;
    }
    
    public void setWindowSize(double value) {
        windowSize = value;
    }
}
