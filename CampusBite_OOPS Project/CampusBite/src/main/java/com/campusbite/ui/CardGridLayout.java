package com.campusbite.ui;

import java.awt.*;

/**
 * Responsive card grid: as many columns as fit (each at least {@code minCardWidth} wide), and the cards stretch to use
 * the full row width, so there is never a blank strip on the right. A child with client property "fullRow" spans the width.
 */
public class CardGridLayout implements LayoutManager {
    private final int minCardWidth, cardHeight, gap;

    public CardGridLayout(int minCardWidth, int cardHeight, int gap) {
        this.minCardWidth = minCardWidth; this.cardHeight = cardHeight; this.gap = gap;
    }

    @Override public void layoutContainer(Container parent) { arrange(parent, true); }
    @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(minCardWidth, arrange(parent, false)); }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(minCardWidth, 0); }
    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }

    private int arrange(Container parent, boolean apply) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = Math.max(minCardWidth, parent.getWidth() - in.left - in.right);
            int cols = Math.max(1, (width + gap) / (minCardWidth + gap));
            int cardW = (width - (cols - 1) * gap) / cols;
            int col = 0, y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                boolean full = c instanceof javax.swing.JComponent jc && jc.getClientProperty("fullRow") != null;
                if (full) {
                    if (col > 0) { y += cardHeight + gap; col = 0; }
                    int h = c.getPreferredSize().height;
                    if (apply) c.setBounds(in.left, y, width, h);
                    y += h + gap;
                } else {
                    if (apply) c.setBounds(in.left + col * (cardW + gap), y, cardW, cardHeight);
                    if (++col == cols) { col = 0; y += cardHeight + gap; }
                }
            }
            if (col > 0) y += cardHeight + gap;
            return Math.max(0, y - gap) + in.bottom;
        }
    }
}
