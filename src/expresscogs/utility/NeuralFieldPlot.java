package expresscogs.utility;

import javafx.scene.chart.XYChart;

public class NeuralFieldPlot {
    private NeuralFieldSensor sensor;
    private TimeSeriesPlot plot = TimeSeriesPlot.line();
    private BufferedDataSeries series;
    private boolean enabled = true;
    
    public NeuralFieldPlot(NeuralFieldSensor sensor) {
        this.sensor = sensor;
        series = plot.addSeries("Neural Field");
        series.setMaxLength(sensor.getPosition().length);
        plot.setAutoRanging(false, true);
        plot.setXLimits(0, 1);
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
    
    public void bufferNeuralField(double t) {}
    
    public void updatePlot(double t) {
        if (!enabled) {
            return;
        }
        plot.bufferPoints("Neural Field", sensor.getPosition().data, sensor.getActivity().data);
        plot.addPoints();
    }
}
