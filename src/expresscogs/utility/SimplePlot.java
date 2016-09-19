package expresscogs.utility;

import java.util.List;

import org.jblas.DoubleMatrix;

import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Pair;

public class SimplePlot {
    public static class Line {
        public Line(DoubleMatrix x, DoubleMatrix y) {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> lineChart = new LineChart<Number, Number>(xAxis, yAxis);

            ObservableList<Series<Number, Number>> dataset = lineChart.getData();
            for (int c = 0; c < y.columns; ++c)
                dataset.add(new Series<Number, Number>());
            for (int r = 0; r < y.rows; ++r)
                for (int c = 0; c < y.columns; ++c)
                    dataset.get(c).getData().add(new Data<Number, Number>(x.get(r), y.get(r, c)));

            container.getChildren().add(lineChart);
        }
    }

    public static class Scatter {
        private NumberAxis xAxis;
        private NumberAxis yAxis;
        private ScatterChart<Number, Number> scatter;
        // private Series<Number,Number> series;

        public Scatter() {
            xAxis = new NumberAxis();
            yAxis = new NumberAxis();

            xAxis.setAutoRanging(false);
            xAxis.setMinorTickVisible(false);
            yAxis.setAutoRanging(false);
            yAxis.setMinorTickVisible(false);

            scatter = new ScatterChart<Number, Number>(xAxis, yAxis);
            // series = new Series<Number,Number>();
            // scatter.getData().add(series);
            scatter.setAnimated(false);

            container.getChildren().add(scatter);
        }

        public void addSeries(Series<Number, Number> series) {
            scatter.getData().add(series);
        }

        public void addPoints(List<Pair<Number, Number>> points) {
            final Series<Number, Number> series = new Series<Number, Number>();
            scatter.getData().add(series);
            for (Pair<Number, Number> point : points) {
                Platform.runLater(() -> {
                    series.getData().add(new Data<Number, Number>(point.getKey(), point.getValue()));
                });
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

    public static class HeatMap {
        private ImageView imageView;
        private WritableImage image;

        public HeatMap(int width, int height) {
            imageView = new ImageView();
            image = new WritableImage(width * 10, height * 10);
            //image = new WritableImage(width, height);
            imageView.setImage(image);
            container.getChildren().add(imageView);
            imageView.fitHeightProperty().bind(container.heightProperty().multiply(0.8));
            imageView.fitWidthProperty().bind(container.heightProperty().multiply(0.8 * width / (double) height));
        }

        public void setValues(DoubleMatrix values) {
            // DoubleMatrix scaledValues = values.sub(values.min());
            // scaledValues.divi(scaledValues.max());
            DoubleMatrix scaledValues = values.div(5);
            resample(scaledValues, 10);
            //copyValues(scaledValues);
        }

        private void resample(DoubleMatrix input, int scale) {
            Color startColor = Color.BLUE;
            Color endColor = Color.RED;
            PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < input.columns; y++) {
                for (int x = 0; x < input.rows; x++) {
                    Color color = (Color) Interpolator.LINEAR.interpolate(startColor, endColor, input.get(x, y));
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            writer.setColor(x * scale + dx, y * scale + dy, color);
                        }
                    }
                }
            }
        }

        private void copyValues(DoubleMatrix input) {
            Color startColor = Color.BLUE;
            Color endColor = Color.RED;
            PixelWriter writer = image.getPixelWriter();
            for (int y = 0; y < input.columns; y++) {
                for (int x = 0; x < input.rows; x++) {
                    Color color = (Color) Interpolator.LINEAR.interpolate(startColor, endColor, input.get(x, y));
                    writer.setColor(x, y, color);
                }
            }
        }
    }

    private static Pane container;

    public static void init(Stage stage) {
        container = new VBox();
        Scene scene = new Scene(container, 800, 600);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);
        stage.show();
    }
}
