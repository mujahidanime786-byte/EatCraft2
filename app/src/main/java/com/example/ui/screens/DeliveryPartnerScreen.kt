package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LocationHelper
import com.example.model.AppPortal
import com.example.model.DeliveryPartner
import com.example.model.FoodOrder
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.TechnovaAnnouncementBar
import com.example.ui.components.TechnovaTopHeader
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
import com.example.ui.theme.WarningAmberSoft
import com.example.ui.theme.WhiteBg
import com.example.viewmodel.CraftEatsViewModel
import kotlinx.coroutines.launch

private enum class DeliveryTab(val label: String, val icon: ImageVector) {
    ACTIVE("Deliveries", Icons.Filled.DeliveryDining),
    EARNINGS("Earnings", Icons.Filled.AccountBalanceWallet),
    PROFILE("Partner ID", Icons.Filled.Person)
}

@Composable
fun DeliveryPartnerScreen(
    viewModel: CraftEatsViewModel,
    onOpenPortalSwitcher: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val partnerProfile by viewModel.deliveryPartnerProfile.collectAsStateWithLifecycle()
    val openOffers by viewModel.openDeliveryOffers.collectAsStateWithLifecycle()
    val driverOrders by viewModel.driverOrders.collectAsStateWithLifecycle()
    val lastGps by viewModel.lastCapturedGps.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(DeliveryTab.ACTIVE) }
    var pendingGpsOrder by remember { mutableStateOf<FoodOrder?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && pendingGpsOrder != null) {
            val targetOrder = pendingGpsOrder!!
            scope.launch {
                LocationHelper.fetchCurrentDeviceLocation(context)
                    .onSuccess { sample -> viewModel.publishLiveGpsTelemetry(targetOrder, sample) }
                    .onFailure { err -> viewModel.postMessage(err.localizedMessage ?: "Device GPS fix unavailable") }
            }
        } else if (!granted) {
            viewModel.postMessage("Location permission is required to share real GPS telemetry.")
        }
        pendingGpsOrder = null
    }

    Scaffold(
        containerColor = WhiteBg,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Column {
                TechnovaAnnouncementBar()
                TechnovaTopHeader(
                    activePortal = AppPortal.DELIVERY,
                    cartCount = 0,
                    showCart = false,
                    showSearch = false,
                    onSearchClick = {},
                    onPortalSwitcherClick = onOpenPortalSwitcher,
                    onCartClick = {}
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = WhiteBg, tonalElevation = 0.dp) {
                DeliveryTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RoyalBlue,
                            selectedTextColor = RoyalBlue,
                            indicatorColor = RoyalBlueSoft,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (activeTab) {
            DeliveryTab.ACTIVE -> {
                val activeAssigned = driverOrders.filter { it.status !in listOf("DELIVERED", "CANCELLED", "REJECTED") }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PartnerStatusHeader(
                            partner = partnerProfile,
                            onToggleOnline = { viewModel.setDriverOnline(it) },
                            onGoToProfile = { activeTab = DeliveryTab.PROFILE }
                        )
                    }

                    if (activeAssigned.isNotEmpty()) {
                        item { Text("Active Assignments (${activeAssigned.size})", style = MaterialTheme.typography.titleLarge, color = MainText) }
                        items(activeAssigned, key = { it.id }) { order ->
                            ActiveDeliveryCard(
                                order = order,
                                lastGpsText = lastGps?.let {
                                    "Last GPS: ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)} (±${it.accuracyMeters.toInt()}m)"
                                },
                                onNavigateMaps = { lat, lng, label ->
                                    val uri = Uri.parse("google.navigation:q=$lat,$lng")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")))
                                    }
                                },
                                onShareGps = {
                                    if (LocationHelper.hasLocationPermission(context)) {
                                        scope.launch {
                                            LocationHelper.fetchCurrentDeviceLocation(context)
                                                .onSuccess { sample -> viewModel.publishLiveGpsTelemetry(order, sample) }
                                                .onFailure { err -> viewModel.postMessage(err.localizedMessage ?: "Device GPS fix unavailable") }
                                        }
                                    } else {
                                        pendingGpsOrder = order
                                        permLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onAdvanceStatus = { nextStatus ->
                                    viewModel.advanceDeliveryOrderStatus(order, nextStatus)
                                }
                            )
                        }
                    }

                    item {
                        Text("Available Delivery Offers (${openOffers.size})", style = MaterialTheme.typography.titleLarge, color = MainText)
                    }

                    if (partnerProfile?.approvalStatus != "APPROVED") {
                        item {
                            Surface(shape = RoundedCornerShape(12.dp), color = WarningAmberSoft, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Your partner profile must be APPROVED by Super Admin before claiming assignments. Open Partner ID tab to register or check verification status.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MainText,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else if (openOffers.isEmpty()) {
                        item {
                            Surface(shape = RoundedCornerShape(12.dp), color = SecondaryLavenderBg, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No open pickup offers right now. When a kitchen marks an order Preparing or Ready, it appears here in real time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryText,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(openOffers, key = { it.id }) { offer ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhiteBg,
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(offer.restaurantName, style = MaterialTheme.typography.titleMedium, color = MainText)
                                        Surface(color = SuccessGreenSoft, shape = RoundedCornerShape(6.dp)) {
                                            Text("₹${offer.payoutInr} Pay", color = SuccessGreen, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    Text("Pickup: ${offer.restaurantAddress}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    Text("Drop Area: ${offer.areaLabel} • ${offer.distanceKm} km", style = MaterialTheme.typography.bodySmall, color = MainText)
                                    Button(
                                        onClick = { viewModel.claimDeliveryOffer(offer) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Accept Assignment")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DeliveryTab.EARNINGS -> {
                val completed = driverOrders.filter { it.status == "DELIVERED" }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = DarkNavy, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Total Partner Earnings", style = MaterialTheme.typography.labelMedium, color = WhiteBg.copy(alpha = 0.7f))
                                Text("₹${partnerProfile?.totalEarningsInr ?: 0L}", style = MaterialTheme.typography.displaySmall, color = WhiteBg)
                                Text("${partnerProfile?.completedDeliveries ?: 0L} Completed Deliveries • ★ ${partnerProfile?.rating ?: 4.9}", style = MaterialTheme.typography.bodySmall, color = WhiteBg.copy(alpha = 0.8f))
                            }
                        }
                    }
                    item { Text("Delivery History (${completed.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
                    items(completed, key = { "hist_${it.id}" }) { ord ->
                        Surface(shape = RoundedCornerShape(12.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(ord.restaurantName, style = MaterialTheme.typography.titleSmall, color = MainText)
                                    Text(ord.deliveryAddressLabel, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                }
                                Text("+₹55", style = MaterialTheme.typography.titleSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            DeliveryTab.PROFILE -> {
                var name by remember(partnerProfile) { mutableStateOf(partnerProfile?.displayName ?: "") }
                var vehicleType by remember(partnerProfile) { mutableStateOf(partnerProfile?.vehicleType ?: "Electric Scooter") }
                var plate by remember(partnerProfile) { mutableStateOf(partnerProfile?.vehiclePlate ?: "") }
                var city by remember(partnerProfile) { mutableStateOf(partnerProfile?.city ?: "Bengaluru") }

                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Partner Verification & Vehicle", style = MaterialTheme.typography.titleLarge, color = MainText)
                    Surface(shape = RoundedCornerShape(10.dp), color = SecondaryLavenderBg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Verification Status: ${partnerProfile?.approvalStatus ?: "NOT REGISTERED"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (partnerProfile?.approvalStatus == "APPROVED") SuccessGreen else RoyalBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vehicleType, onValueChange = { vehicleType = it }, label = { Text("Vehicle Type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Vehicle Plate") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Operating City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { viewModel.registerOrUpdateDeliveryPartner(name, vehicleType, plate, city) },
                        enabled = name.trim().length >= 2 && plate.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Save Partner Verification Profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerStatusHeader(
    partner: DeliveryPartner?,
    onToggleOnline: (Boolean) -> Unit,
    onGoToProfile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SecondaryLavenderBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(partner?.displayName ?: "Register as Partner", style = MaterialTheme.typography.titleMedium, color = MainText)
                Text(
                    if (partner == null) "Complete your vehicle profile to begin"
                    else "${partner.vehicleType} • ${partner.approvalStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            if (partner == null) {
                Button(onClick = onGoToProfile, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)) {
                    Text("Register")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (partner.online) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall, color = if (partner.online) SuccessGreen else ErrorRed)
                    Switch(checked = partner.online, onCheckedChange = onToggleOnline)
                }
            }
        }
    }
}

@Composable
private fun ActiveDeliveryCard(
    order: FoodOrder,
    lastGpsText: String?,
    onNavigateMaps: (Double, Double, String) -> Unit,
    onShareGps: () -> Unit,
    onAdvanceStatus: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = WhiteBg,
        border = BorderStroke(1.5.dp, RoyalBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Active: ${order.restaurantName}", style = MaterialTheme.typography.titleMedium, color = MainText)
                    Text("To: ${order.deliveryAddressLabel}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                OrderStatusBadge(statusRaw = order.status)
            }

            if (lastGpsText != null) {
                Surface(color = SuccessGreenSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(lastGpsText, style = MaterialTheme.typography.labelSmall, color = SuccessGreen, modifier = Modifier.padding(8.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShareGps,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share Real GPS", color = RoyalBlue)
                }
                OutlinedButton(
                    onClick = {
                        val toCustomer = order.status in listOf("PICKED_UP", "ON_THE_WAY")
                        onNavigateMaps(
                            if (toCustomer) order.deliveryLat else order.restaurantLat,
                            if (toCustomer) order.deliveryLng else order.restaurantLng,
                            if (toCustomer) order.customerName else order.restaurantName
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Google Maps", color = DarkNavy)
                }
            }

            val nextStep = when (order.status) {
                "DRIVER_ASSIGNED" -> "REACHED_RESTAURANT" to "Mark Reached Restaurant"
                "REACHED_RESTAURANT" -> "PICKED_UP" to "Confirm Order Picked Up"
                "PICKED_UP" -> "ON_THE_WAY" to "Mark On The Way"
                "ON_THE_WAY" -> "DELIVERED" to "Complete Delivery"
                else -> null
            }
            if (nextStep != null) {
                Button(
                    onClick = { onAdvanceStatus(nextStep.first) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(nextStep.second)
                }
            }
        }
    }
}
