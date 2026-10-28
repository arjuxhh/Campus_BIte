# CampusBite Desktop Application

A Java Swing desktop prototype for a smart college canteen, designed to open in IntelliJ IDEA. It uses Java OOP classes and no third-party Java libraries.

## Requirements
- JDK 17 or newer
- IntelliJ IDEA (Community Edition is sufficient)

## Open and run in IntelliJ
1. Extract `CampusBite_Desktop_App.zip`.
2. Open IntelliJ IDEA and choose **File → Open**.
3. Select the extracted `CampusBite_Desktop_App` folder (the one containing `pom.xml`).
4. Allow IntelliJ to load the Maven project and select JDK 17 or newer as the Project SDK.
5. Open `src/main/java/com/campusbite/CampusBiteApplication.java`.
6. Click the green run icon next to `main()`.

## Features
- Sign-in dialog with Student/Teacher demo access and protected Admin demo sign-in (admin / admin123; no real account database)
- Responsive menu grid: 1, 2 or 3+ columns depending on window size, live search, category counts, sold-out handling
- Cart with thumbnails, quantity controls and live subtotal
- Checkout with pickup slot, festival coupon and optional loyalty-point redemption, with a live "You pay" preview
- Demo payment screen (QR shown only for UPI) and order confirmation with token number
- Order tracking; customers can cancel while "Order placed" and request a demo refund afterwards
- Admin: overview panel, order status management (only valid next steps), refund review, add menu items (with optional photo), mark dishes sold out

## OOP concepts
- Encapsulation: private fields in `MenuItem`, `User`, and `Order`
- Abstraction: abstract `User` class
- Inheritance: `Student`, `Teacher`, and `Admin` extend `User`
- Polymorphism: role-specific `getRole()` and `canManageOrders()` overrides
- Service layer: `CanteenService` owns menu and order operations

## Important prototype limitations
This is a local desktop demo. Orders are stored in memory and reset when the app closes. It does not connect to MySQL, authenticate real accounts, process actual payments, or transfer refunds. Sign-in accepts any non-empty name and password for demonstration only. Cancel/refund actions update the in-memory demo order status; contact the canteen for any real payment issue. The included QR is only a demo placeholder.

## Project layout
- `ui/ImageCatalog` finds, crops, scales and caches every picture; `assets/dishes/` holds dish photos (see the README.txt in that folder)
- `ui/CardGridLayout` + `ui/ViewportPanel` make the menu responsive; `ui/Theme` holds colours and look-and-feel-independent buttons
- `service/CanteenService` owns pricing, offers, loyalty points and the order status rules
- `src/test/.../ServiceSmokeTest` - run its `main()` to check the business rules

## What changed in 1.1
- Wrong/duplicated food photos replaced by a name-based photo lookup; dishes without a photo get a neutral tile instead of a wrong picture
- Logo reduced from 1.2 MB to a 256 px master; images cached (the old code re-decoded them on every keystroke)
- Fixed: line breaks not showing in dialogs, loyalty label never updating, duplicate order window after checkout, wasted loyalty points,
  points not returned on cancel, invalid status jumps, NaN/Infinity prices, duplicate dishes, repeating token numbers, search placeholder, buttons ignoring colours on macOS/Windows
