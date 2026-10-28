package com.campusbite.service;

import com.campusbite.model.*;
import java.util.*;

import static com.campusbite.model.Order.Status.*;

/** Menu, offers, loyalty points and order life-cycle. All money/points rules live here (single source of truth). */
public class CanteenService {
    public record Offer(String code, String title, int percent, double minSpend) {
        public String label() { return code + " - " + title + " (" + percent + "% off ₹" + (int) minSpend + "+)"; }
    }

    public record Quote(double subtotal, double offerDiscount, int pointsRedeemed, double pointsDiscount) {
        public double discount() { return offerDiscount + pointsDiscount; }
        public double total() { return Math.max(0, round2(subtotal - discount())); }
    }

    public record Stats(int totalOrders, int activeOrders, double completedSales, int pendingRefunds) { }

    private static final List<Offer> OFFERS = List.of(
            new Offer("ONAM10", "Onam Sadya Special", 10, 100),
            new Offer("SATTVA15", "Sattva Fest", 15, 80),
            new Offer("TECH20", "Tech Fest", 20, 150));

    private static final Map<Order.Status, Set<Order.Status>> NEXT = new EnumMap<>(Order.Status.class);
    static {
        NEXT.put(PLACED, EnumSet.of(PREPARING, CANCELLED));
        NEXT.put(PREPARING, EnumSet.of(READY_FOR_PICKUP, CANCELLED));
        NEXT.put(READY_FOR_PICKUP, EnumSet.of(COMPLETED, CANCELLED));
        NEXT.put(COMPLETED, EnumSet.noneOf(Order.Status.class));
        NEXT.put(CANCELLED, EnumSet.of(REFUND_REQUESTED));
        NEXT.put(REFUND_REQUESTED, EnumSet.of(REFUNDED, CANCELLED));
        NEXT.put(REFUNDED, EnumSet.noneOf(Order.Status.class));
    }

    private final List<MenuItem> menu = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final Map<String, Integer> loyaltyPoints = new HashMap<>();
    private int nextOrderId = 1001;
    private int nextMenuId = 17;

    public CanteenService() {
        add(1, "Malabar Chicken Biriyani", "Lunch", 120, "Aromatic Kerala-style biriyani", "🍛");
        add(2, "Kerala Meals", "Lunch", 70, "Homestyle rice and curry", "🍚");
        add(3, "Idli & Sambar", "Breakfast", 30, "Soft idli with sambar", "🥣");
        add(4, "Masala Dosa", "Breakfast", 45, "Crispy dosa with potato masala", "🥞");
        add(5, "Kerala Porotta & Curry", "Lunch", 65, "Flaky porotta with curry", "🫓");
        add(6, "Chicken Cutlet", "Snacks", 20, "Crispy savoury snack", "🍗");
        add(7, "Pazham Pori", "Snacks", 20, "Kerala banana fritter", "🍌");
        add(8, "Egg Puffs", "Snacks", 20, "Flaky puff with egg filling", "🥐");
        add(9, "Parippu Vada", "Snacks", 10, "Crunchy lentil fritter", "🟠");
        add(10, "Sulaimani Tea", "Drinks", 10, "Refreshing black tea", "🍵");
        add(11, "Filter Coffee", "Drinks", 15, "Fresh filter coffee", "☕");
        add(12, "Milk Tea", "Drinks", 10, "Classic milk tea", "🫖");
        add(13, "Samosa", "Snacks", 12, "Crispy potato-filled samosa", "🥟");
        add(14, "Chapati & Chicken Curry", "Lunch", 70, "Chapati with chicken curry", "🍽️");
        add(15, "Bread Omelette", "Breakfast", 30, "Toasted bread and omelette", "🍳");
        add(16, "Veg Fried Rice", "Lunch", 60, "Vegetable fried rice", "🥡");
    }

    private void add(int id, String name, String cat, double price, String desc, String emoji) {
        menu.add(new MenuItem(id, name, cat, price, desc, emoji));
    }

    // ---- menu -------------------------------------------------------------------------------------------------
    public List<MenuItem> getMenu() { return Collections.unmodifiableList(menu); }

    public MenuItem addMenuItem(String name, String category, double price, String description, String emoji, String imagePath) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > 40) throw new IllegalArgumentException("Enter an item name (up to 40 characters).");
        if (!Double.isFinite(price) || price <= 0 || price > 5000) throw new IllegalArgumentException("Enter a price between ₹1 and ₹5000.");
        for (MenuItem m : menu) if (m.getName().equalsIgnoreCase(name)) throw new IllegalArgumentException("\"" + name + "\" is already on the menu.");
        String desc = description == null || description.isBlank() ? "Freshly prepared on campus" : description.trim();
        MenuItem item = new MenuItem(nextMenuId++, name, category, round2(price), desc, emoji, imagePath);
        menu.add(item);
        return item;
    }

    public void setAvailable(MenuItem item, boolean available) { item.setAvailable(available); }

    // ---- offers, loyalty, pricing --------------------------------------------------------------------------------
    public List<Offer> getOffers() { return OFFERS; }

    public Offer findOffer(String code) {
        for (Offer o : OFFERS) if (o.code().equals(code)) return o;
        return null;
    }

    public int getLoyaltyPoints(String name) { return loyaltyPoints.getOrDefault(key(name), 0); }

    /** Price a cart. Points are redeemed in blocks of 100 (= ₹10) and never beyond the amount payable. */
    public Quote quote(Map<MenuItem, Integer> cart, String offerCode, boolean redeem, int pointsBalance) {
        double subtotal = cart.entrySet().stream().mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
        Offer offer = findOffer(offerCode);
        double offerDiscount = offer != null && subtotal >= offer.minSpend() ? round2(subtotal * offer.percent() / 100.0) : 0;
        double payable = subtotal - offerDiscount;
        int blocks = redeem ? Math.min(Math.max(0, pointsBalance) / 100, (int) Math.floor(payable / 10.0)) : 0;
        return new Quote(round2(subtotal), offerDiscount, blocks * 100, blocks * 10.0);
    }

    // ---- orders -----------------------------------------------------------------------------------------------
    public List<Order> getOrders() { return Collections.unmodifiableList(orders); }

    public Order placeOrder(User user, String pickup, Map<MenuItem, Integer> cart, Quote quote, String offerLabel, String paymentMethod) {
        if (cart.isEmpty()) throw new IllegalStateException("Your cart is empty.");
        for (MenuItem m : cart.keySet()) if (!m.isAvailable()) throw new IllegalStateException(m.getName() + " is sold out. Remove it from your cart.");
        int balance = getLoyaltyPoints(user.getName());
        if (quote.pointsRedeemed() > balance) throw new IllegalStateException("Not enough loyalty points.");
        Order o = new Order(nextOrderId++, user.getName(), user.getRole(), pickup, cart, quote.discount(), quote.pointsRedeemed(), offerLabel);
        o.setPaymentMethod(paymentMethod);
        orders.add(o);
        loyaltyPoints.put(key(user.getName()), balance - o.getPointsRedeemed() + o.getPointsEarned());
        return o;
    }

    public Set<Order.Status> nextStatuses(Order o) { return NEXT.get(o.getStatus()); }

    /** Moves an order along its life-cycle. Returns null on success or a message explaining why it is not allowed. */
    public String updateStatus(Order o, Order.Status next) {
        if (!nextStatuses(o).contains(next)) return "An order that is \"" + label(o.getStatus()) + "\" cannot be changed to \"" + label(next) + "\".";
        o.setStatus(next);
        if ((next == CANCELLED || next == REFUNDED) && !o.isPointsReversed()) {   // give back redeemed points, take back earned ones
            String k = key(o.getCustomer());
            int balance = loyaltyPoints.getOrDefault(k, 0) + o.getPointsRedeemed() - o.getPointsEarned();
            loyaltyPoints.put(k, Math.max(0, balance));
            o.markPointsReversed();
        }
        return null;
    }

    public Stats stats() {
        double sales = 0; int active = 0, refunds = 0;
        for (Order o : orders) {
            switch (o.getStatus()) {
                case COMPLETED -> sales += o.getTotal();
                case PLACED, PREPARING, READY_FOR_PICKUP -> active++;
                case REFUND_REQUESTED -> refunds++;
                default -> { }
            }
        }
        return new Stats(orders.size(), active, sales, refunds);
    }

    private static String label(Order.Status s) { return s.name().replace('_', ' ').toLowerCase(Locale.ROOT); }
    private static String key(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT); }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
