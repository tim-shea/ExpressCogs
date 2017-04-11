package expresscogs.utility;

public class SignalSelectionPlot extends BufferedPlot {
    private final double plotWidth = 1.0;
    
    private SignalDetectionSensor signalSensor;
    private BufferedDataSeries signalSeries;
    private BufferedDataSeries noiseSeries;
    
    public SignalSelectionPlot(SignalDetectionSensor signalSensor) {
        createLine();
        this.signalSensor = signalSensor;
        signalSeries = addSeries("Signal");
        signalSeries.setMaxLength(0);
        noiseSeries = addSeries("Noise");
        noiseSeries.setMaxLength(0);
        setAutoRanging(false, true);
    }
    
    @Override
    public void updateBuffers(double t) {
        if (!isEnabled()) {
            return;
        }
        signalSeries.bufferPoint(t, signalSensor.getSignalStrength());
        noiseSeries.bufferPoint(t, signalSensor.getNoiseStrength());
    }
    
    @Override
    public void updatePlot(double t) {
        if (!isEnabled()) {
            return;
        }
        signalSeries.setMinXValue(t - plotWidth);
        noiseSeries.setMinXValue(t - plotWidth);
        signalSeries.addBuffered();
        noiseSeries.addBuffered();
        setXLimits(t - plotWidth, t);
    }
}
