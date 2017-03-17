package expresscogs.simulation;

import expresscogs.network.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.PointRange;

import expresscogs.utility.BandPassFilter;
import expresscogs.utility.HeatMap;
import expresscogs.utility.SpikeRasterPlot;
import expresscogs.utility.TimeSeriesPlot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * SignalSelectionNetwork is a simulation which instantiates a topological
 * spiking neural network translation of the Gurney, Prescott, & Redgrave
 * Basal Ganglia (GPR-BG)(2001).
 * 
 * GPR-BG describes the intrinsic function of the basal ganglia as signal
 * selection and control pathways which are implemented in terms of
 * inhibitory off-center, on-surround circuits.
 * 
 * This translation of the model uses topologically-constrained connectivity
 * for the majority of the synaptic pathways in the model.
 * 
 * @author Tim
 */
public class SignalSelectionNetwork extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    // Network manages the synapse and neuron groups
    private Network network;
    // Flag for synchronization
    private boolean waitForSync;
    // Flag for simulation state
    private boolean pauseSimulation;

    /* Simulation parameters -----------------------------------------------------------------------------------------*/

    // Total number of simulation steps (duration = dt * tSteps)
    private int tSteps = 100000;
    // Duration of simulation step in seconds
    private double dt = 0.001;
    private double lowBackgroundInput = 0.2e-3;
    private double highBackgroundInput = 0.4e-3;
    private int groupSize = 500;
    private double connectivity = 0.1;
    private double narrow = 0.05;
    private double wide = 0.5;
    private double minWeight = 0;
    private double maxWeight = 1e-4;
    private StimulusGenerator thlInput;

    // Neuron groups
    private NeuronGroup thl;
    private NeuronGroup ctx;
    private NeuronGroup str;
    private NeuronGroup stn;
    private NeuronGroup gpi;
    private NeuronGroup gpe;
    private NeuronGroup st2;
    
    // Buffered data for visualization
    private DoubleMatrix spikeCounts;
    
    // Charts for visualization
    private SpikeRasterPlot raster;
    private TimeSeriesPlot lfpPlot;
    private DoubleMatrix distanceToElectrode;
    private DoubleMatrix record;
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("ExpressCogs");
        createNetwork();
        createVisualization(stage);
        runSimulation(stage);
    }
    
    /** Create the network structure, consisting of 5 neuron groups and 6 synaptic pathways. */
    private void createNetwork() {
        network = new Network();
        
        // Create the neuron groups and add them to the network
        thlInput = new StimulusGenerator();
        thl = NeuronFactory.createLifExcitatory("THL", groupSize, thlInput);
        ctx = NeuronFactory.createLifExcitatory("CTX", groupSize, highBackgroundInput);
        str = NeuronFactory.createLifInhibitory("STR", groupSize, lowBackgroundInput);
        st2 = NeuronFactory.createLifInhibitory("ST2", groupSize, lowBackgroundInput);
        stn = NeuronFactory.createLifExcitatory("STN", groupSize, lowBackgroundInput);
        gpi = NeuronFactory.createLifInhibitory("GPI", groupSize / 4, highBackgroundInput);
        gpe = NeuronFactory.createLifInhibitory("GPE", groupSize / 4, highBackgroundInput);
        network.addNeuronGroups(thl, ctx, str, st2, stn, gpi, gpe);
        
        // Setup the selection pathway synapse groups
        SynapseGroup thlCtx = SynapseFactory.connectNeighborhood(thl, ctx, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStr = SynapseFactory.connectNeighborhood(ctx, str, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup ctxStn = SynapseFactory.connectNeighborhood(ctx, stn, connectivity, wide, minWeight, 2 * maxWeight);
        SynapseGroup strGpi = SynapseFactory.connectNeighborhood(str, gpi, connectivity, narrow, minWeight, 0.75 * maxWeight);
        SynapseGroup stnGpi = SynapseFactory.connectNeighborhood(stn, gpi, connectivity, narrow, minWeight, 2 * maxWeight);
        SynapseGroup gpiThl = SynapseFactory.connectNeighborhood(gpi, thl, connectivity, narrow, minWeight, 0.75 * maxWeight);
        network.addSynapseGroups(thlCtx, ctxStr, ctxStn, strGpi, stnGpi, gpiThl);
        
        // Setup the control pathway synapse groups
        SynapseGroup ctxSt2 = SynapseFactory.connectNeighborhood(ctx, st2, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup st2Gpe = SynapseFactory.connectNeighborhood(st2, gpe, connectivity, narrow, minWeight, maxWeight);
        SynapseGroup stnGpe = SynapseFactory.connectNeighborhood(stn, gpe, connectivity, narrow, minWeight, 1 * maxWeight);
        SynapseGroup gpeStn = SynapseFactory.connectNeighborhood(gpe, stn, connectivity, narrow, minWeight, 1 * maxWeight);
        SynapseGroup gpeGpi = SynapseFactory.connectNeighborhood(gpe, gpi, connectivity, narrow, minWeight, 1 * maxWeight);
        network.addSynapseGroups(st2Gpe, gpeStn, gpeGpi, stnGpe, ctxSt2);
    }
    
    /** Setup a visualization of the network activity. */
    private void createVisualization(Stage stage) {
        VBox container = new VBox();
        container.setPadding(new Insets(10, 10, 10, 10));
        container.setSpacing(10);
        Scene scene = new Scene(container, 1200, 800);
        scene.getStylesheets().add("styles/plotstyles.css");
        stage.setScene(scene);

        FlowPane toolbar = new FlowPane();
        toolbar.setHgap(10);
        
        Button runButton = new Button("run");
        runButton.setOnAction(event -> pauseSimulation = false);
        
        Button pauseButton = new Button("pause");
        pauseButton.setOnAction(event -> pauseSimulation = true);
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/desktop/"));
        fileChooser.setInitialFileName("lfp_data.csv");
        Button saveButton = new Button("save");
        saveButton.setOnAction(event -> {
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                NumberFormat format = DecimalFormat.getNumberInstance();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write("t,in,lfp,theta");
                    writer.newLine();
                    for (int i = 0; i < record.rows; ++i) {
                        writer.write(format.format(record.get(i, 0)) + ',');
                        writer.write(format.format(record.get(i, 1)) + ',');
                        writer.write(format.format(record.get(i, 2)) + ',');
                        writer.write(format.format(record.get(i, 3)));
                        writer.newLine();
                    }
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        Label intensityLabel = new Label("Intensity");
        Slider intensitySlider = new Slider();
        intensitySlider.setValue(thlInput.getIntensity());
        intensitySlider.setMin(0);
        intensitySlider.setMax(5e-3);
        intensitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            thlInput.setIntensity(newValue.doubleValue());
        });
        
        Label widthLabel = new Label("Width");
        Slider widthSlider = new Slider();
        widthSlider.setValue(thlInput.getWidth());
        widthSlider.setMin(0.01);
        widthSlider.setMax(0.25);
        widthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            thlInput.setWidth(newValue.doubleValue());
        });
        
        Label durationLabel = new Label("Duration");
        Slider durationSlider = new Slider();
        durationSlider.setValue(thlInput.getDuration() / 1000.0);
        durationSlider.setMin(0.25);
        durationSlider.setMax(1.0);
        durationSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            thlInput.setDuration((int)(1000 * newValue.doubleValue()));
        });
        
        Label intervalLabel = new Label("Interval");
        Slider intervalSlider = new Slider();
        intervalSlider.setValue(thlInput.getInterval() / 1000.0);
        intervalSlider.setMin(0.0);
        intervalSlider.setMax(1.0);
        intervalSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            thlInput.setInterval((int)(1000 * newValue.doubleValue()));
        });
        
        toolbar.getChildren().addAll(runButton, pauseButton, saveButton, intensityLabel, intensitySlider,
                widthLabel, widthSlider, durationLabel, durationSlider, intervalLabel, intervalSlider);
        container.getChildren().add(toolbar);
        
        TimeSeriesPlot.init(container);
        raster = new SpikeRasterPlot(network);
        lfpPlot = TimeSeriesPlot.line();
        lfpPlot.addSeries("LFP");
        lfpPlot.addSeries("Theta");
        lfpPlot.setAutoRanging(false, true);
        DoubleMatrix dx = stn.getXPosition().sub(0.5);
        dx.muli(dx);
        DoubleMatrix dy = stn.getYPosition().sub(0.5);
        dy.muli(dy);
        distanceToElectrode = MatrixFunctions.sqrt(dx.add(dy));
        
        stage.show();
    }
    
    /** Run the simulation on a new thread. */
    private void runSimulation(Stage stage) {
        waitForSync = false;
        pauseSimulation = true;
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int windowLength = 67;
                double[] filterWeights = BandPassFilter.sincFilter2(windowLength, 4, 8, 1000, BandPassFilter.filterType.BAND_PASS);
                filterWeights = BandPassFilter.createWindow(filterWeights, null, windowLength, BandPassFilter.windowType.HAMMING);
                DoubleMatrix lfpFilter = new DoubleMatrix(filterWeights);
                DoubleMatrix lfpData = new DoubleMatrix(windowLength);
                record = new DoubleMatrix(tSteps, 4);
                for (int step = 0; step < tSteps; step += 1) {
                    while (pauseSimulation) {
                        Thread.sleep(50);
                    }
                    
                    final double t = step * dt;
                    try {
                        network.update(step);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    raster.bufferSpikes(t);
                    
                    // Calculate the local field potential for an electrode in the subthalamic nucleus
                    double lfp = 0;
                    for (int i = 0; i < stn.getSize(); ++i) {
                        DoubleMatrix c = stn.getChangeInMembranePotential();
                        lfp = c.div(distanceToElectrode).sum();
                    }
                    lfpPlot.bufferPoint("LFP", t, lfp);
                    record.put(step, 0, t);
                    record.put(step, 1, thlInput.getState());
                    record.put(step, 2, lfp);
                    
                    lfpData.put(new IntervalRange(0, windowLength - 1), new PointRange(0),
                            lfpData.get(new IntervalRange(1, windowLength), new PointRange(0)));
                    lfpData.put(windowLength - 1, lfp);
                    double theta = lfpData.dot(lfpFilter);
                    record.put(step, 3, theta);
                    
                    lfpPlot.bufferPoint("Theta", t, theta);
                    
                    if (step % 10 == 0 || step == tSteps - 1) {
                        waitForSync = true;
                        Platform.runLater(() -> {
                            raster.updatePlot(t);
                            lfpPlot.addPoints();
                            lfpPlot.setXLimits(t - 0.25, t);
                            waitForSync = false;
                        });
                        while (waitForSync) {
                            Thread.sleep(10);
                        }
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        stage.setOnCloseRequest(event -> {
            thread.interrupt();
        });
        thread.start();
    }
}
