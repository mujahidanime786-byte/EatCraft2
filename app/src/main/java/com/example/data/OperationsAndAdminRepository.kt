package com.example.data

import com.example.model.AuditLog
import com.example.model.BusinessTenant
import com.example.model.Coupon
import com.example.model.DeliveryOffer
import com.example.model.DeliveryPartner
import com.example.model.FoodOrder
import com.example.model.LiveLocation
import com.example.model.LocationSample
import com.example.model.MenuItem
import com.example.model.PaymentRecord
import com.example.model.Restaurant
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class OperationsAndAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")

    fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email == AuthRepository.BOOTSTRAP_ADMIN_EMAIL
    }

    // --- RESTAURANT PARTNER WORKFLOWS ---

    fun observeOwnedRestaurants(uid: String): Flow<List<Restaurant>> =
        db.collection("restaurants")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { it.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "restaurants", uid) }

    fun observeOwnedMenuItems(uid: String): Flow<List<MenuItem>> =
        db.collection("menu_items")
            .whereEqualTo("ownerUid", uid)
            .snapshots()
            .map { it.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "menu_items", uid) }

    fun observeRestaurantOrders(uid: String): Flow<List<FoodOrder>> =
        db.collection("orders")
            .whereEqualTo("restaurantOwnerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(FoodOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "orders", uid) }

    suspend fun registerRestaurant(
        name: String,
        cuisine: String,
        city: String,
        addressLabel: String,
        tenantId: String,
        deliveryFeeInr: Long,
        minOrderInr: Long
    ): Result<Unit> {
        val id = "rest_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "restaurants/$id"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Restaurant name must be at least 2 characters" }
            require(cuisine.trim().length >= 2) { "Cuisine is required" }
            require(addressLabel.trim().length >= 3) { "Address is required" }
            val status = if (isBootstrapAdmin()) "APPROVED" else "PENDING"
            val payload = mapOf(
                "name" to name.trim().take(120),
                "cuisine" to cuisine.trim().take(80),
                "city" to city.trim().ifBlank { "Bengaluru" }.take(80),
                "addressLabel" to addressLabel.trim().take(200),
                "tenantId" to tenantId.ifBlank { "tenant_north_culinary" },
                "ownerUid" to uid,
                "rating" to 4.5,
                "reviewCount" to 0L,
                "deliveryTimeMins" to 28L,
                "deliveryFeeInr" to deliveryFeeInr.coerceIn(0L, 500L),
                "minOrderInr" to minOrderInr.coerceIn(0L, 5000L),
                "commissionPercent" to 15L,
                "openNow" to true,
                "approvalStatus" to status,
                "imageKey" to "biryani",
                "logoLabel" to name.trim().take(2).uppercase(),
                "promoTag" to "NEW PARTNER",
                "latitude" to 12.9716,
                "longitude" to 77.5946,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleRestaurantOpen(restaurantId: String, openNow: Boolean): Result<Unit> {
        val path = "restaurants/$restaurantId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("openNow" to openNow, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMenuItem(
        existingId: String?,
        restaurant: Restaurant,
        name: String,
        description: String,
        category: String,
        priceInr: Long,
        discountPercent: Long,
        veg: Boolean,
        available: Boolean,
        prepTimeMins: Long,
        imageKey: String,
        imageUri: String?,
        customizations: List<String>
    ): Result<Unit> {
        val docId = existingId ?: "item_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "menu_items/$docId"
        return try {
            val uid = requireUserId()
            require(name.trim().length >= 2) { "Dish name must be at least 2 characters" }
            val cleanPrice = priceInr.coerceIn(1L, 50000L)
            val cleanDiscount = discountPercent.coerceIn(0L, 90L)
            val originalPrice = if (cleanDiscount > 0L) {
                ((cleanPrice * 100L) / (100L - cleanDiscount)).coerceIn(cleanPrice, 50000L)
            } else {
                cleanPrice
            }
            val cleanCustomizations = customizations.map { it.trim() }.filter { it.isNotEmpty() }.take(8)
            if (existingId == null) {
                val payload = mapOf(
                    "restaurantId" to restaurant.id,
                    "restaurantName" to restaurant.name.take(120),
                    "tenantId" to restaurant.tenantId,
                    "ownerUid" to uid,
                    "name" to name.trim().take(120),
                    "description" to description.trim().ifBlank { "Freshly prepared dish." }.take(400),
                    "category" to category.trim().ifBlank { "Indian" }.take(60),
                    "priceInr" to cleanPrice,
                    "originalPriceInr" to originalPrice,
                    "discountPercent" to cleanDiscount,
                    "veg" to veg,
                    "available" to available,
                    "prepTimeMins" to prepTimeMins.coerceIn(1L, 120L),
                    "imageKey" to imageKey.ifBlank { "biryani" }.take(80),
                    "imageUri" to imageUri?.takeIf { it.isNotBlank() }?.take(600),
                    "rating" to 4.6,
                    "reviewCount" to 12L,
                    "customizations" to cleanCustomizations,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                db.document(path).set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to name.trim().take(120),
                    "description" to description.trim().ifBlank { "Freshly prepared dish." }.take(400),
                    "category" to category.trim().ifBlank { "Indian" }.take(60),
                    "priceInr" to cleanPrice,
                    "originalPriceInr" to originalPrice,
                    "discountPercent" to cleanDiscount,
                    "veg" to veg,
                    "available" to available,
                    "prepTimeMins" to prepTimeMins.coerceIn(1L, 120L),
                    "imageKey" to imageKey.ifBlank { "biryani" }.take(80),
                    "customizations" to cleanCustomizations,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!imageUri.isNullOrBlank()) {
                    updates["imageUri"] = imageUri.take(600)
                }
                db.document(path).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMenuItemAvailability(itemId: String, available: Boolean): Result<Unit> {
        val path = "menu_items/$itemId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("available" to available, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRestaurantOrderStatus(
        order: FoodOrder,
        nextStatus: String,
        prepTimeMins: Long = order.prepTimeMins,
        rejectionReason: String? = null
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to nextStatus,
                "prepTimeMins" to prepTimeMins.coerceIn(5L, 120L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!rejectionReason.isNullOrBlank()) {
                updates["cancellationReason"] = rejectionReason.trim().take(200)
            }
            db.document(path).update(updates).await()

            // When marked PREPARING or READY_FOR_PICKUP, publish a PII-free delivery offer if not yet created
            if (nextStatus in listOf("PREPARING", "READY_FOR_PICKUP")) {
                val offerRef = db.collection("delivery_offers").document(order.id)
                val existingOffer = offerRef.get().await()
                if (!existingOffer.exists()) {
                    val distanceKm = LocationHelper.haversineKm(
                        order.restaurantLat,
                        order.restaurantLng,
                        order.deliveryLat,
                        order.deliveryLng
                    ).coerceIn(0.5, 50.0)
                    val payout = (35L + (distanceKm * 10.0).toLong()).coerceIn(35L, 450L)
                    val offerPayload = mapOf(
                        "orderId" to order.id,
                        "tenantId" to order.tenantId,
                        "restaurantId" to order.restaurantId,
                        "restaurantName" to order.restaurantName.take(120),
                        "restaurantOwnerUid" to uid,
                        "restaurantAddress" to "${order.restaurantName} Kitchen Hub".take(200),
                        "areaLabel" to order.deliveryAddressLabel.substringBefore(":").ifBlank { "City Center" }.take(100),
                        "payoutInr" to payout,
                        "distanceKm" to distanceKm,
                        "status" to "OPEN",
                        "restaurantLat" to order.restaurantLat,
                        "restaurantLng" to order.restaurantLng,
                        "deliveryLat" to order.deliveryLat,
                        "deliveryLng" to order.deliveryLng,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    offerRef.set(offerPayload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DELIVERY PARTNER WORKFLOWS ---

    fun observeDeliveryPartnerProfile(uid: String): Flow<DeliveryPartner?> =
        db.collection("delivery_partners").document(uid)
            .snapshots()
            .map { it.toObject(DeliveryPartner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.GET, "delivery_partners/$uid", uid) }

    fun observeOpenDeliveryOffers(): Flow<List<DeliveryOffer>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("delivery_offers")
                .whereEqualTo("status", "OPEN")
                .snapshots()
                .map { it.toObjects(DeliveryOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "delivery_offers", uid) }
        )
    }

    fun observeDriverOrders(uid: String): Flow<List<FoodOrder>> =
        db.collection("orders")
            .whereEqualTo("driverUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(FoodOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "orders", uid) }

    suspend fun registerOrUpdateDeliveryPartner(
        displayName: String,
        vehicleType: String,
        vehiclePlate: String,
        city: String
    ): Result<Unit> {
        var path = "delivery_partners"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("delivery_partners").document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to displayName.trim().take(100),
                        "vehicleType" to vehicleType.trim().take(40),
                        "vehiclePlate" to vehiclePlate.trim().uppercase().take(30),
                        "city" to city.trim().take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val approval = if (isBootstrapAdmin()) "APPROVED" else "PENDING"
                docRef.set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to displayName.trim().ifBlank { "Delivery Partner" }.take(100),
                        "vehicleType" to vehicleType.trim().ifBlank { "Electric Scooter" }.take(40),
                        "vehiclePlate" to vehiclePlate.trim().uppercase().ifBlank { "KA01EV0001" }.take(30),
                        "city" to city.trim().ifBlank { "Bengaluru" }.take(80),
                        "approvalStatus" to approval,
                        "online" to false,
                        "completedDeliveries" to 0L,
                        "totalEarningsInr" to 0L,
                        "rating" to 4.9,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setDriverOnline(online: Boolean): Result<Unit> {
        var path = "delivery_partners"
        return try {
            val uid = requireUserId()
            path = "delivery_partners/$uid"
            db.document(path).update(
                mapOf("online" to online, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDeliveryOffer(offer: DeliveryOffer, driverName: String): Result<Unit> {
        val offerPath = "delivery_offers/${offer.orderId}"
        val orderPath = "orders/${offer.orderId}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.document(offerPath),
                mapOf(
                    "status" to "CLAIMED",
                    "driverUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.document(orderPath),
                mapOf(
                    "driverUid" to uid,
                    "driverName" to driverName.ifBlank { "Partner" }.take(100),
                    "status" to "DRIVER_ASSIGNED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun advanceDeliveryOrderStatus(
        order: FoodOrder,
        nextStatus: String,
        partnerProfile: DeliveryPartner?
    ): Result<Unit> {
        val orderPath = "orders/${order.id}"
        return try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to nextStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nextStatus == "DELIVERED" && order.paymentMethod == "COD") {
                updates["paymentStatus"] = "PAID"
            }
            db.document(orderPath).update(updates).await()

            if (nextStatus == "DELIVERED" && partnerProfile != null) {
                db.collection("delivery_partners").document(uid).update(
                    mapOf(
                        "completedDeliveries" to (partnerProfile.completedDeliveries + 1L),
                        "totalEarningsInr" to (partnerProfile.totalEarningsInr + 55L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun publishLiveGpsTelemetry(
        order: FoodOrder,
        sample: LocationSample
    ): Result<Unit> {
        val locPath = "live_locations/${order.id}"
        return try {
            val uid = requireUserId()
            val locRef = db.document(locPath)
            val existing = locRef.get().await()
            if (existing.exists()) {
                locRef.update(
                    mapOf(
                        "latitude" to sample.latitude,
                        "longitude" to sample.longitude,
                        "accuracyMeters" to sample.accuracyMeters,
                        "speedMps" to sample.speedMps,
                        "bearing" to sample.bearing,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                locRef.set(
                    mapOf(
                        "orderId" to order.id,
                        "customerUid" to order.customerUid,
                        "restaurantOwnerUid" to order.restaurantOwnerUid,
                        "driverUid" to uid,
                        "tenantId" to order.tenantId,
                        "latitude" to sample.latitude,
                        "longitude" to sample.longitude,
                        "accuracyMeters" to sample.accuracyMeters,
                        "speedMps" to sample.speedMps,
                        "bearing" to sample.bearing,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("delivery_partners").document(uid).update(
                mapOf(
                    "currentLat" to sample.latitude,
                    "currentLng" to sample.longitude,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, locPath)
            Result.failure(e)
        }
    }

    // --- SUPER ADMIN WORKFLOWS ---

    fun observeAllTenants(): Flow<List<BusinessTenant>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tenants")
                .snapshots()
                .map { it.toObjects(BusinessTenant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "tenants", uid) }
        )
    }

    fun observeAllRestaurantsAdmin(): Flow<List<Restaurant>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("restaurants")
                .snapshots()
                .map { it.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "restaurants", uid) }
        )
    }

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "users", uid) }
        )
    }

    fun observeAllPartnersAdmin(): Flow<List<DeliveryPartner>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("delivery_partners")
                .snapshots()
                .map { it.toObjects(DeliveryPartner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "delivery_partners", uid) }
        )
    }

    fun observeAllOrdersAdmin(): Flow<List<FoodOrder>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FoodOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "orders", uid) }
        )
    }

    fun observeAllLiveLocationsAdmin(): Flow<List<LiveLocation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("live_locations")
                .snapshots()
                .map { it.toObjects(LiveLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "live_locations", uid) }
        )
    }

    fun observeAllPaymentsAdmin(): Flow<List<PaymentRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "payments", uid) }
        )
    }

    fun observeAllCouponsAdmin(): Flow<List<Coupon>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("coupons")
                .snapshots()
                .map { it.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "coupons", uid) }
        )
    }

    fun observeAllTicketsAdmin(): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("support_tickets")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "support_tickets", uid) }
        )
    }

    fun observeAuditLogsAdmin(): Flow<List<AuditLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "audit_logs", uid) }
        )
    }

    suspend fun createBusinessTenant(
        name: String,
        code: String,
        city: String,
        plan: String,
        commissionPercent: Long,
        contactEmail: String?
    ): Result<Unit> {
        val id = "tenant_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "tenants/$id"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "name" to name.trim().take(120),
                "code" to code.trim().uppercase().take(30),
                "city" to city.trim().take(80),
                "status" to "ACTIVE",
                "plan" to plan,
                "ownerUid" to uid,
                "contactEmail" to contactEmail?.trim()?.takeIf { it.isNotEmpty() }?.take(200),
                "commissionPercent" to commissionPercent.coerceIn(0L, 40L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.document(path).set(payload).await()
            writeAuditLog("CREATE_TENANT", "tenants", id, "Created business client ${name.trim()} ($code)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTenantStatus(tenantId: String, nextStatus: String): Result<Unit> {
        val path = "tenants/$tenantId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("status" to nextStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAuditLog("UPDATE_TENANT_STATUS", "tenants", tenantId, "Changed tenant status to $nextStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRestaurantApprovalAndCommission(
        restaurantId: String,
        approvalStatus: String,
        commissionPercent: Long
    ): Result<Unit> {
        val path = "restaurants/$restaurantId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "approvalStatus" to approvalStatus,
                    "commissionPercent" to commissionPercent.coerceIn(0L, 40L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                "MODERATE_RESTAURANT",
                "restaurants",
                restaurantId,
                "Set status=$approvalStatus, commission=$commissionPercent%"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePartnerApproval(partnerUid: String, approvalStatus: String): Result<Unit> {
        val path = "delivery_partners/$partnerUid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("approvalStatus" to approvalStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAuditLog("MODERATE_PARTNER", "delivery_partners", partnerUid, "Set partner approval=$approvalStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAccountStatus(targetUid: String, accountStatus: String): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("accountStatus" to accountStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            writeAuditLog("UPDATE_USER_STATUS", "users", targetUid, "Set accountStatus=$accountStatus")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminRefundOrder(order: FoodOrder, paymentRecord: PaymentRecord?): Result<Unit> {
        val orderPath = "orders/${order.id}"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.document(orderPath),
                mapOf(
                    "status" to "CANCELLED",
                    "paymentStatus" to "REFUNDED",
                    "cancellationReason" to "Admin authorized refund",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (paymentRecord != null) {
                batch.update(
                    db.collection("payments").document(paymentRecord.id),
                    mapOf(
                        "status" to "REFUNDED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            writeAuditLog("REFUND_ORDER", "orders", order.id, "Authorized refund for ₹${order.totalInr}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun createCoupon(
        code: String,
        title: String,
        discountPercent: Long,
        maxDiscountInr: Long,
        minOrderInr: Long,
        tenantId: String
    ): Result<Unit> {
        val id = "cpn_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "coupons/$id"
        return try {
            requireUserId()
            val cleanCode = code.trim().uppercase().take(30)
            db.document(path).set(
                mapOf(
                    "code" to cleanCode,
                    "title" to title.trim().take(120),
                    "discountPercent" to discountPercent.coerceIn(1L, 80L),
                    "maxDiscountInr" to maxDiscountInr.coerceIn(1L, 5000L),
                    "minOrderInr" to minOrderInr.coerceIn(0L, 10000L),
                    "active" to true,
                    "tenantId" to tenantId.ifBlank { "global" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("CREATE_COUPON", "coupons", id, "Published coupon $cleanCode ($discountPercent% off)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveSupportTicket(ticketId: String, reply: String): Result<Unit> {
        val path = "support_tickets/$ticketId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to "RESOLVED",
                    "adminReply" to reply.trim().take(800),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("RESOLVE_TICKET", "support_tickets", ticketId, "Resolved support ticket")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun writeAuditLog(action: String, targetType: String, targetId: String, summary: String) {
        val user = auth.currentUser ?: return
        val logId = "aud_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        db.collection("audit_logs").document(logId).set(
            mapOf(
                "adminUid" to user.uid,
                "adminEmail" to (user.email ?: "admin@crafteats.app"),
                "action" to action.take(80),
                "targetType" to targetType.take(60),
                "targetId" to targetId.take(128),
                "summary" to summary.take(400),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun seedInitialEcosystemIfNeeded(): Result<Boolean> {
        if (!isBootstrapAdmin()) return Result.success(false)
        val uid = auth.currentUser?.uid ?: return Result.success(false)
        return try {
            val existingRestaurants = db.collection("restaurants").limit(1).get().await()
            if (!existingRestaurants.isEmpty) return Result.success(false)

            val batch = db.batch()
            // 1. Two independent Business Clients (multi-tenant)
            batch.set(
                db.collection("tenants").document("tenant_north_culinary"),
                mapOf(
                    "name" to "North Culinary Group",
                    "code" to "NCG-01",
                    "city" to "Bengaluru",
                    "status" to "ACTIVE",
                    "plan" to "ENTERPRISE",
                    "ownerUid" to uid,
                    "contactEmail" to "ops@northculinary.in",
                    "commissionPercent" to 15L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("tenants").document("tenant_south_spice"),
                mapOf(
                    "name" to "South Spice Collective",
                    "code" to "SSC-02",
                    "city" to "Bengaluru",
                    "status" to "ACTIVE",
                    "plan" to "GROWTH",
                    "ownerUid" to uid,
                    "contactEmail" to "care@southspice.in",
                    "commissionPercent" to 12L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Four Curated Restaurants
            val restaurants = listOf(
                mapOf(
                    "id" to "rest_royal_darbar",
                    "name" to "Royal Darbar Kitchen",
                    "cuisine" to "North Indian • Biryani",
                    "city" to "Bengaluru",
                    "addressLabel" to "100 Feet Rd, Indiranagar",
                    "tenantId" to "tenant_north_culinary",
                    "rating" to 4.8,
                    "reviewCount" to 342L,
                    "deliveryTimeMins" to 25L,
                    "deliveryFeeInr" to 29L,
                    "minOrderInr" to 149L,
                    "commissionPercent" to 15L,
                    "imageKey" to "biryani",
                    "logoLabel" to "RD",
                    "promoTag" to "20% OFF",
                    "latitude" to 12.9784,
                    "longitude" to 77.6408
                ),
                mapOf(
                    "id" to "rest_napoli_oven",
                    "name" to "Napoli Woodfire Oven",
                    "cuisine" to "Pizza • Fast Food",
                    "city" to "Bengaluru",
                    "addressLabel" to "80 Feet Rd, Koramangala",
                    "tenantId" to "tenant_north_culinary",
                    "rating" to 4.7,
                    "reviewCount" to 218L,
                    "deliveryTimeMins" to 22L,
                    "deliveryFeeInr" to 0L,
                    "minOrderInr" to 199L,
                    "commissionPercent" to 15L,
                    "imageKey" to "pizza",
                    "logoLabel" to "NO",
                    "promoTag" to "FREE DELIVERY",
                    "latitude" to 12.9352,
                    "longitude" to 77.6245
                ),
                mapOf(
                    "id" to "rest_madras_tiffin",
                    "name" to "Madras Heritage Tiffin",
                    "cuisine" to "South Indian • Healthy Food",
                    "city" to "Bengaluru",
                    "addressLabel" to "4th Block, Jayanagar",
                    "tenantId" to "tenant_south_spice",
                    "rating" to 4.9,
                    "reviewCount" to 410L,
                    "deliveryTimeMins" to 18L,
                    "deliveryFeeInr" to 19L,
                    "minOrderInr" to 99L,
                    "commissionPercent" to 12L,
                    "imageKey" to "promo_dark",
                    "logoLabel" to "MH",
                    "promoTag" to "TOP RATED",
                    "latitude" to 12.9299,
                    "longitude" to 77.5824
                ),
                mapOf(
                    "id" to "rest_wok_smash",
                    "name" to "Wok & Smash Craft",
                    "cuisine" to "Chinese • Burgers • Desserts",
                    "city" to "Bengaluru",
                    "addressLabel" to "27th Main, HSR Layout",
                    "tenantId" to "tenant_south_spice",
                    "rating" to 4.6,
                    "reviewCount" to 189L,
                    "deliveryTimeMins" to 28L,
                    "deliveryFeeInr" to 25L,
                    "minOrderInr" to 149L,
                    "commissionPercent" to 12L,
                    "imageKey" to "hero_food",
                    "logoLabel" to "WS",
                    "promoTag" to "FLAT ₹50 OFF",
                    "latitude" to 12.9121,
                    "longitude" to 77.6446
                )
            )
            restaurants.forEach { r ->
                val rid = r["id"] as String
                val data: MutableMap<String, Any> = r.toMutableMap()
                data.remove("id")
                data["ownerUid"] = uid
                data["openNow"] = true
                data["approvalStatus"] = "APPROVED"
                data["createdAt"] = FieldValue.serverTimestamp()
                data["updatedAt"] = FieldValue.serverTimestamp()
                batch.set(db.collection("restaurants").document(rid), data)
            }

            // 3. Twelve Menu Items covering all 10 cuisines
            data class SeedDish(
                val id: String,
                val restaurantId: String,
                val restaurantName: String,
                val tenantId: String,
                val name: String,
                val desc: String,
                val category: String,
                val price: Long,
                val origPrice: Long,
                val discount: Long,
                val veg: Boolean,
                val prep: Long,
                val imgKey: String,
                val rating: Double,
                val reviews: Long,
                val addons: List<String>
            )
            val dishes = listOf(
                SeedDish("item_dum_biryani", "rest_royal_darbar", "Royal Darbar Kitchen", "tenant_north_culinary", "Hyderabadi Dum Biryani", "Slow-cooked saffron basmati rice layered with aromatic spices and mirchi ka salan.", "Biryani", 329L, 399L, 18L, false, 22L, "biryani", 4.9, 128L, listOf("Extra Raita (+₹30)", "Double Masala (+₹45)", "Roasted Egg (+₹25)")),
                SeedDish("item_butter_chicken", "rest_royal_darbar", "Royal Darbar Kitchen", "tenant_north_culinary", "Classic Butter Chicken & Naan", "Char-grilled tikka simmered in silky tomato-makhani gravy with 2 garlic naans.", "North Indian", 369L, 429L, 14L, false, 24L, "promo_dark", 4.8, 96L, listOf("Extra Garlic Naan (+₹49)", "Extra Butter (+₹20)")),
                SeedDish("item_paneer_thali", "rest_royal_darbar", "Royal Darbar Kitchen", "tenant_north_culinary", "Shahi Paneer Craft Thali", "Paneer butter masala, dal makhani, jeera pulao, 2 rotis, and gulab jamun.", "Indian", 289L, 329L, 12L, true, 20L, "promo_dark", 4.7, 75L, listOf("Sweet Lassi (+₹60)", "Extra Gulab Jamun (+₹35)")),
                SeedDish("item_margherita", "rest_napoli_oven", "Napoli Woodfire Oven", "tenant_north_culinary", "Artisan Woodfire Margherita", "San Marzano tomato sauce, fior di latte mozzarella, cold-pressed olive oil, fresh basil.", "Pizza", 349L, 399L, 13L, true, 18L, "pizza", 4.8, 114L, listOf("Extra Mozzarella (+₹70)", "Truffle Oil Drizzle (+₹65)")),
                SeedDish("item_pepperoni_feast", "rest_napoli_oven", "Napoli Woodfire Oven", "tenant_north_culinary", "Smoked Pepperoni Flatbread", "Crispy blistered sourdough crust topped with spiced pepperoni and aged parmesan.", "Pizza", 429L, 499L, 14L, false, 20L, "pizza", 4.7, 87L, listOf("Jalapeno Dip (+₹30)", "Stuffed Crust (+₹85)")),
                SeedDish("item_garlic_knots", "rest_napoli_oven", "Napoli Woodfire Oven", "tenant_north_culinary", "Herb Roasted Garlic Knots", "Warm oven-baked sourdough knots brushed with roasted garlic butter.", "Fast Food", 159L, 189L, 16L, true, 12L, "pizza", 4.6, 64L, listOf("Marinara Dip (+₹25)", "Cheesy Dip (+₹35)")),
                SeedDish("item_masala_dosa", "rest_madras_tiffin", "Madras Heritage Tiffin", "tenant_south_spice", "Ghee Roast Masala Dosa", "Crisp golden lentil crepe with spiced potato palya, sambar, and 3 fresh chutneys.", "South Indian", 149L, 179L, 17L, true, 14L, "hero_food", 4.9, 162L, listOf("Extra Ghee (+₹25)", "Filter Coffee (+₹45)")),
                SeedDish("item_quinoa_bowl", "rest_madras_tiffin", "Madras Heritage Tiffin", "tenant_south_spice", "Millet & Avocado Power Bowl", "High-protein sprouted millet bowl with roasted paneer, greens, and lemon tahini.", "Healthy Food", 259L, 299L, 13L, true, 15L, "hero_food", 4.8, 52L, listOf("Extra Avocado (+₹60)", "Chia Seed Topping (+₹25)")),
                SeedDish("item_schezwan_noodles", "rest_wok_smash", "Wok & Smash Craft", "tenant_south_spice", "Wok-Tossed Schezwan Hakka", "High-flame wok noodles tossed with crisp bell peppers, scallions, and house chili oil.", "Chinese", 229L, 269L, 15L, true, 16L, "hero_food", 4.6, 78L, listOf("Crispy Chilli Oil (+₹30)", "Extra Tofu (+₹50)")),
                SeedDish("item_smash_burger", "rest_wok_smash", "Wok & Smash Craft", "tenant_south_spice", "Double Truffle Smash Burger", "Brioche bun, double seared patty, aged cheddar, caramelized onions, truffle aioli.", "Burgers", 299L, 349L, 14L, false, 18L, "hero_food", 4.8, 103L, listOf("Peri-Peri Fries (+₹79)", "Extra Cheddar (+₹40)")),
                SeedDish("item_tiramisu_jar", "rest_wok_smash", "Wok & Smash Craft", "tenant_south_spice", "Artisan Espresso Tiramisu", "Velvety mascarpone cream layered over espresso-soaked savoiardi and cocoa dust.", "Desserts", 199L, 239L, 17L, true, 10L, "biryani", 4.9, 91L, listOf("Salted Caramel Drizzle (+₹30)", "Choco Shavings (+₹25)")),
                SeedDish("item_paneer_biryani", "rest_royal_darbar", "Royal Darbar Kitchen", "tenant_north_culinary", "Subz Zafrani Paneer Biryani", "Fragrant saffron basmati rice cooked on dum with marinated malai paneer cubes.", "Biryani", 279L, 329L, 15L, true, 20L, "biryani", 4.7, 84L, listOf("Burhani Raita (+₹30)", "Extra Paneer (+₹55)"))
            )
            dishes.forEach { d ->
                batch.set(
                    db.collection("menu_items").document(d.id),
                    mapOf(
                        "restaurantId" to d.restaurantId,
                        "restaurantName" to d.restaurantName,
                        "tenantId" to d.tenantId,
                        "ownerUid" to uid,
                        "name" to d.name,
                        "description" to d.desc,
                        "category" to d.category,
                        "priceInr" to d.price,
                        "originalPriceInr" to d.origPrice,
                        "discountPercent" to d.discount,
                        "veg" to d.veg,
                        "available" to true,
                        "prepTimeMins" to d.prep,
                        "imageKey" to d.imgKey,
                        "rating" to d.rating,
                        "reviewCount" to d.reviews,
                        "customizations" to d.addons,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 4. Three Active Coupons
            listOf(
                Triple("cpn_craft50", "CRAFT50", Triple("20% Off Orders Above ₹249", 20L, Pair(100L, 249L))),
                Triple("cpn_freedel", "FREEDEL", Triple("15% Instant Savings on ₹149+", 15L, Pair(60L, 149L))),
                Triple("cpn_royal25", "ROYAL25", Triple("25% Off Feast Orders ₹499+", 25L, Pair(150L, 499L)))
            ).forEach { (id, code, details) ->
                batch.set(
                    db.collection("coupons").document(id),
                    mapOf(
                        "code" to code,
                        "title" to details.first,
                        "discountPercent" to details.second,
                        "maxDiscountInr" to details.third.first,
                        "minOrderInr" to details.third.second,
                        "active" to true,
                        "tenantId" to "global",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 5. Initial Approved Delivery Partner profile for seamless verification testing
            batch.set(
                db.collection("delivery_partners").document(uid),
                mapOf(
                    "uid" to uid,
                    "displayName" to (auth.currentUser?.displayName ?: "CraftEats Lead Rider"),
                    "vehicleType" to "Electric Scooter",
                    "vehiclePlate" to "KA05CE2026",
                    "city" to "Bengaluru",
                    "approvalStatus" to "APPROVED",
                    "online" to true,
                    "completedDeliveries" to 12L,
                    "totalEarningsInr" to 1480L,
                    "rating" to 4.9,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed")
            Result.failure(e)
        }
    }

    private fun logAndRethrowIfNeeded(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
