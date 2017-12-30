package expresscogs.gui;

import expresscogs.network.UniformNoiseGenerator;
import expresscogs.utility.ViewUtility;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class NoiseGeneratorTool extends TitledPane {
    private UniformNoiseGenerator generator;
    private VBox controls;
    
    public NoiseGeneratorTool(UniformNoiseGenerator generator) {
        this.generator = generator;
        this.setAnimated(false);
        setText("Noise Generator");
        controls = new VBox();
        controls.setPadding(new Insets(10, 10, 10, 10));
        createSliders();
        setContent(controls);
    }
    
    private void createSliders() {
        ViewUtility.createSlider(controls, "Noise Scale", generator.getScale(), 0, 2e-3, (observable, oldValue, newValue) -> {
            generator.setScale(newValue.doubleValue());
        });
    }
}
