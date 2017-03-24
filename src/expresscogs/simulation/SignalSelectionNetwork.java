package expresscogs.simulation;

import expresscogs.gui.ResizingSeparator;
import expresscogs.gui.StimulusGeneratorTool;
import expresscogs.gui.SynapseScalingTool;
import expresscogs.network.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import expresscogs.utility.SpikeRasterPlot;
import expresscogs.utility.TimeSeriesPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
    
    // Network manages the synapse and neuron groups
    private Network network;
    // Flag for synchronization
    private boolean waitForSync;
    // Flag for simulation state
    private boolean pauseSimulation;

    /* Simulation parameters -----------------------------------------------------------------------------------------*/

    // Total number of simulation steps (duration = dt * tSteps)
    private int tSteps = 100000;
    // Duration of simulation step in seconds
    private double dt = 0.001;
    private double lowBackgroundInput = 0.0e-3;
    private double highBackgroundInput = 0.2e-3;
    private int groupSize = 500;
    private double connectivity = 0.1;
    private double narrow = 0.05;
    private double wide = 0.5;
    private double minWeight = 0;
    private double maxWeight = 2e-4;
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
    private LocalFieldPotentialPlot lfpPlot;
    private DoubleMatrix record;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        createNetwork();
        createVisualization(stage);
        runSimulation(stage);
    }
    
    /** Create the network structure, consisting of 5 neuron groups and 6 synaptic pathways. */
    private void createNetwork() {
        network = new Network();
        
        // Create the neuron groups and add them to the network
        thlInput = new ContinuousStimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, highBackgroundInput);
        str = NeuronFactory.createLifInhibitory("STR", groupSize, lowBackgroundInput);
        st2 = NeuronFactory.createLifInhibitory("ST2", groupSize, lowBackgroundInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, lowBackgroundInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize, lowBackgroundInput);
        gpe = NeuronFactory.createLifInhibitory("GPE", groupSize, highBackgroundInput);
        network.addNeuronGroups(thl, ctx, str, st2, stn, gpi, gpe);
        
        // Setup the selection pathway synapse groups
        SynapseGroup thlCtx = SynapseFactory.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseFactory.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, 1 * maxWeight);
        SynapseGroup ctxStn = SynapseFactory.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, 2 * maxWeight);
        SynapseGroup strGpi = SynapseFactory.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, 1.5 * maxWeight);
        SynapseGroup stnGpi = SynapseFactory.connectNeighborhood(stn, gpi, connectivity, wide, minWeight, 2 * maxWeight);
        SynapseGroup gpiThl = SynapseFactory.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, 0.5 * maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
        
        // Setup the control pathway synapse groups
        SynapseGroup ctxSt2 = SynapseFactory.connectNeighborhood(ctx, st2, connectivity, narrow, minWeight, 0 * maxWeight);
        SynapseGroup st2Gpe = SynapseFactory.connectNeighborhood(st2, gpe, connectivity, narrow, minWeight, 0 * maxWeight);
        SynapseGroup stnGpe = SynapseFactory.connectNeighborhood(stn, gpe, connectivity, wide, minWeight, 0 * maxWeight);
        SynapseGroup gpeStn = SynapseFactory.connectNeighborhood(gpe, stn, connectivity, narrow, minWeight, 0 * maxWeight);
        SynapseGroup gpeGpi = SynapseFactory.connectNeighborhood(gpe, gpi, connectivity, narrow, minWeight, 0 * maxWeight);
        network.addSynapseGroups(st2Gpe, gpeStn, gpeGpi, stnGpe, ctxSt2);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox mainContainer = new VBox();
        mainContainer.setPadding(new Insets(10, 10, 10, 10));
        mainContainer.setSpacing(10);
        Scene scene = new Scene(mainContainer, 1200, 800);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);

        FlowPane toolbar = new FlowPane();
        toolbar.setHgap(10);
        
        Button runButton = new Button("run");
        runButton.setOnAction(event -> pauseSimulation = false);
        
        Button pauseButton = new Button("pause");
        pauseButton.setOnAction(event -> pauseSimulation = true);
        
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
                    writer.write("t,in,lfp,theta");
                    writer.newLine();
                    for (int i = 0; i < record.rows; ++i) {
                        writer.write(format.format(record.get(i, 0)) + ',');
                        writer.write(format.format(record.get(i, 1)) + ',');
                        writer.write(format.format(record.get(i, 2)));
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
        SynapseScalingTool synapseTool = new SynapseScalingTool(network);
        
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
        raster = new SpikeRasterPlot(network);
        ResizingSeparator plotSeparator = new ResizingSeparator(raster.getChart(), Orientation.HORIZONTAL);
        plotContainer.getChildren().add(plotSeparator);
        lfpPlot = new LocalFieldPotentialPlot(stn);
        VBox.setVgrow(lfpPlot.getChart(), Priority.ALWAYS);
        
        stage.show();
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        pauseSimulation = true;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                record = new DoubleMatrix(tSteps, 3);
                for (int step = 0; step < tSteps; step += 1) {
                    while (pauseSimulation) {
                        Thread.sleep(50);
                    }
                    
                    final double t = step * dt;
                    try {
                        network.update(step);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    raster.bufferSpikes(t);
                    lfpPlot.bufferLfp(t);
                    
                    record.put(step, 0, t);
                    record.put(step, 1, 0);
                    record.put(step, 2, lfpPlot.getLfp());
                    
                    if (step % 100 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.updatePlot(t);
                            lfpPlot.updatePlot(t);
                            waitForSync = false;
                        });
                        Thread.sleep(0);
                        while (waitForSync) {
                            Thread.sleep(1);
                        }
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        stage.setOnCloseRequest(event -> {
            thread.interrupt();
        });
        thread.start();
    }
}
