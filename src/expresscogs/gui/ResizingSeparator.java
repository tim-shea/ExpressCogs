package expresscogs.gui;

import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class ResizingSeparator extends Separator {
    private Region region;
    private boolean dragging;

    public ResizingSeparator(Region region, Orientation orientation) {
        this.region = region;
        setOrientation(orientation);
        if (orientation == Orientation.VERTICAL) {
            setCursor(Cursor.E_RESIZE);
        } else {
            setCursor(Cursor.N_RESIZE);
        }
        setMinWidth(10);
        setMinHeight(10);
        setOnMousePressed(this::mousePressed);
        setOnMouseDragged(this::mouseDragged);
        setOnMouseReleased(this::mouseReleased);
    }

    protected void mouseReleased(MouseEvent event) {
        dragging = false;
    }

    protected void mouseDragged(MouseEvent event) {
        if (dragging) {
            if (getOrientation() == Orientation.VERTICAL) {
                region.setPrefWidth(event.getSceneX());
            } else {
                region.setPrefHeight(event.getSceneY());
            }
        }
    }

    protected void mousePressed(MouseEvent event) {
        dragging = true;
    }
}
