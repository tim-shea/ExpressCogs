package expresscogs.utility;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public class BufferedPlot {
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private XYChart<Number, Number> chart;
    private boolean enabled = true;
    
    protected BufferedPlot() {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setAutoRanging(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
    }
    
    public XYChart<Number, Number> getChart() {
        return chart;
    }
    
    protected void createScatter() {
        chart = new ScatterChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
    }
    
    protected void createLine() {
        chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
    }
    
    public BufferedDataSeries addSeries(String label) {
        BufferedDataSeries series = new BufferedDataSeries(label);
        chart.getData().add(series.getSeries());
        return series;
    }
    
    public void setLimits(double xLower, double xUpper, double yLower, double yUpper) {
        setXLimits(xLower, xUpper);
        setYLimits(yLower, yUpper);
    }
    
    public void setXLimits(double xLower, double xUpper) {
        xAxis.setLowerBound(xLower);
        xAxis.setUpperBound(xUpper);
        xAxis.setTickUnit((xUpper - xLower) / 10);
    }
    
    public void setYLimits(double yLower, double yUpper) {
        yAxis.setLowerBound(yLower);
        yAxis.setUpperBound(yUpper);
        yAxis.setTickUnit((yUpper - yLower) / 10);
    }
    
    public void setAutoRanging(boolean xAuto, boolean yAuto) {
        xAxis.setAutoRanging(xAuto);
        yAxis.setAutoRanging(yAuto);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean value) {
        enabled = value;
    }
    
    public void updateBuffers(double t) {}
    
    public void updatePlot(double t) {}
}
