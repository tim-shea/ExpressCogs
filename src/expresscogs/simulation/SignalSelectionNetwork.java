package expresscogs.simulation;

import expresscogs.gui.ResizingSeparator;
import expresscogs.gui.SimulationTool;
import expresscogs.gui.StimulusGeneratorTool;
import expresscogs.gui.SynapseScalingTool;
import expresscogs.network.*;
import expresscogs.network.synapses.NeighborhoodTopology;
import expresscogs.network.synapses.SynapseFactory;
import expresscogs.network.synapses.SynapseGroup;
import expresscogs.network.synapses.SynapseGroupTopology;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jblas.DoubleMatrix;
import expresscogs.utility.LocalFieldPotentialPlot;
import expresscogs.utility.NeuralFieldPlot;
import expresscogs.utility.SpikeRasterPlot;
import expresscogs.utility.TimeSeriesPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * SignalSelectionNetwork is a simulation which instantiates a topological
 * spiking neural network translation of the Gurney, Prescott, & Redgrave
 * Basal Ganglia (GPR-BG)(2001).
 * 
 * GPR-BG describes the intrinsic function of the basal ganglia as signal
 * selection and control pathways which are implemented in terms of
 * inhibitory off-center, on-surround circuits.
 * 
 * This translation of the model uses topologically-constrained connectivity
 * for the majority of the synaptic pathways in the model.
 * 
 * @author Tim
 */
public class SignalSelectionNetwork extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    private Simulation simulation;
    private Network network;
    private int tMax = 100000;
    private double lowBackgroundInput = 0.0e-3;
    private double highBackgroundInput = 0.2e-3;
    private int groupSize = 1000;
    private SynapseGroupTopology narrow = new NeighborhoodTopology(0.1, 0.05);
    private SynapseGroupTopology wide = new NeighborhoodTopology(0.1, 0.5);
    private double weightScale = 1e-4;
    private ContinuousStimulusGenerator thlInput;
    
    // Neuron groups
    private NeuronGroup thl;
    private NeuronGroup ctx;
    private NeuronGroup str;
    private NeuronGroup stn;
    private NeuronGroup gpi;
    private NeuronGroup gpe;
    private NeuronGroup st2;
    
    // Charts for visualization
    private SpikeRasterPlot rasterPlot;
    private NeuralFieldPlot fieldPlot;
    private LocalFieldPotentialPlot lfpPlot;
    private DoubleMatrix record;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        createNetwork();
        createVisualization(stage);
        runSimulation(stage);
    }
    
    /** Create the network structure */
    private void createNetwork() {
        network = new Network();
        
        // Create the neuron groups and add them to the network
        thlInput = new ContinuousStimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, highBackgroundInput);
        str = NeuronFactory.createLifInhibitory("STR", groupSize, lowBackgroundInput);
        st2 = NeuronFactory.createLifInhibitory("ST2", groupSize, lowBackgroundInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, lowBackgroundInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize / 4, lowBackgroundInput);
        gpe = NeuronFactory.createLifInhibitory("GPE", groupSize / 4, highBackgroundInput);
        network.addNeuronGroups(thl, ctx, str, st2, stn, gpi, gpe);
        
        // Setup the selection pathway synapse groups
        SynapseGroup thlCtx = SynapseFactory.connectWithDelay(thl, ctx, narrow, 1 * weightScale);
        SynapseGroup ctxStr = SynapseFactory.connectWithDelay(ctx, str, narrow, 0.5 * weightScale);
        SynapseGroup ctxStn = SynapseFactory.connectWithDelay(ctx, stn, wide, 1 * weightScale);
        SynapseGroup strGpi = SynapseFactory.connectWithDelay(str, gpi, narrow, 0.5 * weightScale);
        SynapseGroup stnGpi = SynapseFactory.connectWithDelay(stn, gpi, wide, 1 * weightScale);
        SynapseGroup gpiThl = SynapseFactory.connectWithDelay(gpi, thl, narrow, 1 * weightScale);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
        
        // Setup the control pathway synapse groups
        SynapseGroup ctxSt2 = SynapseFactory.connectWithDelay(ctx, st2, narrow, 0.5 * weightScale);
        SynapseGroup st2Gpe = SynapseFactory.connectWithDelay(st2, gpe, narrow, 0.5 * weightScale);
        SynapseGroup stnGpe = SynapseFactory.connectWithDelay(stn, gpe, wide, 1 * weightScale);
        SynapseGroup gpeStn = SynapseFactory.connectWithDelay(gpe, stn, narrow, 0.5 * weightScale);
        SynapseGroup gpeGpi = SynapseFactory.connectWithDelay(gpe, gpi, narrow, 0.5 * weightScale);
        network.addSynapseGroups(ctxSt2, st2Gpe, stnGpe, gpeStn, gpeGpi);
        
        record = new DoubleMatrix(tMax, 7);
        simulation = new Simulation() {
            @Override
            public void updateModel() {
                final double t = getTime();
                network.update(getStep());
                
                rasterPlot.bufferSpikes(t);
                fieldPlot.bufferNeuralField(t);
                lfpPlot.bufferLfp(t);
                
                record.put(getStep(), 0, getTime());
                record.put(getStep(), 1, thlInput.getNoise());
                record.put(getStep(), 2, thlInput.getIntensity());
                record.put(getStep(), 3, thl.getSpikes().sum());
                record.put(getStep(), 4, ctx.getSpikes().sum());
                record.put(getStep(), 5, stn.getSpikes().sum());
                //record.put(getStep(), 6, lfpPlot.getLfp());
            }
            
            @Override
            public void updateVisualization() {
                final double t = getTime();
                sync();
                Platform.runLater(() -> {
                    rasterPlot.updatePlot(t);
                    fieldPlot.updatePlot(t);
                    lfpPlot.updatePlot(t);
                    sync();
                });
            }
        };
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox mainContainer = new VBox();
        mainContainer.setPadding(new Insets(10, 10, 10, 10));
        mainContainer.setSpacing(10);
        Scene scene = new Scene(mainContainer, 1280, 720);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);
        
        SimulationTool simulationTool = new SimulationTool(simulation);
        StimulusGeneratorTool stimulusTool = new StimulusGeneratorTool(thlInput);
        SynapseScalingTool synapseTool = new SynapseScalingTool(network, 0, weightScale * 2);
        
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
        
        HBox plotControlsContainer = new HBox();
        plotContainer.getChildren().addAll(plotControlsContainer);
        ComboBox<String> neuronGroupCombo = new ComboBox<String>();
        neuronGroupCombo.getItems().addAll("THL", "CTX", "STR", "ST2", "STN", "GPI", "GPE");
        neuronGroupCombo.valueProperty().addListener((listener, oldValue, newValue) -> {
            fieldPlot.setNeuronGroup(network.getNeuronGroup(newValue));
        });
        plotControlsContainer.getChildren().add(neuronGroupCombo);
        
        rasterPlot = new SpikeRasterPlot(network, 100);
        TitledPane rasterPlotContainer = new TitledPane("Spike Raster Plot", rasterPlot.getChart());
        rasterPlotContainer.expandedProperty().addListener((listener, oldValue, newValue) -> {
            rasterPlot.setEnabled(newValue);
        });
        rasterPlotContainer.setAnimated(false);
        plotContainer.getChildren().add(rasterPlotContainer);
        ResizingSeparator rasterSeparator = new ResizingSeparator(rasterPlot.getChart(), Orientation.HORIZONTAL);
        plotContainer.getChildren().add(rasterSeparator);
        VBox.setVgrow(rasterPlot.getChart(), Priority.ALWAYS);
        
        fieldPlot = new NeuralFieldPlot();
        TitledPane fieldPlotContainer = new TitledPane("Neural Field Plot", fieldPlot.getChart());
        fieldPlotContainer.setAnimated(false);
        fieldPlotContainer.expandedProperty().addListener((listener, oldValue, newValue) -> {
            fieldPlot.setEnabled(newValue);
        });
        plotContainer.getChildren().add(fieldPlotContainer);
        VBox.setVgrow(fieldPlot.getChart(), Priority.ALWAYS);
        neuronGroupCombo.setValue("THL");
        ResizingSeparator plotSeparator = new ResizingSeparator(fieldPlot.getChart(), Orientation.HORIZONTAL);
        plotContainer.getChildren().add(plotSeparator);
        
        lfpPlot = new LocalFieldPotentialPlot(stn);
        TitledPane lfpPlotContainer = new TitledPane("Local Field Potential Plot", lfpPlot.getChart());
        lfpPlotContainer.setAnimated(false);
        lfpPlotContainer.expandedProperty().addListener((listener, oldValue, newValue) -> {
            lfpPlot.setEnabled(newValue);
        });
        plotContainer.getChildren().add(lfpPlotContainer);
        VBox.setVgrow(lfpPlot.getChart(), Priority.ALWAYS);
        
        stage.show();
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        stage.setOnCloseRequest(event -> {
            simulation.stop();
            network.shutdown();
        });
    }
}
