package expresscogs.utility;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.PointRange;

import expresscogs.network.NeuronGroup;
import javafx.scene.chart.XYChart;

public class LocalFieldPotentialPlot extends BufferedPlot {
    private final int WINDOW_LENGTH = 27;
    private final double lowCutoff = 0.5;
    private final double highCutoff = 60;
    private final int frequency = 1000;
    private final double plotWidth = 1.0;
    
    private LocalFieldPotentialSensor sensor;
    private BufferedDataSeries series;
    private double[] filterWeights;
    private DoubleMatrix lfpFilter;
    private DoubleMatrix lfpData;
    private int step = 0;
    
    public LocalFieldPotentialPlot(LocalFieldPotentialSensor sensor) {
        createLine();
        this.sensor = sensor;
        series = addSeries("LFP");
        series.setMaxLength(0);
        setAutoRanging(false, true);
        filterWeights = BandPassFilter.sincFilter2(WINDOW_LENGTH, lowCutoff, highCutoff, frequency, BandPassFilter.filterType.BAND_PASS);
        filterWeights = BandPassFilter.createWindow(filterWeights, null, WINDOW_LENGTH, BandPassFilter.windowType.HANNING);
        lfpFilter = new DoubleMatrix(filterWeights);
        lfpData = new DoubleMatrix(WINDOW_LENGTH);
    }
    
    @Override
    public void updateBuffers(double t) {
        if (!isEnabled()) {
            return;
        }
        lfpData.put(new IntervalRange(0, WINDOW_LENGTH - 1), new PointRange(0),
                lfpData.get(new IntervalRange(1, WINDOW_LENGTH), new PointRange(0)));
        lfpData.put(WINDOW_LENGTH - 1, sensor.getLfp());
        double filteredLfp = lfpData.dot(lfpFilter);
        series.bufferPoint(t - (1.0 / frequency) * (WINDOW_LENGTH / 2), filteredLfp);
    }
    
    @Override
    public void updatePlot(double t) {
        if (!isEnabled()) {
            return;
        }
        series.setMinXValue(t - plotWidth);
        series.addBuffered();
        setXLimits(t - plotWidth, t);
    }
}
