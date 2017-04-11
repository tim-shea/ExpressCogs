package expresscogs.simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jblas.DoubleMatrix;

import expresscogs.gui.SimulationView;
import expresscogs.network.Network;

public class SignalSelectionCli implements SimulationView {
    private static String name;
    
    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("Usage: java -jar ExpressCogs.jar <batch-name> <num-sims> <num-timesteps> <num-threads>");
            return;
        }
        name = args[0];
        int sims = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int steps = args.length > 2 ? Integer.parseInt(args[2]) : 600000;
        int threads = args.length > 3 ? Integer.parseInt(args[3]) : 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> runs = new LinkedList<Callable<Void>>();
        for (int i = 0; i < sims; ++i) {
            final String id = name + i;
            runs.add(() -> {
                SignalSelectionCli cli = new SignalSelectionCli(id, steps);
                cli.run();
                cli.saveToCsv();
                return null;
            });
        }
        executor.invokeAll(runs);
        executor.shutdown();
        Network.shutdownUpdater();
    }
    
    private SignalSelectionNetwork simulation;
    private String id;
    private int stepsBetweenView = 1000;
    private int timesteps = 10000;
    private long startTime;
    
    public SignalSelectionCli(String id, int timesteps) {
        this.id = id;
        this.timesteps = timesteps;
        System.out.println("Start: " + id + " for " + (timesteps / 1000.0) + "s");
        simulation = new SignalSelectionNetwork(this);
    }
    
    public int getStepsBetweenView() {
        return stepsBetweenView;
    }
    
    public void setStepsBetweenView(int value) {
        stepsBetweenView = value;
    }
    
    public void run() {
        startTime = System.currentTimeMillis();
        Network.setUpdateThreads(1);
        simulation.runInThread(timesteps);
        System.out.println("Finish: " + id + " in " + getElapsedTime() + "s");
        simulation.stop();
        simulation.getRecord();
    }
    
    @Override
    public void update() {
        if (simulation.getStep() % stepsBetweenView == 0 || simulation.getStep() == timesteps - 1) {
            System.out.println("Update: " + id + " at " + simulation.getTime() + "s in " + getElapsedTime() + "s");
        }
    }
    
    public void saveToCsv() {
        File directory = new File(System.getProperty("user.home") + "/ExpressCogs/" + name);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(System.getProperty("user.home") + "/ExpressCogs/" + name + "/" + id + ".csv");
        System.out.println("Saving: " + id + " as " + file.toString());
        NumberFormat format = DecimalFormat.getNumberInstance();
        try {
            DoubleMatrix record = simulation.getRecord();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("t,snr,pos,thl,ctx,str,st2,stn,gpi,gpe,lfp,sig,nos");
            for (int i = 0; i < 25; ++i) {
                writer.write(",n" + i);
            }
            writer.newLine();
            for (int i = 0; i < record.rows; ++i) {
                for (int j = 0; j < record.columns; ++j) {
                    writer.write((j == 0 ? "" : ",") + format.format(record.get(i, j)));
                }
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private double getElapsedTime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
}