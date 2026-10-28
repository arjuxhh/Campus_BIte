package com.campusbite.ui;

import javax.swing.*;
import java.awt.*;

/** Panel for a JScrollPane that always follows the viewport width (no clipped or horizontally-scrolling content). */
public class ViewportPanel extends JPanel implements Scrollable {
    public ViewportPanel() { super(); }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(48, r.height - 48); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
