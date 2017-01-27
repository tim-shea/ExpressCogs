package expresscogs.utility;

import java.util.HashMap;
import expresscogs.gui.ResizeListener;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;

public class TimeSeriesPlot {
    static Pane container;
    
    public static void init(Pane container) {
        TimeSeriesPlot.container = container;
    }
    
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
    
    private void createScatter() {
        chart = new ScatterChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
        ResizeListener.makeResizable(chart, ResizeListener.Mode.VERTICAL);
        container.getChildren().add(chart);
    }
    
    private void createLine() {
        chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setAnimated(false);
        container.getChildren().add(chart);
    }
    
    public void addSeries(String label) {
        BufferedDataSeries buffer = new BufferedDataSeries(label);
        chart.getData().add(buffer.getSeries());
        data.put(label, buffer);
    }
    
    public void bufferPoint(String seriesLabel, double t, double x) {
        data.get(seriesLabel).bufferData(t, x);
    }
    
    public void bufferPoints(String seriesLabel, double t, double[] points) {
        for (double x : points) {
            data.get(seriesLabel).bufferData(t, x);
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
