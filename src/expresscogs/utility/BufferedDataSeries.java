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
    
    public BufferedDataSeries(String name) {
        series.setName(name);
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(int value) {
        maxLength = value;
    }
    
    public Series<Number,Number> getSeries() {
        return series;
    }
    
    public void bufferData(double x, double y) {
        buffer.add(new Data<Number,Number>(x, y));
    }
    
    public void addBuffered() {
        ObservableList<Data<Number,Number>> data = series.getData();
        data.addAll(buffer);
        buffer.clear();
        if (data.size() > maxLength) {
            data.remove(0, data.size() - maxLength);
        }
    }
}