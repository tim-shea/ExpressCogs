package expresscogs.test;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.jblas.DoubleMatrix;
import expresscogs.network.Network;
import expresscogs.network.NeuronGroup;
import expresscogs.utility.SimplePlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Pair;

public class SimpleNetworkTest extends Application {
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
        List<Pair<Number,Number>> spikes = new LinkedList<Pair<Number,Number>>();
        int exc = 0, inh = 0;
        for (int step = 0; step < tSteps; step += 1) {
            network.update(dt);
            if (step > 0)
                t.put(step, t.get(step - 1) + dt);
            
            int[] excSpikes = network.getNeuronGroup(0).getSpikes().findIndices();
            exc += excSpikes.length;
            for (int i : excSpikes)
                spikes.add(new Pair<Number,Number>(t.get(step), i));
            
            int[] inhSpikes = network.getNeuronGroup(1).getSpikes().findIndices();
            inh += inhSpikes.length;
            for (int i : inhSpikes)
                spikes.add(new Pair<Number,Number>(t.get(step), i + 1200));
        }
        System.out.println("Exc: " + (exc / (1200 * tSteps * dt)) + " Hz");
        System.out.println("Inh: " + (inh / (300 * tSteps * dt)) + " Hz");
        SimplePlot.Scatter plot = new SimplePlot.Scatter(stage);
        plot.addPoints(spikes);
        plot.setLimits(0, tSteps * dt, 0, 1500);
    }
    
    private void startTopologicalTest(Stage stage) {
        int tSteps = 10000;
        double dt = 0.001;
        int numberNeurons = 2000;
        Network network = Network.createTopologicalNetwork(numberNeurons);
        DoubleMatrix t = DoubleMatrix.zeros(tSteps);
        
        Series<Number,Number> inSpikes = new Series<Number,Number>();
        Series<Number,Number> excSpikes = new Series<Number,Number>();
        Series<Number,Number> inhSpikes = new Series<Number,Number>();
        Series<Number,Number> outSpikes = new Series<Number,Number>();
        
        SimplePlot.Scatter plot = new SimplePlot.Scatter(stage);
        plot.addSeries(inSpikes);
        plot.addSeries(excSpikes);
        plot.addSeries(inhSpikes);
        plot.addSeries(outSpikes);
        plot.setLimits(0, tSteps * dt, 0, 5);
        
        //SimplePlot.HeatMap map = new SimplePlot.HeatMap(50, 30, stage);
        DoubleMatrix firingRates = DoubleMatrix.zeros(50, 30);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int step = 0; step < tSteps; step += 1) {
                    network.update(dt);
                    if (step > 0) {
                        t.put(step, t.get(step - 1) + dt);
                    }
                    
                    NeuronGroup in = network.getNeuronGroup(0);
                    plotSpikes(inSpikes, in.getXPosition().get(in.getSpikes()), t.get(step));
                    //plotSpikes(inSpikes, DoubleMatrix.linspace(0, 1, in.getSize()).get(in.getSpikes()), t.get(step));
                    
                    NeuronGroup exc = network.getNeuronGroup(1);
                    firingRates.muli(0.999).addi(exc.getSpikes().reshape(50, 30));
                    plotSpikes(excSpikes, exc.getXPosition().get(exc.getSpikes()).mul(3).add(1), t.get(step));
                    //plotSpikes(excSpikes, DoubleMatrix.linspace(1, 4, exc.getSize()).get(exc.getSpikes()), t.get(step));
                    
                    NeuronGroup inh = network.getNeuronGroup(2);
                    plotSpikes(inhSpikes, inh.getXPosition().get(inh.getSpikes()).add(4), t.get(step));
                    //plotSpikes(inhSpikes, DoubleMatrix.linspace(4, 5, inh.getSize()).get(inh.getSpikes()), t.get(step));
                    
                    NeuronGroup out = network.getNeuronGroup(3);
                    plotSpikes(outSpikes, out.getXPosition().get(out.getSpikes()), t.get(step));
                    //plotSpikes(outSpikes, DoubleMatrix.linspace(0, 1, out.getSize()).get(out.getSpikes()), t.get(step));
                    
                    //final double tPlot = t.get(step);
                    //Platform.runLater(() -> {
                    //    plot.setLimits(tPlot - 1, tPlot, 0, 5);
                    //});
                    
                    /*
                    if (step % 100 == 0) {
                        Platform.runLater(() -> {
                            map.setValues(firingRates);
                        });
                    }
                    */
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    private void plotSpikes(final Series<Number,Number> series, final DoubleMatrix x, final double t) {
        Platform.runLater(() -> {
            for (int i = 0; i < x.length; ++i) {
                series.getData().add(new Data<Number,Number>(t, x.get(i)));
            }
            /*
            series.getData().removeIf(new Predicate<Data<Number, Number>>() {
                @Override
                public boolean test(Data<Number, Number> point) {
                    return (point.getXValue().doubleValue() < t - 1);
                }
            });
            */
        });
    }
}