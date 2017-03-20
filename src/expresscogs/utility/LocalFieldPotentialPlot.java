package expresscogs.utility;

import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;
import javafx.scene.chart.XYChart;

public class LocalFieldPotentialPlot {
    private TimeSeriesPlot plot = TimeSeriesPlot.line();
    private NeuronGroup neurons;
    private List<BufferedDataSeries> data = new LinkedList<BufferedDataSeries>();
    private double windowSize = 0.5;
    private double lfp;
    private DoubleMatrix distanceToElectrode;
    
    //int windowLength = 67;
    //double[] filterWeights = BandPassFilter.sincFilter2(windowLength, 1, 30, 1000, BandPassFilter.filterType.BAND_PASS);
    //filterWeights = BandPassFilter.createWindow(filterWeights, null, windowLength, BandPassFilter.windowType.HAMMING);
    //DoubleMatrix lfpFilter = new DoubleMatrix(filterWeights);
    //DoubleMatrix lfpData = new DoubleMatrix(windowLength);
    
    public LocalFieldPotentialPlot(NeuronGroup neurons) {
        this.neurons = neurons;
        BufferedDataSeries series = plot.addSeries("LFP");
        series.setMaxLength(0);
        data.add(series);
        plot.setAutoRanging(false, true);
        setElectrodePosition(0.5, 0.5);
    }
    
    private void setElectrodePosition(double x, double y) {
        DoubleMatrix dx = neurons.getXPosition().sub(0.5);
        dx.muli(dx);
        DoubleMatrix dy = neurons.getYPosition().sub(0.5);
        dy.muli(dy);
        distanceToElectrode = MatrixFunctions.sqrt(dx.add(dy));
    }
    
    public XYChart<Number, Number> getChart() {
        return plot.getChart();
    }
    
    public void bufferLfp(double t) {
        DoubleMatrix c = neurons.getExcitatoryConductance().add(neurons.getInhibitoryConductance());
        lfp = c.divi(distanceToElectrode).sum();
        plot.bufferPoint("LFP", t, lfp);
        
        //lfpData.put(new IntervalRange(0, windowLength - 1), new PointRange(0),
        //        lfpData.get(new IntervalRange(1, windowLength), new PointRange(0)));
        //lfpData.put(windowLength - 1, lfp);
        //double theta = lfpData.dot(lfpFilter);
        //lfpPlot.bufferPoint("Theta", t - dt * (windowLength / 2), theta);
    }
    
    public double getLfp() {
        return lfp;
    }
    
    public void updatePlot(double t) {
        for (BufferedDataSeries series : data) {
            series.setMinXValue(t - windowSize);
        }
        plot.addPoints();
        plot.setXLimits(t - windowSize, t);
    }
    
    public double getWindowSize() {
        return windowSize;
    }
    
    public void setWindowSize(double value) {
        windowSize = value;
    }
}
