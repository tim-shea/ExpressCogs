package expresscogs.network;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import expresscogs.network.synapses.SynapseGroup;

public class Network {
    public static void setUpdateThreads(int numThreads) {
        if (numThreads < 2) {
            shutdownUpdater();
        } else {
            executor = Executors.newFixedThreadPool(numThreads);
        }
    }
    
    public static void shutdownUpdater() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
            executor = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static ExecutorService executor;
    
    private List<NeuronGroup> neuronGroups = new ArrayList<NeuronGroup>();
    private List<SynapseGroup> synapseGroups = new ArrayList<SynapseGroup>();
    private List<Callable<Void>> neuronGroupUpdaters = new LinkedList<Callable<Void>>();
    private List<Callable<Void>> synapseGroupUpdaters = new LinkedList<Callable<Void>>();
    private int step; 
    
    public void addNeuronGroups(NeuronGroup... groups) {
        for (NeuronGroup neurons : groups) {
            neuronGroups.add(neurons);
            neuronGroupUpdaters.add(() -> {
                neurons.update(step);
                return null;
            });
        }
    }
    
    public List<NeuronGroup> getNeuronGroups() {
        return neuronGroups;
    }
    
    public NeuronGroup getNeuronGroup(int index) {
        return neuronGroups.get(index);
    }
    
    public NeuronGroup getNeuronGroup(String name) {
        for (NeuronGroup neurons : neuronGroups) {
            if (neurons.getName().equals(name)) {
                return neurons;
            }
        }
        return null;
    }
    
    public void addSynapseGroups(SynapseGroup... groups) {
        for (SynapseGroup synapses : groups) {
            synapseGroups.add(synapses);
            synapseGroupUpdaters.add(() -> {
                synapses.update(step);
                return null;
            });
        }
    }
    
    public List<SynapseGroup> getSynapseGroups() {
        return synapseGroups;
    }
    
    public SynapseGroup getSynapseGroup(int index) {
        return synapseGroups.get(index);
    }
    
    public SynapseGroup getSynapseGroup(String name) {
        for (SynapseGroup synapses : synapseGroups) {
            if (synapses.getName().equals(name)) {
                return synapses;
            }
        }
        return null;
    }
    
    public void update(int step) {
        this.step = step;
        if (executor == null) {
            serialUpdate();
        } else {
            parallelUpdate();
        }
    }
    
    public void parallelUpdate() {
        try {
            executor.invokeAll(neuronGroupUpdaters);
            executor.invokeAll(synapseGroupUpdaters);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void serialUpdate() {
        for (NeuronGroup neurons : neuronGroups) {
            neurons.update(step);
        }
        for (SynapseGroup synapses : synapseGroups) {
            synapses.update(step);
        }
    }
}
