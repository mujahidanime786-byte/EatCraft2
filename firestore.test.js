const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";

const CUSTOMER_UID = "cust_alice_001";
const OTHER_CUSTOMER_UID = "cust_bob_002";
const OWNER_UID = "rest_owner_003";
const DRIVER_UID = "driver_ravi_004";
const UNVERIFIED_DRIVER_UID = "driver_unverified_005";
const ADMIN_UID = "super_admin_000";
const ADMIN_EMAIL = "mujahidimam427@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function futureDate(msAhead = 60000) {
  return new Date(Date.now() + msAhead);
}

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function customerContext(uid = CUSTOMER_UID) {
  return testEnv.authenticatedContext(uid, {
    email: `${uid}@crafteats.test`,
    email_verified: true,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. UNAUTHENTICATED BOUNDARY
test("Unauthenticated requests fail across all top-level and subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();

  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("restaurants").where("approvalStatus", "==", "APPROVED").get());
  await assertFails(unauthDb.collection("menu_items").where("available", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("delivery_offers").where("status", "==", "OPEN").get());
  await assertFails(unauthDb.collection("tenants").get());
  await assertFails(unauthDb.collection("audit_logs").get());

  await assertFails(
    unauthDb.collection("users").doc(CUSTOMER_UID).set({
      uid: CUSTOMER_UID,
      email: "a@b.com",
      displayName: "Unauth",
      role: "CUSTOMER",
      accountStatus: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// 2. USERS, ADDRESSES, CART, NOTIFICATIONS
test("User profile creation, update, anti-escalation, and subcollections match Kotlin payloads", async () => {
  const custDb = customerContext(CUSTOMER_UID).firestore();
  const bobDb = customerContext(OTHER_CUSTOMER_UID).firestore();
  const ts = pastDate(10000);

  // Valid customer signup matching CatalogAndCustomerRepository.ensureUserProfile()
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      uid: CUSTOMER_UID,
      email: "alice@crafteats.test",
      displayName: "Alice",
      photoUrl: "https://example.com/alice.png",
      role: "CUSTOMER",
      accountStatus: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject role self-escalation to SUPER_ADMIN or SUSPENDED status
  await assertFails(
    bobDb.collection("users").doc(OTHER_CUSTOMER_UID).set({
      uid: OTHER_CUSTOMER_UID,
      email: "bob@crafteats.test",
      displayName: "Bob",
      role: "SUPER_ADMIN",
      accountStatus: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId `id` injection or future timestamp
  await assertFails(
    bobDb.collection("users").doc("extra_user").set({
      id: "extra_user",
      uid: "extra_user",
      email: "x@y.com",
      displayName: "Hacker",
      role: "CUSTOMER",
      accountStatus: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(OTHER_CUSTOMER_UID).set({
      uid: OTHER_CUSTOMER_UID,
      email: "bob@crafteats.test",
      displayName: "Bob",
      role: "CUSTOMER",
      accountStatus: "ACTIVE",
      createdAt: futureDate(),
      updatedAt: futureDate(),
    })
  );

  // Valid partial profile update
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      displayName: "Alice Sharma",
      phone: "+919876543210",
      updatedAt: pastDate(1000),
    })
  );

  // Reject non-admin changing their role
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read/list denied
  await assertFails(bobDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(custDb.collection("users").get());

  // Address subcollection CRUD (matching saveAddress)
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("addresses").doc("addr_1").set({
      userId: CUSTOMER_UID,
      label: "Home",
      addressLine: "12 MG Road, Indiranagar",
      landmark: "Near Metro",
      city: "Bengaluru",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      primaryAddress: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("addresses").get()
  );
  await assertFails(
    bobDb.collection("users").doc(CUSTOMER_UID).collection("addresses").get()
  );

  // Cart subcollection CRUD (matching addToCart & updateCartQuantity)
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("cart").doc("cart_1").set({
      userId: CUSTOMER_UID,
      restaurantId: "rest_royal_darbar",
      restaurantName: "Royal Darbar Kitchen",
      restaurantOwnerUid: OWNER_UID,
      tenantId: "tenant_north_culinary",
      menuItemId: "item_dum_biryani",
      name: "Hyderabadi Dum Biryani",
      priceInr: 329,
      quantity: 2,
      veg: false,
      imageKey: "biryani",
      selectedAddOn: "Extra Raita (+₹30)",
      addOnPriceInr: 30,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("cart").doc("cart_1").update({
      quantity: 3,
      updatedAt: pastDate(500),
    })
  );

  // Notifications subcollection
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("notifications").doc("notif_1").set({
      userId: CUSTOMER_UID,
      title: "Order Placed",
      body: "Order #ord_1 has been sent to kitchen",
      category: "ORDER",
      orderId: "ord_1",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).collection("notifications").doc("notif_1").update({
      read: true,
      updatedAt: pastDate(500),
    })
  );
});

// 3. SUPER ADMIN, TENANTS & SPOOFED ADMIN GUARD
test("Super Admin bootstrap email_verified guard & multi-tenant client CRUD", async () => {
  const adminDb = adminContext().firestore();
  const spoofedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const custDb = customerContext(CUSTOMER_UID).firestore();
  const ts = pastDate(5000);

  // Spoofed unverified email cannot write tenants
  await assertFails(
    spoofedAdminDb.collection("tenants").doc("tenant_1").set({
      name: "Spoofed Tenant",
      code: "SPF-01",
      city: "Delhi",
      status: "ACTIVE",
      plan: "ENTERPRISE",
      ownerUid: ADMIN_UID,
      commissionPercent: 15,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified bootstrap admin can create, list, and suspend tenants
  await assertSucceeds(
    adminDb.collection("tenants").doc("tenant_north_culinary").set({
      name: "North Culinary Group",
      code: "NCG-01",
      city: "Bengaluru",
      status: "ACTIVE",
      plan: "ENTERPRISE",
      ownerUid: ADMIN_UID,
      contactEmail: "ops@northculinary.in",
      commissionPercent: 15,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("tenants").doc("tenant_north_culinary").update({
      status: "SUSPENDED",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(adminDb.collection("tenants").get());
  await assertFails(custDb.collection("tenants").get());
});

// 4. RESTAURANTS & MENU ITEMS (FILTERED QUERIES & MODERATION GUARD)
test("Restaurants and menu items enforce owner isolation, moderation bounds, and filtered list queries", async () => {
  const ownerDb = customerContext(OWNER_UID).firestore();
  const adminDb = adminContext().firestore();
  const custDb = customerContext(CUSTOMER_UID).firestore();
  const ts = pastDate(8000);

  // Restaurant owner submits new restaurant -> must start PENDING with 15% commission
  await assertSucceeds(
    ownerDb.collection("restaurants").doc("rest_owner_1").set({
      name: "Spice Atelier",
      cuisine: "Indian • Biryani",
      city: "Bengaluru",
      addressLabel: "100 Feet Rd, Indiranagar",
      tenantId: "tenant_north_culinary",
      ownerUid: OWNER_UID,
      rating: 4.5,
      reviewCount: 0,
      deliveryTimeMins: 28,
      deliveryFeeInr: 29,
      minOrderInr: 149,
      commissionPercent: 15,
      openNow: true,
      approvalStatus: "PENDING",
      imageKey: "biryani",
      logoLabel: "SA",
      promoTag: "NEW PARTNER",
      latitude: 12.9716,
      longitude: 77.5946,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin cannot self-approve restaurant or forge commission
  await assertFails(
    ownerDb.collection("restaurants").doc("rest_forged").set({
      name: "Forged Kitchen",
      cuisine: "Pizza",
      city: "Bengaluru",
      addressLabel: "MG Road",
      tenantId: "tenant_north_culinary",
      ownerUid: OWNER_UID,
      rating: 4.9,
      reviewCount: 0,
      deliveryTimeMins: 20,
      deliveryFeeInr: 0,
      minOrderInr: 99,
      commissionPercent: 15,
      openNow: true,
      approvalStatus: "APPROVED",
      imageKey: "pizza",
      latitude: 12.97,
      longitude: 77.59,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin approves restaurant & adjusts commission
  await assertSucceeds(
    adminDb.collection("restaurants").doc("rest_owner_1").update({
      approvalStatus: "APPROVED",
      commissionPercent: 18,
      updatedAt: pastDate(4000),
    })
  );

  // Owner adds a menu item (restaurant exists & belongs to owner)
  await assertSucceeds(
    ownerDb.collection("menu_items").doc("item_1").set({
      restaurantId: "rest_owner_1",
      restaurantName: "Spice Atelier",
      tenantId: "tenant_north_culinary",
      ownerUid: OWNER_UID,
      name: "Royal Saffron Biryani",
      description: "Aged basmati cooked over slow charcoal fire.",
      category: "Biryani",
      priceInr: 329,
      originalPriceInr: 399,
      discountPercent: 18,
      veg: false,
      available: true,
      prepTimeMins: 22,
      imageKey: "biryani",
      rating: 4.6,
      reviewCount: 12,
      customizations: ["Extra Raita (+₹30)"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer cannot create menu item in someone else's restaurant
  await assertFails(
    custDb.collection("menu_items").doc("item_hijack").set({
      restaurantId: "rest_owner_1",
      restaurantName: "Spice Atelier",
      tenantId: "tenant_north_culinary",
      ownerUid: CUSTOMER_UID,
      name: "Free Pizza",
      description: "Hijacked",
      category: "Pizza",
      priceInr: 1,
      originalPriceInr: 1,
      discountPercent: 0,
      veg: true,
      available: true,
      prepTimeMins: 10,
      imageKey: "pizza",
      rating: 5.0,
      reviewCount: 0,
      customizations: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered queries matching CatalogAndCustomerRepository & OperationsAndAdminRepository succeed
  await assertSucceeds(custDb.collection("restaurants").where("approvalStatus", "==", "APPROVED").get());
  await assertSucceeds(custDb.collection("menu_items").where("available", "==", true).get());
  await assertSucceeds(ownerDb.collection("restaurants").where("ownerUid", "==", OWNER_UID).get());
  await assertSucceeds(ownerDb.collection("menu_items").where("ownerUid", "==", OWNER_UID).get());

  // Unfiltered top-level queries by non-admin are rejected
  await assertFails(custDb.collection("restaurants").get());
  await assertFails(custDb.collection("menu_items").get());
});

// 5. FULL MULTI-ROLE ORDER LIFECYCLE + PII-FREE DELIVERY OFFERS + LIVE GPS + PAYMENTS
test("Full multi-role order workflow, delivery offer claim, live GPS telemetry, and terminal guards", async () => {
  const adminDb = adminContext().firestore();
  const custDb = customerContext(CUSTOMER_UID).firestore();
  const bobDb = customerContext(OTHER_CUSTOMER_UID).firestore();
  const ownerDb = customerContext(OWNER_UID).firestore();
  const driverDb = customerContext(DRIVER_UID).firestore();
  const unverifiedDriverDb = customerContext(UNVERIFIED_DRIVER_UID).firestore();
  const ts = pastDate(20000);

  // Seed restaurant and verified + unverified delivery partners
  await assertSucceeds(
    adminDb.collection("restaurants").doc("rest_1").set({
      name: "Napoli Woodfire Oven",
      cuisine: "Pizza",
      city: "Bengaluru",
      addressLabel: "80 Feet Rd, Koramangala",
      tenantId: "tenant_north_culinary",
      ownerUid: OWNER_UID,
      rating: 4.8,
      reviewCount: 210,
      deliveryTimeMins: 22,
      deliveryFeeInr: 0,
      minOrderInr: 149,
      commissionPercent: 15,
      openNow: true,
      approvalStatus: "APPROVED",
      imageKey: "pizza",
      latitude: 12.9352,
      longitude: 77.6245,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified driver registers -> approved by admin
  await assertSucceeds(
    driverDb.collection("delivery_partners").doc(DRIVER_UID).set({
      uid: DRIVER_UID,
      displayName: "Ravi Kumar",
      vehicleType: "Electric Scooter",
      vehiclePlate: "KA01EV4422",
      city: "Bengaluru",
      approvalStatus: "PENDING",
      online: false,
      completedDeliveries: 0,
      totalEarningsInr: 0,
      rating: 4.9,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("delivery_partners").doc(DRIVER_UID).update({
      approvalStatus: "APPROVED",
      updatedAt: pastDate(15000),
    })
  );

  // Unverified driver registers (stays PENDING)
  await assertSucceeds(
    unverifiedDriverDb.collection("delivery_partners").doc(UNVERIFIED_DRIVER_UID).set({
      uid: UNVERIFIED_DRIVER_UID,
      displayName: "Pending Driver",
      vehicleType: "Scooter",
      vehiclePlate: "KA01XX9999",
      city: "Bengaluru",
      approvalStatus: "PENDING",
      online: false,
      completedDeliveries: 0,
      totalEarningsInr: 0,
      rating: 4.8,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Step A: Customer creates order & payment
  const orderId = "ord_test_101";
  await assertSucceeds(
    custDb.collection("orders").doc(orderId).set({
      customerUid: CUSTOMER_UID,
      customerName: "Alice",
      restaurantId: "rest_1",
      restaurantName: "Napoli Woodfire Oven",
      restaurantOwnerUid: OWNER_UID,
      tenantId: "tenant_north_culinary",
      status: "PLACED",
      items: ["1x Artisan Woodfire Margherita - ₹349"],
      subtotalInr: 349,
      deliveryFeeInr: 0,
      discountInr: 0,
      totalInr: 349,
      paymentMethod: "RAZORPAY_UPI",
      paymentStatus: "PAID",
      razorpayOrderId: "order_rzp_1",
      razorpayPaymentId: "pay_rzp_1",
      deliveryAddressLabel: "Home: 12 MG Road, Bengaluru",
      deliveryLat: 12.9784,
      deliveryLng: 77.6408,
      restaurantLat: 12.9352,
      restaurantLng: 77.6245,
      prepTimeMins: 22,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    custDb.collection("payments").doc("pay_101").set({
      orderId,
      customerUid: CUSTOMER_UID,
      restaurantId: "rest_1",
      restaurantOwnerUid: OWNER_UID,
      tenantId: "tenant_north_culinary",
      amountInr: 349,
      commissionInr: 52,
      method: "RAZORPAY_UPI",
      status: "PAID",
      razorpayOrderId: "order_rzp_1",
      razorpayPaymentId: "pay_rzp_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-customer order read fails
  await assertFails(bobDb.collection("orders").doc(orderId).get());

  // Step B: Restaurant owner advances ACCEPTED -> PREPARING -> checks non-existent offer -> creates delivery_offer
  await assertSucceeds(
    ownerDb.collection("orders").doc(orderId).update({
      status: "ACCEPTED",
      prepTimeMins: 20,
      updatedAt: pastDate(12000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("orders").doc(orderId).update({
      status: "PREPARING",
      prepTimeMins: 20,
      updatedAt: pastDate(11000),
    })
  );
  // Confirm non-existent get succeeds for restaurantOwnerUid (fixed resource == null rule!)
  await assertSucceeds(ownerDb.collection("delivery_offers").doc(orderId).get());

  await assertSucceeds(
    ownerDb.collection("delivery_offers").doc(orderId).set({
      orderId,
      tenantId: "tenant_north_culinary",
      restaurantId: "rest_1",
      restaurantName: "Napoli Woodfire Oven",
      restaurantOwnerUid: OWNER_UID,
      restaurantAddress: "Napoli Woodfire Oven Kitchen Hub",
      areaLabel: "Home",
      payoutInr: 55,
      distanceKm: 5.1,
      status: "OPEN",
      restaurantLat: 12.9352,
      restaurantLng: 77.6245,
      deliveryLat: 12.9784,
      deliveryLng: 77.6408,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    })
  );

  // Driver queries open delivery offers
  await assertSucceeds(driverDb.collection("delivery_offers").where("status", "==", "OPEN").get());

  // Unverified driver CANNOT claim delivery offer or order
  await assertFails(
    unverifiedDriverDb.collection("delivery_offers").doc(orderId).update({
      status: "CLAIMED",
      driverUid: UNVERIFIED_DRIVER_UID,
      updatedAt: pastDate(9000),
    })
  );
  await assertFails(
    unverifiedDriverDb.collection("orders").doc(orderId).update({
      driverUid: UNVERIFIED_DRIVER_UID,
      driverName: "Pending Driver",
      status: "DRIVER_ASSIGNED",
      updatedAt: pastDate(9000),
    })
  );

  // Verified driver claims delivery offer & order
  await assertSucceeds(
    driverDb.collection("delivery_offers").doc(orderId).update({
      status: "CLAIMED",
      driverUid: DRIVER_UID,
      updatedAt: pastDate(8000),
    })
  );
  await assertSucceeds(
    driverDb.collection("orders").doc(orderId).update({
      driverUid: DRIVER_UID,
      driverName: "Ravi Kumar",
      status: "DRIVER_ASSIGNED",
      updatedAt: pastDate(8000),
    })
  );

  // Driver queries own assigned orders
  await assertSucceeds(driverDb.collection("orders").where("driverUid", "==", DRIVER_UID).get());

  // Step C: Driver checks non-existent live_locations doc -> publishes GPS telemetry -> updates it
  await assertSucceeds(driverDb.collection("live_locations").doc(orderId).get());
  await assertSucceeds(
    driverDb.collection("live_locations").doc(orderId).set({
      orderId,
      customerUid: CUSTOMER_UID,
      restaurantOwnerUid: OWNER_UID,
      driverUid: DRIVER_UID,
      tenantId: "tenant_north_culinary",
      latitude: 12.9501,
      longitude: 77.6302,
      accuracyMeters: 8.5,
      speedMps: 6.2,
      bearing: 45.0,
      createdAt: pastDate(7000),
      updatedAt: pastDate(7000),
    })
  );
  await assertSucceeds(
    driverDb.collection("live_locations").doc(orderId).update({
      latitude: 12.9612,
      longitude: 77.6355,
      accuracyMeters: 6.0,
      speedMps: 7.1,
      bearing: 42.0,
      updatedAt: pastDate(6000),
    })
  );

  // Customer observes live_locations & order
  await assertSucceeds(custDb.collection("live_locations").where("customerUid", "==", CUSTOMER_UID).get());
  await assertSucceeds(custDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get());

  // Step D: Driver completes delivery -> updates partner stats
  await assertSucceeds(
    driverDb.collection("orders").doc(orderId).update({
      status: "DELIVERED",
      updatedAt: pastDate(5000),
    })
  );
  await assertSucceeds(
    driverDb.collection("delivery_partners").doc(DRIVER_UID).update({
      completedDeliveries: 1,
      totalEarningsInr: 55,
      updatedAt: pastDate(5000),
    })
  );

  // Terminal order CANNOT be mutated by customer, owner, or driver
  await assertFails(
    custDb.collection("orders").doc(orderId).update({
      status: "CANCELLED",
      cancellationReason: "Late",
      updatedAt: pastDate(3000),
    })
  );
  await assertFails(
    driverDb.collection("orders").doc(orderId).update({
      status: "ON_THE_WAY",
      updatedAt: pastDate(3000),
    })
  );
});

// 6. COUPONS, REVIEWS, SUPPORT TICKETS & IMMUTABLE AUDIT LOGS
test("Coupons, reviews, support tickets, and immutable audit_logs enforce permissions", async () => {
  const adminDb = adminContext().firestore();
  const custDb = customerContext(CUSTOMER_UID).firestore();
  const bobDb = customerContext(OTHER_CUSTOMER_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    adminDb.collection("restaurants").doc("rest_1").set({
      name: "Royal Darbar Kitchen",
      cuisine: "Biryani",
      city: "Bengaluru",
      addressLabel: "100 Feet Rd",
      tenantId: "tenant_north_culinary",
      ownerUid: OWNER_UID,
      rating: 4.8,
      reviewCount: 10,
      deliveryTimeMins: 25,
      deliveryFeeInr: 29,
      minOrderInr: 149,
      commissionPercent: 15,
      openNow: true,
      approvalStatus: "APPROVED",
      imageKey: "biryani",
      latitude: 12.97,
      longitude: 77.64,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin creates coupon & audit log
  await assertSucceeds(
    adminDb.collection("coupons").doc("cpn_1").set({
      code: "CRAFT50",
      title: "50% Off First Royal Feast",
      discountPercent: 50,
      maxDiscountInr: 150,
      minOrderInr: 249,
      active: true,
      tenantId: "global",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(custDb.collection("coupons").where("active", "==", true).get());
  await assertFails(
    custDb.collection("coupons").doc("cpn_hack").set({
      code: "FREE100",
      title: "Hack",
      discountPercent: 80,
      maxDiscountInr: 5000,
      minOrderInr: 0,
      active: true,
      tenantId: "global",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer creates review & support ticket
  await assertSucceeds(
    custDb.collection("reviews").doc("rev_1").set({
      orderId: "ord_test_101",
      restaurantId: "rest_1",
      customerUid: CUSTOMER_UID,
      customerName: "Alice",
      rating: 5,
      comment: "Amazing saffron biryani!",
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(custDb.collection("reviews").where("published", "==", true).get());

  await assertSucceeds(
    custDb.collection("support_tickets").doc("tkt_1").set({
      userId: CUSTOMER_UID,
      userName: "Alice",
      role: "CUSTOMER",
      subject: "Missing extra raita",
      message: "Please assist with refund for raita add-on.",
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    custDb.collection("support_tickets").where("userId", "==", CUSTOMER_UID).get()
  );
  await assertFails(bobDb.collection("support_tickets").doc("tkt_1").get());
  await assertSucceeds(
    adminDb.collection("support_tickets").doc("tkt_1").update({
      status: "RESOLVED",
      adminReply: "Refunded ₹30 to source account.",
      updatedAt: pastDate(2000),
    })
  );

  // Audit logs are append-only by admin and strictly immutable (no update/delete even by admin)
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("aud_1").set({
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "CREATE_COUPON",
      targetType: "coupons",
      targetId: "cpn_1",
      summary: "Published CRAFT50",
      createdAt: ts,
    })
  );
  await assertFails(
    adminDb.collection("audit_logs").doc("aud_1").update({
      summary: "Tampered summary",
    })
  );
  await assertFails(adminDb.collection("audit_logs").doc("aud_1").delete());
  await assertFails(custDb.collection("audit_logs").get());
});
