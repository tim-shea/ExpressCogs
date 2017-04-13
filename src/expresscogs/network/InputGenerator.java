package expresscogs.network;

import org.jblas.DoubleMatrix;

public interface InputGenerator {
    public static InputGenerator createNullGenerator() {
        return new InputGenerator() {
            private DoubleMatrix zeros;
            
            @Override
            public void setNeuronGroup(NeuronGroup neurons) {
                zeros = DoubleMatrix.zeros(neurons.getSize());
            }
            
            @Override
            public DoubleMatrix generate() {
                return zeros;
            }
        };
    }
    
    void setNeuronGroup(NeuronGroup neurons);
    DoubleMatrix generate();
}
