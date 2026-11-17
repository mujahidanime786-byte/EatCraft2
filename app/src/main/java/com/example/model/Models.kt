package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppPortal(val label: String) {
    CUSTOMER("Customer"),
    RESTAURANT("Restaurant Partner"),
    DELIVERY("Delivery Partner"),
    SUPER_ADMIN("Super Admin")
}

enum class OrderStatus(val label: String) {
    PLACED("Order Placed"),
    ACCEPTED("Accepted by Kitchen"),
    PREPARING("Preparing Food"),
    READY_FOR_PICKUP("Ready for Pickup"),
    DRIVER_ASSIGNED("Partner Assigned"),
    REACHED_RESTAURANT("Reached Restaurant"),
    PICKED_UP("Order Picked Up"),
    ON_THE_WAY("On the Way"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PLACED
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val role: String = "CUSTOMER",
    val accountStatus: String = "ACTIVE",
    val tenantId: String? = null,
    val restaurantId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val addressLine: String = "",
    val landmark: String? = null,
    val city: String = "Bengaluru",
    val pincode: String = "560001",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    val primaryAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantOwnerUid: String = "",
    val tenantId: String = "",
    val menuItemId: String = "",
    val name: String = "",
    val priceInr: Long = 0L,
    val quantity: Long = 1L,
    val veg: Boolean = true,
    val imageKey: String = "biryani",
    val imageUri: String? = null,
    val selectedAddOn: String? = null,
    val addOnPriceInr: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "ORDER",
    val orderId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessTenant(
    @DocumentId val id: String = "",
    val name: String = "",
    val code: String = "",
    val city: String = "",
    val status: String = "ACTIVE",
    val plan: String = "ENTERPRISE",
    val ownerUid: String = "",
    val contactEmail: String? = null,
    val commissionPercent: Long = 15L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Restaurant(
    @DocumentId val id: String = "",
    val name: String = "",
    val cuisine: String = "",
    val city: String = "Bengaluru",
    val addressLabel: String = "",
    val tenantId: String = "",
    val ownerUid: String = "",
    val rating: Double = 4.5,
    val reviewCount: Long = 0L,
    val deliveryTimeMins: Long = 30L,
    val deliveryFeeInr: Long = 29L,
    val minOrderInr: Long = 149L,
    val commissionPercent: Long = 15L,
    val openNow: Boolean = true,
    val approvalStatus: String = "PENDING",
    val imageKey: String = "biryani",
    val logoLabel: String? = null,
    val promoTag: String? = null,
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val tenantId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Indian",
    val priceInr: Long = 199L,
    val originalPriceInr: Long = 249L,
    val discountPercent: Long = 0L,
    val veg: Boolean = true,
    val available: Boolean = true,
    val prepTimeMins: Long = 20L,
    val imageKey: String = "biryani",
    val imageUri: String? = null,
    val rating: Double = 4.6,
    val reviewCount: Long = 48L,
    val customizations: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FoodOrder(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantOwnerUid: String = "",
    val tenantId: String = "",
    val driverUid: String? = null,
    val driverName: String? = null,
    val status: String = "PLACED",
    val items: List<String> = emptyList(),
    val subtotalInr: Long = 0L,
    val deliveryFeeInr: Long = 0L,
    val discountInr: Long = 0L,
    val totalInr: Long = 0L,
    val couponCode: String? = null,
    val paymentMethod: String = "RAZORPAY_UPI",
    val paymentStatus: String = "CREATED",
    val razorpayOrderId: String? = null,
    val razorpayPaymentId: String? = null,
    val deliveryAddressLabel: String = "",
    val deliveryLat: Double = 12.9716,
    val deliveryLng: Double = 77.5946,
    val restaurantLat: Double = 12.9716,
    val restaurantLng: Double = 77.5946,
    val prepTimeMins: Long = 25L,
    val cancellationReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryOffer(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val tenantId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantOwnerUid: String = "",
    val restaurantAddress: String = "",
    val areaLabel: String = "",
    val payoutInr: Long = 45L,
    val distanceKm: Double = 2.8,
    val status: String = "OPEN",
    val driverUid: String? = null,
    val restaurantLat: Double = 12.9716,
    val restaurantLng: Double = 77.5946,
    val deliveryLat: Double = 12.9780,
    val deliveryLng: Double = 77.6010,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryPartner(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val vehicleType: String = "Electric Scooter",
    val vehiclePlate: String = "",
    val city: String = "Bengaluru",
    val approvalStatus: String = "PENDING",
    val online: Boolean = false,
    val completedDeliveries: Long = 0L,
    val totalEarningsInr: Long = 0L,
    val rating: Double = 4.9,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val tenantId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveLocation(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val customerUid: String = "",
    val restaurantOwnerUid: String = "",
    val driverUid: String = "",
    val tenantId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val speedMps: Double = 0.0,
    val bearing: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val customerUid: String = "",
    val restaurantId: String = "",
    val restaurantOwnerUid: String = "",
    val tenantId: String = "",
    val amountInr: Long = 0L,
    val commissionInr: Long = 0L,
    val method: String = "RAZORPAY_UPI",
    val status: String = "CREATED",
    val razorpayOrderId: String? = null,
    val razorpayPaymentId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val discountPercent: Long = 10L,
    val maxDiscountInr: Long = 100L,
    val minOrderInr: Long = 199L,
    val active: Boolean = true,
    val tenantId: String = "global",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val restaurantId: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val role: String = "CUSTOMER",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val speedMps: Double,
    val bearing: Double,
    val capturedAtMillis: Long
)
