package expresscogs.utility;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class BufferedDataSeries {
    private Series<Number,Number> series = new Series<Number,Number>();
    private List<Data<Number,Number>> buffer = new ArrayList<Data<Number,Number>>();
    private int maxLength = 1000;
    private double minXValue = 0.0;
    private boolean useMinXValue = false;
    
    public BufferedDataSeries(String name) {
        series.setName(name);
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(int value) {
        maxLength = value;
    }
    
    public double getMinXValue() {
        return minXValue;
    }
    
    public void setMinXValue(double value) {
        minXValue = value;
        useMinXValue = true;
    }
    
    public Series<Number,Number> getSeries() {
        return series;
    }
    
    public void bufferPoint(double x, double y) {
        buffer.add(new Data<Number,Number>(x, y));
    }
    
    public void bufferPoints(double x, double[] ys) {
        for (double y : ys) {
            bufferPoint(x, y);
        }
    }
    
    public void bufferPoints(double[] xs, double[] ys) {
        for (int i = 0; i < xs.length; ++i) {
            bufferPoint(xs[i], ys[i]);
        }
    }
    
    public void addBuffered() {
        ObservableList<Data<Number,Number>> data = series.getData();
        data.addAll(buffer);
        buffer.clear();
        if (maxLength > 0 && data.size() > maxLength) {
            data.remove(0, data.size() - maxLength);
        }
        if (useMinXValue) {
            data.removeIf(point -> {
                return point.getXValue().doubleValue() < minXValue;
            });
        }
    }
}