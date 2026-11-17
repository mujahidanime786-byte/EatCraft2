package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppPortal
import com.example.ui.components.LiveRouteMapCanvas
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.SalesBarChartCanvas
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
import com.example.ui.theme.WhiteBg
import com.example.viewmodel.CraftEatsViewModel

private enum class AdminTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Filled.Dashboard),
    CLIENTS("Clients", Icons.Filled.Business),
    PARTNERS("Partners", Icons.Filled.Storefront),
    LIVE_ORDERS("Live & Orders", Icons.Filled.Map),
    GOVERNANCE("Governance", Icons.Filled.Gavel)
}

@Composable
fun SuperAdminScreen(
    viewModel: CraftEatsViewModel,
    onOpenPortalSwitcher: () -> Unit
) {
    val tenants by viewModel.adminTenants.collectAsStateWithLifecycle()
    val restaurants by viewModel.adminRestaurants.collectAsStateWithLifecycle()
    val users by viewModel.adminUsers.collectAsStateWithLifecycle()
    val partners by viewModel.adminPartners.collectAsStateWithLifecycle()
    val orders by viewModel.adminOrders.collectAsStateWithLifecycle()
    val liveLocations by viewModel.adminLiveLocations.collectAsStateWithLifecycle()
    val payments by viewModel.adminPayments.collectAsStateWithLifecycle()
    val coupons by viewModel.adminCoupons.collectAsStateWithLifecycle()
    val tickets by viewModel.adminTickets.collectAsStateWithLifecycle()
    val auditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var showCreateTenantDialog by remember { mutableStateOf(false) }
    var showCreateCouponDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        Scaffold(
            containerColor = WhiteBg,
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                Column {
                    TechnovaAnnouncementBar()
                    TechnovaTopHeader(
                        activePortal = AppPortal.SUPER_ADMIN,
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
                if (!isWide) {
                    NavigationBar(containerColor = DarkNavy, tonalElevation = 0.dp) {
                        AdminTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = WhiteBg,
                                    selectedTextColor = WhiteBg,
                                    indicatorColor = RoyalBlue,
                                    unselectedIconColor = WhiteBg.copy(alpha = 0.6f),
                                    unselectedTextColor = WhiteBg.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isWide) {
                    NavigationRail(
                        containerColor = DarkNavy,
                        contentColor = WhiteBg,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = "CodeCraft Super Admin",
                            tint = RoyalBlue
                        )
                        Spacer(Modifier.height(16.dp))
                        AdminTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (activeTab) {
                        AdminTab.OVERVIEW -> AdminOverviewTab(
                            tenantCount = tenants.size,
                            restaurantCount = restaurants.size,
                            userCount = users.size,
                            partnerCount = partners.size,
                            orders = orders,
                            payments = payments,
                            onSeedCatalog = { viewModel.initializeCatalogManually() }
                        )

                        AdminTab.CLIENTS -> AdminTenantsTab(
                            tenants = tenants,
                            restaurants = restaurants,
                            orders = orders,
                            onCreateTenant = { showCreateTenantDialog = true },
                            onToggleStatus = { tId, next -> viewModel.updateTenantStatus(tId, next) }
                        )

                        AdminTab.PARTNERS -> AdminPartnersTab(
                            restaurants = restaurants,
                            partners = partners,
                            onModerateRestaurant = { id, status, comm ->
                                viewModel.moderateRestaurant(id, status, comm)
                            },
                            onModeratePartner = { uid, status ->
                                viewModel.moderatePartner(uid, status)
                            }
                        )

                        AdminTab.LIVE_ORDERS -> AdminLiveAndOrdersTab(
                            tenants = tenants,
                            orders = orders,
                            liveLocations = liveLocations,
                            payments = payments,
                            onRefundOrder = { ord, pay -> viewModel.adminRefundOrder(ord, pay) }
                        )

                        AdminTab.GOVERNANCE -> AdminGovernanceTab(
                            users = users,
                            payments = payments,
                            coupons = coupons,
                            tickets = tickets,
                            auditLogs = auditLogs,
                            onToggleUserStatus = { uid, status -> viewModel.updateUserAccountStatus(uid, status) },
                            onCreateCoupon = { showCreateCouponDialog = true },
                            onResolveTicket = { id, reply -> viewModel.resolveSupportTicket(id, reply) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateTenantDialog) {
        CreateTenantDialog(
            onDismiss = { showCreateTenantDialog = false },
            onSave = { name, code, city, plan, comm, email ->
                viewModel.createBusinessTenant(name, code, city, plan, comm, email)
                showCreateTenantDialog = false
            }
        )
    }

    if (showCreateCouponDialog) {
        CreateCouponDialog(
            onDismiss = { showCreateCouponDialog = false },
            onSave = { code, title, pct, maxDisc, minOrd ->
                viewModel.createCoupon(code, title, pct, maxDisc, minOrd, "global")
                showCreateCouponDialog = false
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    tenantCount: Int,
    restaurantCount: Int,
    userCount: Int,
    partnerCount: Int,
    orders: List<com.example.model.FoodOrder>,
    payments: List<com.example.model.PaymentRecord>,
    onSeedCatalog: () -> Unit
) {
    val activeOrders = orders.count { it.status !in listOf("DELIVERED", "CANCELLED", "REJECTED") }
    val completedOrders = orders.count { it.status == "DELIVERED" }
    val cancelledOrders = orders.count { it.status in listOf("CANCELLED", "REJECTED") }
    val grossValue = orders.filter { it.status !in listOf("CANCELLED", "REJECTED") }.sumOf { it.totalInr }
    val commissionTotal = payments.filter { it.status != "REFUNDED" }.sumOf { it.commissionInr }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = DarkNavy, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CodeCraft Super Admin Console", style = MaterialTheme.typography.titleLarge, color = WhiteBg)
                    Text("Centralized Multi-Tenant Operations, Commissions & Live Telemetry", style = MaterialTheme.typography.bodySmall, color = WhiteBg.copy(alpha = 0.75f))
                }
            }
        }

        if (restaurantCount == 0) {
            item {
                Button(
                    onClick = onSeedCatalog,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Initialize Multi-Tenant Demo Ecosystem")
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Business Clients", tenantCount.toString(), Modifier.weight(1f))
                AdminKpiCard("Restaurants", restaurantCount.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Customers", userCount.toString(), Modifier.weight(1f))
                AdminKpiCard("Delivery Partners", partnerCount.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Gross Order Value", "₹$grossValue", Modifier.weight(1f))
                AdminKpiCard("Commission Earned", "₹$commissionTotal", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Active Orders", activeOrders.toString(), Modifier.weight(1f))
                AdminKpiCard("Done / Cancelled", "$completedOrders / $cancelledOrders", Modifier.weight(1f))
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WhiteBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Platform Financial Volume (INR)", style = MaterialTheme.typography.titleMedium, color = MainText)
                    SalesBarChartCanvas(
                        values = listOf(
                            "GMV" to grossValue.coerceAtLeast(100L),
                            "Comm" to commissionTotal.coerceAtLeast(25L),
                            "Orders" to (orders.size.toLong() * 100L).coerceAtLeast(50L),
                            "Payout" to (grossValue - commissionTotal).coerceAtLeast(75L)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhiteBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
            Text(value, style = MaterialTheme.typography.titleLarge, color = MainText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminTenantsTab(
    tenants: List<com.example.model.BusinessTenant>,
    restaurants: List<com.example.model.Restaurant>,
    orders: List<com.example.model.FoodOrder>,
    onCreateTenant: () -> Unit,
    onToggleStatus: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Multi-Tenant Business Clients", style = MaterialTheme.typography.titleLarge, color = MainText)
                Button(
                    onClick = onCreateTenant,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Client")
                }
            }
        }
        items(tenants, key = { it.id }) { tenant ->
            val restCount = restaurants.count { it.tenantId == tenant.id }
            val tenantOrders = orders.filter { it.tenantId == tenant.id }
            val tenantGmv = tenantOrders.sumOf { it.totalInr }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WhiteBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${tenant.name} (${tenant.code})", style = MaterialTheme.typography.titleMedium, color = MainText)
                            Text("${tenant.city} • ${tenant.plan} • ${tenant.commissionPercent}% fee", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                        Surface(
                            color = if (tenant.status == "ACTIVE") SuccessGreenSoft else RoyalBlueSoft,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                tenant.status,
                                color = if (tenant.status == "ACTIVE") SuccessGreen else ErrorRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text("Isolated Scope: $restCount Kitchens • ${tenantOrders.size} Orders • ₹$tenantGmv GMV", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                    OutlinedButton(
                        onClick = {
                            onToggleStatus(tenant.id, if (tenant.status == "ACTIVE") "SUSPENDED" else "ACTIVE")
                        }
                    ) {
                        Text(if (tenant.status == "ACTIVE") "Suspend Client" else "Activate Client")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPartnersTab(
    restaurants: List<com.example.model.Restaurant>,
    partners: List<com.example.model.DeliveryPartner>,
    onModerateRestaurant: (String, String, Long) -> Unit,
    onModeratePartner: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Restaurants (${restaurants.size})", style = MaterialTheme.typography.titleLarge, color = MainText) }
        items(restaurants, key = { it.id }) { rest ->
            Surface(shape = RoundedCornerShape(12.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rest.name, style = MaterialTheme.typography.titleSmall, color = MainText)
                            Text("Tenant: ${rest.tenantId} • Comm: ${rest.commissionPercent}%", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                        Text(rest.approvalStatus, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onModerateRestaurant(rest.id, "APPROVED", rest.commissionPercent) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = WhiteBg)
                        ) { Text("Approve") }
                        OutlinedButton(
                            onClick = { onModerateRestaurant(rest.id, "SUSPENDED", rest.commissionPercent) },
                            border = BorderStroke(1.dp, ErrorRed)
                        ) { Text("Suspend", color = ErrorRed) }
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = BorderGray)
            Spacer(Modifier.height(6.dp))
            Text("Delivery Partners (${partners.size})", style = MaterialTheme.typography.titleLarge, color = MainText)
        }
        items(partners, key = { it.id }) { p ->
            Surface(shape = RoundedCornerShape(12.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(p.displayName, style = MaterialTheme.typography.titleSmall, color = MainText)
                            Text("${p.vehicleType} (${p.vehiclePlate}) • ${p.completedDeliveries} deliveries", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                        Text(p.approvalStatus, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onModeratePartner(p.uid, "APPROVED") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = WhiteBg)
                        ) { Text("Verify & Approve") }
                        OutlinedButton(
                            onClick = { onModeratePartner(p.uid, "SUSPENDED") },
                            border = BorderStroke(1.dp, ErrorRed)
                        ) { Text("Suspend", color = ErrorRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLiveAndOrdersTab(
    tenants: List<com.example.model.BusinessTenant>,
    orders: List<com.example.model.FoodOrder>,
    liveLocations: List<com.example.model.LiveLocation>,
    payments: List<com.example.model.PaymentRecord>,
    onRefundOrder: (com.example.model.FoodOrder, com.example.model.PaymentRecord?) -> Unit
) {
    var selectedTenantFilter by remember { mutableStateOf<String?>(null) }
    var selectedLiveOrderId by remember { mutableStateOf<String?>(null) }

    val filteredOrders = remember(orders, selectedTenantFilter) {
        if (selectedTenantFilter == null) orders else orders.filter { it.tenantId == selectedTenantFilter }
    }
    val inspectedOrder = filteredOrders.firstOrNull { it.id == selectedLiveOrderId } ?: filteredOrders.firstOrNull()
    val inspectedLoc = inspectedOrder?.let { ord -> liveLocations.firstOrNull { it.orderId == ord.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Live Tracking & Multi-Tenant Orders", style = MaterialTheme.typography.titleLarge, color = MainText)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedTenantFilter == null,
                        onClick = { selectedTenantFilter = null },
                        label = { Text("All Clients") }
                    )
                }
                items(tenants, key = { "flt_${it.id}" }) { t ->
                    FilterChip(
                        selected = selectedTenantFilter == t.id,
                        onClick = { selectedTenantFilter = t.id },
                        label = { Text(t.code) }
                    )
                }
            }
        }

        if (inspectedOrder != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live Telemetry Inspector: #${inspectedOrder.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.titleSmall, color = MainText)
                        LiveRouteMapCanvas(
                            restaurantLat = inspectedOrder.restaurantLat,
                            restaurantLng = inspectedOrder.restaurantLng,
                            deliveryLat = inspectedOrder.deliveryLat,
                            deliveryLng = inspectedOrder.deliveryLng,
                            liveLocation = inspectedLoc
                        )
                        Text(
                            if (inspectedLoc != null) "GPS: ${String.format("%.4f", inspectedLoc.latitude)}, ${String.format("%.4f", inspectedLoc.longitude)} • Accuracy ±${inspectedLoc.accuracyMeters.toInt()}m"
                            else "No live rider GPS ping for selected order yet.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (inspectedLoc != null) SuccessGreen else SecondaryText
                        )
                    }
                }
            }
        }

        items(filteredOrders, key = { it.id }) { order ->
            val payment = payments.firstOrNull { it.orderId == order.id }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WhiteBg,
                border = BorderStroke(1.dp, if (order.id == inspectedOrder?.id) RoyalBlue else BorderGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLiveOrderId = order.id }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${order.restaurantName} • ₹${order.totalInr}", style = MaterialTheme.typography.titleSmall, color = MainText)
                            Text("Tenant: ${order.tenantId} • Customer: ${order.customerName}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                        OrderStatusBadge(statusRaw = order.status)
                    }
                    if (order.status !in listOf("CANCELLED", "REJECTED") && order.paymentStatus != "REFUNDED") {
                        OutlinedButton(
                            onClick = { onRefundOrder(order, payment) },
                            border = BorderStroke(1.dp, ErrorRed)
                        ) {
                            Text("Authorize Cancel & Refund", color = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGovernanceTab(
    users: List<com.example.model.UserProfile>,
    payments: List<com.example.model.PaymentRecord>,
    coupons: List<com.example.model.Coupon>,
    tickets: List<com.example.model.SupportTicket>,
    auditLogs: List<com.example.model.AuditLog>,
    onToggleUserStatus: (String, String) -> Unit,
    onCreateCoupon: () -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Coupons & Promotions (${coupons.size})", style = MaterialTheme.typography.titleMedium, color = MainText)
                Button(onClick = onCreateCoupon, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)) {
                    Text("+ Coupon")
                }
            }
        }
        items(coupons.take(5), key = { it.id }) { c ->
            Surface(shape = RoundedCornerShape(10.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${c.code} • ${c.title}", style = MaterialTheme.typography.bodySmall, color = MainText)
                    Text("${c.discountPercent}% off", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                }
            }
        }

        item { Text("Users (${users.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
        items(users, key = { it.id }) { u ->
            Surface(shape = RoundedCornerShape(10.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${u.displayName} (${u.role})", style = MaterialTheme.typography.titleSmall, color = MainText)
                        Text("${u.email} • ${u.accountStatus}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                    TextButton(onClick = {
                        onToggleUserStatus(u.uid, if (u.accountStatus == "ACTIVE") "SUSPENDED" else "ACTIVE")
                    }) {
                        Text(if (u.accountStatus == "ACTIVE") "Suspend" else "Activate", color = RoyalBlue)
                    }
                }
            }
        }

        item { Text("Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
        items(tickets, key = { it.id }) { t ->
            Surface(shape = RoundedCornerShape(10.dp), color = WhiteBg, border = BorderStroke(1.dp, BorderGray), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${t.subject} (${t.userName})", style = MaterialTheme.typography.titleSmall, color = MainText)
                        Text(t.status, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                    }
                    Text(t.message, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    if (t.status != "RESOLVED") {
                        Button(
                            onClick = { onResolveTicket(t.id, "Reviewed & resolved by CodeCraft Support Team.") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = WhiteBg)
                        ) { Text("Mark Resolved") }
                    }
                }
            }
        }

        item { Text("Payments Ledger (${payments.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
        items(payments.take(6), key = { it.id }) { p ->
            Surface(shape = RoundedCornerShape(10.dp), color = SecondaryLavenderBg, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.method} • #${p.orderId.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = MainText)
                    Text("₹${p.amountInr} (Comm ₹${p.commissionInr}) • ${p.status}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                }
            }
        }

        item { Text("Immutable Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
        items(auditLogs.take(12), key = { it.id }) { log ->
            Surface(shape = RoundedCornerShape(10.dp), color = DarkNavy, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${log.action} -> ${log.targetType}/${log.targetId}", style = MaterialTheme.typography.labelMedium, color = WhiteBg)
                    Text(log.summary, style = MaterialTheme.typography.bodySmall, color = WhiteBg.copy(alpha = 0.8f))
                    Text("By ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = RoyalBlueSoft)
                }
            }
        }
    }
}

@Composable
private fun CreateTenantDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Mumbai") }
    var commission by remember { mutableStateOf("15") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text("Create Business Client", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Client Code (e.g. BOM-03)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = commission, onValueChange = { commission = it }, label = { Text("Commission %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, code, city, "ENTERPRISE", commission.toLongOrNull() ?: 15L, email) },
                enabled = name.trim().length >= 2 && code.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) { Text("Create Client") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateCouponDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, Long) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var pct by remember { mutableStateOf("20") }
    var maxDisc by remember { mutableStateOf("100") }
    var minOrd by remember { mutableStateOf("249") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text("Publish Coupon", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Coupon Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Promo Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pct, onValueChange = { pct = it }, label = { Text("% Off") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxDisc, onValueChange = { maxDisc = it }, label = { Text("Max ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = minOrd, onValueChange = { minOrd = it }, label = { Text("Min Order ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(code, title, pct.toLongOrNull() ?: 15L, maxDisc.toLongOrNull() ?: 100L, minOrd.toLongOrNull() ?: 199L) },
                enabled = code.trim().length >= 2 && title.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
