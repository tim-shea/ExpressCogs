package expresscogs.gui;

import expresscogs.network.Network;
import expresscogs.network.SynapseGroup;
import expresscogs.utility.ViewUtility;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

public class SynapseScalingTool extends VBox {
    private Network network;
    
    public SynapseScalingTool(Network network) {
        this.network = network;
        setPadding(new Insets(10, 10, 10, 10));
        createSliders();
    }
    
    private void createSliders() {
        for (SynapseGroup group : network.getSynapseGroups()) {
            ViewUtility.createSlider(this, "Scale " + group.getName(), group.getWeightScale(), 0, 2, (observable, oldValue, newValue) -> {
                group.setWeightScale(newValue.doubleValue());
            });
        }
    }
}
