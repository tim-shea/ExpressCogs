package expresscogs.test;

import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;
import expresscogs.network.Network;
import expresscogs.utility.SimplePlot;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Pair;

public class SimpleNetworkTest extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        int tSteps = 30000;
        double dt = 0.001;
        Network network = Network.createTopologicalNetwork();
        DoubleMatrix t = DoubleMatrix.zeros(tSteps);
        DoubleMatrix probe = DoubleMatrix.zeros(tSteps, 5);
        List<Pair<Number,Number>> spikes = new LinkedList<Pair<Number,Number>>();
        for (int step = 0; step < tSteps; step += 1) {
            network.update(dt);
            if (step > 0)
                t.put(step, t.get(step - 1) + dt);
            probe.putRow(step, network.getNeuronGroup(0).getPotentials().get(new int[]{ 0, 1, 2, 3, 4}));
            //for (int i : network.getNeuronGroup(0).getSpikes().findIndices())
            //    spikes.add(new Pair<Number,Number>(t.get(step), i));
            for (int i : network.getNeuronGroup(1).getSpikes().findIndices())
                spikes.add(new Pair<Number,Number>(t.get(step), i));
            for (int i : network.getNeuronGroup(2).getSpikes().findIndices())
                spikes.add(new Pair<Number,Number>(t.get(step), i + 160));
            //for (int i : network.getNeuronGroup(3).getSpikes().findIndices())
            //    spikes.add(new Pair<Number,Number>(t.get(step), i + 200));
        }
        //new SimplePlot.Line(stage, t, probe);
        new SimplePlot.Scatter(stage, spikes);
    }
}