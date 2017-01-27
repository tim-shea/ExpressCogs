package expresscogs.simulation;

import expresscogs.network.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jblas.DoubleMatrix;

import expresscogs.utility.HeatMap;
import expresscogs.utility.TimeSeriesPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
 *
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
    private double lowInput = 0.4e-3;
    private double highInput = 1.5 * lowInput;
    private int groupSize = 250;
    private double connectivity = 0.1;
    private double narrow = 0.1;
    private double wide = 0.5;
    private double minWeight = 0;
    private double maxWeight = 1e-4;

    // Neuron groups
    private NeuronGroup thl;
    private NeuronGroup ctx;
    private NeuronGroup str;
    private NeuronGroup str2;
    private NeuronGroup stn;
    private NeuronGroup gpi;
    private NeuronGroup gpe;
    
    // Buffered data for visualization
    private DoubleMatrix spikeCounts;
    
    // Charts for visualization
    private TimeSeriesPlot raster;
    private HeatMap heatmap;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        createNetwork();
        createVisualization(stage);
        runSimulation(stage);
    }
    
    /** Create the network structure, consisting of 5 neuron groups and 6 synaptic
     * pathways. */
    private void createNetwork() {
        network = new Network();
        
        // Create the neuron groups and add them to the network
        InputGenerator thlInput = new StimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, lowInput);
        str = NeuronFactory.createLifInhibitory("STR", groupSize, lowInput);
        str2 = NeuronFactory.createLifInhibitory("STR2", groupSize, lowInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, highInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize, lowInput);
        gpe = NeuronFactory.createLifInhibitory("GPE", groupSize, lowInput);
        network.addNeuronGroups(thl, ctx, str, str2, stn, gpi, gpe);
        
        // Create the synapse groups and add them to the network
        SynapseGroup thlCtx = SynapseFactory.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseFactory.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStn = SynapseFactory.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, maxWeight);
        SynapseGroup ctxStr2 = SynapseFactory.connectNeighborhood(ctx, str2, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup strGpi = SynapseFactory.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup str2Gpe = SynapseFactory.connectNeighborhood(str2, gpe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup stnGpi = SynapseFactory.connectNeighborhood(stn, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup stnGpe = SynapseFactory.connectNeighborhood(stn, gpe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpiThl = SynapseFactory.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpeStn = SynapseFactory.connectNeighborhood(gpe, stn, 0, narrow, minWeight, maxWeight);
        SynapseGroup gpeGpi = SynapseFactory.connectNeighborhood(gpe, gpi, 0, narrow, minWeight, maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, ctxStr2, strGpi, str2Gpe, stnGpi, stnGpe, gpiThl, gpeStn, gpeGpi);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox container = new VBox();
        container.setPadding(new Insets(10, 10, 10, 10));
        container.setSpacing(10);
        Scene scene = new Scene(container, 800, 600);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);

        FlowPane toolbar = new FlowPane();
        toolbar.setHgap(10);
        Button runButton = new Button("run");
        runButton.setOnAction(event -> pauseSimulation = false);
        Button pauseButton = new Button("pause");
        pauseButton.setOnAction(event -> pauseSimulation = true);
        toolbar.getChildren().addAll(runButton, pauseButton);
        container.getChildren().add(toolbar);

        HBox mainPanel = new HBox();

        TimeSeriesPlot.init(container);
        raster = TimeSeriesPlot.scatter();
        raster.addSeries("THL");
        raster.addSeries("CTX");
        raster.addSeries("STR");
        raster.addSeries("STN");
        raster.addSeries("GPI");
        heatmap = new HeatMap(groupSize, 5);
        spikeCounts = DoubleMatrix.zeros(groupSize, 5);

        stage.show();
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        pauseSimulation = true;
        Task<Void> task = new Task<Void>() {
            DoubleMatrix sample = DoubleMatrix.rand(groupSize).lti(0.2);
            
            @Override
            protected Void call() throws Exception {
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

                    raster.bufferPoints("THL", t, thl.getXPosition().get(thl.getSpikes().and(sample)).data);
                    raster.bufferPoints("CTX", t, ctx.getXPosition().get(ctx.getSpikes().and(sample)).add(1).data);
                    raster.bufferPoints("STR", t, str.getXPosition().get(str.getSpikes().and(sample)).add(2).data);
                    raster.bufferPoints("STN", t, stn.getXPosition().get(stn.getSpikes().and(sample)).add(3).data);
                    raster.bufferPoints("GPI", t, gpi.getXPosition().get(gpi.getSpikes().and(sample)).add(4).data);
                    spikeCounts.muli(0.99);
                    spikeCounts.putColumn(0, spikeCounts.getColumn(0).add(thl.getSpikes()));
                    spikeCounts.putColumn(1, spikeCounts.getColumn(1).add(ctx.getSpikes()));
                    spikeCounts.putColumn(2, spikeCounts.getColumn(2).add(str.getSpikes()));
                    spikeCounts.putColumn(3, spikeCounts.getColumn(3).add(stn.getSpikes()));
                    spikeCounts.putColumn(4, spikeCounts.getColumn(4).add(gpi.getSpikes()));
                    
                    if (step % 50 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.addPoints();
                            raster.setLimits(t - 0.5, t, 0, 5);
                            heatmap.setValues(spikeCounts);
                            waitForSync = false;
                        });
                        while (waitForSync) {
                            Thread.sleep(100);
                        }
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        stage.setOnCloseRequest(event -> thread.interrupt());
        thread.start();
    }
}
