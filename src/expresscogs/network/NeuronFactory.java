package expresscogs.network;

public final class NeuronFactory {
    public enum NeuronModel {
        LIF,
        ADEX
    }
    
    public static NeuronGroup createLifExcitatory(String name, int size) {
        return create(NeuronModel.LIF, name, size, true, InputGenerator.NullGenerator);
    }
    
    public static NeuronGroup createLifExcitatory(String name, int size, double noiseScale) {
        return create(NeuronModel.LIF, name, size, true, new UniformNoiseGenerator(noiseScale));
    }

    public static NeuronGroup createLifExcitatory(String name, int size, InputGenerator generator) {
        return create(NeuronModel.LIF, name, size, true, generator);
    }

    public static NeuronGroup createLifInhibitory(String name, int size) {
        return create(NeuronModel.LIF, name, size, false, InputGenerator.NullGenerator);
    }

    public static NeuronGroup createLifInhibitory(String name, int size, double noiseScale) {
        return create(NeuronModel.LIF, name, size, false, new UniformNoiseGenerator(noiseScale));
    }

    public static NeuronGroup createLifInhibitory(String name, int size, InputGenerator generator) {
        return create(NeuronModel.LIF, name, size, false, generator);
    }
    
    public static NeuronGroup create(NeuronModel model, String name, int size, boolean excitatory, InputGenerator generator) {
        if (model == NeuronModel.ADEX) {
            return new AdExNeuronGroup(name, size, excitatory, generator);
        } else {
            return new LifNeuronGroup(name, size, excitatory, generator);
        }
    }
}
