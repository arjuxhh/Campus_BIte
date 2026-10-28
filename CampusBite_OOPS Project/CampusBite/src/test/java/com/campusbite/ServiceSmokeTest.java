package com.campusbite;

import com.campusbite.model.*;
import com.campusbite.service.CanteenService;
import java.util.*;

/** Plain-Java checks (no JUnit needed): run this class's main() from IntelliJ. */
public class ServiceSmokeTest {
    static void check(boolean ok, String what) { if (!ok) throw new AssertionError("FAILED: " + what); System.out.println("ok  - " + what); }

    public static void main(String[] args) {
        CanteenService s = new CanteenService();
        MenuItem biriyani = s.getMenu().get(0), idli = s.getMenu().get(2);
        Map<MenuItem, Integer> cart = new LinkedHashMap<>(); cart.put(biriyani, 1);

        var q = s.quote(cart, "ONAM10", true, 250);
        check(q.offerDiscount() == 12.0 && q.pointsRedeemed() == 200 && q.total() == 88.0, "ONAM10 + 250 pts on ₹120 -> ₹88, 200 pts used");
        Map<MenuItem, Integer> small = new LinkedHashMap<>(); small.put(s.getMenu().get(3), 1);   // ₹45
        q = s.quote(small, null, true, 600);
        check(q.pointsRedeemed() == 400 && q.total() == 5.0, "points never exceed the amount payable (no wasted points)");
        check(s.quote(small, "TECH20", false, 0).offerDiscount() == 0, "coupon below minimum spend gives no discount");

        User alex = new Student("Alex");
        Order o = s.placeOrder(alex, "10:00 AM", cart, s.quote(cart, null, false, 0), "No offer", "UPI (demo)");
        check(s.getLoyaltyPoints("alex") == 12, "earns 1 point per ₹10 (case-insensitive name)");
        check(s.updateStatus(o, Order.Status.COMPLETED) != null, "PLACED cannot jump straight to COMPLETED");
        check(s.updateStatus(o, Order.Status.CANCELLED) == null && s.getLoyaltyPoints("Alex") == 0, "cancel takes back earned points");
        check(s.updateStatus(o, Order.Status.CANCELLED) != null, "cannot cancel twice");
        check(s.updateStatus(o, Order.Status.REFUND_REQUESTED) == null && s.updateStatus(o, Order.Status.REFUNDED) == null, "cancel -> refund request -> refunded");
        check(s.updateStatus(o, Order.Status.PLACED) != null, "refunded is final");

        idli.setAvailable(false);
        Map<MenuItem, Integer> c2 = new LinkedHashMap<>(); c2.put(idli, 1);
        try { s.placeOrder(alex, "10:00 AM", c2, s.quote(c2, null, false, 0), "No offer", "UPI"); check(false, "sold-out item rejected"); }
        catch (IllegalStateException expected) { check(true, "sold-out item cannot be ordered"); }

        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -5, 0}) {
            try { s.addMenuItem("Test", "Snacks", bad, "", "🍽️", null); check(false, "bad price " + bad); }
            catch (IllegalArgumentException expected) { }
        }
        check(true, "NaN / Infinity / zero / negative prices rejected");
        try { s.addMenuItem("masala dosa", "Breakfast", 40, "", "🥞", null); check(false, "duplicate"); } catch (IllegalArgumentException e) { check(true, "duplicate dish names rejected"); }
        check(new MenuItem(1, "Idli & Sambar", "Breakfast", 30, "", "🥣").getSlug().equals("idli-sambar"), "photo slug: Idli & Sambar -> idli-sambar");
        System.out.println("All checks passed.");
    }
}
