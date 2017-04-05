package expresscogs.simulation;

import javafx.concurrent.Task;

public abstract class Simulation {
    // Flag for synchronizing a running simulation with the calling thread
    private boolean waitForSync = false;
    // Flag for a running simulation
    private boolean run = false;
    // Duration of simulation step in seconds
    private double dt = 0.001;
    // Current simulation timestep
    private int step = 0;
    // Asynchronous simulation thread
    private Thread thread;
    
    public void runInThread(int timesteps) {
        run = true;
        while (step < timesteps && run) {
            update();
            ++step;
        }
    }
    
    public void runAsync(int timesteps) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                run = true;
                while (step < timesteps && run) {
                    try {
                        update();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    try {
                        while (waitForSync && run) {
                            Thread.sleep(5);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ++step;
                }
                return null;
            }
        };
        thread = new Thread(task);
        thread.start();
    }
    
    public void stop() {
        run = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void sync() {
        waitForSync = !waitForSync;
    }
    
    public abstract void update();
    
    public int getStep() {
        return step;
    }
    
    public double getTime() {
        return step * dt;
    }
}
