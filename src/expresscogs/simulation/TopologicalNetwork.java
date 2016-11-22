package expresscogs.simulation;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import expresscogs.network.NeuronGroup;
import expresscogs.network.InputGenerator;
import expresscogs.network.Network;
import expresscogs.network.AdExNeuronGroup;
import expresscogs.network.SynapseGroup;
import expresscogs.utility.BufferedDataSeries;
import expresscogs.utility.HeatMap;
import expresscogs.utility.TimeSeriesPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class TopologicalNetwork extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    private Network network;
    private boolean waitForSync;
    private int tSteps = 100000;
    private double dt = 0.001;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        createTopologicalNetwork();
        startNetworkVisualization(stage);
    }
    
    private void createTopologicalNetwork() {
        network = new Network();
        /*
        new TopologicalLayerFactory().createRecurrent(network);
        NeuronGroup input = AdExNeuronGroup.createExcitatory("IN", 200, new InputGenerator() {
            int step = 0;
            public DoubleMatrix generate(NeuronGroup neurons) {
                DoubleMatrix x = neurons.getXPosition();
                DoubleMatrix i = x.sub(0.5 + 0.3 * Math.sin(step / 600.0));
                MatrixFunctions.absi(i).subi(0.2).negi().muli(6e-9);
                i.put(i.lt(0), 0);
                step++;
                if (step % 25000 > 20000)
                    i = DoubleMatrix.zeros(neurons.getSize());
                //DoubleMatrix i = DoubleMatrix.rand(neurons.getSize()).muli(1.22e-9);
                return i;
            }
        });
        NeuronGroup output = AdExNeuronGroup.createExcitatory("OUT", 200, 1.2e-9);
        network.addNeuronGroups(input, output);
        double conn = 0.1, nbh = 0.02, minW = 1e-8, maxW = 4e-8;
        SynapseGroup inputToExcitatory = SynapseGroup.connectNeighborhood(input, network.getNeuronGroup("EXC"), conn, 4 * nbh, minW, maxW);
        // SynapseGroup inputToExcitatory = SynapseGroup.connectUniformRandom(input, excitatory, 0.1, maxW);
        SynapseGroup excitatoryToOutput = SynapseGroup.connectNeighborhood(network.getNeuronGroup("EXC"), output, conn, nbh / 2, minW, maxW);
        network.addSynapseGroups(inputToExcitatory, excitatoryToOutput);
        */
    }
    
    private void startNetworkVisualization(Stage stage) {
        TimeSeriesPlot.init(stage);
        TimeSeriesPlot raster = TimeSeriesPlot.scatter();
        raster.addSeries("IN");
        raster.addSeries("EXC");
        raster.addSeries("INH");
        raster.addSeries("OUT");
        
        HeatMap map = new HeatMap(30, 25);
        DoubleMatrix firingRates = DoubleMatrix.zeros(30, 25);
        
        NeuronGroup in = network.getNeuronGroup("IN");
        NeuronGroup exc = network.getNeuronGroup("EXC");
        NeuronGroup inh = network.getNeuronGroup("INH");
        NeuronGroup out = network.getNeuronGroup("OUT");
        
        DoubleMatrix inSubSample = DoubleMatrix.rand(in.getSize()).lti(0.25);
        DoubleMatrix excSubSample = DoubleMatrix.rand(exc.getSize()).lti(0.1);
        DoubleMatrix inhSubSample = DoubleMatrix.rand(inh.getSize()).lti(0.1);
        DoubleMatrix outSubSample = DoubleMatrix.rand(out.getSize()).lti(0.25);
        
        waitForSync = false;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    network.update(step);
                    raster.bufferPoints("IN", t, in.getXPosition().get(inSubSample.and(in.getSpikes())).data);
                    raster.bufferPoints("EXC", t, exc.getXPosition().get(excSubSample.and(exc.getSpikes())).mul(3).add(1).data);
                    raster.bufferPoints("INH", t, inh.getXPosition().get(inhSubSample.and(inh.getSpikes())).add(4).data);
                    raster.bufferPoints("OUT", t, out.getXPosition().get(outSubSample.and(out.getSpikes())).add(5).data);
                    firingRates.muli(0.998).addi(exc.getSpikes().reshape(30, 25));
                    if (step % 500 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.addPoints();
                            raster.setLimits(t - 5, t, 0, 6);
                            map.setValues(firingRates);
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
        new Thread(task).start();
    }
    
    private void bufferSpikes(BufferedDataSeries series, DoubleMatrix x, double t) {
        for (int i = 0; i < x.length; ++i) {
            series.bufferData(t, x.get(i));
        }
    }
}
