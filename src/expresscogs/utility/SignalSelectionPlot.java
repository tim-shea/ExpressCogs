package expresscogs.utility;

import javafx.scene.chart.XYChart;

public class SignalSelectionPlot {
    private final double plotWidth = 1.0;
    
    private NeuralFieldSensor sensor;
    private TimeSeriesPlot plot;
    private BufferedDataSeries signalSeries;
    private BufferedDataSeries strengthSeries;
    private boolean enabled = true;
    
    public SignalSelectionPlot(NeuralFieldSensor sensor) {
        this.sensor = sensor;
        plot = TimeSeriesPlot.line();
        signalSeries = plot.addSeries("Signal");
        signalSeries.setMaxLength(0);
        strengthSeries = plot.addSeries("Strength");
        strengthSeries.setMaxLength(0);
        plot.setAutoRanging(false, true);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean value) {
        enabled = value;
    }
    
    public XYChart<Number, Number> getChart() {
        return plot.getChart();
    }
    
    public void bufferSignal(double t) {
        if (!enabled) {
            return;
        }
        plot.bufferPoint("Signal", t, sensor.getSignalCenter());
        plot.bufferPoint("Strength", t, sensor.getSignalStrength());
    }
    
    public void updatePlot(double t) {
        if (!enabled) {
            return;
        }
        signalSeries.setMinXValue(t - plotWidth);
        strengthSeries.setMinXValue(t - plotWidth);
        plot.addPoints();
        plot.setXLimits(t - plotWidth, t);
    }
}
