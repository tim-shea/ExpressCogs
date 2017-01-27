package expresscogs.gui;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * {@link ResizeListener} can be used to add mouse listeners to a {@link Region}
 * and make it resizable by the user by clicking and dragging the border in the
 * same way as a window.
 * <p>
 * Only height resizing is currently implemented. Usage: <pre>DragResizer.makeResizable(myAnchorPane);</pre>
 *
 * @author atill
 *
 */
public class ResizeListener {
    public enum Mode {
        HORIZONTAL,
        VERTICAL,
        BOTH
    }

    public static void makeResizable(Region region, Mode mode) {
        ResizeListener listener = new ResizeListener(region, mode);
        region.setOnMousePressed(listener::mousePressed);
        region.setOnMouseDragged(listener::mouseDragged);
        region.setOnMouseMoved(listener::mouseOver);
        region.setOnMouseReleased(listener::mouseReleased);
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
            region.setCursor(Cursor.SE_RESIZE);
        } else {
            region.setCursor(Cursor.DEFAULT);
        }
    }

    protected boolean isInDraggableZone(MouseEvent event) {
        return (event.getY() > (region.getHeight() - RESIZE_MARGIN) &&
                event.getX() > (region.getWidth() - RESIZE_MARGIN));
    }

    protected void mouseDragged(MouseEvent event) {
        if (dragging) {
            if (mode == Mode.HORIZONTAL || mode == Mode.BOTH) {
                region.setMinWidth(region.getMinWidth() + (event.getX() - x));
                region.setMaxWidth(region.getMaxWidth() + (event.getX() - x));
                x = event.getX();
            }
            if (mode == Mode.VERTICAL || mode == Mode.BOTH) {
                region.setMinHeight(region.getMinHeight() + (event.getY() - y));
                region.setMaxHeight(region.getMaxHeight() + (event.getY() - y));
                y = event.getY();
            }
        }
    }

    protected void mousePressed(MouseEvent event) {
        if (isInDraggableZone(event)) {
            dragging = true;
            if (mode == Mode.HORIZONTAL || mode == Mode.BOTH) {
                if (!initMinWidth) {
                    region.setMinWidth(region.getWidth());
                    region.setMaxWidth(region.getWidth());
                    initMinWidth = true;
                }
                x = event.getX();
            }
            if (mode == Mode.VERTICAL || mode == Mode.BOTH) {
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
