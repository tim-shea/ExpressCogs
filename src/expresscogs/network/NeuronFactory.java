package expresscogs.network;

public final class NeuronFactory {
    public enum NeuronModel {
        LIF,
        ADEX
    }
    
    public static NeuronGroup createLifExcitatory(String name, int size) {
        return create(NeuronModel.LIF, name, size, true, InputGenerator.createNullGenerator());
    }
    
    public static NeuronGroup createLifExcitatory(String name, int size, double noiseScale) {
        return create(NeuronModel.LIF, name, size, true, new UniformNoiseGenerator(noiseScale));
    }
    
    public static NeuronGroup createLifExcitatory(String name, int size, InputGenerator generator) {
        return create(NeuronModel.LIF, name, size, true, generator);
    }
    
    public static NeuronGroup createLifInhibitory(String name, int size) {
        return create(NeuronModel.LIF, name, size, false, InputGenerator.createNullGenerator());
    }
    
    public static NeuronGroup createLifInhibitory(String name, int size, double noiseScale) {
        return create(NeuronModel.LIF, name, size, false, new UniformNoiseGenerator(noiseScale));
    }
    
    public static NeuronGroup createLifInhibitory(String name, int size, InputGenerator generator) {
        return create(NeuronModel.LIF, name, size, false, generator);
    }
    
    public static NeuronGroup create(NeuronModel model, String name, int size, boolean excitatory, InputGenerator generator) {
        NeuronGroup neurons;
        if (model == NeuronModel.ADEX) {
            neurons = new AdExNeuronGroup(name, size, excitatory, generator);
        } else {
            neurons = new LifNeuronGroup(name, size, excitatory, generator);
        }
        generator.setNeuronGroup(neurons);
        return neurons;
    }
}
