package expresscogs.simulation;

import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;
import expresscogs.network.SynapseFactory;
import expresscogs.network.NeuronFactory;
import expresscogs.network.AdExNeuronGroup;
import expresscogs.network.SynapseGroup;
import expresscogs.utility.TimeSeriesPlot;
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
    
    // Charts for visualization
    private TimeSeriesPlot potentials;
    private TimeSeriesPlot conductances;
    
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
        neurons = NeuronFactory.createLifExcitatory("N", groupSize, backgroundInput);
        network.addNeuronGroups(neurons);
        
        // Create the synapse groups and add them to the network
        double connectivity = 1.0;
        double minWeight = 0.5e-9;
        double maxWeight = 1e-9;
        SynapseGroup synapses = SynapseFactory.connectUniformRandom(neurons, neurons,
                connectivity, minWeight, maxWeight);
        network.addSynapseGroups(synapses);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        TimeSeriesPlot.init(stage);
        potentials = TimeSeriesPlot.line();
        conductances = TimeSeriesPlot.line();
        
        potentials.addSeries("v0");
        potentials.addSeries("v1");
        conductances.addSeries("i0");
        conductances.addSeries("i1");
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
                    
                    potentials.bufferPoint("v0", t, neurons.getPotentials().get(0));
                    potentials.bufferPoint("v1", t, neurons.getPotentials().get(1));
                    conductances.bufferPoint("i0", t, neurons.getExcitatoryConductance().get(0) * 1e8);
                    conductances.bufferPoint("i1", t, neurons.getExcitatoryConductance().get(1) * 1e8);
                    
                    if (step % 1 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            potentials.addPoints();
                            potentials.setLimits(t - 0.1, t, -0.1, 0.05);
                            conductances.addPoints();
                            conductances.setLimits(t - 0.1, t, 0, 1);
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
        stage.setOnCloseRequest(evt -> {
            thread.interrupt();
        });
        thread.start();
    }
}
