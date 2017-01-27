package expresscogs.utility;

import expresscogs.gui.ResizeListener;
import org.jblas.DoubleMatrix;

import javafx.animation.Interpolator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class HeatMap {
    private DoubleMatrix values;
    private ImageView imageView;
    private WritableImage image;
    private ScrollPane pane;
    private int width;
    private int height;
    private int widthScale;
    private int heightScale;

    public HeatMap(int width, int height) {
        this.width = width;
        this.height = height;
        values = DoubleMatrix.zeros(width, height);
        imageView = new ImageView();
        pane = new ScrollPane();
        pane.setMinHeight(100);
        pane.setMinWidth(100);
        pane.setContent(imageView);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setPannable(true);
        TimeSeriesPlot.container.getChildren().add(pane);
        pane.widthProperty().addListener((observable, oldValue, newValue) -> rescale());
        pane.heightProperty().addListener((observable, oldValue, newValue) -> rescale());
        imageView.fitWidthProperty().bind(pane.widthProperty());
        imageView.fitHeightProperty().bind(pane.heightProperty().subtract(2));
        ResizeListener.makeResizable(pane, ResizeListener.Mode.BOTH);
        rescale();
    }

    private void rescale() {
        if (pane.getWidth() > 0 && pane.getHeight() > 0) {
            widthScale = Math.max((int) pane.getWidth() / width, 1);
            heightScale = Math.max((int) pane.getHeight() / height, 1);
            image = new WritableImage(width * widthScale, height * heightScale);
            imageView.setImage(image);
            resample();
        }
    }

    public void setValues(DoubleMatrix values) {
        if (image != null) {
            this.values = values.div(values.max());
            resample();
        }
    }

    private void resample() {
        Color startColor = Color.color(0.216, 0.494, 0.722);
        Color endColor = Color.color(0.894, 0.102, 0.110);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = (Color) Interpolator.LINEAR.interpolate(startColor, endColor, values.get(x, y));
                for (int dy = 0; dy < heightScale; dy++) {
                    for (int dx = 0; dx < widthScale; dx++) {
                        writer.setColor(x * widthScale + dx, y * heightScale + dy, color);
                    }
                }
            }
        }
    }
}