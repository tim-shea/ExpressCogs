package expresscogs.gui;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * {@link DragResizer} can be used to add mouse listeners to a {@link Region}
 * and make it resizable by the user by clicking and dragging the border in the
 * same way as a window.
 * <p>
 * Only height resizing is currently implemented. Usage: <pre>DragResizer.makeResizable(myAnchorPane);</pre>
 *
 * @author atill
 *
 */
public class ResizingSeparator {
    private static final int RESIZE_MARGIN = 5;
    private final Region region;
    private double y;
    private boolean initMinHeight;
    private boolean dragging;

    private ResizingSeparator(Region region) {
        this.region = region;
    }

    public static void makeResizable(Region region) {
        ResizingSeparator resizer = new ResizingSeparator(region);
        region.setOnMousePressed(event -> resizer.mousePressed(event));
        region.setOnMouseDragged(event -> resizer.mouseDragged(event));
        region.setOnMouseMoved(event -> resizer.mouseOver(event));
        region.setOnMouseReleased(event -> resizer.mouseReleased(event));
    }

    protected void mouseReleased(MouseEvent event) {
        dragging = false;
        region.setCursor(Cursor.DEFAULT);
    }

    protected void mouseOver(MouseEvent event) {
        if(isInDraggableZone(event) || dragging) {
            region.setCursor(Cursor.S_RESIZE);
        }
        else {
            region.setCursor(Cursor.DEFAULT);
        }
    }

    protected boolean isInDraggableZone(MouseEvent event) {
        return event.getY() > (region.getHeight() - RESIZE_MARGIN);
    }

    protected void mouseDragged(MouseEvent event) {
        if (dragging) {
            region.setMinHeight(region.getMinHeight() + (event.getY() - y));
            y = event.getY();
        }
    }

    protected void mousePressed(MouseEvent event) {
        if (isInDraggableZone(event)) {
            dragging = true;
            if (!initMinHeight) {
                region.setMinHeight(region.getHeight());
                initMinHeight = true;
            }
            y = event.getY();
        }
    }
}
