package expresscogs.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jblas.DoubleMatrix;

import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;
import expresscogs.utility.SimplePlot;
import expresscogs.utility.SimplePlot.BufferedDataSeries;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Pair;

public class SimpleNetworkTest extends Application {
    private boolean wait;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        stage.setTitle("ExpressCogs");
        startTopologicalTest(stage);
    }
    
    private void startMotorTest(Stage stage) {
        int tSteps = 30000;
        double dt = 0.001;
        Network network = Network.createMotorNetwork(4, 300);
        DoubleMatrix t = DoubleMatrix.zeros(tSteps);
        List<Pair<Number, Number>> spikes = new LinkedList<Pair<Number, Number>>();
        int exc = 0, inh = 0;
        for (int step = 0; step < tSteps; step += 1) {
            network.update(dt);
            if (step > 0)
                t.put(step, t.get(step - 1) + dt);

            int[] excSpikes = network.getNeuronGroup(0).getSpikes().findIndices();
            exc += excSpikes.length;
            for (int i : excSpikes)
                spikes.add(new Pair<Number, Number>(t.get(step), i));

            int[] inhSpikes = network.getNeuronGroup(1).getSpikes().findIndices();
            inh += inhSpikes.length;
            for (int i : inhSpikes)
                spikes.add(new Pair<Number, Number>(t.get(step), i + 1200));
        }
        System.out.println("Exc: " + (exc / (1200 * tSteps * dt)) + " Hz");
        System.out.println("Inh: " + (inh / (300 * tSteps * dt)) + " Hz");
        SimplePlot.init(stage);
        SimplePlot.Scatter plot = new SimplePlot.Scatter();
        plot.addPoints(spikes);
        plot.setLimits(0, tSteps * dt, 0, 1500);
    }
    
    private void startTopologicalTest(Stage stage) {
        int tSteps = 100000;
        double dt = 0.001;
        Network network = Network.createTopologicalNetwork();
        
        BufferedDataSeries inSpikes = new BufferedDataSeries();
        BufferedDataSeries excSpikes = new BufferedDataSeries();
        BufferedDataSeries inhSpikes = new BufferedDataSeries();
        BufferedDataSeries outSpikes = new BufferedDataSeries();
        
        SimplePlot.init(stage);
        SimplePlot.Scatter raster = new SimplePlot.Scatter();
        raster.addSeries(inSpikes.getSeries());
        raster.addSeries(excSpikes.getSeries());
        raster.addSeries(inhSpikes.getSeries());
        raster.addSeries(outSpikes.getSeries());
        raster.setLimits(0, tSteps * dt, 0, 5);
        
        SimplePlot.HeatMap map = new SimplePlot.HeatMap(30, 25);
        DoubleMatrix firingRates = DoubleMatrix.zeros(30, 25);
        
        NeuronGroup in = network.getNeuronGroup("IN");
        NeuronGroup exc = network.getNeuronGroup("MAIN_EXC");
        NeuronGroup inh = network.getNeuronGroup("MAIN_INH");
        NeuronGroup out = network.getNeuronGroup("OUT");
        
        DoubleMatrix inSubSample = DoubleMatrix.rand(in.getSize()).lti(0.25);
        DoubleMatrix excSubSample = DoubleMatrix.rand(exc.getSize()).lti(0.1);
        DoubleMatrix inhSubSample = DoubleMatrix.rand(inh.getSize()).lti(0.1);
        DoubleMatrix outSubSample = DoubleMatrix.rand(out.getSize()).lti(0.25);
        
        wait = false;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    final double t = step * dt;
                    network.update(dt);
                    bufferSpikes(inSpikes, in.getXPosition().get(inSubSample.and(in.getSpikes())), t);
                    bufferSpikes(excSpikes, exc.getXPosition().get(excSubSample.and(exc.getSpikes())).mul(3).add(1), t);
                    bufferSpikes(inhSpikes, inh.getXPosition().get(inhSubSample.and(inh.getSpikes())).add(4), t);
                    bufferSpikes(outSpikes, out.getXPosition().get(outSubSample.and(out.getSpikes())), t);
                    firingRates.muli(0.998).addi(exc.getSpikes().reshape(30, 25));
                    if (step % 500 == 0) {
                        while (wait) {
                            Thread.sleep(10);
                        }
                        Platform.runLater(() -> {
                            inSpikes.addBuffered();
                            excSpikes.addBuffered();
                            inhSpikes.addBuffered();
                            outSpikes.addBuffered();
                            raster.setLimits(t - 5, t, 0, 5);
                            map.setValues(firingRates);
                            wait = false;
                        });
                        wait = true;
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