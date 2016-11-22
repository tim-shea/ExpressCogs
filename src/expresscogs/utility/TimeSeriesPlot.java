package expresscogs.utility;

import java.util.HashMap;
import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

public class TimeSeriesPlot {
    static Pane container;
    
    public static void init(Stage stage) {
        container = new VBox();
        container.setPadding(new Insets(10, 10, 10, 10));
        ((VBox)container).setSpacing(10);
        Scene scene = new Scene(container, 800, 600);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);
        stage.show();
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
        xAxis.setLowerBound(xLower);
        xAxis.setUpperBound(xUpper);
        xAxis.setTickUnit((xUpper - xLower) / 10);
        yAxis.setLowerBound(yLower);
        yAxis.setUpperBound(yUpper);
        yAxis.setTickUnit((yUpper - yLower) / 10);
    }
}
