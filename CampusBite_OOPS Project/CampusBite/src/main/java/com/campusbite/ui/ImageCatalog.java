package com.campusbite.ui;

import com.campusbite.model.MenuItem;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Single place that finds, crops, scales and caches every picture.
 *
 * Asset folders (src/main/resources/assets):
 *   branding/   app logo (256 px master)
 *   payments/   demo UPI QR
 *   dishes/     one photo per dish, named after the dish: idli-sambar.jpg, masala-dosa.jpg ...
 *
 * A dish picture is looked up as: photo chosen by the admin -> assets/dishes/<slug>.jpg|png -> generated placeholder.
 * Photos are centre-cropped (never stretched) and cached at 1x and 2x so they stay sharp on high-DPI screens.
 */
public final class ImageCatalog {
    private static final String DISHES = "/assets/dishes/";
    private static final Map<String, Optional<BufferedImage>> RAW = new HashMap<>();
    private static final Map<String, ImageIcon> ICONS = new HashMap<>();
    private static final Map<String, BufferedImage> PHOTOS = new HashMap<>();
    private static final Map<String, Color[]> TINTS = Map.of(
            "Breakfast", new Color[]{new Color(255, 239, 214), new Color(255, 190, 115), new Color(170, 83, 8)},
            "Lunch", new Color[]{new Color(255, 238, 216), new Color(250, 190, 125), new Color(171, 83, 10)},
            "Snacks", new Color[]{new Color(255, 231, 211), new Color(250, 173, 125), new Color(169, 70, 25)},
            "Drinks", new Color[]{new Color(255, 239, 220), new Color(250, 197, 137), new Color(174, 91, 12)});
    private static final Color[] DEFAULT_TINT = {new Color(246, 244, 238), new Color(226, 218, 204), Theme.NAVY};

    private ImageCatalog() { }

    public static ImageIcon logo(int size) { return cached("logo|" + size, "/assets/branding/logo-256.png", size, size); }

    public static ImageIcon qr(int size) { return cached("qr|" + size, "/assets/payments/demo-upi-qr.png", size, size); }

    public static ImageIcon dish(MenuItem item, int w, int h) {
        String key = "dish|" + item.getSlug() + "|" + item.getImagePath() + "|" + w + "x" + h;
        ImageIcon icon = ICONS.get(key);
        if (icon == null) {
            BufferedImage src = dishSource(item);
            icon = src != null ? icon(src, w, h)
                    : new ImageIcon(new BaseMultiResolutionImage(placeholder(item, w, h), placeholder(item, w * 2, h * 2)));
            ICONS.put(key, icon);
        }
        return icon;
    }

    /**
     * The dish picture at working size (max 800 px wide): the real photo, or a generated tile when there is none.
     * Cards crop it to whatever width the responsive grid gives them, so one cached image serves every window size.
     */
    public static BufferedImage dishImage(MenuItem item) {
        String key = "photo|" + item.getSlug() + "|" + item.getImagePath();
        BufferedImage img = PHOTOS.get(key);
        if (img == null) {
            BufferedImage src = dishSource(item);
            if (src == null) img = placeholder(item, 640, 400);
            else img = src.getWidth() > 800 ? scale(src, 800, Math.max(1, src.getHeight() * 800 / src.getWidth())) : src;
            PHOTOS.put(key, img);
        }
        return img;
    }

    /** True when a real photo exists for this dish (otherwise a generated placeholder is shown). */
    public static boolean hasPhoto(MenuItem item) { return dishSource(item) != null; }

    // ---- loading ------------------------------------------------------------------------------------------------
    private static ImageIcon cached(String key, String resource, int w, int h) {
        ImageIcon icon = ICONS.get(key);
        if (icon == null) {
            BufferedImage src = resource(resource);
            icon = src == null ? new ImageIcon() : icon(src, w, h);
            ICONS.put(key, icon);
        }
        return icon;
    }

    private static BufferedImage dishSource(MenuItem item) {
        if (item.getImagePath() != null) {
            BufferedImage chosen = file(item.getImagePath());
            if (chosen != null) return chosen;
        }
        BufferedImage jpg = resource(DISHES + item.getSlug() + ".jpg");
        return jpg != null ? jpg : resource(DISHES + item.getSlug() + ".png");
    }

    private static BufferedImage resource(String path) {
        return RAW.computeIfAbsent(path, p -> {
            try (InputStream in = ImageCatalog.class.getResourceAsStream(p)) {
                return in == null ? Optional.<BufferedImage>empty() : Optional.ofNullable(ImageIO.read(in));
            } catch (IOException e) { return Optional.empty(); }
        }).orElse(null);
    }

    private static BufferedImage file(String path) {
        return RAW.computeIfAbsent("file:" + path, p -> {
            try { return Optional.ofNullable(ImageIO.read(new File(path))); } catch (IOException e) { return Optional.empty(); }
        }).orElse(null);
    }

    // ---- scaling ------------------------------------------------------------------------------------------------
    private static ImageIcon icon(BufferedImage src, int w, int h) {
        return new ImageIcon(new BaseMultiResolutionImage(cover(src, w, h), cover(src, w * 2, h * 2)));
    }

    /** Fill w x h by cropping the centre of the source (same idea as CSS object-fit: cover). */
    static BufferedImage cover(BufferedImage src, int w, int h) {
        int sw = src.getWidth(), sh = src.getHeight();
        double target = (double) w / h, ratio = (double) sw / sh;
        int cw, ch;
        if (ratio > target) { ch = sh; cw = (int) Math.round(sh * target); } else { cw = sw; ch = (int) Math.round(sw / target); }
        return scale(src.getSubimage((sw - cw) / 2, (sh - ch) / 2, Math.max(1, cw), Math.max(1, ch)), w, h);
    }

    /** Down-scales in halving steps, which keeps big photos (like the old 1254 px logo) crisp instead of jagged. */
    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) cur = step(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        return step(cur, w, h);
    }

    private static BufferedImage step(BufferedImage src, int w, int h) {
        int type = src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // ---- placeholder for dishes without a photo -----------------------------------------------------------------
    private static BufferedImage placeholder(MenuItem item, int w, int h) {
        Color[] tint = TINTS.getOrDefault(item.getCategory(), DEFAULT_TINT);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, tint[0], w, h, tint[1]));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillOval(-h / 3, -h / 3, h, h);
        g.fillOval(w - h / 2, h - h / 2, h, h);
        Font emoji = emojiFont(item.getEmoji(), (int) (h * 0.42));
        if (emoji != null) {
            g.setFont(emoji);
            g.setColor(tint[2]);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(item.getEmoji(), (w - fm.stringWidth(item.getEmoji())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        } else {                                              // no emoji font installed: draw a monogram instead of empty boxes
            int d = (int) (h * 0.52);
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval((w - d) / 2, (h - d) / 2, d, d);
            g.setColor(tint[2]);
            g.setFont(new Font("SansSerif", Font.BOLD, d / 2));
            FontMetrics fm = g.getFontMetrics();
            String s = initials(item.getName());
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
        return img;
    }

    private static Font emojiFont(String glyph, int size) {
        if (glyph == null || glyph.isBlank()) return null;
        for (String name : new String[]{"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Noto Emoji", "Symbola"}) {
            Font f = new Font(name, Font.PLAIN, size);
            if (f.getFamily().equalsIgnoreCase(name) && f.canDisplayUpTo(glyph) == -1) return f;
        }
        return null;
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("[\\s&]+")) if (!part.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(part.charAt(0)));
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
