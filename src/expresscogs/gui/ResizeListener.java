package expresscogs.gui;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class ResizeListener {
    public enum Mode {
        HORIZONTAL,
        VERTICAL
    }

    public static void makeResizable(Region region, Mode mode) {
        ResizeListener listener = new ResizeListener(region, mode);
        region.setOnMousePressed(listener::mousePressed);
        region.setOnMouseDragged(listener::mouseDragged);
        region.setOnMouseMoved(listener::mouseOver);
        region.setOnMouseReleased(listener::mouseReleased);
        if (mode == Mode.HORIZONTAL) {
            region.setStyle("-fx-border-color: transparent darkgray transparent transparent");
        } else {
            region.setStyle("-fx-border-color: transparent transparent darkgray transparent");
        }
    }

    private static final int RESIZE_MARGIN = 16;
    private final Region region;
    private Mode mode;
    private double x;
    private double y;
    private boolean initMinWidth;
    private boolean initMinHeight;
    private boolean dragging;

    private ResizeListener(Region region, Mode mode) {
        this.region = region;
        this.mode = mode;
    }

    protected void mouseReleased(MouseEvent event) {
        if (dragging) {
            dragging = false;
            region.setCursor(Cursor.DEFAULT);
        }
    }

    protected void mouseOver(MouseEvent event) {
        if (isInDraggableZone(event) || dragging) {
            if (mode == Mode.HORIZONTAL) {
                region.setCursor(Cursor.E_RESIZE);
            } else {
                region.setCursor(Cursor.S_RESIZE);
            }
        } else {
            region.setCursor(Cursor.DEFAULT);
        }
    }

    protected boolean isInDraggableZone(MouseEvent event) {
        if (mode == Mode.HORIZONTAL) {
            return event.getX() > (region.getWidth() - RESIZE_MARGIN); 
        } else {
            return event.getY() > (region.getHeight() - RESIZE_MARGIN);
        }
    }

    protected void mouseDragged(MouseEvent event) {
        if (dragging) {
            if (mode == Mode.HORIZONTAL) {
                region.setMinWidth(region.getMinWidth() + (event.getX() - x));
                region.setMaxWidth(region.getMaxWidth() + (event.getX() - x));
                x = event.getX();
            } else {
                region.setMinHeight(region.getMinHeight() + (event.getY() - y));
                region.setMaxHeight(region.getMaxHeight() + (event.getY() - y));
                y = event.getY();
            }
        }
    }

    protected void mousePressed(MouseEvent event) {
        if (isInDraggableZone(event)) {
            dragging = true;
            if (mode == Mode.HORIZONTAL) {
                if (!initMinWidth) {
                    region.setMinWidth(region.getWidth());
                    region.setMaxWidth(region.getWidth());
                    initMinWidth = true;
                }
                x = event.getX();
            } else {
                if (!initMinHeight) {
                    region.setMinHeight(region.getHeight());
                    region.setMaxHeight(region.getHeight());
                    initMinHeight = true;
                }
                y = event.getY();
            }
        }
    }
}
