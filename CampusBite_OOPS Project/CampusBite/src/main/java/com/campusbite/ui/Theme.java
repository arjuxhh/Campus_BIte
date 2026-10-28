package com.campusbite.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Colours and small widget factories shared by every screen. */
public final class Theme {
    public static final Color NAVY = new Color(52, 48, 43), TEAL = new Color(245, 112, 10), PALE = new Color(255, 237, 218),
            BG = new Color(247, 247, 244), GOLD = new Color(214, 104, 18), TEXT = new Color(52, 48, 43),
            MUTED = new Color(125, 122, 116), WHITE = Color.WHITE, BORDER = new Color(232, 231, 225),
            DANGER = new Color(176, 55, 48), DANGER_BG = new Color(252, 232, 225), SUCCESS = new Color(204, 102, 15);

    private Theme() { }

    public static Font font(boolean bold, int size) { return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size); }

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(bold, size));
        l.setForeground(color);
        return l;
    }

    /** A label that honours "\n" (a plain JLabel prints newlines as nothing). */
    public static JLabel multiline(String text, int size, boolean bold, Color color) {
        return label("<html>" + esc(text).replace("\n", "<br>") + "</html>", size, bold, color);
    }

    public static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static Color blend(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Rounded button painted by us, so colours look identical on Windows, macOS and Linux look-and-feels. */
    public static JButton button(String text, Color bg, Color fg) {
        return new RoundedButton(text, bg, fg);
    }

    /** Search box with a placeholder that works with every look-and-feel. */
    public static JTextField searchField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!getText().isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        f.setFont(font(false, 14));
        f.setToolTipText("Search food by name, category or description");
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 12, 9, 12)));
        return f;
    }

    private static final class RoundedButton extends JButton {
        private boolean hover;

        RoundedButton(String text, Color bg, Color fg) {
            super(text);
            setBackground(bg);
            setForeground(fg);
            setFont(font(true, 13));
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 15, 10, 15));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = getBackground();
            boolean light = (base.getRed() * 299 + base.getGreen() * 587 + base.getBlue() * 114) / 1000 > 170;
            Color c = base;
            if (!isEnabled()) c = new Color(226, 230, 236);
            else if (getModel().isPressed()) c = blend(base, light ? Color.BLACK : Color.BLACK, 0.16);
            else if (hover) c = blend(base, light ? Color.BLACK : Color.WHITE, 0.08);
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
