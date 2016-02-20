package expresscogs.utility;

import java.util.List;

import org.jblas.DoubleMatrix;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Pair;

public class SimplePlot {
    public static class Line {
        public Line(Stage stage, DoubleMatrix x, DoubleMatrix y) {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number,Number> lineChart = new LineChart<Number,Number>(xAxis, yAxis);
            
            ObservableList<Series<Number,Number>> dataset = lineChart.getData();
            for (int c = 0; c < y.columns; ++c)
                dataset.add(new Series<Number,Number>());
            for (int r = 0; r < y.rows; ++r)
                for (int c = 0; c < y.columns; ++c)
                    dataset.get(c).getData().add(new Data<Number,Number>(x.get(r), y.get(r, c)));
            
            Scene scene = new Scene(lineChart, 800, 600);
            stage.setScene(scene);
            stage.show();
        }
    }
    
    public static class Scatter {
        public Scatter(Stage stage, List<Pair<Number,Number>> points) {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number,Number> scatter = new ScatterChart<Number,Number>(xAxis, yAxis);
            
            Series<Number,Number> series = new Series<Number,Number>();
            for (Pair<Number,Number> point : points)
                series.getData().add(new Data<Number,Number>(point.getKey(), point.getValue()));
            scatter.getData().add(series);
            
            Scene scene = new Scene(scatter, 800, 600);
            stage.setScene(scene);
            stage.show();
        }
    }
}
