package com.campusbite;

import com.campusbite.model.*;
import com.campusbite.model.MenuItem;
import com.campusbite.service.CanteenService;
import com.campusbite.service.CanteenService.Quote;
import com.campusbite.ui.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.util.List;
import java.util.*;

public class CampusBiteApplication {
    private static final String[] CATEGORIES = {"All", "Breakfast", "Lunch", "Snacks", "Drinks"};
    private static final Map<String, String> CATEGORY_ICON = Map.of("All", "✦", "Breakfast", "☀", "Lunch", "◈", "Snacks", "◇", "Drinks", "☕");
    private static final String[] SLOTS = {"10:00 AM – 10:15 AM", "11:00 AM – 11:15 AM", "12:30 PM – 12:45 PM",
            "1:30 PM – 1:45 PM", "3:00 PM – 3:15 PM", "3:45 PM – 4:00 PM"};
    private static final int CARD_MIN_W = 220, CARD_H = 272, SIDE_W = 310;

    private final CanteenService service = new CanteenService();
    private final Map<MenuItem, Integer> cart = new LinkedHashMap<>();
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale.Builder().setLanguage("en").setRegion("IN").build());
    private final Map<String, JButton> categoryButtons = new LinkedHashMap<>();

    private JFrame frame;
    private User user;
    private String category = "All";
    private ViewportPanel menuPanel;
    private JScrollPane menuScroll;
    private JPanel cartItemsPanel, adminBody;
    private JLabel cartTotalLabel, cartCountLabel, resultLabel;
    private JTextField searchField;
    private JButton loyaltyButton, refundsButton;

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new CampusBiteApplication().start());
    }

    private void start() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        showWelcome();
    }

    private boolean isAdmin() { return user != null && user.canManageOrders(); }

    // =================================================================================================================
    //  Sign-in
    // =================================================================================================================
    private void showWelcome() {
        JTextField name = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JComboBox<String> role = new JComboBox<>(new String[]{"Student", "Teacher", "Admin"});
        for (JComponent c : new JComponent[]{name, password, role}) c.setFont(Theme.font(false, 14));
        for (JComponent c : new JComponent[]{name, password})
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(9, 10, 9, 10)));

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.WHITE);
        shell.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel hero = new JPanel(new BorderLayout(12, 0));
        hero.setBackground(Theme.TEAL);
        hero.setBorder(new EmptyBorder(22, 24, 22, 24));
        hero.add(new JLabel(ImageCatalog.logo(64)), BorderLayout.WEST);
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.add(Theme.label("Welcome to CampusBite", 23, true, Theme.WHITE));
        heroText.add(Box.createVerticalStrut(7));
        heroText.add(Theme.label("Your campus favourites, just a few clicks away.", 12, false, new Color(255, 242, 224)));
        hero.add(heroText, BorderLayout.CENTER);
        shell.add(hero, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.WHITE);
        form.setBorder(new EmptyBorder(22, 26, 18, 26));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        Object[][] rows = {
                {Theme.label("Sign in to continue", 18, true, Theme.NAVY), new Insets(5, 0, 5, 0)},
                {Theme.label("Enter your details and choose your campus role.", 12, false, Theme.MUTED), new Insets(0, 0, 5, 0)},
                {Theme.label("FULL NAME", 10, true, Theme.TEAL), new Insets(15, 0, 4, 0)}, {name, new Insets(2, 0, 5, 0)},
                {Theme.label("PASSWORD", 10, true, Theme.TEAL), new Insets(10, 0, 4, 0)}, {password, new Insets(2, 0, 5, 0)},
                {Theme.label("SIGN IN AS", 10, true, Theme.TEAL), new Insets(10, 0, 4, 0)}, {role, new Insets(2, 0, 5, 0)},
                {Theme.label("Demo sign-in · Admin: admin / admin123", 11, false, Theme.MUTED), new Insets(10, 0, 0, 0)}};
        for (int i = 0; i < rows.length; i++) { g.gridy = i; g.insets = (Insets) rows[i][1]; form.add((Component) rows[i][0], g); }
        shell.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footer.setBackground(new Color(250, 249, 246));
        JButton cancel = Theme.button("Cancel", Theme.WHITE, Theme.MUTED);
        JButton signIn = Theme.button("Sign In  →", Theme.TEAL, Theme.WHITE);
        footer.add(cancel);
        footer.add(signIn);
        shell.add(footer, BorderLayout.SOUTH);

        JDialog dialog = new JDialog((Frame) null, "CampusBite | Sign In", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(shell);
        dialog.setSize(510, 590);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.setIconImage(ImageCatalog.logo(64).getImage());

        boolean[] signedIn = {false};
        Runnable submit = () -> {
            String n = name.getText().trim(), pw = new String(password.getPassword());
            if (n.isEmpty() || pw.isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Please enter your name and password to sign in.", "Missing details", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selected = (String) role.getSelectedItem();
            if ("Admin".equals(selected) && !((n.equalsIgnoreCase("admin") || n.equalsIgnoreCase("canteen admin")) && pw.equals("admin123"))) {
                JOptionPane.showMessageDialog(dialog, "Admin demo access requires name 'admin' and password 'admin123'.", "Admin sign-in failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            user = switch (selected) { case "Teacher" -> new Teacher(n); case "Admin" -> new Admin("Canteen Admin"); default -> new Student(n); };
            signedIn[0] = true;
            dialog.dispose();
        };
        signIn.addActionListener(e -> submit.run());
        password.addActionListener(e -> submit.run());
        name.addActionListener(e -> password.requestFocusInWindow());
        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(signIn);
        dialog.setVisible(true);
        if (signedIn[0]) buildWindow();
    }

    // =================================================================================================================
    //  Main window:  header (brand · search · account)  |  sidebar  |  menu grid  |  cart or admin overview
    // =================================================================================================================
    private void buildWindow() {
        frame = new JFrame("CampusBite | Smart Campus Canteen");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(1060, screen.width), Math.min(640, screen.height)));
        frame.setSize(Math.min(1280, screen.width - 40), Math.min(800, screen.height - 40));
        frame.setLocationRelativeTo(null);
        frame.setIconImage(ImageCatalog.logo(64).getImage());
        frame.setContentPane(buildRoot());
        frame.setVisible(true);
    }

    /** Whole main screen as one panel (kept separate from the JFrame so it can be rendered/tested without a window). */
    private JPanel buildRoot() {
        categoryButtons.clear();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(sidebar(), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(Theme.BG);
        left.add(introPanel(), BorderLayout.NORTH);
        menuPanel = new ViewportPanel();
        menuPanel.setLayout(new CardGridLayout(CARD_MIN_W, CARD_H, 14));
        menuPanel.setBackground(Theme.BG);
        menuScroll = new JScrollPane(menuPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.getViewport().setBackground(Theme.BG);
        menuScroll.getVerticalScrollBar().setUnitIncrement(24);
        left.add(menuScroll, BorderLayout.CENTER);
        content.add(left, BorderLayout.CENTER);
        content.add(isAdmin() ? adminPanel() : cartPanel(), BorderLayout.EAST);
        root.add(content, BorderLayout.CENTER);

        refreshMenu(false);
        refreshSide();
        return root;
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(Theme.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(10, 20, 10, 20)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(ImageCatalog.logo(44)));
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        texts.add(Theme.label("CampusBite", 21, true, Theme.NAVY));
        texts.add(Theme.label("SMART CAMPUS CANTEEN", 10, true, Theme.TEAL));
        brand.add(texts);
        p.add(brand, BorderLayout.WEST);

        searchField = Theme.searchField("Search dishes, e.g. dosa, tea, biriyani…");
        searchField.setPreferredSize(new Dimension(380, 40));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshMenu(true); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshMenu(true); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshMenu(true); }
        });
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(searchField);
        p.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        if (!isAdmin()) {
            JButton orders = Theme.button("My orders", Theme.PALE, Theme.TEAL);
            orders.addActionListener(e -> showOrders());
            right.add(orders);
        }
        JButton profile = Theme.button(user.getName() + " · " + user.getRole(), Theme.NAVY, Theme.WHITE);
        profile.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Switch user? Your current cart will be cleared.", "Switch user", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                frame.dispose();
                cart.clear();
                showWelcome();
            }
        });
        right.add(profile);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JComponent sidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.WHITE);
        p.setBorder(new EmptyBorder(20, 12, 16, 12));
        p.add(sectionLabel("BROWSE MENU"));
        for (String c : CATEGORIES) {
            JButton b = sideButton(CATEGORY_ICON.get(c) + "   " + c);
            b.addActionListener(e -> { category = c; refreshMenu(true); });
            categoryButtons.put(c, b);
            p.add(b);
            p.add(Box.createVerticalStrut(5));
        }
        p.add(Box.createVerticalStrut(14));
        if (isAdmin()) {
            p.add(sectionLabel("ADMIN TOOLS"));
            JButton orders = sideButton("▤   Manage orders");
            orders.addActionListener(e -> showOrders());
            refundsButton = sideButton("↺   Refunds");
            refundsButton.addActionListener(e -> showRefundRequests());
            JButton add = sideButton("+   Add menu item");
            add.addActionListener(e -> addMenuItemDialog());
            for (JButton b : new JButton[]{orders, refundsButton, add}) { p.add(b); p.add(Box.createVerticalStrut(5)); }
        } else {
            p.add(sectionLabel("MY ACCOUNT"));
            JButton offers = sideButton("%   Festival offers");
            offers.addActionListener(e -> showFestivalOffers());
            loyaltyButton = sideButton("★   Loyalty");
            loyaltyButton.addActionListener(e -> showLoyalty());
            for (JButton b : new JButton[]{offers, loyaltyButton}) { p.add(b); p.add(Box.createVerticalStrut(5)); }
        }
        p.add(Box.createVerticalGlue());
        p.add(Box.createVerticalStrut(12));
        JPanel hours = new JPanel(new BorderLayout(0, 5));
        hours.setBackground(new Color(255, 237, 218));
        hours.setBorder(new EmptyBorder(12, 12, 12, 12));
        hours.setAlignmentX(Component.LEFT_ALIGNMENT);
        hours.setPreferredSize(new Dimension(176, 98));
        hours.setMaximumSize(new Dimension(176, 98));
        hours.add(Theme.label("CANTEEN HOURS", 10, true, new Color(190, 89, 12)), BorderLayout.NORTH);
        hours.add(Theme.multiline("9:30 AM – 4:30 PM\nOrder ahead and skip the queue.", 12, false, Theme.WHITE), BorderLayout.CENTER);
        p.add(hours);

        JScrollPane scroll = new JScrollPane(p, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.WHITE);
        scroll.setPreferredSize(new Dimension(200, 0));
        return scroll;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = Theme.label(text, 11, true, new Color(125, 122, 116));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 4, 8, 0));
        return l;
    }

    private JButton sideButton(String text) {
        JButton b = Theme.button(text, Theme.WHITE, Theme.MUTED);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(9, 14, 9, 14));
        b.setMaximumSize(new Dimension(176, 42));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private JPanel introPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 6));
        p.setBackground(Theme.BG);
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Theme.label(greeting + ", " + user.getName() + "!", 24, true, Theme.NAVY));
        heading.add(Box.createVerticalStrut(4));
        heading.add(Theme.label(isAdmin() ? "Keep the menu fresh and orders moving." : "Discover campus favourites and order ahead for a faster pickup.", 13, false, Theme.MUTED));
        p.add(heading, BorderLayout.CENTER);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        for (String s : new String[]{"Freshly prepared", "Easy pickup"}) {
            JLabel l = Theme.label("✓ " + s, 10, true, Theme.TEAL);
            l.setOpaque(true);
            l.setBackground(Theme.PALE);
            l.setBorder(new EmptyBorder(6, 8, 6, 8));
            badges.add(l);
        }
        resultLabel = Theme.label(" ", 12, true, Theme.TEAL);
        JPanel resultRow = new JPanel(new BorderLayout());
        resultRow.setOpaque(false);
        resultRow.setBorder(new EmptyBorder(8, 2, 0, 0));
        resultRow.add(resultLabel, BorderLayout.WEST);
        resultRow.add(badges, BorderLayout.EAST);
        p.add(resultRow, BorderLayout.SOUTH);
        return p;
    }

    // =================================================================================================================
    //  Menu grid
    // =================================================================================================================
    private void refreshMenu(boolean scrollToTop) {
        if (menuPanel == null) return;
        menuPanel.removeAll();
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Map<String, Integer> counts = new HashMap<>();
        int shown = 0;
        for (MenuItem item : service.getMenu()) {
            counts.merge("All", 1, Integer::sum);
            counts.merge(item.getCategory(), 1, Integer::sum);
            if (!category.equals("All") && !item.getCategory().equals(category)) continue;
            String searchable = (item.getName() + " " + item.getCategory() + " " + item.getDescription()).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            menuPanel.add(foodCard(item));
            shown++;
        }
        for (Map.Entry<String, JButton> e : categoryButtons.entrySet()) {
            e.getValue().setText(CATEGORY_ICON.get(e.getKey()) + "   " + e.getKey() + "  ·  " + counts.getOrDefault(e.getKey(), 0));
            e.getValue().setBackground(e.getKey().equals(category) ? Theme.PALE : Theme.WHITE);
            e.getValue().setForeground(e.getKey().equals(category) ? Theme.TEAL : Theme.MUTED);
        }
        resultLabel.setText((category.equals("All") ? "All dishes" : category) + " · " + shown + (shown == 1 ? " dish" : " dishes")
                + (query.isEmpty() ? "" : "  matching “" + searchField.getText().trim() + "”"));
        if (shown == 0) {
            JPanel empty = new JPanel(new GridLayout(0, 1, 0, 8));
            empty.setBackground(Theme.BG);
            empty.setBorder(new EmptyBorder(40, 20, 40, 20));
            empty.setPreferredSize(new Dimension(520, 130));
            empty.putClientProperty("fullRow", Boolean.TRUE);
            JLabel title = Theme.label("No dishes found", 18, true, Theme.NAVY);
            JLabel hint = Theme.label("Try another search or choose a different category.", 12, false, Theme.MUTED);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            hint.setHorizontalAlignment(SwingConstants.CENTER);
            empty.add(title);
            empty.add(hint);
            menuPanel.add(empty);
        }
        menuPanel.revalidate();
        menuPanel.repaint();
        if (scrollToTop) SwingUtilities.invokeLater(() -> menuScroll.getVerticalScrollBar().setValue(0));
    }

    /** Photo with a category tag (and a "sold out" veil). Centre-crops to any card width, never stretches. */
    private static final class DishImage extends JComponent {
        private final java.awt.image.BufferedImage image;
        private final String tag;
        private final boolean soldOut;

        DishImage(java.awt.image.BufferedImage image, String tag, boolean soldOut, int height) {
            this.image = image; this.tag = tag.toUpperCase(Locale.ROOT); this.soldOut = soldOut;
            setPreferredSize(new Dimension(100, height));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight(), iw = image.getWidth(), ih = image.getHeight();
            double target = (double) w / h;
            int sw, sh;
            if ((double) iw / ih > target) { sh = ih; sw = (int) Math.round(ih * target); } else { sw = iw; sh = (int) Math.round(iw / target); }
            int sx = (iw - sw) / 2, sy = (ih - sh) / 2;
            g2.drawImage(image, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
            g2.setFont(Theme.font(true, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(tag) + 16;
            g2.setColor(new Color(255, 255, 255, 225));
            g2.fillRoundRect(10, 10, tw, 22, 22, 22);
            g2.setColor(Theme.TEAL);
            g2.drawString(tag, 18, 10 + (22 - fm.getHeight()) / 2 + fm.getAscent());
            if (soldOut) {
                g2.setColor(new Color(255, 255, 255, 170));
                g2.fillRect(0, 0, w, h);
                g2.setFont(Theme.font(true, 12));
                fm = g2.getFontMetrics();
                String s = "SOLD OUT";
                int bw = fm.stringWidth(s) + 28, x = (w - bw) / 2, y = (h - 30) / 2;
                g2.setColor(Theme.NAVY);
                g2.fillRoundRect(x, y, bw, 30, 30, 30);
                g2.setColor(Color.WHITE);
                g2.drawString(s, x + 14, y + (30 - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    private JPanel foodCard(MenuItem item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(new DishImage(ImageCatalog.dishImage(item), item.getCategory(), !item.isAvailable(), 148), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setBackground(Theme.WHITE);
        body.setBorder(new EmptyBorder(12, 13, 12, 13));
        JLabel name = Theme.label(item.getName(), 14, true, Theme.TEXT);
        name.setToolTipText(item.getName());
        body.add(name, BorderLayout.NORTH);
        JLabel desc = Theme.label("<html>" + Theme.esc(item.getDescription()) + "</html>", 11, false, Theme.MUTED);
        desc.setVerticalAlignment(SwingConstants.TOP);
        body.add(desc, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(Theme.label(money.format(item.getPrice()), 17, true, Theme.NAVY), BorderLayout.WEST);
        JButton action;
        if (isAdmin()) {
            action = Theme.button(item.isAvailable() ? "Mark sold out" : "Mark available", item.isAvailable() ? Theme.DANGER_BG : Theme.PALE, item.isAvailable() ? Theme.DANGER : Theme.TEAL);
            action.setToolTipText(item.isAvailable() ? "Mark this dish as sold out" : "Mark this dish as available again");
            action.addActionListener(e -> { service.setAvailable(item, !item.isAvailable()); refreshMenu(false); });
        } else if (item.isAvailable()) {
            action = Theme.button("+ Add", Theme.TEAL, Theme.WHITE);
            action.setToolTipText("Add " + item.getName() + " to your cart");
            action.addActionListener(e -> { cart.merge(item, 1, Integer::sum); refreshCart(); });
        } else {
            action = Theme.button("Sold out", Theme.BG, Theme.MUTED);
            action.setEnabled(false);
        }
        action.setBorder(new EmptyBorder(9, 12, 9, 12));
        if (isAdmin()) { action.setFont(Theme.font(true, 12)); action.setBorder(new EmptyBorder(9, 8, 9, 8)); }
        bottom.add(action, BorderLayout.EAST);
        body.add(bottom, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void addMenuItemDialog() {
        JTextField name = new JTextField(18), price = new JTextField(10), description = new JTextField(18);
        JComboBox<String> cat = new JComboBox<>(new String[]{"Breakfast", "Lunch", "Snacks", "Drinks"});
        JComboBox<String> emoji = new JComboBox<>(new String[]{"🍛", "🍚", "🥣", "🥞", "🫓", "🍗", "🍌", "🥐", "🟠", "🍵", "☕", "🫖", "🥟", "🍳", "🥡", "🍽️"});
        File[] photo = {null};
        JButton choose = Theme.button("Choose photo…", Theme.PALE, Theme.TEAL);
        JLabel chosen = Theme.label("No photo (a coloured tile with the icon is used)", 11, false, Theme.MUTED);
        choose.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (jpg, png)", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            try {
                if (ImageIO.read(fc.getSelectedFile()) == null) throw new java.io.IOException();
                photo[0] = fc.getSelectedFile();
                chosen.setText(photo[0].getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "That file could not be read as an image.", "Photo not usable", JOptionPane.WARNING_MESSAGE);
            }
        });
        Font f = Theme.font(false, 14);
        for (JComponent c : new JComponent[]{name, price, description, cat, emoji}) c.setFont(f);
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setBorder(new EmptyBorder(18, 18, 18, 18));
        form.setBackground(Theme.WHITE);
        form.add(Theme.label("ITEM NAME", 11, true, Theme.TEAL)); form.add(name);
        form.add(Theme.label("CATEGORY", 11, true, Theme.TEAL)); form.add(cat);
        form.add(Theme.label("PRICE (₹)", 11, true, Theme.TEAL)); form.add(price);
        form.add(Theme.label("DESCRIPTION", 11, true, Theme.TEAL)); form.add(description);
        form.add(Theme.label("ICON (if no photo)", 11, true, Theme.TEAL)); form.add(emoji);
        form.add(choose); form.add(chosen);
        while (true) {
            if (JOptionPane.showConfirmDialog(frame, form, "Add a new CampusBite menu item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                double p = Double.parseDouble(price.getText().trim());
                MenuItem item = service.addMenuItem(name.getText(), (String) cat.getSelectedItem(), p, description.getText(), (String) emoji.getSelectedItem(),
                        photo[0] == null ? null : photo[0].getAbsolutePath());
                category = "All";
                refreshMenu(false);
                JOptionPane.showMessageDialog(frame, "Menu item added successfully!\n\n" + item.getName() + " — " + money.format(item.getPrice()), "Item added", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter the price as a number, for example 45.", "Check item details", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Check item details", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // =================================================================================================================
    //  Right column: cart (students/teachers) or overview (admin)
    // =================================================================================================================
    private void refreshSide() { if (isAdmin()) refreshAdmin(); else refreshCart(); refreshAccountButtons(); }

    private void refreshAccountButtons() {
        if (loyaltyButton != null) loyaltyButton.setText("★   Loyalty · " + service.getLoyaltyPoints(user.getName()) + " pts");
        if (refundsButton != null) refundsButton.setText("↺   Refunds · " + service.stats().pendingRefunds());
    }

    private JPanel cartPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(16, 14, 14, 14)));
        p.setPreferredSize(new Dimension(SIDE_W, 0));
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(Theme.label("Your cart", 20, true, Theme.NAVY), BorderLayout.WEST);
        cartCountLabel = Theme.label("0 items", 11, false, Theme.MUTED);
        title.add(cartCountLabel, BorderLayout.EAST);
        p.add(title, BorderLayout.NORTH);

        ViewportPanel column = new ViewportPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        cartItemsPanel = column;
        cartItemsPanel.setBackground(Theme.WHITE);
        JScrollPane sc = new JScrollPane(cartItemsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getViewport().setBackground(Theme.WHITE);
        p.add(sc, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        JPanel total = new JPanel(new BorderLayout());
        total.setOpaque(false);
        total.add(Theme.label("Subtotal", 13, false, Theme.MUTED), BorderLayout.WEST);
        cartTotalLabel = Theme.label("₹0.00", 20, true, Theme.NAVY);
        total.add(cartTotalLabel, BorderLayout.EAST);
        bottom.add(total, BorderLayout.NORTH);
        JButton checkout = Theme.button("Continue to pickup  →", Theme.TEAL, Theme.WHITE);
        checkout.addActionListener(e -> checkout());
        bottom.add(checkout, BorderLayout.CENTER);
        JButton clear = Theme.button("Clear cart", Theme.BG, Theme.MUTED);
        clear.addActionListener(e -> { cart.clear(); refreshCart(); });
        bottom.add(clear, BorderLayout.SOUTH);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void refreshCart() {
        if (cartItemsPanel == null) return;
        cartItemsPanel.removeAll();
        int count = 0;
        double total = 0;
        for (Map.Entry<MenuItem, Integer> entry : new ArrayList<>(cart.entrySet())) {
            MenuItem item = entry.getKey();
            int qty = entry.getValue();
            double line = item.getPrice() * qty;
            count += qty;
            total += line;
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(Theme.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(10, 2, 10, 2)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel thumb = new JLabel(ImageCatalog.dish(item, 52, 52));
            thumb.setVerticalAlignment(SwingConstants.TOP);
            row.add(thumb, BorderLayout.WEST);
            JPanel details = new JPanel(new BorderLayout(0, 4));
            details.setOpaque(false);
            JPanel names = new JPanel(new GridLayout(0, 1, 0, 2));
            names.setOpaque(false);
            JLabel nm = Theme.label(item.getName(), 12, true, Theme.TEXT);
            nm.setToolTipText(item.getName());
            names.add(nm);
            names.add(Theme.label(money.format(item.getPrice()) + " each", 10, false, Theme.MUTED));
            details.add(names, BorderLayout.NORTH);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            controls.setOpaque(false);
            JButton minus = Theme.button("−", Theme.BG, Theme.NAVY), plus = Theme.button("+", Theme.PALE, Theme.TEAL);
            minus.setBorder(new EmptyBorder(4, 9, 4, 9));
            plus.setBorder(new EmptyBorder(4, 9, 4, 9));
            minus.addActionListener(e -> { if (qty <= 1) cart.remove(item); else cart.put(item, qty - 1); refreshCart(); });
            plus.addActionListener(e -> { cart.put(item, qty + 1); refreshCart(); });
            controls.add(minus);
            controls.add(Theme.label(String.valueOf(qty), 12, true, Theme.TEXT));
            controls.add(plus);
            JPanel bottomRow = new JPanel(new BorderLayout());
            bottomRow.setOpaque(false);
            bottomRow.add(controls, BorderLayout.WEST);
            bottomRow.add(Theme.label(money.format(line), 13, true, Theme.NAVY), BorderLayout.EAST);
            details.add(bottomRow, BorderLayout.SOUTH);
            row.add(details, BorderLayout.CENTER);
            cartItemsPanel.add(row);
        }
        if (cart.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBackground(Theme.WHITE);
            empty.setBorder(new EmptyBorder(55, 8, 20, 8));
            JLabel icon = Theme.label("🛍", 38, false, Theme.TEAL);
            JLabel msg = Theme.label("Your cart is empty", 16, true, Theme.NAVY);
            JLabel hint = Theme.label("<html><div style='text-align:center;width:205px'>Browse the menu and add your campus favourites. They will appear here.</div></html>", 11, false, Theme.MUTED);
            for (JComponent c : new JComponent[]{icon, msg, hint}) c.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(icon); empty.add(Box.createVerticalStrut(14)); empty.add(msg); empty.add(Box.createVerticalStrut(7)); empty.add(hint);
            cartItemsPanel.add(empty);
        }
        cartItemsPanel.add(Box.createVerticalGlue());
        cartCountLabel.setText(count + (count == 1 ? " item" : " items"));
        cartTotalLabel.setText(money.format(total));
        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JPanel adminPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(16, 14, 14, 14)));
        p.setPreferredSize(new Dimension(SIDE_W, 0));
        p.add(Theme.label("Today at a glance", 20, true, Theme.NAVY), BorderLayout.NORTH);
        adminBody = new JPanel(new BorderLayout(0, 14));
        adminBody.setOpaque(false);
        p.add(adminBody, BorderLayout.CENTER);
        return p;
    }

    private void refreshAdmin() {
        if (adminBody == null) return;
        adminBody.removeAll();
        CanteenService.Stats s = service.stats();
        JPanel stats = new JPanel(new GridLayout(2, 2, 10, 10));
        stats.setOpaque(false);
        stats.add(adminStat("TOTAL ORDERS", String.valueOf(s.totalOrders()), new Color(255, 237, 218)));
        stats.add(adminStat("ACTIVE ORDERS", String.valueOf(s.activeOrders()), new Color(255, 243, 229)));
        stats.add(adminStat("COMPLETED SALES", money.format(s.completedSales()), new Color(255, 240, 224)));
        stats.add(adminStat("PENDING REFUNDS", String.valueOf(s.pendingRefunds()), new Color(252, 232, 225)));
        adminBody.add(stats, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        RevenueChart revenueChart = new RevenueChart(service.getOrders(), money);
        revenueChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(revenueChart);
        details.add(Box.createVerticalStrut(12));

        JPanel recent = new JPanel();
        recent.setOpaque(false);
        recent.setLayout(new BoxLayout(recent, BoxLayout.Y_AXIS));
        recent.setAlignmentX(Component.LEFT_ALIGNMENT);
        recent.add(Theme.label("RECENT ORDERS", 11, true, Theme.TEAL));
        recent.add(Box.createVerticalStrut(8));
        List<Order> all = new ArrayList<>(service.getOrders());
        Collections.reverse(all);
        if (all.isEmpty()) recent.add(Theme.label("No orders yet.", 12, false, Theme.MUTED));
        for (Order o : all.subList(0, Math.min(6, all.size()))) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(Theme.label("#" + o.getId() + " · " + o.getCustomer(), 12, true, Theme.TEXT), BorderLayout.WEST);
            row.add(Theme.label(money.format(o.getTotal()) + " · " + statusText(o.getStatus()), 11, false, Theme.MUTED), BorderLayout.EAST);
            recent.add(row);
        }
        details.add(recent);
        JScrollPane detailsScroll = new JScrollPane(details);
        detailsScroll.setBorder(BorderFactory.createEmptyBorder());
        detailsScroll.setOpaque(false);
        detailsScroll.getViewport().setOpaque(false);
        detailsScroll.getVerticalScrollBar().setUnitIncrement(14);
        adminBody.add(detailsScroll, BorderLayout.CENTER);
        adminBody.revalidate();
        adminBody.repaint();
    }

    /** Seven-day completed-revenue chart. Values are calculated from real orders in this session. */
    private static final class RevenueChart extends JPanel {
        private final double[] daily = new double[7];
        private final String[] days = new String[7];
        private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale.Builder().setLanguage("en").setRegion("IN").build());
        private final Color orange = new Color(202, 82, 17);
        private double max;

        RevenueChart(List<Order> orders, NumberFormat ignored) {
            setOpaque(true);
            setBackground(Theme.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(10, 10, 8, 10)));
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("EE", Locale.ENGLISH);
            for (int i = 0; i < 7; i++) days[i] = today.minusDays(6 - i).format(fmt);
            for (Order order : orders) {
                if (order.getStatus() != Order.Status.COMPLETED) continue;
                java.time.LocalDate date = order.getCreatedDateTime().toLocalDate();
                long offset = java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(6), date);
                if (offset >= 0 && offset < 7) daily[(int) offset] += order.getTotal();
            }
            for (double amount : daily) max = Math.max(max, amount);
            setPreferredSize(new Dimension(260, 190));
            setMinimumSize(new Dimension(220, 190));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.NAVY);
            g.setFont(Theme.font(true, 12));
            g.drawString("REVENUE ANALYTICS", 10, 17);
            g.setFont(Theme.font(false, 10));
            g.setColor(Theme.MUTED);
            g.drawString("Completed sales · last 7 days", 10, 32);
            int left = 12, right = 8, top = 43, bottom = 28;
            int chartW = Math.max(40, w - left - right), chartH = Math.max(35, h - top - bottom);
            g.setStroke(new BasicStroke(1f));
            g.setColor(Theme.BORDER);
            for (int i = 0; i < 4; i++) {
                int y = top + i * chartH / 3;
                g.drawLine(left, y, left + chartW, y);
            }
            int slot = chartW / 7;
            for (int i = 0; i < 7; i++) {
                int barW = Math.max(7, Math.min(18, slot - 6));
                int x = left + i * slot + (slot - barW) / 2;
                int barH = max <= 0 ? 0 : (int) Math.round((daily[i] / max) * (chartH - 5));
                g.setColor(new Color(255, 224, 195));
                g.fillRoundRect(x, top + chartH - Math.max(2, barH), barW, Math.max(2, barH), 6, 6);
                g.setColor(orange);
                if (daily[i] > 0) g.fillRoundRect(x, top + chartH - Math.max(2, barH), barW, Math.max(2, barH), 6, 6);
                g.setColor(Theme.MUTED);
                g.setFont(Theme.font(false, 9));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(days[i], x + barW / 2 - fm.stringWidth(days[i]) / 2, top + chartH + 14);
            }
            g.setColor(Theme.NAVY);
            g.setFont(Theme.font(true, 10));
            String total = "7-day total: " + currency.format(java.util.Arrays.stream(daily).sum());
            g.drawString(total, 10, h - 6);
            g.dispose();
        }
    }

    private JPanel adminStat(String heading, String value, Color bg) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        p.add(Theme.label(heading, 10, true, Theme.MUTED));
        p.add(Theme.label(value, 20, true, Theme.NAVY));
        return p;
    }

    // =================================================================================================================
    //  Checkout, payment, receipt
    // =================================================================================================================
    private void checkout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Add at least one item to your cart first.", "Cart is empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (MenuItem m : cart.keySet()) {
            if (!m.isAvailable()) {
                JOptionPane.showMessageDialog(frame, m.getName() + " is sold out. Please remove it from your cart.", "Item unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        List<CanteenService.Offer> offers = service.getOffers();
        JComboBox<String> pickup = new JComboBox<>(SLOTS);
        JComboBox<String> offer = new JComboBox<>();
        offer.addItem("No offer");
        for (CanteenService.Offer o : offers) offer.addItem(o.label());
        int points = service.getLoyaltyPoints(user.getName());
        JCheckBox redeem = new JCheckBox("Redeem loyalty points (100 points = ₹10) · Available: " + points);
        redeem.setEnabled(points >= 100);
        JLabel summary = new JLabel();
        Runnable update = () -> {
            Quote q = service.quote(cart, selectedOfferCode(offer, offers), redeem.isSelected(), points);
            summary.setText("<html>Subtotal: " + money.format(q.subtotal())
                    + (q.offerDiscount() > 0 ? "<br>Festival offer: −" + money.format(q.offerDiscount()) : "")
                    + (q.pointsRedeemed() > 0 ? "<br>Points (" + q.pointsRedeemed() + "): −" + money.format(q.pointsDiscount()) : "")
                    + "<br><b>You pay: " + money.format(q.total()) + "</b></html>");
            summary.setFont(Theme.font(false, 13));
        };
        offer.addActionListener(e -> update.run());
        redeem.addActionListener(e -> update.run());
        update.run();

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.add(Theme.label("Choose your pickup time", 14, true, Theme.NAVY));
        form.add(pickup);
        form.add(Theme.label("Festival offer / coupon", 13, true, Theme.NAVY));
        form.add(offer);
        form.add(redeem);
        form.add(summary);
        if (JOptionPane.showConfirmDialog(frame, form, "Pickup, offers & rewards", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String code = selectedOfferCode(offer, offers);
        Quote q = service.quote(cart, code, redeem.isSelected(), points);
        CanteenService.Offer applied = service.findOffer(code);
        String label = applied != null && q.offerDiscount() > 0 ? applied.code() + " - " + applied.title() : "No offer";
        showPayment((String) pickup.getSelectedItem(), label, q);
    }

    private String selectedOfferCode(JComboBox<String> box, List<CanteenService.Offer> offers) {
        int i = box.getSelectedIndex();
        return i <= 0 ? null : offers.get(i - 1).code();
    }

    private void showFestivalOffers() {
        StringBuilder sb = new StringBuilder();
        for (CanteenService.Offer o : service.getOffers())
            sb.append(o.title().toUpperCase(Locale.ROOT)).append('\n').append(o.code()).append(" · ").append(o.percent()).append("% off orders of ₹").append((int) o.minSpend()).append(" or more\n\n");
        sb.append("Select one coupon during checkout. Offers are demo promotions and only one coupon can be used per order.");
        JOptionPane.showMessageDialog(frame, sb.toString(), "CampusBite · Festival offers", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLoyalty() {
        int pts = service.getLoyaltyPoints(user.getName());
        JOptionPane.showMessageDialog(frame, "CAMPUSBITE LOYALTY REWARDS\n\nYour current balance: " + pts + " points\nEarn 1 point for every ₹10 spent after discounts.\nRedeem 100 points for ₹10 off at checkout.\nCancelled or refunded orders give redeemed points back and remove the points they earned.\n\nPoints are stored for this app session in this prototype.",
                "Loyalty rewards", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPayment(String pickup, String offerLabel, Quote quote) {
        JPanel panel = new JPanel(new BorderLayout(18, 12));
        panel.setBackground(Theme.WHITE);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        panel.setPreferredSize(new Dimension(650, 350));
        JPanel options = new JPanel(new GridLayout(0, 1, 9, 9));
        options.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        JRadioButton upi = paymentOption("UPI / Scan QR", "Scan with any UPI app", new Color(255, 237, 218), true);
        JRadioButton card = paymentOption("Card Payment", "Debit / Credit card (demo)", new Color(255, 243, 229), false);
        JRadioButton counter = paymentOption("Pay at Counter", "Pay when collecting your order", new Color(255, 240, 224), false);
        JRadioButton token = paymentOption("Token Number", "Reserve now and pay at the counter", new Color(255, 243, 229), false);
        for (JRadioButton b : new JRadioButton[]{upi, card, counter, token}) { group.add(b); options.add(b); }
        panel.add(options, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel qr = new JLabel(ImageCatalog.qr(155));
        qr.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(qr);
        right.add(Box.createVerticalStrut(8));
        right.add(Theme.label("ORDER TOTAL", 11, true, Theme.TEAL));
        right.add(Theme.label(money.format(quote.total()), 24, true, Theme.NAVY));
        right.add(Box.createVerticalStrut(5));
        right.add(Theme.label("Saved: " + money.format(quote.discount()), 12, true, Theme.TEAL));
        right.add(Theme.label(offerLabel, 10, false, Theme.MUTED));
        right.add(Box.createVerticalStrut(8));
        right.add(Theme.label("Pickup: " + pickup, 12, false, Theme.MUTED));
        panel.add(right, BorderLayout.EAST);
        java.awt.event.ActionListener toggleQr = e -> qr.setVisible(upi.isSelected());   // the QR only makes sense for UPI
        for (JRadioButton b : new JRadioButton[]{upi, card, counter, token}) b.addActionListener(toggleQr);
        panel.add(Theme.label("DEMO CHECKOUT · No real payment is processed", 11, true, Theme.GOLD), BorderLayout.SOUTH);

        if (JOptionPane.showConfirmDialog(frame, panel, "Choose payment method", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String method = upi.isSelected() ? "UPI (demo)" : card.isSelected() ? "Card (demo)" : counter.isSelected() ? "Pay at counter" : "Token number / pay at counter";
        Order order;
        try {
            order = service.placeOrder(user, pickup, new LinkedHashMap<>(cart), quote, offerLabel, method);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Could not place order", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cart.clear();
        refreshSide();
        showReceipt(order);
    }

    private JRadioButton paymentOption(String title, String subtitle, Color bg, boolean selected) {
        JRadioButton b = new JRadioButton("<html><b>" + title + "</b><br><span style='font-size:11px;color:#697689'>" + subtitle + "</span></html>", selected);
        b.setFont(Theme.font(false, 14));
        b.setForeground(Theme.NAVY);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(12, 12, 12, 12));
        return b;
    }

    private void showReceipt(Order order) {
        // Use a real dialog with a scrollable receipt instead of JOptionPane's fixed-height
        // content area, which clipped the last rows on Windows/high-DPI displays.
        JDialog dialog = new JDialog(frame, "CampusBite · Order confirmed", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(560, 560));

        JPanel receipt = new JPanel(new BorderLayout(0, 16));
        receipt.setBackground(Theme.WHITE);
        receipt.setBorder(new EmptyBorder(24, 28, 20, 28));

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 7));
        head.setOpaque(false);
        JLabel check = Theme.label("✓", 38, true, Theme.SUCCESS);
        JLabel title = Theme.label("ORDER CONFIRMED!", 25, true, Theme.NAVY);
        JLabel subtitle = Theme.label("Your order has been placed successfully.", 14, false, Theme.MUTED);
        for (JLabel label : new JLabel[]{check, title, subtitle}) {
            label.setHorizontalAlignment(SwingConstants.CENTER);
            head.add(label);
        }
        receipt.add(head, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(new Color(245, 249, 250));
        details.setBorder(new EmptyBorder(12, 16, 12, 16));
        String[][] rows = {
                {"ORDER ID", "#" + order.getId()},
                {"TOKEN NUMBER", order.getTokenNumber()},
                {"PICKUP TIME", order.getPickup()},
                {"PAYMENT", order.getPaymentMethod()},
                {"OFFER / REWARD", order.getOfferName() + " · " + order.getPointsRedeemed() + " pts redeemed"},
                {"TOTAL", money.format(order.getTotal())},
                {"POINTS EARNED", order.getPointsEarned() + " points"}
        };
        for (int i = 0; i < rows.length; i++) {
            GridBagConstraints left = new GridBagConstraints();
            left.gridx = 0; left.gridy = i; left.weightx = 0; left.anchor = GridBagConstraints.NORTHWEST;
            left.insets = new Insets(8, 0, 8, 14);
            JLabel key = Theme.label(rows[i][0], 11, true, Theme.MUTED);
            key.setPreferredSize(new Dimension(118, 22));
            details.add(key, left);

            GridBagConstraints right = new GridBagConstraints();
            right.gridx = 1; right.gridy = i; right.weightx = 1; right.fill = GridBagConstraints.HORIZONTAL;
            right.anchor = GridBagConstraints.NORTHWEST;
            right.insets = new Insets(6, 0, 6, 0);
            String valueText = Theme.esc(rows[i][1]);
            JLabel value = Theme.label("<html><div style='width:280px'>" + valueText + "</div></html>",
                    i == 1 ? 19 : 13, true, i == 1 ? Theme.SUCCESS : Theme.NAVY);
            value.setVerticalAlignment(SwingConstants.TOP);
            details.add(value, right);
        }
        JScrollPane detailScroll = new JScrollPane(details);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        receipt.add(detailScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setBackground(Theme.WHITE);
        JButton track = Theme.button("Track my order", Theme.TEAL, Theme.WHITE);
        JButton done = Theme.button("Done", new Color(245, 245, 242), Theme.NAVY);
        track.addActionListener(e -> { dialog.dispose(); showTrackingDialog(order); });
        done.addActionListener(e -> dialog.dispose());
        actions.add(track);
        actions.add(done);
        receipt.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(receipt);
        dialog.setSize(620, 650);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    // =================================================================================================================
    //  Orders, refunds, tracking
    // =================================================================================================================
    private void showOrders() {
        List<Order> visible = new ArrayList<>();
        for (Order o : service.getOrders()) if (isAdmin() || o.getCustomer().equalsIgnoreCase(user.getName())) visible.add(o);
        Collections.reverse(visible);                                           // newest first
        if (visible.isEmpty()) {
            JOptionPane.showMessageDialog(frame, isAdmin() ? "No orders yet." : "You have no orders yet. Place an order to see tracking here.", "Order tracking", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultTableModel model = new DefaultTableModel(new String[]{"Order", "Token", "Customer", "Items", "Pickup", "Total", "Payment", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Order o : visible) model.addRow(new Object[]{"#" + o.getId(), o.getTokenNumber(), o.getCustomer(), o.getItemsSummary(), o.getPickup(),
                money.format(o.getTotal()), o.getPaymentMethod(), statusText(o.getStatus())});
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {70, 75, 100, 230, 140, 85, 150, 140};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(940, 390));
        panel.add(Theme.label(isAdmin() ? "Order management" : "My orders & refunds", 19, true, Theme.NAVY), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (isAdmin()) {
            JComboBox<Order.Status> status = new JComboBox<>();
            status.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                    return super.getListCellRendererComponent(l, v == null ? "" : statusText((Order.Status) v), i, s, f);
                }
            });
            JButton update = Theme.button("Update status", Theme.TEAL, Theme.WHITE);
            update.setEnabled(false);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                status.removeAllItems();
                int r = table.getSelectedRow();
                if (r >= 0) for (Order.Status s : service.nextStatuses(visible.get(r))) status.addItem(s);
                update.setEnabled(status.getItemCount() > 0);
            });
            update.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0 || status.getSelectedItem() == null) { JOptionPane.showMessageDialog(frame, "Select an order first."); return; }
                Order o = visible.get(row);
                Order.Status next = (Order.Status) status.getSelectedItem();
                if (next == Order.Status.REFUNDED && JOptionPane.showConfirmDialog(frame, "Confirm marking this demo order as refunded? No real money will be transferred.", "Confirm demo refund", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                String error = service.updateStatus(o, next);
                if (error != null) { JOptionPane.showMessageDialog(frame, error, "Status not changed", JOptionPane.WARNING_MESSAGE); return; }
                model.setValueAt(statusText(o.getStatus()), row, 7);
                table.clearSelection();
                table.setRowSelectionInterval(row, row);
                refreshSide();
            });
            actions.add(Theme.label("Move selected order to:", 12, true, Theme.TEXT));
            actions.add(status);
            actions.add(update);
        } else {
            JButton track = Theme.button("Track selected order", new Color(255, 237, 218), Theme.TEAL);
            track.addActionListener(e -> { int row = table.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(frame, "Select an order to track."); return; } showTrackingDialog(visible.get(row)); });
            JButton cancel = Theme.button("Cancel selected order", Theme.DANGER_BG, Theme.DANGER);
            cancel.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(frame, "Select an order first."); return; }
                Order o = visible.get(row);
                if (o.getStatus() != Order.Status.PLACED) {
                    JOptionPane.showMessageDialog(frame, "Orders can only be cancelled while their status is 'Order placed'. Please contact the canteen for help with later orders.", "Cannot cancel", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                JPanel confirmPanel = new JPanel(new BorderLayout(10, 12));
                confirmPanel.setBorder(new EmptyBorder(18, 20, 18, 20));
                confirmPanel.setPreferredSize(new Dimension(460, 230));
                confirmPanel.add(Theme.label("CANCEL THIS ORDER?", 22, true, Theme.DANGER), BorderLayout.NORTH);
                confirmPanel.add(Theme.multiline("Order #" + o.getId() + "\n" + o.getItemsSummary() + "\nTotal: " + money.format(o.getTotal()) + "\n\nThis action cannot be undone. Refunds are demo requests only.", 14, false, Theme.TEXT), BorderLayout.CENTER);
                if (JOptionPane.showConfirmDialog(frame, confirmPanel, "Confirm order cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
                String error = service.updateStatus(o, Order.Status.CANCELLED);
                if (error != null) { JOptionPane.showMessageDialog(frame, error); return; }
                model.setValueAt(statusText(o.getStatus()), row, 7);
                refreshAccountButtons();
                JOptionPane.showMessageDialog(frame, "Order #" + o.getId() + " cancelled. You may now submit a refund request if needed.", "Order cancelled", JOptionPane.INFORMATION_MESSAGE);
            });
            JButton refund = Theme.button("Request refund", Theme.PALE, Theme.TEAL);
            refund.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(frame, "Select an order first."); return; }
                Order o = visible.get(row);
                if (o.getStatus() != Order.Status.CANCELLED) { JOptionPane.showMessageDialog(frame, "Refund requests are available for cancelled orders only.", "Refund unavailable", JOptionPane.WARNING_MESSAGE); return; }
                if (JOptionPane.showConfirmDialog(frame, "Submit a refund request for order #" + o.getId() + "? This is a demo request; no money will be transferred.", "Request refund", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                String error = service.updateStatus(o, Order.Status.REFUND_REQUESTED);
                if (error != null) { JOptionPane.showMessageDialog(frame, error); return; }
                model.setValueAt(statusText(o.getStatus()), row, 7);
                JOptionPane.showMessageDialog(frame, "Refund request submitted for admin review. This demo does not issue real refunds.");
            });
            actions.add(track); actions.add(cancel); actions.add(refund);
        }
        panel.add(actions, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(frame, panel, isAdmin() ? "Admin · Order management" : "CampusBite · My orders", JOptionPane.PLAIN_MESSAGE);
        refreshSide();
    }

    private void showRefundRequests() {
        if (!isAdmin()) { JOptionPane.showMessageDialog(frame, "Administrator access is required.", "Access denied", JOptionPane.ERROR_MESSAGE); return; }
        List<Order> refunds = new ArrayList<>();
        for (Order o : service.getOrders()) if (o.getStatus() == Order.Status.REFUND_REQUESTED) refunds.add(o);
        if (refunds.isEmpty()) { JOptionPane.showMessageDialog(frame, "There are no pending refund requests.", "Refund management", JOptionPane.INFORMATION_MESSAGE); return; }
        DefaultTableModel model = new DefaultTableModel(new String[]{"Order", "Customer", "Items", "Total", "Payment", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Order o : refunds) model.addRow(new Object[]{"#" + o.getId(), o.getCustomer(), o.getItemsSummary(), money.format(o.getTotal()), o.getPaymentMethod(), statusText(o.getStatus())});
        JTable table = new JTable(model);
        table.setRowHeight(30);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(760, 330));
        panel.add(Theme.label("REFUND REQUESTS · ADMIN REVIEW", 18, true, Theme.NAVY), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approve = Theme.button("Approve & mark refunded (demo)", Theme.TEAL, Theme.WHITE);
        JButton reject = Theme.button("Reject request", Theme.DANGER_BG, Theme.DANGER);
        approve.addActionListener(e -> resolveRefund(table, model, refunds, panel, true));
        reject.addActionListener(e -> resolveRefund(table, model, refunds, panel, false));
        actions.add(reject);
        actions.add(approve);
        panel.add(actions, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(frame, panel, "CampusBite · Refund management", JOptionPane.PLAIN_MESSAGE);
        refreshSide();
    }

    private void resolveRefund(JTable table, DefaultTableModel model, List<Order> refunds, JPanel panel, boolean approve) {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(panel, "Select a refund request first."); return; }
        Order o = refunds.get(r);
        String question = approve ? "Mark order #" + o.getId() + " as refunded? This records a demo status only; no money is transferred."
                : "Reject refund request for order #" + o.getId() + "?";
        if (JOptionPane.showConfirmDialog(panel, question, approve ? "Confirm demo refund" : "Reject refund", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        String error = service.updateStatus(o, approve ? Order.Status.REFUNDED : Order.Status.CANCELLED);
        if (error != null) { JOptionPane.showMessageDialog(panel, error); return; }
        model.removeRow(r);                                                     // handled requests leave the list
        refunds.remove(r);
        JOptionPane.showMessageDialog(panel, approve ? "Refund marked as completed in the demo." : "Refund request rejected. Order remains cancelled.");
    }

    private void showTrackingDialog(Order o) {
        JPanel p = new JPanel(new BorderLayout(12, 14));
        p.setBackground(Theme.WHITE);
        p.setBorder(new EmptyBorder(22, 24, 22, 24));
        p.setPreferredSize(new Dimension(520, 340));
        JPanel head = new JPanel(new GridLayout(0, 1, 0, 6));
        head.setOpaque(false);
        head.add(Theme.label("TRACK YOUR ORDER", 22, true, Theme.NAVY));
        head.add(Theme.label("Order #" + o.getId() + "   •   Token " + o.getTokenNumber(), 13, true, Theme.TEAL));
        p.add(head, BorderLayout.NORTH);
        JPanel stages = new JPanel(new GridLayout(0, 1, 8, 8));
        stages.setBackground(new Color(245, 248, 251));
        stages.setBorder(new EmptyBorder(16, 18, 16, 18));
        String[] names = {"1. Order placed", "2. Preparing", "3. Ready for pickup", "4. Completed"};
        int current = switch (o.getStatus()) { case PLACED -> 0; case PREPARING -> 1; case READY_FOR_PICKUP -> 2; case COMPLETED -> 3; default -> -1; };
        for (int i = 0; i < names.length; i++) stages.add(Theme.label((i <= current ? "●   " : "○   ") + names[i], 15, true, i <= current ? Theme.SUCCESS : Theme.MUTED));
        if (current < 0) stages.add(Theme.label("Current status: " + statusText(o.getStatus()), 14, true, new Color(173, 65, 55)));
        p.add(stages, BorderLayout.CENTER);
        JPanel foot = new JPanel(new GridLayout(0, 1, 5, 5));
        foot.setOpaque(false);
        foot.add(Theme.label("Pickup: " + o.getPickup(), 12, false, Theme.TEXT));
        foot.add(Theme.label("Payment: " + o.getPaymentMethod() + "   |   Total: " + money.format(o.getTotal()), 12, true, Theme.NAVY));
        p.add(foot, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(frame, p, "CampusBite · Live demo tracking", JOptionPane.PLAIN_MESSAGE);
    }

    private String statusText(Order.Status s) {
        return switch (s) {
            case PLACED -> "Order placed"; case PREPARING -> "Preparing"; case READY_FOR_PICKUP -> "Ready for pickup"; case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled"; case REFUND_REQUESTED -> "Refund requested"; case REFUNDED -> "Refunded (demo)";
        };
    }
}
