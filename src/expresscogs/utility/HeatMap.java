package expresscogs.utility;

import org.jblas.DoubleMatrix;

import javafx.animation.Interpolator;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class HeatMap {
    private ImageView imageView;
    private WritableImage image;

    public HeatMap(int width, int height) {
        imageView = new ImageView();
        image = new WritableImage(width * 10, height * 10);
        imageView.setImage(image);
        TimeSeriesPlot.container.getChildren().add(imageView);
        imageView.fitHeightProperty().bind(TimeSeriesPlot.container.heightProperty().multiply(0.45));
        imageView.fitWidthProperty().bind(TimeSeriesPlot.container.widthProperty().multiply(0.97));
    }

    public void setValues(DoubleMatrix values) {
        DoubleMatrix scaledValues = values.div(values.max());
        resample(scaledValues, 10);
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
}