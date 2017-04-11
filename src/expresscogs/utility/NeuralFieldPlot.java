package expresscogs.utility;

public class NeuralFieldPlot extends BufferedPlot {
    private NeuralFieldSensor sensor;
    private BufferedDataSeries series;
    
    public NeuralFieldPlot(NeuralFieldSensor sensor) {
        createLine();
        this.sensor = sensor;
        series = addSeries("Neural Field");
        series.setMaxLength(sensor.getPosition().length);
        setAutoRanging(false, true);
        setXLimits(0, 1);
    }
    
    @Override
    public void updatePlot(double t) {
        if (!isEnabled()) {
            return;
        }
        series.bufferPoints(sensor.getPosition().data, sensor.getActivity().data);
        series.addBuffered();
    }
}
