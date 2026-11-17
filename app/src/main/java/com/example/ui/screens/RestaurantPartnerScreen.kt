package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppPortal
import com.example.model.FoodOrder
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.ui.components.FoodThumbnail
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.SalesBarChartCanvas
import com.example.ui.components.TechnovaAnnouncementBar
import com.example.ui.components.TechnovaTopHeader
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
import com.example.ui.theme.WarningAmberSoft
import com.example.ui.theme.WhiteBg
import com.example.viewmodel.CraftEatsViewModel

private enum class RestaurantTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Filled.Dashboard),
    ORDERS("Orders", Icons.AutoMirrored.Filled.ReceiptLong),
    MENU("Menu", Icons.Filled.RestaurantMenu),
    PROFILE("Kitchen", Icons.Filled.Storefront)
}

@Composable
fun RestaurantPartnerScreen(
    viewModel: CraftEatsViewModel,
    onOpenPortalSwitcher: () -> Unit
) {
    val ownedRestaurants by viewModel.ownedRestaurants.collectAsStateWithLifecycle()
    val ownedMenuItems by viewModel.ownedMenuItems.collectAsStateWithLifecycle()
    val orders by viewModel.restaurantOrders.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(RestaurantTab.OVERVIEW) }
    var selectedRestaurantId by remember(ownedRestaurants) {
        mutableStateOf(ownedRestaurants.firstOrNull()?.id)
    }
    var showAddKitchenDialog by remember { mutableStateOf(false) }
    var dishBeingEdited by remember { mutableStateOf<MenuItem?>(null) }
    var showCreateDishDialog by remember { mutableStateOf(false) }

    val activeRestaurant = ownedRestaurants.firstOrNull { it.id == selectedRestaurantId }
        ?: ownedRestaurants.firstOrNull()
    val restaurantDishes = ownedMenuItems.filter { activeRestaurant == null || it.restaurantId == activeRestaurant.id }
    val restaurantOrders = orders.filter { activeRestaurant == null || it.restaurantId == activeRestaurant.id }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        Scaffold(
            containerColor = WhiteBg,
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                Column {
                    TechnovaAnnouncementBar()
                    TechnovaTopHeader(
                        activePortal = AppPortal.RESTAURANT,
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
                    NavigationBar(containerColor = WhiteBg, tonalElevation = 0.dp) {
                        RestaurantTab.entries.forEach { tab ->
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
                        RestaurantTab.entries.forEach { tab ->
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
                    if (activeRestaurant == null) {
                        EmptyPartnerOnboarding(onRegisterClick = { showAddKitchenDialog = true })
                    } else {
                        when (activeTab) {
                            RestaurantTab.OVERVIEW -> PartnerOverviewTab(
                                restaurant = activeRestaurant,
                                allOwned = ownedRestaurants,
                                orders = restaurantOrders,
                                onSelectRestaurant = { selectedRestaurantId = it },
                                onToggleOpen = { viewModel.toggleRestaurantOpen(activeRestaurant.id, it) },
                                onRegisterNewBranch = { showAddKitchenDialog = true }
                            )

                            RestaurantTab.ORDERS -> PartnerOrdersTab(
                                orders = restaurantOrders,
                                onUpdateOrder = { ord, nextStatus, prep, reason ->
                                    viewModel.updateRestaurantOrderStatus(ord, nextStatus, prep, reason)
                                }
                            )

                            RestaurantTab.MENU -> PartnerMenuTab(
                                dishes = restaurantDishes,
                                onAddDish = {
                                    dishBeingEdited = null
                                    showCreateDishDialog = true
                                },
                                onEditDish = { dish ->
                                    dishBeingEdited = dish
                                    showCreateDishDialog = true
                                },
                                onToggleAvailability = { dish, avail ->
                                    viewModel.toggleMenuItemAvailability(dish.id, avail)
                                }
                            )

                            RestaurantTab.PROFILE -> PartnerProfileTab(
                                restaurant = activeRestaurant,
                                onToggleOpen = { viewModel.toggleRestaurantOpen(activeRestaurant.id, it) },
                                onRegisterNew = { showAddKitchenDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddKitchenDialog) {
        RegisterKitchenDialog(
            onDismiss = { showAddKitchenDialog = false },
            onSubmit = { name, cuisine, city, addr, fee, minOrd ->
                viewModel.registerRestaurant(name, cuisine, city, addr, "tenant_north_culinary", fee, minOrd)
                showAddKitchenDialog = false
            }
        )
    }

    if (showCreateDishDialog && activeRestaurant != null) {
        DishEditorDialog(
            existing = dishBeingEdited,
            onDismiss = { showCreateDishDialog = false },
            onSave = { name, desc, cat, price, disc, veg, prep, imageUri, addons ->
                viewModel.saveMenuItem(
                    existingId = dishBeingEdited?.id,
                    restaurant = activeRestaurant,
                    name = name,
                    description = desc,
                    category = cat,
                    priceInr = price,
                    discountPercent = disc,
                    veg = veg,
                    available = dishBeingEdited?.available ?: true,
                    prepTimeMins = prep,
                    imageKey = if (cat.contains("Pizza", true)) "pizza" else "biryani",
                    imageUri = imageUri,
                    customizations = addons
                )
                showCreateDishDialog = false
            }
        )
    }
}

@Composable
private fun EmptyPartnerOnboarding(onRegisterClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SecondaryLavenderBg,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Partner Kitchen Onboarding", style = MaterialTheme.typography.headlineSmall, color = MainText)
                Text(
                    "Register your restaurant to manage menus, receive real-time orders, and connect with delivery partners.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Button(
                    onClick = onRegisterClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Register Restaurant Kitchen")
                }
            }
        }
    }
}

@Composable
private fun PartnerOverviewTab(
    restaurant: Restaurant,
    allOwned: List<Restaurant>,
    orders: List<FoodOrder>,
    onSelectRestaurant: (String) -> Unit,
    onToggleOpen: (Boolean) -> Unit,
    onRegisterNewBranch: () -> Unit
) {
    val totalSales = orders.filter { it.status != "CANCELLED" && it.status != "REJECTED" }.sumOf { it.totalInr }
    val pendingCount = orders.count { it.status == "PLACED" }
    val activeCount = orders.count { it.status in listOf("ACCEPTED", "PREPARING", "READY_FOR_PICKUP", "DRIVER_ASSIGNED", "PICKED_UP", "ON_THE_WAY") }

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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(restaurant.name, style = MaterialTheme.typography.titleLarge, color = MainText)
                            Text("Tenant: ${restaurant.tenantId} • ${restaurant.approvalStatus}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (restaurant.openNow) "OPEN" else "CLOSED", style = MaterialTheme.typography.labelSmall, color = if (restaurant.openNow) SuccessGreen else ErrorRed)
                            Switch(checked = restaurant.openNow, onCheckedChange = onToggleOpen)
                        }
                    }
                    if (allOwned.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allOwned.forEach { branch ->
                                Surface(
                                    color = if (branch.id == restaurant.id) RoyalBlue else WhiteBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderGray),
                                    modifier = Modifier.clickable { onSelectRestaurant(branch.id) }
                                ) {
                                    Text(
                                        branch.name.take(14),
                                        color = if (branch.id == restaurant.id) WhiteBg else MainText,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Total Orders", orders.size.toString(), Modifier.weight(1f))
                MetricTile("Total Sales", "₹$totalSales", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Pending", pendingCount.toString(), Modifier.weight(1f))
                MetricTile("Active Prep", activeCount.toString(), Modifier.weight(1f))
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
                    Text("Revenue Analytics (INR)", style = MaterialTheme.typography.titleMedium, color = MainText)
                    val chartData = listOf(
                        "Gross" to totalSales.coerceAtLeast(100L),
                        "Net" to ((totalSales * (100L - restaurant.commissionPercent)) / 100L).coerceAtLeast(80L),
                        "Avg" to (if (orders.isNotEmpty()) totalSales / orders.size else 280L),
                        "Goal" to 5000L
                    )
                    SalesBarChartCanvas(values = chartData)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onRegisterNewBranch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Register Another Kitchen Branch")
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhiteBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MainText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PartnerOrdersTab(
    orders: List<FoodOrder>,
    onUpdateOrder: (FoodOrder, String, Long, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Incoming & Active Kitchen Orders", style = MaterialTheme.typography.titleLarge, color = MainText)
        }
        if (orders.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = SecondaryLavenderBg, modifier = Modifier.fillMaxWidth()) {
                    Text("No orders for this kitchen yet.", style = MaterialTheme.typography.bodyMedium, color = SecondaryText, modifier = Modifier.padding(18.dp))
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Order #${order.id.takeLast(8).uppercase()} • ${order.customerName}", style = MaterialTheme.typography.titleSmall, color = MainText)
                                Text("₹${order.totalInr} • ${order.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            }
                            OrderStatusBadge(statusRaw = order.status)
                        }
                        order.items.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall, color = MainText)
                        }
                        if (!order.driverName.isNullOrBlank()) {
                            Text("Rider Assigned: ${order.driverName}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (order.status) {
                                "PLACED" -> {
                                    Button(
                                        onClick = { onUpdateOrder(order, "ACCEPTED", 20L, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = WhiteBg)
                                    ) { Text("Accept (20m)") }
                                    OutlinedButton(
                                        onClick = { onUpdateOrder(order, "REJECTED", order.prepTimeMins, "Kitchen busy") },
                                        border = BorderStroke(1.dp, ErrorRed)
                                    ) { Text("Reject", color = ErrorRed) }
                                }
                                "ACCEPTED" -> {
                                    Button(
                                        onClick = { onUpdateOrder(order, "PREPARING", order.prepTimeMins, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                                    ) { Text("Start Preparing") }
                                }
                                "PREPARING" -> {
                                    Button(
                                        onClick = { onUpdateOrder(order, "READY_FOR_PICKUP", order.prepTimeMins, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                                    ) { Text("Mark Ready & Dispatch Offer") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerMenuTab(
    dishes: List<MenuItem>,
    onAddDish: () -> Unit,
    onEditDish: (MenuItem) -> Unit,
    onToggleAvailability: (MenuItem, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Menu Management (${dishes.size})", style = MaterialTheme.typography.titleLarge, color = MainText)
                Button(
                    onClick = onAddDish,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                    modifier = Modifier.testTag("partner_add_dish_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Dish")
                }
            }
        }
        items(dishes, key = { it.id }) { dish ->
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
                    FoodThumbnail(
                        imageKey = dish.imageKey,
                        imageUri = dish.imageUri,
                        contentDescription = dish.name,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VegNonVegBadge(veg = dish.veg)
                            Text(dish.name, style = MaterialTheme.typography.titleSmall, color = MainText)
                        }
                        Text("${dish.category} • ₹${dish.priceInr} (${dish.discountPercent}% off)", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                    Switch(checked = dish.available, onCheckedChange = { onToggleAvailability(dish, it) })
                    IconButton(onClick = { onEditDish(dish) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit dish", tint = RoyalBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerProfileTab(
    restaurant: Restaurant,
    onToggleOpen: (Boolean) -> Unit,
    onRegisterNew: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SecondaryLavenderBg,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(restaurant.name, style = MaterialTheme.typography.titleLarge, color = MainText)
                Text("Cuisine: ${restaurant.cuisine}", style = MaterialTheme.typography.bodyMedium, color = MainText)
                Text("Address: ${restaurant.addressLabel}, ${restaurant.city}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                Text("Approval Status: ${restaurant.approvalStatus}", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                Text("Platform Commission: ${restaurant.commissionPercent}%", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                HorizontalDivider(color = BorderGray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Accepting Orders", style = MaterialTheme.typography.titleSmall, color = MainText)
                    Switch(checked = restaurant.openNow, onCheckedChange = onToggleOpen)
                }
            }
        }
        Button(
            onClick = onRegisterNew,
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Register Additional Restaurant")
        }
    }
}

@Composable
private fun RegisterKitchenDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("North Indian • Biryani") }
    var city by remember { mutableStateOf("Bengaluru") }
    var address by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("29") }
    var minOrder by remember { mutableStateOf("149") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text("Register Partner Kitchen", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Restaurant Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cuisine, onValueChange = { cuisine = it }, label = { Text("Cuisines") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Kitchen Address") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Fee ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text("Min ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        name,
                        cuisine,
                        city,
                        address,
                        deliveryFee.toLongOrNull() ?: 29L,
                        minOrder.toLongOrNull() ?: 149L
                    )
                },
                enabled = name.trim().length >= 2 && address.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DishEditorDialog(
    existing: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Long, Boolean, Long, String?, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Indian") }
    var price by remember { mutableStateOf((existing?.priceInr ?: 249L).toString()) }
    var discount by remember { mutableStateOf((existing?.discountPercent ?: 10L).toString()) }
    var veg by remember { mutableStateOf(existing?.veg ?: true) }
    var prepTime by remember { mutableStateOf((existing?.prepTimeMins ?: 20L).toString()) }
    var addOns by remember { mutableStateOf(existing?.customizations?.joinToString(", ") ?: "Extra Cheese (+₹45)") }
    var pickedImageUri by remember { mutableStateOf(existing?.imageUri) }

    // Zero-permission Android Photo Picker per Google Play Policy
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text(if (existing == null) "Add Menu Dish" else "Edit Menu Dish", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = RoyalBlue)
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedImageUri != null) "Photo Selected (Tap to Change)" else "Pick Food Photo (Photo Picker)", color = RoyalBlue)
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. Biryani, Pizza)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price ₹") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount %") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = prepTime, onValueChange = { prepTime = it }, label = { Text("Prep Time (mins)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = addOns, onValueChange = { addOns = it }, label = { Text("Customizations (comma separated)") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Vegetarian Dish", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = veg, onCheckedChange = { veg = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addOnList = addOns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(
                        name, desc, category,
                        price.toLongOrNull() ?: 199L,
                        discount.toLongOrNull() ?: 0L,
                        veg,
                        prepTime.toLongOrNull() ?: 20L,
                        pickedImageUri,
                        addOnList
                    )
                },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) { Text("Save Dish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
