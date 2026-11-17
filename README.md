# CraftEats — Multi-Tenant Food Delivery Ecosystem (Android)

**CraftEats** (operated on the **CodeCraft** platform) is a native Android **Kotlin + Jetpack Compose** multi-vendor food delivery ecosystem designed with a crisp minimalist ecommerce aesthetic (`#FFFFFF` surfaces, `#080F20` Dark Navy bars, `#315CEB` Royal Blue actions, `#F3F4FA` soft lavender highlights, and **Inter** typography), backed by **Firebase Authentication** and **Cloud Firestore**.

---

## Connected Portals (4-in-1 Architecture)

1. **Customer App**
   - Promotional announcement bar, header with cart badge & portal switcher, hero banner (`"Delicious Food. Delivered To Your Door."`), trust strip, 10 cuisine filters (`Indian`, `North Indian`, `South Indian`, `Chinese`, `Pizza`, `Burgers`, `Biryani`, `Desserts`, `Fast Food`, `Healthy Food`), promo banners, **Trending Food**, and **Popular Restaurants**.
   - Full restaurant menu browsing with Veg/Non-Veg badges, INR pricing & discounts, customization/add-ons, persistent Firestore cart, coupons, GPS & saved delivery addresses (`FusedLocationProviderClient`), Razorpay Test Mode flow (UPI / Card / NetBanking) + COD, 6-step order tracking + live GPS route telemetry, cancellation while `PLACED`/`ACCEPTED`, ratings & reviews, notifications, and support tickets.

2. **Restaurant Partner Dashboard**
   - KPI overview (total/today's orders, total sales, monthly revenue, pending/active counts) + interactive Canvas sales chart.
   - Menu management with zero-permission **Android Photo Picker** (`PickVisualMedia`), Veg/Non-Veg toggle, INR pricing & discounts, preparation time, customizations, availability switch, and kitchen open/closed toggle.
   - Real-time order pipeline (`PLACED` -> `ACCEPTED` -> `PREPARING` -> `READY_FOR_PICKUP`) that publishes PII-free `delivery_offers` for verified drivers.

3. **Delivery Partner App**
   - Partner onboarding & verification status (`PENDING`, `APPROVED`, `SUSPENDED`), online/offline availability switch, open `delivery_offers` pickup feed, active 6-step delivery progression (`DRIVER_ASSIGNED` -> `REACHED_RESTAURANT` -> `PICKED_UP` -> `ON_THE_WAY` -> `DELIVERED`), Google Maps navigation intents, real device GPS telemetry via `FusedLocationProviderClient` (zero simulated coordinates), and earnings ledger.

4. **Central Super Admin Dashboard**
   - Restricted to verified platform administrators (`mujahidimam427@gmail.com`, `email_verified == true` and/or `/admins/{uid}`).
   - Multi-tenant **Business Clients** (`tenants`) management & suspension, Restaurant & Delivery Partner approvals + commission percentages, User account moderation, All Orders filtered by tenant + Admin Refunds, Live Tracking GPS inspector, Coupon publishing, Support Ticket resolution, and immutable `audit_logs`.

---

## Firebase & Security Model

- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetGoogleIdOption` for silent launch sign-in; `GetSignInWithGoogleOption` on explicit sign-in).
- **Database**: Named Cloud Firestore database (`R.string.firestore_database_id` = `ai-studio-android-crafteat-b84a2b3e-12d5-4942-819e-69285bc9c70b`).
- **Collections**:
  - `users/{uid}` (+ subcollections `addresses`, `cart`, `notifications`)
  - `tenants/{tenantId}`
  - `restaurants/{restaurantId}`
  - `menu_items/{itemId}`
  - `orders/{orderId}`
  - `delivery_offers/{orderId}` (PII-free offer feed until claimed)
  - `delivery_partners/{partnerId}`
  - `live_locations/{orderId}` (driver-to-customer real GPS telemetry)
  - `payments/{paymentId}`
  - `coupons/{couponId}`
  - `reviews/{reviewId}`
  - `support_tickets/{ticketId}`
  - `audit_logs/{logId}` (append-only, immutable)
  - `admins/{adminId}` (read-only)

---

## Testing & Verification

### 1. Security Rules Suite (Node.js + Firestore Emulator)
Tests unauthenticated denial, cross-user isolation, role anti-escalation, verified email checks, tenant CRUD, full multi-role order/offer/telemetry progression, and immutable `audit_logs`:
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

### 2. Android Compilation & Unit Tests
```bash
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
```

---

## Platform Scope & Production Integration Notes

- **Android Native Runtime**: Implemented completely as a native Android **Kotlin + Jetpack Compose** app backed by **Firebase Authentication** and **Cloud Firestore**. External web/server runtimes (Next.js frontend, Express/Cloud Functions REST backend, Firebase Realtime Database, and server-triggered FCM pushes) are outside the Android client runtime; in-app notifications and order/location streams synchronize directly via Cloud Firestore snapshot listeners.
- **Razorpay & Maps**: Checkout runs a transparent **Razorpay Test Mode** simulator (UPI, Card, NetBanking) alongside Cash on Delivery, while Google Maps turn-by-turn routing launches native `geo:` / Google Maps intents and live tracking renders real GPS telemetry on a Compose Canvas. For live merchant settlements, connect server-side Razorpay webhook signature verification (Cloud Functions) and store API keys in the AI Studio **Secrets** panel rather than source code.
