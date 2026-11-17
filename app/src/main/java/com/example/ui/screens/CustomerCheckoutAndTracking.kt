package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocationHelper
import com.example.model.DeliveryAddress
import com.example.model.FoodOrder
import com.example.model.Restaurant
import com.example.model.UiState
import com.example.ui.components.LiveRouteMapCanvas
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.OrderStatusTimeline
import com.example.ui.components.VegNonVegBadge
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MainText
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SecondaryLavenderBg
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WhiteBg
import com.example.viewmodel.CraftEatsViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CustomerCartAndCheckoutScreen(
    viewModel: CraftEatsViewModel,
    restaurants: List<Restaurant>,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val coupons by viewModel.activeCoupons.collectAsStateWithLifecycle()
    val appliedCoupon by viewModel.appliedCoupon.collectAsStateWithLifecycle()

    var couponInput by remember { mutableStateOf("") }
    var selectedAddressId by remember(addresses) { mutableStateOf(addresses.firstOrNull()?.id) }
    var selectedPaymentMethod by remember { mutableStateOf("RAZORPAY_UPI") }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showRazorpayTestSheet by remember { mutableStateOf(false) }

    val activeRestaurant = remember(cartItems, restaurants) {
        val rid = cartItems.firstOrNull()?.restaurantId
        restaurants.firstOrNull { it.id == rid }
    }
    val subtotal = cartItems.sumOf { (it.priceInr + (it.addOnPriceInr ?: 0L)) * it.quantity }
    val deliveryFee = if (subtotal >= 199L || activeRestaurant == null) 0L else activeRestaurant.deliveryFeeInr
    val discount = appliedCoupon?.let { c ->
        if (subtotal >= c.minOrderInr) ((subtotal * c.discountPercent) / 100L).coerceAtMost(c.maxDiscountInr) else 0L
    } ?: 0L
    val total = (subtotal + deliveryFee - discount).coerceAtLeast(0L)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                }
                Text("Cart & Secure Checkout", style = MaterialTheme.typography.titleLarge, color = MainText)
            }
        }

        if (cartItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Your cart is empty", style = MaterialTheme.typography.titleMedium, color = MainText)
                        Text("Browse Trending Food or Explore Cuisines to add fresh dishes.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                        ) {
                            Text("Explore Restaurants")
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoyalBlueSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ordering from ${cartItems.first().restaurantName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = RoyalBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(cartItems, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VegNonVegBadge(veg = item.veg)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, color = MainText)
                            if (!item.selectedAddOn.isNullOrBlank()) {
                                Text(item.selectedAddOn, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                            }
                            Text("₹${(item.priceInr + (item.addOnPriceInr ?: 0L)) * item.quantity}", style = MaterialTheme.typography.labelMedium, color = MainText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1L) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity", tint = MainText)
                            }
                            Text(item.quantity.toString(), style = MaterialTheme.typography.titleSmall, color = MainText)
                            IconButton(onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1L) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = RoyalBlue)
                            }
                        }
                    }
                }
            }

            // Coupon section
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Apply Coupon", style = MaterialTheme.typography.titleSmall, color = MainText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                placeholder = { Text("Enter CRAFT50") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { viewModel.applyCouponCode(couponInput, subtotal) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                            ) {
                                Text("Apply")
                            }
                        }
                        if (coupons.isNotEmpty()) {
                            Text(
                                "Available: ${coupons.joinToString(" • ") { it.code }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue
                            )
                        }
                        if (appliedCoupon != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Applied ${appliedCoupon!!.code} (-₹$discount)", color = SuccessGreen, style = MaterialTheme.typography.labelMedium)
                                TextButton(onClick = { viewModel.removeCoupon() }) { Text("Remove", color = ErrorRed) }
                            }
                        }
                    }
                }
            }

            // Delivery Address section
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Delivery Address", style = MaterialTheme.typography.titleSmall, color = MainText)
                            TextButton(onClick = { showAddressDialog = true }) {
                                Text("+ Add Address", color = RoyalBlue)
                            }
                        }
                        if (addresses.isEmpty()) {
                            Text("No saved address yet. Add one with GPS or manual details.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        } else {
                            addresses.forEach { addr ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAddressId = addr.id }
                                ) {
                                    RadioButton(
                                        selected = selectedAddressId == addr.id,
                                        onClick = { selectedAddressId = addr.id }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${addr.label} • ${addr.city}", style = MaterialTheme.typography.labelMedium, color = MainText)
                                        Text(addr.addressLine, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Payment Method (Razorpay Test Mode + COD)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Payment Method (Razorpay Test Mode & COD)", style = MaterialTheme.typography.titleSmall, color = MainText)
                        val methods = listOf(
                            "RAZORPAY_UPI" to "UPI (GPay, PhonePe, Paytm) • Razorpay Test",
                            "RAZORPAY_CARD" to "Credit / Debit Card • Razorpay Test",
                            "RAZORPAY_NETBANKING" to "NetBanking • Razorpay Test",
                            "COD" to "Cash on Delivery (Pay at doorstep)"
                        )
                        methods.forEach { (code, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaymentMethod = code }
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == code,
                                    onClick = { selectedPaymentMethod = code }
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall, color = MainText)
                            }
                        }
                    }
                }
            }

            // Bill Summary + Place Order
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BillRow("Subtotal", "₹$subtotal")
                        BillRow("Delivery Fee", if (deliveryFee == 0L) "FREE" else "₹$deliveryFee")
                        if (discount > 0L) BillRow("Coupon Discount", "-₹$discount", SuccessGreen)
                        HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                        BillRow("Total Payable", "₹$total", MainText, bold = true)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val chosenAddr = addresses.firstOrNull { it.id == selectedAddressId }
                                val rest = activeRestaurant
                                if (chosenAddr == null) {
                                    viewModel.postMessage("Please add and select a delivery address first.")
                                    showAddressDialog = true
                                } else if (rest == null) {
                                    viewModel.postMessage("Restaurant metadata unavailable.")
                                } else if (selectedPaymentMethod == "COD") {
                                    viewModel.placeOrder(
                                        restaurant = rest,
                                        cart = cartItems,
                                        address = chosenAddr,
                                        paymentMethod = "COD",
                                        razorpayOrderId = null,
                                        razorpayPaymentId = null,
                                        onOrderPlaced = onOrderPlaced
                                    )
                                } else {
                                    showRazorpayTestSheet = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("place_order_button")
                        ) {
                            Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selectedPaymentMethod == "COD") "Place COD Order • ₹$total" else "Pay ₹$total via Razorpay (Test)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddressDialog) {
        AddAddressDialog(
            onDismiss = { showAddressDialog = false },
            onUseCurrentGps = { onCoords ->
                scope.launch {
                    LocationHelper.fetchCurrentDeviceLocation(context)
                        .onSuccess { sample ->
                            viewModel.updateCapturedGps(sample)
                            onCoords(sample.latitude, sample.longitude)
                        }
                        .onFailure { err ->
                            viewModel.postMessage(err.localizedMessage ?: "GPS fix unavailable")
                        }
                }
            },
            onSave = { label, line, city, pincode, lat, lng ->
                viewModel.saveAddress(label, line, null, city, pincode, lat, lng, addresses.isEmpty())
                showAddressDialog = false
            }
        )
    }

    if (showRazorpayTestSheet) {
        val chosenAddr = addresses.firstOrNull { it.id == selectedAddressId }
        val rest = activeRestaurant
        val testOrderRef = remember { "order_rzp_test_${UUID.randomUUID().toString().take(8)}" }
        AlertDialog(
            onDismissRequest = { showRazorpayTestSheet = false },
            containerColor = WhiteBg,
            title = {
                Column {
                    Text("Razorpay Test Checkout", style = MaterialTheme.typography.titleLarge, color = DarkNavy)
                    Text("Test Mode • No real money charged", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Merchant: CraftEats (${rest?.name ?: ""})", style = MaterialTheme.typography.bodyMedium, color = MainText)
                    Text("Order Ref: $testOrderRef", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    Text("Method: $selectedPaymentMethod", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    Text("Amount: ₹$total", style = MaterialTheme.typography.headlineSmall, color = MainText)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRazorpayTestSheet = false
                        if (chosenAddr != null && rest != null) {
                            val payId = "pay_rzp_test_${UUID.randomUUID().toString().take(8)}"
                            viewModel.placeOrder(
                                restaurant = rest,
                                cart = cartItems,
                                address = chosenAddr,
                                paymentMethod = selectedPaymentMethod,
                                razorpayOrderId = testOrderRef,
                                razorpayPaymentId = payId,
                                onOrderPlaced = onOrderPlaced
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = WhiteBg),
                    modifier = Modifier.testTag("razorpay_confirm_test_payment")
                ) {
                    Text("Simulate Success Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRazorpayTestSheet = false }) { Text("Cancel", color = SecondaryText) }
            }
        )
    }
}

@Composable
private fun BillRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MainText,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = MainText)
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun AddAddressDialog(
    onDismiss: () -> Unit,
    onUseCurrentGps: ((Double, Double) -> Unit) -> Unit,
    onSave: (String, String, String, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Home") }
    var line by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Bengaluru") }
    var pincode by remember { mutableStateOf("560038") }
    var lat by remember { mutableStateOf(12.9716) }
    var lng by remember { mutableStateOf(77.5946) }
    var gpsStatus by remember { mutableStateOf("Tap Detect GPS to capture device coordinates") }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onUseCurrentGps { newLat, newLng ->
                lat = newLat
                lng = newLng
                gpsStatus = "GPS Fix: ${String.format("%.4f", newLat)}, ${String.format("%.4f", newLng)}"
            }
        } else {
            gpsStatus = "Location permission denied. Enter address manually."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text("Add Delivery Address", style = MaterialTheme.typography.titleLarge, color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            onUseCurrentGps { newLat, newLng ->
                                lat = newLat
                                lng = newLng
                                gpsStatus = "GPS Fix: ${String.format("%.4f", newLat)}, ${String.format("%.4f", newLng)}"
                            }
                        } else {
                            permLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Detect Device GPS Location", color = RoyalBlue)
                }
                Text(gpsStatus, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home/Work)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("Street / Flat / Building") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = pincode, onValueChange = { pincode = it }, label = { Text("Pincode") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label, line, city, pincode, lat, lng) },
                enabled = line.trim().length >= 3 && city.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) }
        }
    )
}

@Composable
fun CustomerOrdersAndTrackingScreen(viewModel: CraftEatsViewModel) {
    val context = LocalContext.current
    val ordersState by viewModel.customerOrders.collectAsStateWithLifecycle()
    val liveLocations by viewModel.customerLiveLocations.collectAsStateWithLifecycle()
    var reviewTargetOrder by remember { mutableStateOf<FoodOrder?>(null) }

    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Live Order Tracking & History", style = MaterialTheme.typography.headlineSmall, color = MainText)
            Text("Real-time kitchen prep, rider GPS telemetry & order updates.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }

        if (orders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You haven't placed any orders yet. Add a meal to your cart to see real-time tracking here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                val liveLoc = liveLocations.firstOrNull { it.orderId == order.id }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, color = MainText)
                                Text("Order #${order.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            }
                            OrderStatusBadge(statusRaw = order.status)
                        }

                        OrderStatusTimeline(statusRaw = order.status)

                        order.items.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall, color = MainText)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total: ₹${order.totalInr} (${order.paymentMethod})", style = MaterialTheme.typography.labelMedium, color = MainText)
                            Text("Pay: ${order.paymentStatus}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }

                        if (order.status !in listOf("DELIVERED", "CANCELLED", "REJECTED")) {
                            LiveRouteMapCanvas(
                                restaurantLat = order.restaurantLat,
                                restaurantLng = order.restaurantLng,
                                deliveryLat = order.deliveryLat,
                                deliveryLng = order.deliveryLng,
                                liveLocation = liveLoc
                            )
                            if (liveLoc != null) {
                                Surface(color = SuccessGreenSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Live Rider GPS: ${String.format("%.5f", liveLoc.latitude)}, ${String.format("%.5f", liveLoc.longitude)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SuccessGreen
                                            )
                                            Text(
                                                "Accuracy ±${liveLoc.accuracyMeters.toInt()}m • Partner: ${order.driverName ?: "Assigned"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MainText
                                            )
                                        }
                                        IconButton(onClick = {
                                            val uri = Uri.parse("geo:${liveLoc.latitude},${liveLoc.longitude}?q=${liveLoc.latitude},${liveLoc.longitude}(Delivery+Partner)")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }) {
                                            Icon(Icons.Filled.LocationOn, contentDescription = "Open Maps", tint = RoyalBlue)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    if (order.driverUid != null) "Partner assigned (${order.driverName ?: ""}). Waiting for GPS telemetry ping..."
                                    else "Awaiting delivery partner assignment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (order.status in listOf("PLACED", "ACCEPTED")) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelOrder(order.id, "Cancelled by customer before prep") },
                                    border = BorderStroke(1.dp, ErrorRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel Order", color = ErrorRed)
                                }
                            }
                            if (order.status == "DELIVERED") {
                                Button(
                                    onClick = { reviewTargetOrder = order },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rate & Review")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reviewTargetOrder?.let { order ->
        var rating by remember { mutableLongStateOf(5L) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reviewTargetOrder = null },
            containerColor = WhiteBg,
            title = { Text("Rate ${order.restaurantName}", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1L..5L).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) RoyalBlue else BorderGray
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Share your food & delivery experience") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(order.id, order.restaurantId, rating, comment)
                        reviewTargetOrder = null
                    },
                    enabled = comment.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                ) {
                    Text("Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTargetOrder = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CustomerAccountAndSupportScreen(
    viewModel: CraftEatsViewModel,
    onOpenPortalSwitcher: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val tickets by viewModel.mySupportTickets.collectAsStateWithLifecycle()

    var nameInput by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var phoneInput by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var ticketSubject by remember { mutableStateOf("") }
    var ticketMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SecondaryLavenderBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("My Profile & Workspace", style = MaterialTheme.typography.titleLarge, color = MainText)
                    Text("Signed in as ${viewModel.currentUserEmail}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.updateProfile(nameInput, phoneInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                        ) {
                            Text("Save Profile")
                        }
                        OutlinedButton(onClick = onOpenPortalSwitcher) {
                            Text("Switch Portal", color = MainText)
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = RoyalBlue)
                Text("Notifications (${notifications.size})", style = MaterialTheme.typography.titleMedium, color = MainText)
            }
        }
        if (notifications.isEmpty()) {
            item { Text("No notifications yet.", style = MaterialTheme.typography.bodySmall, color = SecondaryText) }
        } else {
            items(notifications.take(5), key = { it.id }) { notif ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (notif.read) WhiteBg else RoyalBlueSoft,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.markNotificationRead(notif.id) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall, color = MainText)
                        Text(notif.body, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = BorderGray)
            Spacer(Modifier.height(6.dp))
            Text("Customer Support Desk", style = MaterialTheme.typography.titleMedium, color = MainText)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ticketSubject,
                onValueChange = { ticketSubject = it },
                label = { Text("Issue Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ticketMessage,
                onValueChange = { ticketMessage = it },
                label = { Text("Describe your issue") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.submitSupportTicket(ticketSubject, ticketMessage)
                    ticketSubject = ""
                    ticketMessage = ""
                },
                enabled = ticketSubject.trim().length >= 3 && ticketMessage.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy, contentColor = WhiteBg)
            ) {
                Text("Submit Support Ticket")
            }
        }

        items(tickets, key = { it.id }) { t ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WhiteBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t.subject, style = MaterialTheme.typography.titleSmall, color = MainText)
                        Text(t.status, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                    }
                    Text(t.message, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    if (!t.adminReply.isNullOrBlank()) {
                        Text("Admin Reply: ${t.adminReply}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    }
                }
            }
        }
    }
}
