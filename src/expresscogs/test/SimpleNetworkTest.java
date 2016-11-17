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
            network.update(step);
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
        
    }
}