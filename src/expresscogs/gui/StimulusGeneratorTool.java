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
        Slider intensitySlider = ViewUtility.createSlider(this, "Intensity", generator.getIntensity(), 0, 3e-3, (observable, oldValue, newValue) -> {
            generator.setIntensity(newValue.doubleValue());
        });
        ViewUtility.createSlider(this, "Width", generator.getWidth(), 0.01, 0.25, (observable, oldValue, newValue) -> {
            generator.setWidth(newValue.doubleValue());
        });
        ViewUtility.createSlider(this, "Position", generator.getPosition(), 0.05, 0.95, (observable, oldValue, newValue) -> {
            generator.setPosition(newValue.doubleValue());
        });
        //Slider noiseSlider = ViewUtility.createSlider(this, "Noise", generator.getNoise(), 0, 3e-3, (observable, oldValue, newValue) -> {
        //    generator.setNoise(newValue.doubleValue());
        //});
        ViewUtility.createSlider(this, "Noise Scale", generator.getNoise(), 0, 3e-3, (observable, oldValue, newValue) -> {
            generator.setIntensity(newValue.doubleValue() * generator.getSignalToNoiseRatio());
            generator.setNoise(newValue.doubleValue());
            intensitySlider.setValue(generator.getIntensity());
            //noiseSlider.setValue(generator.getNoise());
        });
    }
}
