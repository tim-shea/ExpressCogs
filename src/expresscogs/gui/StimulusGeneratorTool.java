package expresscogs.gui;

import expresscogs.network.ContinuousStimulusGenerator;
import expresscogs.utility.ViewUtility;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class StimulusGeneratorTool extends VBox {
    private ContinuousStimulusGenerator generator;
    
    public StimulusGeneratorTool(ContinuousStimulusGenerator generator) {
        this.generator = generator;
        setPadding(new Insets(10, 10, 10, 10));
        createSliders();
    }
    
    private void createSliders() {
        Slider snrSlider = ViewUtility.createSlider(this, "Signal-to-Noise Ratio", generator.getSignalToNoiseRatio(),
                0, 5, (observable, oldValue, newValue) -> {
            generator.setSignalToNoiseRatio(newValue.doubleValue());
        });
        ViewUtility.createSlider(this, "Noise Scale", generator.getNoise(), 0, 3e-3, (observable, oldValue, newValue) -> {
            generator.setNoise(newValue.doubleValue());
            generator.setSignalToNoiseRatio(snrSlider.getValue());
        });
        ViewUtility.createSlider(this, "Width", generator.getWidth(), 0.01, 0.25, (observable, oldValue, newValue) -> {
            generator.setWidth(newValue.doubleValue());
        });
        ViewUtility.createSlider(this, "Position", generator.getPosition(), 0.05, 0.95, (observable, oldValue, newValue) -> {
            generator.setPosition(newValue.doubleValue());
        });
    }
}
