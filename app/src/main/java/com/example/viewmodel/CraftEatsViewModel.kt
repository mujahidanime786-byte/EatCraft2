package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CatalogAndCustomerRepository
import com.example.data.OperationsAndAdminRepository
import com.example.model.AppNotification
import com.example.model.AppPortal
import com.example.model.AuditLog
import com.example.model.BusinessTenant
import com.example.model.CartItem
import com.example.model.Coupon
import com.example.model.DeliveryAddress
import com.example.model.DeliveryOffer
import com.example.model.DeliveryPartner
import com.example.model.FoodOrder
import com.example.model.LiveLocation
import com.example.model.LocationSample
import com.example.model.MenuItem
import com.example.model.PaymentRecord
import com.example.model.Restaurant
import com.example.model.Review
import com.example.model.SupportTicket
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CraftEatsViewModel(
    private val customerRepo: CatalogAndCustomerRepository,
    private val opsRepo: OperationsAndAdminRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isAdmin: Boolean
) : ViewModel() {

    private val _activePortal = MutableStateFlow(AppPortal.CUSTOMER)
    val activePortal: StateFlow<AppPortal> = _activePortal.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _lastCapturedGps = MutableStateFlow<LocationSample?>(null)
    val lastCapturedGps: StateFlow<LocationSample?> = _lastCapturedGps.asStateFlow()

    // Customer & Catalog States
    val userProfile: StateFlow<UserProfile?> = customerRepo.observeUserProfile(currentUserId)
        .catch { e -> handleStreamError("userProfile", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val approvedRestaurants: StateFlow<UiState<List<Restaurant>>> =
        customerRepo.observeApprovedRestaurants().toUiState("restaurants")

    val availableMenuItems: StateFlow<UiState<List<MenuItem>>> =
        customerRepo.observeAvailableMenuItems().toUiState("menu_items")

    val activeCoupons: StateFlow<List<Coupon>> = customerRepo.observeActiveCoupons()
        .catch { e -> handleStreamError("coupons", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publishedReviews: StateFlow<List<Review>> = customerRepo.observePublishedReviews()
        .catch { e -> handleStreamError("reviews", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val cartItems: StateFlow<List<CartItem>> = customerRepo.observeCart(currentUserId)
        .catch { e -> handleStreamError("cart", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val addresses: StateFlow<List<DeliveryAddress>> = customerRepo.observeAddresses(currentUserId)
        .catch { e -> handleStreamError("addresses", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val customerOrders: StateFlow<UiState<List<FoodOrder>>> =
        customerRepo.observeCustomerOrders(currentUserId).toUiState("customerOrders")

    val customerLiveLocations: StateFlow<List<LiveLocation>> =
        customerRepo.observeCustomerLiveLocations(currentUserId)
            .catch { e -> handleStreamError("live_locations", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        customerRepo.observeNotifications(currentUserId)
            .catch { e -> handleStreamError("notifications", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val mySupportTickets: StateFlow<List<SupportTicket>> =
        customerRepo.observeMySupportTickets(currentUserId)
            .catch { e -> handleStreamError("support_tickets", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Restaurant Partner States
    val ownedRestaurants: StateFlow<List<Restaurant>> = opsRepo.observeOwnedRestaurants(currentUserId)
        .catch { e -> handleStreamError("ownedRestaurants", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val ownedMenuItems: StateFlow<List<MenuItem>> = opsRepo.observeOwnedMenuItems(currentUserId)
        .catch { e -> handleStreamError("ownedMenuItems", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val restaurantOrders: StateFlow<List<FoodOrder>> = opsRepo.observeRestaurantOrders(currentUserId)
        .catch { e -> handleStreamError("restaurantOrders", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Delivery Partner States
    val deliveryPartnerProfile: StateFlow<DeliveryPartner?> =
        opsRepo.observeDeliveryPartnerProfile(currentUserId)
            .catch { e -> handleStreamError("deliveryPartnerProfile", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val openDeliveryOffers: StateFlow<List<DeliveryOffer>> = opsRepo.observeOpenDeliveryOffers()
        .catch { e -> handleStreamError("openDeliveryOffers", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val driverOrders: StateFlow<List<FoodOrder>> = opsRepo.observeDriverOrders(currentUserId)
        .catch { e -> handleStreamError("driverOrders", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Super Admin States (Strictly gated by isAdmin to prevent unauthorized queries)
    val adminTenants: StateFlow<List<BusinessTenant>> =
        (if (isAdmin) opsRepo.observeAllTenants() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminTenants", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminRestaurants: StateFlow<List<Restaurant>> =
        (if (isAdmin) opsRepo.observeAllRestaurantsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminRestaurants", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminUsers: StateFlow<List<UserProfile>> =
        (if (isAdmin) opsRepo.observeAllUsersAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminUsers", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminPartners: StateFlow<List<DeliveryPartner>> =
        (if (isAdmin) opsRepo.observeAllPartnersAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminPartners", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminOrders: StateFlow<List<FoodOrder>> =
        (if (isAdmin) opsRepo.observeAllOrdersAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminOrders", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminLiveLocations: StateFlow<List<LiveLocation>> =
        (if (isAdmin) opsRepo.observeAllLiveLocationsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminLiveLocations", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminPayments: StateFlow<List<PaymentRecord>> =
        (if (isAdmin) opsRepo.observeAllPaymentsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminPayments", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminCoupons: StateFlow<List<Coupon>> =
        (if (isAdmin) opsRepo.observeAllCouponsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminCoupons", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminTickets: StateFlow<List<SupportTicket>> =
        (if (isAdmin) opsRepo.observeAllTicketsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminTickets", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val adminAuditLogs: StateFlow<List<AuditLog>> =
        (if (isAdmin) opsRepo.observeAuditLogsAdmin() else flowOf(emptyList()))
            .catch { e -> handleStreamError("adminAuditLogs", e) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            customerRepo.ensureUserProfile()
            if (isAdmin) {
                opsRepo.seedInitialEcosystemIfNeeded()
            }
        }
    }

    fun switchPortal(portal: AppPortal) {
        if (portal == AppPortal.SUPER_ADMIN && !isAdmin) {
            postMessage("Super Admin access requires verified platform administrator credentials.")
            return
        }
        _activePortal.value = portal
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun postMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun updateCapturedGps(sample: LocationSample) {
        _lastCapturedGps.value = sample
    }

    fun applyCouponCode(code: String, subtotalInr: Long) {
        val clean = code.trim().uppercase()
        val match = activeCoupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.active }
        if (match == null) {
            postMessage("Coupon code '$clean' is invalid or inactive.")
        } else if (subtotalInr < match.minOrderInr) {
            postMessage("Minimum order ₹${match.minOrderInr} required for ${match.code}.")
        } else {
            _appliedCoupon.value = match
            postMessage("Applied ${match.code}: ${match.title}")
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
    }

    // --- CUSTOMER ACTIONS ---

    fun addToCart(
        item: MenuItem,
        restaurant: Restaurant,
        selectedAddOn: String?,
        addOnPriceInr: Long
    ) = launchAction("Added ${item.name} to cart") {
        customerRepo.addToCart(item, restaurant, selectedAddOn, addOnPriceInr)
    }

    fun updateCartQuantity(cartItemId: String, newQty: Long) = launchAction(null) {
        customerRepo.updateCartQuantity(cartItemId, newQty)
    }

    fun saveAddress(
        label: String,
        addressLine: String,
        landmark: String?,
        city: String,
        pincode: String,
        latitude: Double,
        longitude: Double,
        primary: Boolean
    ) = launchAction("Delivery address saved") {
        customerRepo.saveAddress(label, addressLine, landmark, city, pincode, latitude, longitude, primary)
    }

    fun deleteAddress(addressId: String) = launchAction("Address removed") {
        customerRepo.deleteAddress(addressId)
    }

    fun placeOrder(
        restaurant: Restaurant,
        cart: List<CartItem>,
        address: DeliveryAddress,
        paymentMethod: String,
        razorpayOrderId: String?,
        razorpayPaymentId: String?,
        onOrderPlaced: (String) -> Unit
    ) {
        viewModelScope.launch {
            val coupon = _appliedCoupon.value
            val subtotal = cart.sumOf { (it.priceInr + (it.addOnPriceInr ?: 0L)) * it.quantity }
            val discount = if (coupon != null && subtotal >= coupon.minOrderInr) {
                ((subtotal * coupon.discountPercent) / 100L).coerceAtMost(coupon.maxDiscountInr)
            } else 0L

            val res = customerRepo.placeOrder(
                customerName = userProfile.value?.displayName ?: "CraftEats Customer",
                restaurant = restaurant,
                cartItems = cart,
                address = address,
                couponCode = if (discount > 0L) coupon?.code else null,
                discountInr = discount,
                paymentMethod = paymentMethod,
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId
            )
            res.onSuccess { orderId ->
                _appliedCoupon.value = null
                postMessage("Order placed! Kitchen has received your order.")
                onOrderPlaced(orderId)
            }.onFailure { err ->
                postMessage(err.localizedMessage ?: "Failed to place order")
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String) = launchAction("Order cancelled") {
        customerRepo.cancelOrder(orderId, reason)
    }

    fun submitReview(orderId: String, restaurantId: String, rating: Long, comment: String) =
        launchAction("Thank you for your review!") {
            customerRepo.submitReview(
                orderId = orderId,
                restaurantId = restaurantId,
                customerName = userProfile.value?.displayName ?: "Customer",
                rating = rating,
                comment = comment
            )
        }

    fun markNotificationRead(notificationId: String) = launchAction(null) {
        customerRepo.markNotificationRead(currentUserId, notificationId)
    }

    fun updateProfile(displayName: String, phone: String?) = launchAction("Profile updated") {
        customerRepo.updateUserProfile(currentUserId, displayName, phone)
    }

    fun submitSupportTicket(subject: String, message: String) = launchAction("Support ticket submitted") {
        customerRepo.createSupportTicket(
            userName = userProfile.value?.displayName ?: "CraftEats User",
            role = _activePortal.value.name,
            subject = subject,
            message = message
        )
    }

    // --- RESTAURANT PARTNER ACTIONS ---

    fun registerRestaurant(
        name: String,
        cuisine: String,
        city: String,
        addressLabel: String,
        tenantId: String,
        deliveryFeeInr: Long,
        minOrderInr: Long
    ) = launchAction("Restaurant registration submitted") {
        opsRepo.registerRestaurant(name, cuisine, city, addressLabel, tenantId, deliveryFeeInr, minOrderInr)
    }

    fun toggleRestaurantOpen(restaurantId: String, openNow: Boolean) = launchAction(null) {
        opsRepo.toggleRestaurantOpen(restaurantId, openNow)
    }

    fun saveMenuItem(
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
    ) = launchAction(if (existingId == null) "Dish added to menu" else "Dish updated") {
        opsRepo.saveMenuItem(
            existingId, restaurant, name, description, category,
            priceInr, discountPercent, veg, available, prepTimeMins, imageKey, imageUri, customizations
        )
    }

    fun toggleMenuItemAvailability(itemId: String, available: Boolean) = launchAction(null) {
        opsRepo.toggleMenuItemAvailability(itemId, available)
    }

    fun updateRestaurantOrderStatus(
        order: FoodOrder,
        nextStatus: String,
        prepTimeMins: Long = order.prepTimeMins,
        rejectionReason: String? = null
    ) = launchAction("Order marked $nextStatus") {
        opsRepo.updateRestaurantOrderStatus(order, nextStatus, prepTimeMins, rejectionReason)
    }

    // --- DELIVERY PARTNER ACTIONS ---

    fun registerOrUpdateDeliveryPartner(
        displayName: String,
        vehicleType: String,
        vehiclePlate: String,
        city: String
    ) = launchAction("Delivery partner profile saved") {
        opsRepo.registerOrUpdateDeliveryPartner(displayName, vehicleType, vehiclePlate, city)
    }

    fun setDriverOnline(online: Boolean) = launchAction(
        if (online) "You are now online for deliveries" else "You are now offline"
    ) {
        opsRepo.setDriverOnline(online)
    }

    fun claimDeliveryOffer(offer: DeliveryOffer) = launchAction("Delivery assignment accepted") {
        val name = deliveryPartnerProfile.value?.displayName ?: userProfile.value?.displayName ?: "Partner"
        opsRepo.claimDeliveryOffer(offer, name)
    }

    fun advanceDeliveryOrderStatus(order: FoodOrder, nextStatus: String) =
        launchAction("Status updated to ${nextStatus.replace('_', ' ')}") {
            opsRepo.advanceDeliveryOrderStatus(order, nextStatus, deliveryPartnerProfile.value)
        }

    fun publishLiveGpsTelemetry(order: FoodOrder, sample: LocationSample) {
        _lastCapturedGps.value = sample
        launchAction("Live GPS coordinates shared") {
            opsRepo.publishLiveGpsTelemetry(order, sample)
        }
    }

    // --- SUPER ADMIN ACTIONS ---

    fun createBusinessTenant(
        name: String,
        code: String,
        city: String,
        plan: String,
        commissionPercent: Long,
        contactEmail: String?
    ) = launchAction("Business client created & audit logged") {
        opsRepo.createBusinessTenant(name, code, city, plan, commissionPercent, contactEmail)
    }

    fun updateTenantStatus(tenantId: String, nextStatus: String) =
        launchAction("Client status set to $nextStatus") {
            opsRepo.updateTenantStatus(tenantId, nextStatus)
        }

    fun moderateRestaurant(restaurantId: String, approvalStatus: String, commissionPercent: Long) =
        launchAction("Restaurant updated ($approvalStatus)") {
            opsRepo.updateRestaurantApprovalAndCommission(restaurantId, approvalStatus, commissionPercent)
        }

    fun moderatePartner(partnerUid: String, approvalStatus: String) =
        launchAction("Delivery partner marked $approvalStatus") {
            opsRepo.updatePartnerApproval(partnerUid, approvalStatus)
        }

    fun updateUserAccountStatus(targetUid: String, accountStatus: String) =
        launchAction("User status set to $accountStatus") {
            opsRepo.updateUserAccountStatus(targetUid, accountStatus)
        }

    fun adminRefundOrder(order: FoodOrder, paymentRecord: PaymentRecord?) =
        launchAction("Refund authorized & logged") {
            opsRepo.adminRefundOrder(order, paymentRecord)
        }

    fun createCoupon(
        code: String,
        title: String,
        discountPercent: Long,
        maxDiscountInr: Long,
        minOrderInr: Long,
        tenantId: String
    ) = launchAction("Coupon published") {
        opsRepo.createCoupon(code, title, discountPercent, maxDiscountInr, minOrderInr, tenantId)
    }

    fun resolveSupportTicket(ticketId: String, reply: String) =
        launchAction("Ticket resolved & logged") {
            opsRepo.resolveSupportTicket(ticketId, reply)
        }

    fun initializeCatalogManually() = launchAction("Multi-tenant catalog initialized") {
        opsRepo.seedInitialEcosystemIfNeeded().map { Unit }
    }

    // --- HELPERS ---

    private fun launchAction(successMessage: String?, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            val result = block()
            result.onSuccess {
                if (successMessage != null) postMessage(successMessage)
            }.onFailure { err ->
                postMessage(err.localizedMessage ?: "Action failed")
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    private fun handleStreamError(label: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream error in $label", firestoreError ?: error)
    }

    private companion object {
        const val TAG = "CraftEatsVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
