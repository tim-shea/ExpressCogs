package expresscogs.simulation;

import expresscogs.network.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

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
    private double backgroundInput = 0.4e-3;
    private int groupSize = 500;
    private double connectivity = 0.1;
    private double narrow = 0.1;
    private double wide = 0.5;
    private double minWeight = 0;
    private double maxWeight = 1e-4;

    // Neuron groups
    private NeuronGroup thl;
    private NeuronGroup ctx;
    private NeuronGroup str;
    private NeuronGroup stn;
    private NeuronGroup gpi;
    private NeuronGroup gpe;
    private NeuronGroup st2;
    private NeuronGroup pfe;
    private NeuronGroup pfi;
    
    // Buffered data for visualization
    private DoubleMatrix spikeCounts;
    
    // Charts for visualization
    private TimeSeriesPlot raster;
    private TimeSeriesPlot lfpPlot;
    private DoubleMatrix distanceToElectrode;
    
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
        InputGenerator thlInput = new StimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, backgroundInput);
        // STR and ST2 = Go and NoGo
        str = NeuronFactory.createLifInhibitory("STR", groupSize, backgroundInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, backgroundInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize, backgroundInput);
        gpe = NeuronFactory.createLifInhibitory("GPE", groupSize, backgroundInput);
        st2 = NeuronFactory.createLifInhibitory("ST2", groupSize, backgroundInput);
        pfe = NeuronFactory.createLifExcitatory("PFE", groupSize, backgroundInput);
        pfi = NeuronFactory.createLifInhibitory("PFI", groupSize, backgroundInput);
        network.addNeuronGroups(thl, ctx, str, stn, gpi, gpe, st2, pfe, pfi);
        
        // Create the synapse groups and add them to the network
        SynapseGroup thlCtx = SynapseFactory.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseFactory.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStn = SynapseFactory.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, maxWeight);
        SynapseGroup strGpi = SynapseFactory.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, 0.5 * maxWeight);
        SynapseGroup stnGpi = SynapseFactory.connectNeighborhood(stn, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpiThl = SynapseFactory.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, 0.2 * maxWeight);
        SynapseGroup st2Gpe = SynapseFactory.connectNeighborhood(st2, gpe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpeStn = SynapseFactory.connectNeighborhood(gpe, stn, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpeGpi = SynapseFactory.connectNeighborhood(gpe, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup stnGpe = SynapseFactory.connectNeighborhood(stn, gpe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxSt2 = SynapseFactory.connectNeighborhood(ctx, st2, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup pfePfe = SynapseFactory.connectNeighborhood(pfe, pfe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup pfePfi = SynapseFactory.connectNeighborhood(pfe, pfi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup pfiPfe = SynapseFactory.connectNeighborhood(pfi, pfe, connectivity, wide, minWeight, maxWeight); // Sombrero connections here not wide TODO
        SynapseGroup ctxPfe = SynapseFactory.connectNeighborhood(ctx, pfe, connectivity, wide, minWeight, maxWeight);
        SynapseGroup pfeStr = SynapseFactory.connectNeighborhood(pfe, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup pfeSt2 = SynapseFactory.connectNeighborhood(pfe, st2, connectivity, narrow, minWeight, maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl, st2Gpe, gpeStn, gpeGpi, stnGpe, ctxSt2,
                pfePfe, pfePfi, pfiPfe, ctxPfe, ctxPfe, pfeStr, pfeSt2);
        
        // Set up learning for CTX-STR, CTX-ST2, CTX-PFE, PFE-CTX, PFE-STR, PFE-ST2
        // Add SNc, and the connections to and from it, as well as the lower half of Chorley+Seth
        // Add Cortex to VTA, and VTA is dopamine
        // What excites dopamine? - we would need some chorley-seth type connections - Sen to DA but what excites DA?
        
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
        
        TimeSeriesPlot.init(container);
        raster = TimeSeriesPlot.scatter();
        raster.addSeries("THL");
        raster.addSeries("CTX");
        raster.addSeries("STR");
        raster.addSeries("STN");
        raster.addSeries("GPI");
        lfpPlot = TimeSeriesPlot.line();
        lfpPlot.addSeries("LFP");
        lfpPlot.setAutoRanging(false, true);
        DoubleMatrix dx = stn.getXPosition().sub(0.5);
        dx.muli(dx);
        DoubleMatrix dy = stn.getYPosition().sub(0.5);
        dy.muli(dy);
        distanceToElectrode = MatrixFunctions.sqrt(dx.add(dy));
        
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
                    
                    // Calculate the local field potential for an electrode in the subthalamic nucleus
                    double lfp = 0;
                    for (int i = 0; i < stn.getSize(); ++i) {
                        DoubleMatrix c = stn.getExcitatoryConductance().add(stn.getInhibitoryConductance());
                        lfp = c.divi(distanceToElectrode).sum();
                    }
                    lfpPlot.bufferPoint("LFP", t, lfp);
                    
                    if (step % 25 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.addPoints();
                            raster.setLimits(t - 0.5, t, 0, 5);
                            lfpPlot.addPoints();
                            lfpPlot.setXLimits(t - 0.5, t);
                            waitForSync = false;
                        });
                        while (waitForSync) {
                            Thread.sleep(10);
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
