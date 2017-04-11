package expresscogs.gui;

import java.io.File;
import expresscogs.simulation.Simulation;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class SimulationTool extends TitledPane {
    private FlowPane buttonBar;
    private VBox controls;
    private int tMax = 100000;
    
    public SimulationTool(Simulation simulation, SimulationView view) {
        setText("Simulation");
        controls = new VBox();
        controls.setPadding(new Insets(10, 10, 10, 10));
        buttonBar = new FlowPane();
        setContent(controls);
        setAnimated(false);
        buttonBar.setHgap(2);
        buttonBar.setPrefWidth(0);
        controls.getChildren().add(buttonBar);
        
        ToggleButton runButton = new ToggleButton("run");
        runButton.setOnAction(event -> {
            if (runButton.isSelected()) {
                simulation.runAsync(tMax);
            } else {
                simulation.stop();
            }
        });
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "/desktop/"));
        fileChooser.setInitialFileName("lfp_data.csv");
        Button saveButton = new Button("save");
        saveButton.setDisable(true);
        /*
        saveButton.setOnAction(event -> {
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                NumberFormat format = DecimalFormat.getNumberInstance();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write("t,ns,in,thl,ctx,stn,lfp");
                    writer.newLine();
                    for (int i = 0; i < record.rows; ++i) {
                        writer.write(format.format(record.get(i, 0)) + ',');
                        writer.write(format.format(record.get(i, 1)) + ',');
                        writer.write(format.format(record.get(i, 2)) + ',');
                        writer.write(format.format(record.get(i, 3)) + ',');
                        writer.write(format.format(record.get(i, 4)) + ',');
                        writer.write(format.format(record.get(i, 5)) + ',');
                        writer.write(format.format(record.get(i, 6)));
                        writer.newLine();
                    }
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        */
        
        ToggleButton slowToggle = new ToggleButton("slow");
        slowToggle.setOnAction(event -> {
            simulation.setSlow(slowToggle.isSelected());
            view.setStepsBetweenView(slowToggle.isSelected() ? 5 : 20);
        });
        slowToggle.setSelected(simulation.isSlow());
        
        buttonBar.getChildren().addAll(runButton, slowToggle, saveButton);
    }
}
