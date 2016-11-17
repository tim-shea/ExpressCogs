package expresscogs.simulation;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.InputGenerator;
import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;
import expresscogs.network.SynapseGroup;
import expresscogs.utility.SimplePlot;
import expresscogs.utility.SimplePlot.BufferedDataSeries;
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
    private BufferedDataSeries thlSpikes;
    private BufferedDataSeries ctxSpikes;
    private BufferedDataSeries strSpikes;
    private BufferedDataSeries stnSpikes;
    private BufferedDataSeries gpiSpikes;
    private BufferedDataSeries ctxFiringRate;
    
    // Charts for visualization
    private SimplePlot.Scatter raster;
    private SimplePlot.Line firingRates;
    
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
        double backgroundInput = 1.25e-9;
        int groupSize = 100;
        
        // Create an input generator which sends activity to 3 random neurons every
        // 1000 timesteps
        InputGenerator thlInput = new InputGenerator() {
            DoubleMatrix i = DoubleMatrix.zeros(groupSize);
            double randomInput = 1.5e-9;
            DoubleMatrix stim = DoubleMatrix.zeros(groupSize);
            int stimDuration = 1000;
            int stimInterval = 1000;
            int stimNumber = 3;
            double stimStrength = 2.5e-9;
            int step = 0;
            
            @Override
            public DoubleMatrix generate(NeuronGroup neurons) {
                i = DoubleMatrix.rand(neurons.getSize()).muli(randomInput);
                if (++step % (stimDuration + stimInterval) == 0) {
                    int[] indices = DoubleMatrix.rand(stimNumber).muli(neurons.getSize() - 1).toIntArray();
                    stim.put(indices, stimStrength);
                } else if (step % (stimDuration + stimInterval) == stimDuration) {
                    stim.fill(0);
                }
                return i.add(stim);
            }
        };
        
        thl = NeuronGroup.createExcitatory("THL", groupSize, thlInput);
        ctx = NeuronGroup.createExcitatory("CTX", groupSize, backgroundInput);
        str = NeuronGroup.createInhibitory("STR", groupSize, backgroundInput);
        stn = NeuronGroup.createExcitatory("STN", groupSize, backgroundInput);
        gpi = NeuronGroup.createInhibitory("GPI", groupSize, backgroundInput);
        network.addNeuronGroups(thl, ctx, str, stn, gpi);
        
        // Create the synapse groups and add them to the network
        double connectivity = 0.2;
        double narrow = 0.02;
        double wide = 0.25;
        double minWeight = 0.1e-9;
        double maxWeight = 1e-9;
        SynapseGroup thlCtx = SynapseGroup.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseGroup.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStn = SynapseGroup.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, maxWeight);
        SynapseGroup strGpi = SynapseGroup.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup stnGpi = SynapseGroup.connectNeighborhood(stn, gpi, connectivity, wide, minWeight, maxWeight);
        SynapseGroup gpiThl = SynapseGroup.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        SimplePlot.init(stage);
        raster = new SimplePlot.Scatter();
        
        thlSpikes = new BufferedDataSeries("THL");
        raster.addSeries(thlSpikes.getSeries());
        
        ctxSpikes = new BufferedDataSeries("CTX");
        raster.addSeries(ctxSpikes.getSeries());
        
        strSpikes = new BufferedDataSeries("STR");
        raster.addSeries(strSpikes.getSeries());
        
        stnSpikes = new BufferedDataSeries("STN");
        stnSpikes.setMaxLength(2500);
        raster.addSeries(stnSpikes.getSeries());
        
        gpiSpikes = new BufferedDataSeries("GPI");
        raster.addSeries(gpiSpikes.getSeries());
        
        firingRates = new SimplePlot.Line();
        
        ctxFiringRate = new BufferedDataSeries("CTX Firing Rate");
        ctxFiringRate.setMaxLength(100);
        firingRates.addSeries(ctxFiringRate.getSeries());
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        Task<Void> task = new Task<Void>() {
            int ctxSpikeCount = 0;
            double dt = 0.001;
            
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    try {
                        network.update(step);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    bufferSpikes(thlSpikes, thl.getXPosition().get(thl.getSpikes()), t);
                    bufferSpikes(ctxSpikes, ctx.getXPosition().get(ctx.getSpikes()).add(1), t);
                    bufferSpikes(strSpikes, str.getXPosition().get(str.getSpikes()).add(2), t);
                    bufferSpikes(stnSpikes, stn.getXPosition().get(stn.getSpikes()).add(3), t);
                    bufferSpikes(gpiSpikes, gpi.getXPosition().get(gpi.getSpikes()).add(4), t);
                    ctxSpikeCount += stn.getSpikes().sum();
                    
                    if (step % 20 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            thlSpikes.addBuffered();
                            ctxSpikes.addBuffered();
                            strSpikes.addBuffered();
                            stnSpikes.addBuffered();
                            gpiSpikes.addBuffered();
                            raster.setLimits(t - 2, t, 0, 5);
                            ctxFiringRate.bufferData(t, ctxSpikeCount);
                            ctxFiringRate.addBuffered();
                            ctxSpikeCount = 0;
                            firingRates.setLimits(t - 2, t, 0, 50);
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
    
    // Buffer a set of spikes in the spike raster for time t
    private void bufferSpikes(BufferedDataSeries series, DoubleMatrix x, double t) {
        for (int i = 0; i < x.length; ++i) {
            series.bufferData(t, x.get(i));
        }
    }
}
