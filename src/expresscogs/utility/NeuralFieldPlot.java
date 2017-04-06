package expresscogs.utility;

import org.jblas.DoubleMatrix;
import expresscogs.network.NeuronGroup;
import javafx.scene.chart.XYChart;

public class NeuralFieldPlot {
    private final double timescale = 1;
    
    private TimeSeriesPlot plot = TimeSeriesPlot.line();
    private NeuronGroup neurons;
    private DoubleMatrix field;
    private BufferedDataSeries series;
    
    public NeuralFieldPlot() {
        series = plot.addSeries("Neural Field");
        plot.setAutoRanging(false, false);
        plot.setXLimits(0, 1);
        plot.setYLimits(0, 100);
    }
    
    public void setNeuronGroup(NeuronGroup neurons) {
        this.neurons = neurons;
        field = DoubleMatrix.zeros(neurons.getSize());
        series.setMaxLength(neurons.getSize());
    }
    
    public XYChart<Number, Number> getChart() {
        return plot.getChart();
    }
    
    public void bufferNeuralField(double t) {
        if (neurons != null) {
            field.addi(neurons.getSpikes());
            field.muli(0.999);
        }
    }
    
    public void updatePlot(double t) {
        if (neurons != null) {
            plot.bufferPoints("Neural Field", neurons.getXPosition().data, field.data);
            plot.addPoints();
        }
    }
}
