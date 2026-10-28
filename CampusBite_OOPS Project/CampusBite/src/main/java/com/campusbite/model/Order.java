package com.campusbite.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Order {
    public enum Status { PLACED, PREPARING, READY_FOR_PICKUP, COMPLETED, CANCELLED, REFUND_REQUESTED, REFUNDED }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");
    private final int id;
    private final String customer, role, pickup, offerName, tokenNumber;
    private final Map<MenuItem, Integer> items;
    private final double subtotal, discount, total;
    private final int pointsRedeemed;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private Status status = Status.PLACED;
    private String paymentMethod = "UPI";
    private boolean pointsReversed;

    public Order(int id, String customer, String role, String pickup, Map<MenuItem, Integer> items) {
        this(id, customer, role, pickup, items, 0, 0, "No offer");
    }

    public Order(int id, String customer, String role, String pickup, Map<MenuItem, Integer> items,
                 double discount, int pointsRedeemed, String offerName) {
        this.id = id; this.customer = customer; this.role = role; this.pickup = pickup;
        this.items = new LinkedHashMap<>(items);
        this.subtotal = items.entrySet().stream().mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
        this.discount = Math.max(0, Math.min(discount, subtotal));
        this.pointsRedeemed = Math.max(0, pointsRedeemed);
        this.offerName = offerName == null ? "No offer" : offerName;
        this.total = Math.max(0, subtotal - this.discount);
        // Unique for the whole session (the old id % 1000 repeated after 1000 orders).
        this.tokenNumber = "CB-" + String.format("%03d", Math.max(1, id - 1000));
    }

    public int getId() { return id; }
    public String getTokenNumber() { return tokenNumber; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String method) { paymentMethod = method; }
    public String getCustomer() { return customer; }
    public String getRole() { return role; }
    public String getPickup() { return pickup; }
    public Map<MenuItem, Integer> getItems() { return new LinkedHashMap<>(items); }
    public double getTotal() { return total; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public int getPointsRedeemed() { return pointsRedeemed; }
    public int getPointsEarned() { return (int) Math.floor(total / 10.0); }
    public String getOfferName() { return offerName; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { status = s; }
    public boolean isPointsReversed() { return pointsReversed; }
    public void markPointsReversed() { pointsReversed = true; }
    public String getCreatedAt() { return createdAt.format(STAMP); }
    public LocalDateTime getCreatedDateTime() { return createdAt; }

    public String getItemsSummary() {
        return items.entrySet().stream().map(e -> e.getValue() + " × " + e.getKey().getName()).collect(Collectors.joining(", "));
    }
}
