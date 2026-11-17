package com.example.data

import com.example.model.AppNotification
import com.example.model.CartItem
import com.example.model.Coupon
import com.example.model.DeliveryAddress
import com.example.model.FoodOrder
import com.example.model.LiveLocation
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.model.Review
import com.example.model.SupportTicket
import com.example.model.UserProfile
import com.google.firebase.Firebase
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

class CatalogAndCustomerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")

    private fun isBootstrapAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email == AuthRepository.BOOTSTRAP_ADMIN_EMAIL
    }

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val profile = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    uid = uid,
                    email = user.email ?: "user@crafteats.app",
                    displayName = user.displayName ?: "CraftEats User"
                )
                Result.success(profile)
            } else {
                val role = if (isBootstrapAdmin()) "SUPER_ADMIN" else "CUSTOMER"
                val payload = mapOf(
                    "uid" to uid,
                    "email" to (user.email ?: "user@crafteats.app").take(200),
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "CraftEats User").take(100),
                    "photoUrl" to user.photoUrl?.toString()?.take(600),
                    "role" to role,
                    "accountStatus" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    uid = uid,
                    email = user.email ?: "user@crafteats.app",
                    displayName = user.displayName ?: "CraftEats User",
                    role = role
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logAndRethrowIfNeeded(error, OperationType.GET, "users/$uid", uid)
            }

    suspend fun updateUserProfile(uid: String, displayName: String, phone: String?): Result<Unit> {
        val path = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Refusing cross-user profile update" }
            val cleanName = displayName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty" }
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!phone.isNullOrBlank()) {
                updates["phone"] = phone.trim().take(25)
            }
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeApprovedRestaurants(): Flow<List<Restaurant>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("restaurants")
                .whereEqualTo("approvalStatus", "APPROVED")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.rating }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "restaurants", uid) }
        )
    }

    fun observeAvailableMenuItems(): Flow<List<MenuItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("menu_items")
                .whereEqualTo("available", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.rating }
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "menu_items", uid) }
        )
    }

    fun observeActiveCoupons(): Flow<List<Coupon>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("coupons")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "coupons", uid) }
        )
    }

    fun observePublishedReviews(): Flow<List<Review>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reviews")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "reviews", uid) }
        )
    }

    fun observeCart(uid: String): Flow<List<CartItem>> =
        db.collection("users").document(uid).collection("cart")
            .snapshots()
            .map { snap ->
                snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "users/$uid/cart", uid) }

    suspend fun addToCart(
        item: MenuItem,
        restaurant: Restaurant,
        selectedAddOn: String?,
        addOnPriceInr: Long
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "anon"}/cart"
        return try {
            val uid = requireUserId()
            val cartCol = db.collection("users").document(uid).collection("cart")
            val existingSnap = cartCol.get().await()
            val existingItems = existingSnap.toObjects(
                CartItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            // Enforce single-restaurant cart cleanly by clearing different restaurant items
            val batch = db.batch()
            existingItems.filter { it.restaurantId != restaurant.id }.forEach { other ->
                batch.delete(cartCol.document(other.id))
            }
            val matching = existingItems.firstOrNull {
                it.restaurantId == restaurant.id &&
                    it.menuItemId == item.id &&
                    (it.selectedAddOn ?: "") == (selectedAddOn ?: "")
            }
            if (matching != null) {
                val docRef = cartCol.document(matching.id)
                path = docRef.path
                val nextQty = (matching.quantity + 1L).coerceAtMost(50L)
                batch.update(
                    docRef,
                    mapOf(
                        "quantity" to nextQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val newId = "cart_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val docRef = cartCol.document(newId)
                path = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "restaurantId" to restaurant.id,
                    "restaurantName" to restaurant.name.take(120),
                    "restaurantOwnerUid" to restaurant.ownerUid,
                    "tenantId" to restaurant.tenantId,
                    "menuItemId" to item.id,
                    "name" to item.name.take(120),
                    "priceInr" to item.priceInr.coerceIn(1L, 50000L),
                    "quantity" to 1L,
                    "veg" to item.veg,
                    "imageKey" to item.imageKey.take(80),
                    "imageUri" to item.imageUri?.take(600),
                    "selectedAddOn" to selectedAddOn?.takeIf { it.isNotBlank() }?.take(80),
                    "addOnPriceInr" to if (!selectedAddOn.isNullOrBlank()) addOnPriceInr.coerceIn(0L, 5000L) else null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQty: Long): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "anon"}/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
            path = docRef.path
            if (newQty <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQty.coerceIn(1L, 50L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAddresses(uid: String): Flow<List<DeliveryAddress>> =
        db.collection("users").document(uid).collection("addresses")
            .snapshots()
            .map { snap ->
                snap.toObjects(DeliveryAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "users/$uid/addresses", uid) }

    suspend fun saveAddress(
        label: String,
        addressLine: String,
        landmark: String?,
        city: String,
        pincode: String,
        latitude: Double,
        longitude: Double,
        primary: Boolean
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "anon"}/addresses"
        return try {
            val uid = requireUserId()
            require(label.trim().isNotEmpty()) { "Address label is required" }
            require(addressLine.trim().length >= 3) { "Address line must be at least 3 characters" }
            require(city.trim().length >= 2) { "City is required" }
            require(pincode.trim().length in 4..12) { "Valid pincode is required" }

            val col = db.collection("users").document(uid).collection("addresses")
            val newId = "addr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val docRef = col.document(newId)
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "label" to label.trim().take(40),
                "addressLine" to addressLine.trim().take(250),
                "landmark" to landmark?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
                "city" to city.trim().take(80),
                "pincode" to pincode.trim().take(12),
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "primaryAddress" to primary,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "anon"}/addresses/$addressId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("addresses").document(addressId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCustomerOrders(uid: String): Flow<List<FoodOrder>> =
        db.collection("orders")
            .whereEqualTo("customerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(FoodOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "orders", uid) }

    fun observeCustomerLiveLocations(uid: String): Flow<List<LiveLocation>> =
        db.collection("live_locations")
            .whereEqualTo("customerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(LiveLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "live_locations", uid) }

    suspend fun placeOrder(
        customerName: String,
        restaurant: Restaurant,
        cartItems: List<CartItem>,
        address: DeliveryAddress,
        couponCode: String?,
        discountInr: Long,
        paymentMethod: String,
        razorpayOrderId: String?,
        razorpayPaymentId: String?
    ): Result<String> {
        var path = "orders"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
            val subtotal = cartItems.sumOf { (it.priceInr + (it.addOnPriceInr ?: 0L)) * it.quantity }
            val deliveryFee = if (subtotal >= 199L) 0L else restaurant.deliveryFeeInr
            val finalDiscount = discountInr.coerceIn(0L, subtotal)
            val total = (subtotal + deliveryFee - finalDiscount).coerceAtLeast(1L)
            val commissionInr = ((subtotal * restaurant.commissionPercent) / 100L).coerceAtLeast(0L)
            val paymentStatus = if (paymentMethod == "COD") "COD_PENDING" else "PAID"

            val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            val orderRef = db.collection("orders").document(orderId)
            val paymentRef = db.collection("payments").document(paymentId)
            val notifRef = db.collection("users").document(uid).collection("notifications").document(notifId)
            path = orderRef.path

            val itemSummaries = cartItems.take(25).map { item ->
                val addOnPart = item.selectedAddOn?.let { " ($it)" } ?: ""
                "${item.quantity}x ${item.name}$addOnPart - ₹${(item.priceInr + (item.addOnPriceInr ?: 0L)) * item.quantity}"
            }

            val orderPayload = mapOf(
                "customerUid" to uid,
                "customerName" to customerName.ifBlank { "Customer" }.take(100),
                "restaurantId" to restaurant.id,
                "restaurantName" to restaurant.name.take(120),
                "restaurantOwnerUid" to restaurant.ownerUid,
                "tenantId" to restaurant.tenantId,
                "status" to "PLACED",
                "items" to itemSummaries,
                "subtotalInr" to subtotal,
                "deliveryFeeInr" to deliveryFee,
                "discountInr" to finalDiscount,
                "totalInr" to total,
                "couponCode" to couponCode?.takeIf { it.isNotBlank() }?.take(40),
                "paymentMethod" to paymentMethod,
                "paymentStatus" to paymentStatus,
                "razorpayOrderId" to razorpayOrderId?.take(80),
                "razorpayPaymentId" to razorpayPaymentId?.take(80),
                "deliveryAddressLabel" to "${address.label}: ${address.addressLine}, ${address.city}".take(250),
                "deliveryLat" to address.latitude.coerceIn(-90.0, 90.0),
                "deliveryLng" to address.longitude.coerceIn(-180.0, 180.0),
                "restaurantLat" to restaurant.latitude.coerceIn(-90.0, 90.0),
                "restaurantLng" to restaurant.longitude.coerceIn(-180.0, 180.0),
                "prepTimeMins" to restaurant.deliveryTimeMins.coerceIn(5L, 120L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val paymentPayload = mapOf(
                "orderId" to orderId,
                "customerUid" to uid,
                "restaurantId" to restaurant.id,
                "restaurantOwnerUid" to restaurant.ownerUid,
                "tenantId" to restaurant.tenantId,
                "amountInr" to total,
                "commissionInr" to commissionInr,
                "method" to paymentMethod,
                "status" to paymentStatus,
                "razorpayOrderId" to razorpayOrderId?.take(80),
                "razorpayPaymentId" to razorpayPaymentId?.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val notifPayload = mapOf(
                "userId" to uid,
                "title" to "Order Placed at ${restaurant.name}".take(120),
                "body" to "Order #$orderId for ₹$total has been sent to the kitchen.".take(400),
                "category" to "ORDER",
                "orderId" to orderId,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Order must exist before payment rule exists() validates it, so commit order + cart clear + notif first, then payment
            val batch = db.batch()
            batch.set(orderRef, orderPayload)
            batch.set(notifRef, notifPayload)
            cartItems.forEach { cartItem ->
                batch.delete(db.collection("users").document(uid).collection("cart").document(cartItem.id))
            }
            batch.commit().await()

            paymentRef.set(paymentPayload).await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to "CANCELLED",
                    "cancellationReason" to reason.ifBlank { "Cancelled by customer" }.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReview(
        orderId: String,
        restaurantId: String,
        customerName: String,
        rating: Long,
        comment: String
    ): Result<Unit> {
        val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "reviews/$reviewId"
        return try {
            val uid = requireUserId()
            require(comment.trim().isNotEmpty()) { "Review comment is required" }
            val payload = mapOf(
                "orderId" to orderId,
                "restaurantId" to restaurantId,
                "customerUid" to uid,
                "customerName" to customerName.ifBlank { "Customer" }.take(100),
                "rating" to rating.coerceIn(1L, 5L),
                "comment" to comment.trim().take(500),
                "published" to true,
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

    fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "users/$uid/notifications", uid) }

    suspend fun markNotificationRead(uid: String, notificationId: String): Result<Unit> {
        val path = "users/$uid/notifications/$notificationId"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Refusing cross-user notification update" }
            db.document(path).update(
                mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMySupportTickets(uid: String): Flow<List<SupportTicket>> =
        db.collection("support_tickets")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error -> logAndRethrowIfNeeded(error, OperationType.LIST, "support_tickets", uid) }

    suspend fun createSupportTicket(userName: String, role: String, subject: String, message: String): Result<Unit> {
        val id = "tkt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "support_tickets/$id"
        return try {
            val uid = requireUserId()
            require(subject.trim().length >= 3) { "Subject must be at least 3 characters" }
            require(message.trim().length >= 5) { "Message must be at least 5 characters" }
            val payload = mapOf(
                "userId" to uid,
                "userName" to userName.ifBlank { "User" }.take(100),
                "role" to role.take(40),
                "subject" to subject.trim().take(120),
                "message" to message.trim().take(800),
                "status" to "OPEN",
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
