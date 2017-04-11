package expresscogs.simulation;

import expresscogs.gui.ResizingSeparator;
import expresscogs.gui.SimulationTool;
import expresscogs.gui.SimulationView;
import expresscogs.gui.StimulusGeneratorTool;
import expresscogs.gui.SynapseScalingTool;
import expresscogs.network.Network;
import expresscogs.utility.BufferedPlot;
import expresscogs.utility.LocalFieldPotentialPlot;
import expresscogs.utility.NeuralFieldPlot;
import expresscogs.utility.SignalSelectionPlot;
import expresscogs.utility.SpikeRasterPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignalSelectionApplication extends Application implements SimulationView {
    public static void main(String[] args) {
        SignalSelectionApplication.launch(args);
    }
    
    private SignalSelectionNetwork simulation;
    private int stepsBetweenView = 20;
    
    // Charts for visualization
    private SpikeRasterPlot rasterPlot;
    private NeuralFieldPlot fieldPlot;
    private LocalFieldPotentialPlot lfpPlot;
    private SignalSelectionPlot signalPlot;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        simulation = new SignalSelectionNetwork(this);
        createVisualization(stage);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox mainContainer = new VBox();
        mainContainer.setPadding(new Insets(10, 10, 10, 10));
        mainContainer.setSpacing(10);
        Scene scene = new Scene(mainContainer, 1280, 720);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);
        
        SimulationTool simulationTool = new SimulationTool(simulation, this);
        StimulusGeneratorTool stimulusTool = new StimulusGeneratorTool(simulation.getInputGenerator());
        SynapseScalingTool synapseTool = new SynapseScalingTool(simulation.getNetwork(), 0, simulation.getWeightScale() * 2);
        
        HBox hbox = new HBox();
        VBox.setVgrow(hbox, Priority.ALWAYS);
        mainContainer.getChildren().add(hbox);
        ScrollPane toolboxScrollPane = new ScrollPane();
        toolboxScrollPane.setStyle("-fx-background-color: transparent");
        toolboxScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        VBox toolbox = new VBox();
        ResizingSeparator toolSeparator = new ResizingSeparator(toolboxScrollPane, Orientation.VERTICAL);
        toolbox.getChildren().addAll(simulationTool, stimulusTool, synapseTool);
        hbox.getChildren().addAll(toolboxScrollPane, toolSeparator);
        toolboxScrollPane.setContent(toolbox);
        toolboxScrollPane.setFitToWidth(true);
        
        ScrollPane plotScrollPane = new ScrollPane();
        plotScrollPane.setStyle("-fx-background-color: transparent");
        plotScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        VBox plotContainer = new VBox();
        plotScrollPane.setContent(plotContainer);
        HBox.setHgrow(plotScrollPane, Priority.ALWAYS);
        plotScrollPane.setFitToHeight(true);
        plotScrollPane.setFitToWidth(true);
        mainContainer.setPadding(new Insets(4, 4, 4, 4));
        mainContainer.setSpacing(10);
        hbox.getChildren().add(plotScrollPane);
        
        rasterPlot = new SpikeRasterPlot(simulation.getNetwork(), 100);
        addPlot(plotContainer, rasterPlot, "Spike Raster Plot", true);
        
        fieldPlot = new NeuralFieldPlot(simulation.getFieldSensor());
        addPlot(plotContainer, fieldPlot, "Neural Field Plot", true);
        
        lfpPlot = new LocalFieldPotentialPlot(simulation.getLfpSensor());
        addPlot(plotContainer, lfpPlot, "Local Field Potential Plot", true);
        
        signalPlot = new SignalSelectionPlot(simulation.getSignalSensor());
        addPlot(plotContainer, signalPlot, "Signal Selection Plot", false);
        
        stage.setOnCloseRequest(event -> {
            simulation.stop();
            Network.shutdownUpdater();
        });
        
        stage.show();
    }
    
    private void addPlot(Pane container, BufferedPlot plot, String title, boolean resizable) {
        TitledPane titlePane = new TitledPane(title, plot.getChart());
        titlePane.expandedProperty().addListener((listener, oldValue, newValue) -> {
            plot.setEnabled(newValue);
        });
        titlePane.setAnimated(false);
        titlePane.setExpanded(false);
        container.getChildren().add(titlePane);
        if (resizable) {
            ResizingSeparator separator = new ResizingSeparator(plot.getChart(), Orientation.HORIZONTAL);
            container.getChildren().add(separator);
        }
        VBox.setVgrow(plot.getChart(), Priority.ALWAYS);
    }
    
    public int getStepsBetweenView() {
        return stepsBetweenView;
    }
    
    public void setStepsBetweenView(int value) {
        stepsBetweenView = value;
    }
    
    @Override
    public void update() {
        updateBuffers();
        if (simulation.getStep() % stepsBetweenView == 0) {
            updatePlots();
        }
    }
    
    private void updateBuffers() {
        final double t = simulation.getTime();
        rasterPlot.updateBuffers(t);
        fieldPlot.updateBuffers(t);
        lfpPlot.updateBuffers(t);
        signalPlot.updateBuffers(t);
    }
    
    private void updatePlots() {
        final double t = simulation.getTime();
        simulation.sync();
        Platform.runLater(() -> {
            rasterPlot.updatePlot(t);
            fieldPlot.updatePlot(t);
            lfpPlot.updatePlot(t);
            signalPlot.updatePlot(t);
            simulation.sync();
        });
    }
}