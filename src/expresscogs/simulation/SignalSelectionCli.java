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
        name = args.length > 1 ? args[1] : "sim";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<Void>> runs = new LinkedList<Callable<Void>>();
        for (int i = 0; i < 2; ++i) {
            final String id = name + i;
            runs.add(() -> {
                SignalSelectionCli cli = new SignalSelectionCli(id);
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
    private int stepsBetweenUpdate = 1000;
    private int timesteps = 10000;
    private long startTime;
    
    public SignalSelectionCli(String id) {
        this.id = id;
        System.out.println("Start: " + id);
        simulation = new SignalSelectionNetwork(this);
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
        if (simulation.getStep() % stepsBetweenUpdate == 0 || simulation.getStep() == timesteps - 1) {
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
            writer.write("t,snr,pos,thl,ctx,str,st2,stn,gpi,gpe,lfp");
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