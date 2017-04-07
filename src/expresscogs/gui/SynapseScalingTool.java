package expresscogs.gui;

import expresscogs.network.Network;
import expresscogs.network.synapses.SynapseGroup;
import expresscogs.utility.ViewUtility;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class SynapseScalingTool extends TitledPane {
    private Network network;
    private VBox controls;
    
    public SynapseScalingTool(Network network, double minWeightScale, double maxWeightScale) {
        this.network = network;
        this.setAnimated(false);
        setText("Synapse Scaling");
        controls = new VBox();
        setContent(controls);
        controls.setPadding(new Insets(10, 10, 10, 10));
        createSliders(minWeightScale, maxWeightScale);
    }
    
    private void createSliders(double minWeightScale, double maxWeightScale) {
        for (SynapseGroup group : network.getSynapseGroups()) {
            ViewUtility.createSlider(controls, "Scale " + group.getName(), group.getWeightScale(),
                    minWeightScale, maxWeightScale, (observable, oldValue, newValue) -> {
                group.setWeightScale(newValue.doubleValue());
            });
        }
    }
}
