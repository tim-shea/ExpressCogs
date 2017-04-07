package expresscogs.utility;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.PointRange;

import expresscogs.network.NeuronGroup;
import javafx.scene.chart.XYChart;

public class LocalFieldPotentialPlot {
    private final int WINDOW_LENGTH = 27;
    private final double lowCutoff = 0.5;
    private final double highCutoff = 60;
    private final int frequency = 1000;
    private final double plotWidth = 1.0;
    
    private TimeSeriesPlot plot = TimeSeriesPlot.line();
    private NeuronGroup neurons;
    private BufferedDataSeries series;
    private DoubleMatrix distanceToElectrode;
    private double[] filterWeights;
    private DoubleMatrix lfpFilter;
    private DoubleMatrix lfpData = new DoubleMatrix(WINDOW_LENGTH);
    private double lfp;
    private double previousTrend = 0;
    private double trend = 0;
    private int detrendLength = 1000;
    private int step = 0;
    private boolean enabled = true;
    
    public LocalFieldPotentialPlot(NeuronGroup neurons) {
        this.neurons = neurons;
        series = plot.addSeries("LFP");
        series.setMaxLength(0);
        plot.setAutoRanging(false, true);
        setElectrodePosition(0.5, 0.5);
        filterWeights = BandPassFilter.sincFilter2(WINDOW_LENGTH, lowCutoff, highCutoff, frequency, BandPassFilter.filterType.BAND_PASS);
        filterWeights = BandPassFilter.createWindow(filterWeights, null, WINDOW_LENGTH, BandPassFilter.windowType.HANNING);
        lfpFilter = new DoubleMatrix(filterWeights);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean value) {
        enabled = value;
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
        if (!enabled) {
            return;
        }
        DoubleMatrix c = neurons.getExcitatoryConductance().sub(neurons.getInhibitoryConductance());
        lfp = c.divi(distanceToElectrode).sum();
        lfpData.put(new IntervalRange(0, WINDOW_LENGTH - 1), new PointRange(0),
                lfpData.get(new IntervalRange(1, WINDOW_LENGTH), new PointRange(0)));
        lfpData.put(WINDOW_LENGTH - 1, lfp);
        double filteredLfp = lfpData.dot(lfpFilter);
        plot.bufferPoint("LFP", t - (1.0 / frequency) * (WINDOW_LENGTH / 2), filteredLfp);
        if (step == detrendLength) {
            previousTrend = trend;
            trend = 0;
            step = 0;
        } else {
            trend += filteredLfp / detrendLength;
            step++;
        }
    }
    
    public void updatePlot(double t) {
        if (!enabled) {
            return;
        }
        series.setMinXValue(t - plotWidth);
        plot.addPoints();
        plot.setXLimits(t - plotWidth, t);
    }
}
