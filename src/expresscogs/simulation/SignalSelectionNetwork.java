package expresscogs.simulation;

import org.jblas.DoubleMatrix;
import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;
import expresscogs.network.SynapseGroup;
import expresscogs.utility.SimplePlot;
import expresscogs.utility.SimplePlot.BufferedDataSeries;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class SignalSelectionNetwork extends Application {
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
        createNetwork();
        startVisualization(stage);
    }
    
    private void createNetwork() {
        network = new Network();
        final DoubleMatrix i = DoubleMatrix.rand(100).muli(0.9e-9);
        NeuronGroup thl = NeuronGroup.createExcitatory("THL", 100, (NeuronGroup neurons) -> {
            DoubleMatrix n = DoubleMatrix.rand(100).muli(0.1e-9);
            return i.add(n);
        });
        NeuronGroup ctx = NeuronGroup.createExcitatory("CTX", 100, 1.2e-9);
        NeuronGroup str = NeuronGroup.createInhibitory("STR", 100, 1.2e-9);
        NeuronGroup stn = NeuronGroup.createExcitatory("STN", 100, 1.2e-9);
        NeuronGroup gpi = NeuronGroup.createInhibitory("GPI", 100, 1.2e-9);
        network.addNeuronGroups(thl, ctx, str, stn, gpi);
        SynapseGroup thlCtx = SynapseGroup.connectNeighborhood(thl, ctx, 0.25, 0.02, 0.1e-9, 1e-9);
        SynapseGroup ctxStr = SynapseGroup.connectNeighborhood(ctx, str, 0.25, 0.02, 0.1e-9, 1e-9);
        SynapseGroup ctxStn = SynapseGroup.connectUniformRandom(ctx, stn, 0.2, 0.1e-9, 1e-9);
        SynapseGroup strGpi = SynapseGroup.connectNeighborhood(str, gpi, 0.25, 0.02, 0.1e-9, 1e-9);
        SynapseGroup stnGpi = SynapseGroup.connectNeighborhood(stn, gpi, 0.25, 0.02, 0.1e-9, 1e-9);
        SynapseGroup gpiThl = SynapseGroup.connectNeighborhood(gpi, thl, 0.25, 0.02, 0.1e-9, 1e-9);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
    }
    
    private void startVisualization(Stage stage) {
        SimplePlot.init(stage);
        SimplePlot.Scatter raster = new SimplePlot.Scatter();
        raster.setLimits(0, 5, 0, 5);
        
        BufferedDataSeries thlSpikes = new BufferedDataSeries("THL");
        raster.addSeries(thlSpikes.getSeries());
        NeuronGroup thl = network.getNeuronGroup("THL");
        
        BufferedDataSeries ctxSpikes = new BufferedDataSeries("CTX");
        raster.addSeries(ctxSpikes.getSeries());
        NeuronGroup ctx = network.getNeuronGroup("CTX");
        
        BufferedDataSeries strSpikes = new BufferedDataSeries("STR");
        raster.addSeries(strSpikes.getSeries());
        NeuronGroup str = network.getNeuronGroup("STR");
        
        BufferedDataSeries stnSpikes = new BufferedDataSeries("STN");
        raster.addSeries(stnSpikes.getSeries());
        NeuronGroup stn = network.getNeuronGroup("STN");
        
        BufferedDataSeries gpiSpikes = new BufferedDataSeries("GPI");
        raster.addSeries(gpiSpikes.getSeries());
        NeuronGroup gpi = network.getNeuronGroup("GPI");
        
        waitForSync = false;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    network.update(dt);
                    
                    bufferSpikes(thlSpikes, thl.getXPosition().get(thl.getSpikes()), t);
                    bufferSpikes(ctxSpikes, ctx.getXPosition().get(ctx.getSpikes()).add(1), t);
                    bufferSpikes(strSpikes, str.getXPosition().get(str.getSpikes()).add(2), t);
                    bufferSpikes(stnSpikes, stn.getXPosition().get(stn.getSpikes()).add(3), t);
                    bufferSpikes(gpiSpikes, gpi.getXPosition().get(gpi.getSpikes()).add(4), t);
                    
                    if (step % 50 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            thlSpikes.addBuffered();
                            ctxSpikes.addBuffered();
                            strSpikes.addBuffered();
                            stnSpikes.addBuffered();
                            gpiSpikes.addBuffered();
                            raster.setLimits(t - 5, t, 0, 5);
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
        Thread thread = new Thread(task);
        stage.setOnCloseRequest(evt -> {
            thread.interrupt();
        });
        thread.start();
    }
    
    private void bufferSpikes(BufferedDataSeries series, DoubleMatrix x, double t) {
        for (int i = 0; i < x.length; ++i) {
            series.bufferData(t, x.get(i));
        }
    }
}
