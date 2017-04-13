package expresscogs.utility;

public class NeuralFieldPlot extends BufferedPlot {
    private NeuralFieldSensor sensor;
    private BufferedDataSeries fieldSeries;
    private BufferedDataSeries meanSeries;
    
    public NeuralFieldPlot(NeuralFieldSensor sensor) {
        createLine();
        this.sensor = sensor;
        fieldSeries = addSeries("Neural Field");
        fieldSeries.setMaxLength(sensor.getPosition().length);
        meanSeries = addSeries("Mean Firing Rate");
        meanSeries.setMaxLength(2);
        setAutoRanging(false, false);
        setYLimits(0, 1000);
        setXLimits(0, 1);
    }
    
    @Override
    public void updatePlot(double t) {
        if (!isEnabled()) {
            return;
        }
        fieldSeries.bufferPoints(sensor.getPosition().data, sensor.getActivity().data);
        fieldSeries.addBuffered();
        double mean = sensor.getActivity().mean();
        meanSeries.bufferPoints(new double[] { 0, 1 }, new double[] { mean, mean });
        meanSeries.addBuffered();
    }
}
