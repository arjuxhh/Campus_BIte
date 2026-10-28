package com.campusbite.model;

import java.util.Locale;

/** A dish on the canteen menu. {@code imagePath} is optional: an admin-chosen photo on disk. */
public class MenuItem {
    private final int id;
    private final String name, category, description, emoji, imagePath;
    private final double price;
    private boolean available = true;

    public MenuItem(int id, String name, String category, double price, String description, String emoji) {
        this(id, name, category, price, description, emoji, null);
    }

    public MenuItem(int id, String name, String category, double price, String description, String emoji, String imagePath) {
        this.id = id; this.name = name; this.category = category; this.price = price;
        this.description = description; this.emoji = emoji; this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public String getEmoji() { return emoji; }
    public String getImagePath() { return imagePath; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { available = value; }

    /** File-name friendly id, e.g. "Idli & Sambar" -> "idli-sambar". Photos in assets/dishes use this name. */
    public String getSlug() {
        return name.toLowerCase(Locale.ROOT).replace("&", " ").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
