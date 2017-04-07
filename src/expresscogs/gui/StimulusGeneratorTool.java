package expresscogs.gui;

import expresscogs.network.ContinuousStimulusGenerator;
import expresscogs.utility.ViewUtility;
import javafx.geometry.Insets;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class StimulusGeneratorTool extends TitledPane {
    private ContinuousStimulusGenerator generator;
    private VBox controls;
    
    public StimulusGeneratorTool(ContinuousStimulusGenerator generator) {
        this.generator = generator;
        this.setAnimated(false);
        setText("Stimulus Generator");
        controls = new VBox();
        controls.setPadding(new Insets(10, 10, 10, 10));
        createSliders();
        setContent(controls);
    }
    
    private void createSliders() {
        Slider snrSlider = ViewUtility.createSlider(controls, "Signal-to-Noise Ratio", generator.getSignalToNoiseRatio(),
                0, 4, (observable, oldValue, newValue) -> {
            generator.setSignalToNoiseRatio(newValue.doubleValue());
        });
        ViewUtility.createSlider(controls, "Noise Scale", generator.getNoise(), 0, 2e-3, (observable, oldValue, newValue) -> {
            generator.setNoise(newValue.doubleValue());
            generator.setSignalToNoiseRatio(snrSlider.getValue());
        });
        ViewUtility.createSlider(controls, "Width", generator.getWidth(), 0.01, 0.25, (observable, oldValue, newValue) -> {
            generator.setWidth(newValue.doubleValue());
        });
        ViewUtility.createSlider(controls, "Position", generator.getPosition(), 0.05, 0.95, (observable, oldValue, newValue) -> {
            generator.setPosition(newValue.doubleValue());
        });
    }
}
