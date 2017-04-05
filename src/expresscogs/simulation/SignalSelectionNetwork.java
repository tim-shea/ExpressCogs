package expresscogs.simulation;

import expresscogs.gui.ResizingSeparator;
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
    private SpikeRasterPlot raster;
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
        SynapseGroup thlCtx = SynapseFactory.connect(thl, ctx, narrow, 1 * weightScale);
        SynapseGroup ctxStr = SynapseFactory.connect(ctx, str, narrow, 0.5 * weightScale);
        SynapseGroup ctxStn = SynapseFactory.connect(ctx, stn, wide, 1 * weightScale);
        SynapseGroup strGpi = SynapseFactory.connect(str, gpi, narrow, 0.5 * weightScale);
        SynapseGroup stnGpi = SynapseFactory.connect(stn, gpi, wide, 1 * weightScale);
        SynapseGroup gpiThl = SynapseFactory.connect(gpi, thl, narrow, 1 * weightScale);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
        
        // Setup the control pathway synapse groups
        SynapseGroup ctxSt2 = SynapseFactory.connect(ctx, st2, narrow, 0.5 * weightScale);
        SynapseGroup st2Gpe = SynapseFactory.connect(st2, gpe, narrow, 0.5 * weightScale);
        SynapseGroup stnGpe = SynapseFactory.connect(stn, gpe, wide, 1 * weightScale);
        SynapseGroup gpeStn = SynapseFactory.connect(gpe, stn, narrow, 0.5 * weightScale);
        SynapseGroup gpeGpi = SynapseFactory.connect(gpe, gpi, narrow, 0.5 * weightScale);
        network.addSynapseGroups(ctxSt2, st2Gpe, stnGpe, gpeStn, gpeGpi);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox mainContainer = new VBox();
        mainContainer.setPadding(new Insets(10, 10, 10, 10));
        mainContainer.setSpacing(10);
        Scene scene = new Scene(mainContainer, 1280, 720);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);
        
        FlowPane toolbar = new FlowPane();
        toolbar.setHgap(10);
        
        Button runButton = new Button("run");
        runButton.setOnAction(event -> simulation.runAsync(tMax));
        
        Button pauseButton = new Button("stop");
        pauseButton.setOnAction(event -> simulation.stop());
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/desktop/"));
        fileChooser.setInitialFileName("lfp_data.csv");
        Button saveButton = new Button("save");
        saveButton.setOnAction(event -> {
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                NumberFormat format = DecimalFormat.getNumberInstance();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write("t,ns,in,thl,ctx,stn,lfp");
                    writer.newLine();
                    for (int i = 0; i < record.rows; ++i) {
                        writer.write(format.format(record.get(i, 0)) + ',');
                        writer.write(format.format(record.get(i, 1)) + ',');
                        writer.write(format.format(record.get(i, 2)) + ',');
                        writer.write(format.format(record.get(i, 3)) + ',');
                        writer.write(format.format(record.get(i, 4)) + ',');
                        writer.write(format.format(record.get(i, 5)) + ',');
                        writer.write(format.format(record.get(i, 6)));
                        writer.newLine();
                    }
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        StimulusGeneratorTool stimulusTool = new StimulusGeneratorTool(thlInput);
        SynapseScalingTool synapseTool = new SynapseScalingTool(network, 0, weightScale * 2);
        
        toolbar.getChildren().addAll(runButton, pauseButton, saveButton);
        mainContainer.getChildren().add(toolbar);
        HBox hbox = new HBox();
        mainContainer.getChildren().add(hbox);
        ScrollPane toolboxScrollPane = new ScrollPane();
        toolboxScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        VBox toolbox = new VBox();
        ResizingSeparator toolSeparator = new ResizingSeparator(toolboxScrollPane, Orientation.VERTICAL);
        toolbox.getChildren().addAll(stimulusTool, synapseTool);
        hbox.getChildren().addAll(toolboxScrollPane, toolSeparator);
        toolboxScrollPane.setContent(toolbox);
        toolboxScrollPane.setFitToWidth(true);
        
        VBox plotContainer = new VBox();
        mainContainer.setPadding(new Insets(10, 10, 10, 10));
        mainContainer.setSpacing(10);
        hbox.getChildren().add(plotContainer);
        HBox.setHgrow(plotContainer, Priority.ALWAYS);
        TimeSeriesPlot.init(plotContainer);
        
        //raster = new SpikeRasterPlot(network, 100);
        //ResizingSeparator plotSeparator = new ResizingSeparator(raster.getChart(), Orientation.HORIZONTAL);
        //plotContainer.getChildren().add(plotSeparator);
        
        ComboBox<String> neuronGroupCombo = new ComboBox<String>();
        neuronGroupCombo.getItems().addAll("THL", "CTX", "STR", "ST2", "STN", "GPI", "GPE");
        neuronGroupCombo.valueProperty().addListener((listener, oldValue, newValue) -> {
            fieldPlot.setNeuronGroup(network.getNeuronGroup(newValue));
        });
        plotContainer.getChildren().add(neuronGroupCombo);
        fieldPlot = new NeuralFieldPlot();
        neuronGroupCombo.setValue("THL");
        ResizingSeparator plotSeparator = new ResizingSeparator(fieldPlot.getChart(), Orientation.HORIZONTAL);
        plotContainer.getChildren().add(plotSeparator);
        
        lfpPlot = new LocalFieldPotentialPlot(stn);
        VBox.setVgrow(lfpPlot.getChart(), Priority.ALWAYS);
        
        stage.show();
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        record = new DoubleMatrix(tMax, 7);
        simulation = new Simulation() {
            @Override
            public void update() {
                final double t = getTime();
                network.update(getStep());
                
                //raster.bufferSpikes(t);
                fieldPlot.bufferNeuralField(t);
                lfpPlot.bufferLfp(t);
                
                record.put(getStep(), 0, getTime());
                record.put(getStep(), 1, thlInput.getNoise());
                record.put(getStep(), 2, thlInput.getIntensity());
                record.put(getStep(), 3, thl.getSpikes().sum());
                record.put(getStep(), 4, ctx.getSpikes().sum());
                record.put(getStep(), 5, stn.getSpikes().sum());
                record.put(getStep(), 6, lfpPlot.getLfp());
                
                if (getStep() % 20 == 0 || getStep() == tMax - 1) {
                    sync();
                    Platform.runLater(() -> {
                        //raster.updatePlot(t);
                        if (getStep() % 1000 == 0)
                            fieldPlot.updatePlot(t);
                        lfpPlot.updatePlot(t);
                        sync();
                    });
                }
            }
        };
        stage.setOnCloseRequest(event -> {
            simulation.stop();
            network.shutdown();
        });
    }
}
