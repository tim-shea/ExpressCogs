package expresscogs.simulation;

import org.jblas.DoubleMatrix;

import expresscogs.network.NeuronGroup;
import expresscogs.network.SynapseFactory;
import expresscogs.network.NeuronFactory;
import expresscogs.network.InputGenerator;
import expresscogs.network.LifNeuronGroup;
import expresscogs.network.Network;
import expresscogs.network.SynapseGroup;
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
    // Total number of simulation steps (duration = dt * tSteps)
    private int tSteps = 100000;
    // Duration of simulation step in seconds
    private double dt = 0.001;
    
    // Neuron groups
    private NeuronGroup thl;
    private NeuronGroup ctx;
    private NeuronGroup str;
    private NeuronGroup stn;
    private NeuronGroup gpi;
    
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
        double backgroundInput = 0.4e-3;
        int groupSize = 500;
        InputGenerator thlInput = new StimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, backgroundInput);
        str = NeuronFactory.createLifInhibitory("STR", groupSize, backgroundInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, backgroundInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize, backgroundInput);
        network.addNeuronGroups(thl, ctx, str, stn, gpi);
        
        // Create the synapse groups and add them to the network
        double connectivity = 0.1;
        double narrow = 0.1;
        double wide = 0.5;
        double minWeight = 0;
        double maxWeight = 1e-4;
        SynapseGroup thlCtx = SynapseFactory.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseFactory.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStn = SynapseFactory.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, maxWeight);
        SynapseGroup strGpi = SynapseFactory.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, 0.5 * maxWeight);
        SynapseGroup stnGpi = SynapseFactory.connectNeighborhood(stn, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup gpiThl = SynapseFactory.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, 0.2 * maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        TimeSeriesPlot.init(stage);
        raster = TimeSeriesPlot.scatter();
        raster.addSeries("THL");
        raster.addSeries("CTX");
        raster.addSeries("STR");
        raster.addSeries("STN");
        raster.addSeries("GPI");
        heatmap = new HeatMap(500, 5);
        spikeCounts = DoubleMatrix.zeros(500, 5);
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        Task<Void> task = new Task<Void>() {
            DoubleMatrix sample = DoubleMatrix.rand(500).lti(0.2);
            
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
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
                    
                    if (step % 10 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.addPoints();
                            raster.setLimits(t - 0.5, t, 0, 5);
                            heatmap.setValues(spikeCounts);
                            waitForSync = false;
                        });
                        while (waitForSync) {
                            Thread.sleep(50);
                        }
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        stage.setOnCloseRequest(evt -> {
            thread.interrupt();
        });
        thread.start();
    }
}
