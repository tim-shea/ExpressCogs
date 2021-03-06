package expresscogs.utility;

import java.util.HashMap;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public class TimeSeriesPlot {
    public static TimeSeriesPlot scatter() {
        TimeSeriesPlot plot = new TimeSeriesPlot();
        plot.createScatter();
        return plot;
    }
    
    public static TimeSeriesPlot line() {
        TimeSeriesPlot plot = new TimeSeriesPlot();
        plot.createLine();
        return plot;
    }
    
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private XYChart<Number, Number> chart;
    private HashMap<String, BufferedDataSeries> data;
    
    private TimeSeriesPlot() {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxis.setAutoRanging(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
        data = new HashMap<String, BufferedDataSeries>();
    }
    
    public XYChart<Number, Number> getChart() {
        return chart;
    }
    
    private void createScatter() {
        chart = new ScatterChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
    }
    
    private void createLine() {
        chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
    }
    
    public BufferedDataSeries addSeries(String label) {
        BufferedDataSeries buffer = new BufferedDataSeries(label);
        chart.getData().add(buffer.getSeries());
        data.put(label, buffer);
        return buffer;
    }
    
    public void bufferPoint(String seriesLabel, double x, double y) {
        data.get(seriesLabel).bufferPoint(x, y);
    }
    
    public void bufferPoints(String seriesLabel, double x, double[] ys) {
        BufferedDataSeries series = data.get(seriesLabel); 
        for (double y : ys) {
            series.bufferPoint(x, y);
        }
    }
    
    public void bufferPoints(String seriesLabel, double[] xs, double[] ys) {
        BufferedDataSeries series = data.get(seriesLabel);
        for (int i = 0; i < xs.length; ++i) {
            series.bufferPoint(xs[i], ys[i]);
        }
    }
    
    public void addPoints() {
        for (BufferedDataSeries series : data.values()) {
            series.addBuffered();
        }
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
}
