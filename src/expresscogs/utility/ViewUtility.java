package expresscogs.utility;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

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
        slider.setLabelFormatter(new StringConverter<Double>() {
            private NumberFormat standardFormat = NumberFormat.getInstance();
            private NumberFormat exponentFormat = new DecimalFormat("0.##E0");
            
            @Override
            public String toString(Double value) {
                if (Math.abs(value) > 0 && Math.abs(value) < 0.01) {
                    return exponentFormat.format(value);
                } else {
                    return standardFormat.format(value);
                }
            }
            
            @Override
            public Double fromString(String string) {
                try {
                    return standardFormat.parse(string).doubleValue();
                } catch (ParseException e) {
                    throw new RuntimeException("Invalid Value String");
                }
            }
        });
        container.getChildren().addAll(label, slider);
        return slider;
    }
}