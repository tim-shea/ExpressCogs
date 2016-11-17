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

public class SimpleNetwork extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    // Network manages the synapse and neuron groups
    private Network network;
    // Flag for synchronization
    private boolean waitForSync;
    // Total number of simulation steps
    private int tSteps = 10000;
    
    // Neuron groups
    private NeuronGroup neurons;
    
    // Buffered data for visualization
    private BufferedDataSeries v0;
    private BufferedDataSeries v1;
    private BufferedDataSeries i0;
    private BufferedDataSeries i1;
    
    // Charts for visualization
    private SimplePlot.Line potentials;
    private SimplePlot.Line conductances;
    
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
        
        // Create the neurons and add them to the network
        double backgroundInput = 1.3e-9;
        int groupSize = 2;
        neurons = NeuronGroup.createExcitatory("N", groupSize, backgroundInput);
        network.addNeuronGroups(neurons);
        
        // Create the synapse groups and add them to the network
        double connectivity = 1.0;
        double minWeight = 0.5e-9;
        double maxWeight = 1e-9;
        SynapseGroup synapses = SynapseGroup.connectUniformRandom(neurons, neurons,
                connectivity, minWeight, maxWeight);
        network.addSynapseGroups(synapses);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        SimplePlot.init(stage);
        potentials = new SimplePlot.Line();
        conductances = new SimplePlot.Line();
        
        v0 = new BufferedDataSeries("v0");
        potentials.addSeries(v0.getSeries());
        v1 = new BufferedDataSeries("v1");
        potentials.addSeries(v1.getSeries());
        i0 = new BufferedDataSeries("i0");
        conductances.addSeries(i0.getSeries());
        i1 = new BufferedDataSeries("i1");
        conductances.addSeries(i1.getSeries());
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        double dt = 0.001;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    network.update(step);
                    
                    v0.bufferData(t, neurons.getPotentials().get(0));
                    v1.bufferData(t, neurons.getPotentials().get(1));
                    i0.bufferData(t, neurons.getExcitatoryConductance().get(0) * 1e8);
                    i1.bufferData(t, neurons.getExcitatoryConductance().get(1) * 1e8);
                    
                    if (step % 1 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            v0.addBuffered();
                            v1.addBuffered();
                            i0.addBuffered();
                            i1.addBuffered();
                            potentials.setLimits(t - 0.1, t, -0.1, 0.05);
                            conductances.setLimits(t - 0.1, t, 0, 1);
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
        stage.setOnCloseRequest(evt -> {
            thread.interrupt();
        });
        thread.start();
    }
}
