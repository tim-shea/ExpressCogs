package expresscogs.simulation;

import expresscogs.gui.SimulationView;
import expresscogs.network.*;
import expresscogs.network.synapses.NeighborhoodTopology;
import expresscogs.network.synapses.SynapseFactory;
import expresscogs.network.synapses.SynapseGroup;
import expresscogs.network.synapses.SynapseGroupTopology;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.PointRange;
import org.jblas.util.Random;

import expresscogs.utility.LocalFieldPotentialSensor;
import expresscogs.utility.NeuralFieldSensor;
import expresscogs.utility.SignalDetectionSensor;

/**
 * RecurrentNetwork is a simulation containing one excitatory and one
 * inhibitory population with topological recurrent connectivity.
 */
public class RecurrentNetwork extends Simulation {
    private Network network;
    private double lowBackgroundInput = 0.25e-3;
    private double highBackgroundInput = 1.25e-3;
    private int groupSize = 5000;
    private SynapseGroupTopology narrow = new NeighborhoodTopology(0.1, 0.01);
    private SynapseGroupTopology wide = new NeighborhoodTopology(0.1, 0.2);
    private double weightScale = 0.25e-4;
    private int synapseDelay = 1;
    
    // Input generators
    private AutoCorrelatedNoiseGenerator excNoise;
    private UniformNoiseGenerator inhNoise;
    
    // Neuron groups
    private NeuronGroup exc;
    private NeuronGroup inh;
    
    // Sensors for recording from neurons
    private LocalFieldPotentialSensor lfpSensor;
    private DoubleMatrix record;
    
    public RecurrentNetwork(SimulationView view) {
        super(view);
        network = new Network();
        
        // Create the neuron groups and add them to the network
        excNoise = new AutoCorrelatedNoiseGenerator(highBackgroundInput);
        inhNoise = new UniformNoiseGenerator(lowBackgroundInput);
        exc = NeuronFactory.createLifExcitatory("EXC", groupSize, excNoise);
        inh = NeuronFactory.createLifInhibitory("INH", groupSize, inhNoise);
        network.addNeuronGroups(exc, inh);
        
        // Setup the selection pathway synapse groups
        SynapseGroup excExc = SynapseFactory.connectWithDelay(exc, exc, narrow, 1 * weightScale, synapseDelay);
        SynapseGroup excInh = SynapseFactory.connectWithDelay(exc, inh, narrow, 1 * weightScale, synapseDelay);
        SynapseGroup inhExc = SynapseFactory.connectWithDelay(inh, exc, narrow, 1 * weightScale, synapseDelay);
        network.addSynapseGroups(excExc, excInh, inhExc);
        
        // Create the sensors
        lfpSensor = new LocalFieldPotentialSensor(exc);
    }
    
    @Override
    public void runInThread(int timesteps) {
        record = new DoubleMatrix(timesteps, 38);
        super.runInThread(timesteps);
    }
    
    @Override
    public void runAsync(int timesteps) {
        record = new DoubleMatrix(timesteps, 38);
        super.runAsync(timesteps);
    }
    
    @Override
    public void updateModel() {
        final double t = getTime();
        network.update(getStep());
        
        lfpSensor.update(t);
        record.put(getStep(), 0, getTime());
        record.put(getStep(), 1, excNoise.getScale());
        record.put(getStep(), 2, inhNoise.getScale());
        record.put(getStep(), 3, exc.getSpikes().sum());
        record.put(getStep(), 4, inh.getSpikes().sum());
        record.put(getStep(), 10, lfpSensor.getLfp());
    }
    
    public Network getNetwork() {
        return network;
    }
    
    public double getWeightScale() {
        return weightScale;
    }
    
    public UniformNoiseGenerator getExcNoise() {
        return excNoise;
    }
    
    public UniformNoiseGenerator getInhNoise() {
        return inhNoise;
    }
    
    public LocalFieldPotentialSensor getLfpSensor() {
        return lfpSensor;
    }
    
    public DoubleMatrix getRecord() {
        return record;
    }
}
