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
 */
public class SignalSelectionNetwork extends Simulation {
    private Network network;
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
    
    // Sensors for recording from neurons
    private LocalFieldPotentialSensor lfpSensor;
    private DoubleMatrix spikeSample;
    private NeuralFieldSensor fieldSensor;
    private SignalDetectionSensor signalSensor;
    private DoubleMatrix record;
    
    public SignalSelectionNetwork(SimulationView view) {
        super(view);
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
        SynapseGroup thlCtx = SynapseFactory.connectWithDelay(thl, ctx, narrow, 1 * weightScale);
        SynapseGroup ctxStr = SynapseFactory.connectWithDelay(ctx, str, narrow, 0.5 * weightScale);
        SynapseGroup ctxStn = SynapseFactory.connectWithDelay(ctx, stn, wide, 1 * weightScale);
        SynapseGroup strGpi = SynapseFactory.connectWithDelay(str, gpi, narrow, 0.5 * weightScale);
        SynapseGroup stnGpi = SynapseFactory.connectWithDelay(stn, gpi, wide, 1 * weightScale);
        SynapseGroup gpiThl = SynapseFactory.connectWithDelay(gpi, thl, narrow, 1 * weightScale);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
        
        // Setup the control pathway synapse groups
        SynapseGroup ctxSt2 = SynapseFactory.connectWithDelay(ctx, st2, narrow, 0.5 * weightScale);
        SynapseGroup st2Gpe = SynapseFactory.connectWithDelay(st2, gpe, narrow, 0.5 * weightScale);
        SynapseGroup stnGpe = SynapseFactory.connectWithDelay(stn, gpe, wide, 1 * weightScale);
        SynapseGroup gpeStn = SynapseFactory.connectWithDelay(gpe, stn, narrow, 0.5 * weightScale);
        SynapseGroup gpeGpi = SynapseFactory.connectWithDelay(gpe, gpi, narrow, 0.5 * weightScale);
        network.addSynapseGroups(ctxSt2, st2Gpe, stnGpe, gpeStn, gpeGpi);
        
        // Create the sensors
        lfpSensor = new LocalFieldPotentialSensor(stn);
        spikeSample = DoubleMatrix.zeros(stn.getSize());
        while (spikeSample.sum() < 25) {
            spikeSample.put(Random.nextInt(stn.getSize()), 1);
        }
        fieldSensor = new NeuralFieldSensor(ctx);
        signalSensor = new SignalDetectionSensor(ctx, thlInput);
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
        fieldSensor.update(t);
        signalSensor.update(t);
        record.put(getStep(), 0, getTime());
        record.put(getStep(), 1, thlInput.getSignalToNoiseRatio());
        record.put(getStep(), 2, thlInput.getPosition());
        record.put(getStep(), 3, thl.getSpikes().sum());
        record.put(getStep(), 4, ctx.getSpikes().sum());
        record.put(getStep(), 5, str.getSpikes().sum());
        record.put(getStep(), 6, st2.getSpikes().sum());
        record.put(getStep(), 7, stn.getSpikes().sum());
        record.put(getStep(), 8, gpi.getSpikes().sum());
        record.put(getStep(), 9, gpe.getSpikes().sum());
        record.put(getStep(), 10, lfpSensor.getLfp());
        record.put(getStep(), 11, signalSensor.getSignalStrength());
        record.put(getStep(), 12, signalSensor.getNoiseStrength());
        record.put(new PointRange(getStep()), new IntervalRange(13, 38), stn.getSpikes().get(spikeSample).transpose());
    }
    
    public Network getNetwork() {
        return network;
    }
    
    public ContinuousStimulusGenerator getInputGenerator() {
        return thlInput;
    }
    
    public double getWeightScale() {
        return weightScale;
    }
    
    public LocalFieldPotentialSensor getLfpSensor() {
        return lfpSensor;
    }
    
    public NeuralFieldSensor getFieldSensor() {
        return fieldSensor;
    }
    
    public SignalDetectionSensor getSignalSensor() {
        return signalSensor;
    }
    
    public DoubleMatrix getRecord() {
        return record;
    }
}
