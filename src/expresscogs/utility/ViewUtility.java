package expresscogs.utility;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;

public final class ViewUtility {
    public static Slider createSlider(Pane container, String name, double initialValue, double minValue, double maxValue, ChangeListener<? super Number> valueListener) {
        Label label = new Label(name);
        Slider slider = new Slider();
        slider.setValue(initialValue);
        slider.setMin(minValue);
        slider.setMax(maxValue);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((maxValue - minValue) / 2);
        slider.setBlockIncrement((maxValue - minValue) / 8);
        slider.valueProperty().addListener(valueListener);
        container.getChildren().addAll(label, slider);
        return slider;
    }
}