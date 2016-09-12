package expresscogs.network;

import org.jblas.DoubleMatrix;

interface InputGenerator {
    public static InputGenerator NullGenerator = new InputGenerator() {
        public DoubleMatrix generate(NeuronGroup neurons) {
            return DoubleMatrix.zeros(neurons.getSize());
        }
    };
    
    DoubleMatrix generate(NeuronGroup neurons);
}
