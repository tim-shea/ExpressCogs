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
        
        // Create an input generator which sends activity to 3 random neurons every
        // 1000 timesteps.
        final DoubleMatrix i = DoubleMatrix.ones(100).muli(0.65e-9);
        final DoubleMatrix stim = DoubleMatrix.zeros(100);
        InputGenerator thlInput = new InputGenerator() {
            private int step = 0;
            @Override
            public DoubleMatrix generate(NeuronGroup neurons) {
                DoubleMatrix n = DoubleMatrix.rand(100).muli(0.05e-9);
                if (++step % 1000 == 0) {
                    stim.fill(0);
                    int[] indices = DoubleMatrix.rand(3).muli(100).toIntArray();
                    stim.put(indices, 2e-9);
                }
                return i.add(n).add(stim);
            }
        };
        
        // Create the neuron groups and add them to the network
        thl = NeuronGroup.createExcitatory("THL", 100, thlInput);
        ctx = NeuronGroup.createExcitatory("CTX", 100, 1.1e-9);
        str = NeuronGroup.createInhibitory("STR", 100, 1.1e-9);
        stn = NeuronGroup.createExcitatory("STN", 100, 1.1e-9);
        gpi = NeuronGroup.createInhibitory("GPI", 100, 1.1e-9);
        network.addNeuronGroups(thl, ctx, str, stn, gpi);
        
        // Create the synapse groups and add them to the network
        SynapseGroup thlCtx = SynapseGroup.connectNeighborhood(thl, ctx, 0.2, 0.02, 0.1e-9, 1e-9);
        SynapseGroup ctxStr = SynapseGroup.connectNeighborhood(ctx, str, 0.2, 0.02, 0.1e-9, 1e-9);
        SynapseGroup ctxStn = SynapseGroup.connectNeighborhood(ctx, stn, 0.2, 0.5, 0.1e-9, 1e-9);
        SynapseGroup strGpi = SynapseGroup.connectNeighborhood(str, gpi, 0.2, 0.02, 0.1e-9, 1e-9);
        SynapseGroup stnGpi = SynapseGroup.connectNeighborhood(stn, gpi, 0.2, 0.02, 0.1e-9, 1e-9);
        SynapseGroup gpiThl = SynapseGroup.connectNeighborhood(gpi, thl, 0.2, 0.02, 0.1e-9, 1e-9);
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
            
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    network.update(dt);
                    
                    bufferSpikes(thlSpikes, thl.getXPosition().get(thl.getSpikes()), t);
                    bufferSpikes(ctxSpikes, ctx.getXPosition().get(ctx.getSpikes()).add(1), t);
                    bufferSpikes(strSpikes, str.getXPosition().get(str.getSpikes()).add(2), t);
                    bufferSpikes(stnSpikes, stn.getXPosition().get(stn.getSpikes()).add(3), t);
                    bufferSpikes(gpiSpikes, gpi.getXPosition().get(gpi.getSpikes()).add(4), t);
                    ctxSpikeCount += ctx.getSpikes().sum();
                    
                    if (step % 50 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            thlSpikes.addBuffered();
                            ctxSpikes.addBuffered();
                            strSpikes.addBuffered();
                            stnSpikes.addBuffered();
                            gpiSpikes.addBuffered();
                            raster.setLimits(t - 5, t, 0, 5);
                            ctxFiringRate.bufferData(t, ctxSpikeCount);
                            ctxFiringRate.addBuffered();
                            ctxSpikeCount = 0;
                            firingRates.setLimits(t - 5, t, 0, 50);
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
