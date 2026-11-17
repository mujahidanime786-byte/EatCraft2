package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AppPortal
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.model.UiState
import com.example.ui.components.FoodThumbnail
import com.example.ui.components.RatingRow
import com.example.ui.components.TechnovaAnnouncementBar
import com.example.ui.components.TechnovaTopHeader
import com.example.ui.components.VegNonVegBadge
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.MainText
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SecondaryLavenderBg
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WhiteBg
import com.example.viewmodel.CraftEatsViewModel

private enum class CustomerTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    BROWSE("Explore", Icons.Filled.Storefront),
    OFFERS("Offers", Icons.Filled.LocalOffer),
    ORDERS("Track", Icons.AutoMirrored.Filled.ReceiptLong),
    ACCOUNT("Account", Icons.Filled.Person)
}

private data class CuisineSpec(val name: String, val icon: ImageVector)

private val ALL_CUISINES = listOf(
    CuisineSpec("Indian", Icons.Filled.LocalDining),
    CuisineSpec("North Indian", Icons.Filled.DinnerDining),
    CuisineSpec("South Indian", Icons.Filled.BakeryDining),
    CuisineSpec("Chinese", Icons.Filled.RamenDining),
    CuisineSpec("Pizza", Icons.Filled.LocalPizza),
    CuisineSpec("Burgers", Icons.Filled.LunchDining),
    CuisineSpec("Biryani", Icons.Filled.RiceBowl),
    CuisineSpec("Desserts", Icons.Filled.Cake),
    CuisineSpec("Fast Food", Icons.Filled.Fastfood),
    CuisineSpec("Healthy Food", Icons.Filled.Eco)
)

@Composable
fun CustomerPortalScreen(
    viewModel: CraftEatsViewModel,
    onOpenPortalSwitcher: () -> Unit
) {
    val restaurantsState by viewModel.approvedRestaurants.collectAsStateWithLifecycle()
    val menuItemsState by viewModel.availableMenuItems.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val coupons by viewModel.activeCoupons.collectAsStateWithLifecycle()
    val reviews by viewModel.publishedReviews.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(CustomerTab.HOME) }
    var selectedRestaurantId by remember { mutableStateOf<String?>(null) }
    var showCartScreen by remember { mutableStateOf(false) }
    var selectedCuisineFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var itemForCustomization by remember { mutableStateOf<Pair<MenuItem, Restaurant>?>(null) }

    val restaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()
    val menuItems = (menuItemsState as? UiState.Success)?.data ?: emptyList()

    if (showCartScreen) {
        BackHandler { showCartScreen = false }
    } else if (selectedRestaurantId != null) {
        BackHandler { selectedRestaurantId = null }
    }

    Scaffold(
        containerColor = WhiteBg,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Column {
                TechnovaAnnouncementBar()
                TechnovaTopHeader(
                    activePortal = AppPortal.CUSTOMER,
                    cartCount = cartItems.sumOf { it.quantity.toInt() },
                    onSearchClick = {
                        showCartScreen = false
                        selectedRestaurantId = null
                        currentTab = CustomerTab.BROWSE
                    },
                    onPortalSwitcherClick = onOpenPortalSwitcher,
                    onCartClick = { showCartScreen = true }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = WhiteBg,
                tonalElevation = 0.dp
            ) {
                CustomerTab.entries.forEach { tab ->
                    val selected = !showCartScreen && selectedRestaurantId == null && currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            showCartScreen = false
                            selectedRestaurantId = null
                            currentTab = tab
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RoyalBlue,
                            selectedTextColor = RoyalBlue,
                            indicatorColor = RoyalBlueSoft,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        ),
                        modifier = Modifier.testTag("customer_nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showCartScreen -> {
                    CustomerCartAndCheckoutScreen(
                        viewModel = viewModel,
                        restaurants = restaurants,
                        onBack = { showCartScreen = false },
                        onOrderPlaced = {
                            showCartScreen = false
                            selectedRestaurantId = null
                            currentTab = CustomerTab.ORDERS
                        }
                    )
                }

                selectedRestaurantId != null -> {
                    val rest = restaurants.firstOrNull { it.id == selectedRestaurantId }
                    if (rest != null) {
                        RestaurantDetailScreen(
                            restaurant = rest,
                            menuItems = menuItems.filter { it.restaurantId == rest.id },
                            reviews = reviews.filter { it.restaurantId == rest.id },
                            onBack = { selectedRestaurantId = null },
                            onAddDish = { dish ->
                                if (dish.customizations.isNotEmpty()) {
                                    itemForCustomization = dish to rest
                                } else {
                                    viewModel.addToCart(dish, rest, null, 0L)
                                }
                            },
                            onOpenCart = { showCartScreen = true },
                            cartCount = cartItems.sumOf { it.quantity.toInt() }
                        )
                    } else {
                        selectedRestaurantId = null
                    }
                }

                else -> {
                    when (currentTab) {
                        CustomerTab.HOME -> CustomerHomeFeed(
                            restaurantsState = restaurantsState,
                            menuItemsState = menuItemsState,
                            isAdmin = viewModel.isAdmin,
                            onInitializeCatalog = { viewModel.initializeCatalogManually() },
                            onExploreRestaurants = { currentTab = CustomerTab.BROWSE },
                            onSelectCuisine = { cuisine ->
                                selectedCuisineFilter = cuisine
                                currentTab = CustomerTab.BROWSE
                            },
                            onOpenOffers = { currentTab = CustomerTab.OFFERS },
                            onOpenRestaurant = { restId -> selectedRestaurantId = restId },
                            onAddDish = { dish ->
                                val rest = restaurants.firstOrNull { it.id == dish.restaurantId }
                                if (rest != null) {
                                    if (dish.customizations.isNotEmpty()) {
                                        itemForCustomization = dish to rest
                                    } else {
                                        viewModel.addToCart(dish, rest, null, 0L)
                                    }
                                }
                            }
                        )

                        CustomerTab.BROWSE -> CustomerSearchAndBrowseScreen(
                            restaurants = restaurants,
                            menuItems = menuItems,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedCuisine = selectedCuisineFilter,
                            onSelectCuisine = { selectedCuisineFilter = it },
                            onOpenRestaurant = { selectedRestaurantId = it },
                            onAddDish = { dish ->
                                val rest = restaurants.firstOrNull { it.id == dish.restaurantId }
                                if (rest != null) {
                                    if (dish.customizations.isNotEmpty()) {
                                        itemForCustomization = dish to rest
                                    } else {
                                        viewModel.addToCart(dish, rest, null, 0L)
                                    }
                                }
                            }
                        )

                        CustomerTab.OFFERS -> CustomerOffersScreen(
                            coupons = coupons,
                            discountedItems = menuItems.filter { it.discountPercent > 0L },
                            restaurants = restaurants,
                            onApplyCouponAndGoToCart = { coupon ->
                                viewModel.postMessage("Use code ${coupon.code} in your cart!")
                                showCartScreen = true
                            },
                            onOpenRestaurant = { selectedRestaurantId = it }
                        )

                        CustomerTab.ORDERS -> CustomerOrdersAndTrackingScreen(
                            viewModel = viewModel
                        )

                        CustomerTab.ACCOUNT -> CustomerAccountAndSupportScreen(
                            viewModel = viewModel,
                            onOpenPortalSwitcher = onOpenPortalSwitcher
                        )
                    }
                }
            }
        }
    }

    itemForCustomization?.let { (dish, restaurant) ->
        DishCustomizationDialog(
            dish = dish,
            onDismiss = { itemForCustomization = null },
            onConfirm = { addOn, addOnPrice ->
                viewModel.addToCart(dish, restaurant, addOn, addOnPrice)
                itemForCustomization = null
            }
        )
    }
}

@Composable
private fun CustomerHomeFeed(
    restaurantsState: UiState<List<Restaurant>>,
    menuItemsState: UiState<List<MenuItem>>,
    isAdmin: Boolean,
    onInitializeCatalog: () -> Unit,
    onExploreRestaurants: () -> Unit,
    onSelectCuisine: (String) -> Unit,
    onOpenOffers: () -> Unit,
    onOpenRestaurant: (String) -> Unit,
    onAddDish: (MenuItem) -> Unit
) {
    val restaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()
    val dishes = (menuItemsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. TECHNOVA-Style Hero Banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SecondaryLavenderBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_food_1794908863452),
                        contentDescription = "Delicious Food Hero Photography",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Delicious Food.\nDelivered To Your Door.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MainText
                        )
                        Text(
                            text = "Fresh. Fast. Every Day.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = RoyalBlue
                        )
                        Text(
                            text = "Discover the best restaurants and enjoy your favourite meals delivered to your doorstep.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onExploreRestaurants,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("hero_explore_restaurants_button")
                        ) {
                            Text("EXPLORE RESTAURANTS", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 2. Trust Strip Below Hero
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WhiteBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val trustItems = listOf(
                        Triple(Icons.Filled.LocalShipping, "Free Delivery", "Orders ₹199+"),
                        Triple(Icons.Filled.Security, "Secure Pay", "UPI & Cards"),
                        Triple(Icons.Filled.Verified, "Fresh Food", "Verified Kitchens"),
                        Triple(Icons.Filled.HeadsetMic, "Support", "24/7 Care")
                    )
                    trustItems.forEach { (icon, title, sub) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(icon, contentDescription = title, tint = MainText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MainText, maxLines = 1)
                            Text(sub, style = MaterialTheme.typography.labelSmall, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (restaurantsState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            }
        } else if (restaurants.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Kitchen Catalog Ready to Initialize", style = MaterialTheme.typography.titleMedium, color = MainText)
                        Text(
                            if (isAdmin) "Tap below to seed curated multi-tenant kitchens, dishes across 10 cuisines, and active coupons into Firestore."
                            else "No approved kitchens are published yet. You can register a Restaurant Partner kitchen from the Workspace Switcher.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        if (isAdmin) {
                            Button(
                                onClick = onInitializeCatalog,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                            ) {
                                Text("Initialize Multi-Tenant Catalog")
                            }
                        }
                    }
                }
            }
        }

        // 3. Explore Cuisines (10 Categories)
        item {
            SectionHeader(
                title = "Explore Cuisines",
                actionText = "View All Cuisines →",
                onAction = onExploreRestaurants
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ALL_CUISINES) { cuisine ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WhiteBg,
                        border = BorderStroke(1.dp, BorderGray),
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onSelectCuisine(cuisine.name) }
                            .testTag("cuisine_card_${cuisine.name.lowercase().replace(" ", "_")}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecondaryLavenderBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(cuisine.icon, contentDescription = cuisine.name, tint = RoyalBlue, modifier = Modifier.size(22.dp))
                            }
                            Text(
                                text = cuisine.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MainText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Three Promotional Banners (TECHNOVA Dark Navy + Lavender + Dark Navy pattern)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    PromoBannerCard(
                        dark = true,
                        title = "Discover Popular\nRestaurants",
                        subtitle = "Top-rated chef kitchens near you.",
                        cta = "EXPLORE NOW →",
                        imageRes = R.drawable.img_promo_dark_1794908863756,
                        onClick = onExploreRestaurants
                    )
                }
                item {
                    PromoBannerCard(
                        dark = false,
                        title = "Delicious Meals\nNear You",
                        subtitle = "Freshly cooked & delivered in 25 mins.",
                        cta = "ORDER MEALS →",
                        imageRes = R.drawable.img_dish_biryani_1794908863543,
                        onClick = onExploreRestaurants
                    )
                }
                item {
                    PromoBannerCard(
                        dark = true,
                        title = "Special Food\nOffers",
                        subtitle = "Save up to 25% with CRAFT50 & ROYAL25.",
                        cta = "VIEW OFFERS →",
                        imageRes = R.drawable.img_dish_pizza_1794908864142,
                        onClick = onOpenOffers
                    )
                }
            }
        }

        // 5. Trending Food Section
        if (dishes.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Trending Food",
                    actionText = "View All Dishes →",
                    onAction = onExploreRestaurants
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(dishes.take(8), key = { it.id }) { dish ->
                        TrendingFoodCard(
                            dish = dish,
                            onAddClick = { onAddDish(dish) },
                            onCardClick = { onOpenRestaurant(dish.restaurantId) }
                        )
                    }
                }
            }
        }

        // 6. Popular Restaurants Section
        if (restaurants.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Popular Restaurants",
                    actionText = "See All →",
                    onAction = onExploreRestaurants
                )
            }
            items(restaurants, key = { it.id }) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onOpenRestaurant(restaurant.id) }
                )
            }
        }

        // 7. Top Kitchens Strip (Matching TECHNOVA "Top Brands. Trusted Quality.")
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SecondaryLavenderBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Top Kitchens. Trusted Quality.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MainText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("DARBAR", "NAPOLI", "MADRAS", "WOK&SMASH").forEach { brand ->
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkNavy
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MainText)
        Text(
            text = actionText,
            style = MaterialTheme.typography.labelMedium,
            color = RoyalBlue,
            modifier = Modifier
                .clickable(onClick = onAction)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun PromoBannerCard(
    dark: Boolean,
    title: String,
    subtitle: String,
    cta: String,
    imageRes: Int,
    onClick: () -> Unit
) {
    val bg = if (dark) DarkNavy else SecondaryLavenderBg
    val fg = if (dark) WhiteBg else MainText
    val subColor = if (dark) WhiteBg.copy(alpha = 0.78f) else SecondaryText
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (dark) null else BorderStroke(1.dp, BorderGray),
        modifier = Modifier
            .width(270.dp)
            .height(145.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subColor, maxLines = 2)
                }
                Spacer(Modifier.height(10.dp))
                Text(cta, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (dark) WhiteBg else RoyalBlue)
            }
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
fun TrendingFoodCard(
    dish: MenuItem,
    onAddClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhiteBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = modifier
            .width(195.dp)
            .clickable(onClick = onCardClick)
            .testTag("dish_card_${dish.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(115.dp)) {
                FoodThumbnail(
                    imageKey = dish.imageKey,
                    imageUri = dish.imageUri,
                    contentDescription = dish.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                if (dish.discountPercent > 0L) {
                    Surface(
                        color = SuccessGreen,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Text(
                            "-${dish.discountPercent}%",
                            color = WhiteBg,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                VegNonVegBadge(
                    veg = dish.veg,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )
            }
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleSmall,
                color = MainText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${dish.restaurantName} • ${dish.prepTimeMins} min",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("₹${dish.priceInr}", style = MaterialTheme.typography.titleMedium, color = MainText)
                if (dish.originalPriceInr > dish.priceInr) {
                    Text(
                        "₹${dish.originalPriceInr}",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = SecondaryText
                    )
                }
            }
            RatingRow(rating = dish.rating, reviewCount = dish.reviewCount)
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("add_to_cart_${dish.id}")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add to Cart", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = WhiteBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("restaurant_card_${restaurant.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(96.dp)) {
                FoodThumbnail(
                    imageKey = restaurant.imageKey,
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
                Surface(
                    color = DarkNavy,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                ) {
                    Text(
                        text = restaurant.logoLabel ?: restaurant.name.take(2).uppercase(),
                        color = WhiteBg,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = if (restaurant.openNow) SuccessGreenSoft else ErrorRedSoft,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (restaurant.openNow) "OPEN" else "CLOSED",
                            color = if (restaurant.openNow) SuccessGreen else ErrorRed,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(restaurant.cuisine, style = MaterialTheme.typography.bodySmall, color = SecondaryText, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Text("${restaurant.rating}", style = MaterialTheme.typography.labelMedium, color = MainText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(14.dp))
                        Text("${restaurant.deliveryTimeMins} mins", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                    Text(
                        text = if (restaurant.deliveryFeeInr == 0L) "Free delivery" else "₹${restaurant.deliveryFeeInr} fee",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (restaurant.deliveryFeeInr == 0L) SuccessGreen else SecondaryText
                    )
                }
                Text(
                    "Min order ₹${restaurant.minOrderInr} • ${restaurant.addressLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomerSearchAndBrowseScreen(
    restaurants: List<Restaurant>,
    menuItems: List<MenuItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCuisine: String?,
    onSelectCuisine: (String?) -> Unit,
    onOpenRestaurant: (String) -> Unit,
    onAddDish: (MenuItem) -> Unit
) {
    var vegOnly by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("RATING") }

    val filteredDishes = remember(menuItems, searchQuery, selectedCuisine, vegOnly, sortBy) {
        menuItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.restaurantName.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            val matchesCuisine = selectedCuisine == null || item.category.equals(selectedCuisine, ignoreCase = true)
            val matchesVeg = !vegOnly || item.veg
            matchesQuery && matchesCuisine && matchesVeg
        }.let { list ->
            when (sortBy) {
                "PRICE" -> list.sortedBy { it.priceInr }
                "FASTEST" -> list.sortedBy { it.prepTimeMins }
                else -> list.sortedByDescending { it.rating }
            }
        }
    }

    val filteredRestaurants = remember(restaurants, searchQuery, selectedCuisine) {
        restaurants.filter { r ->
            val matchesQuery = searchQuery.isBlank() ||
                r.name.contains(searchQuery, ignoreCase = true) ||
                r.cuisine.contains(searchQuery, ignoreCase = true)
            val matchesCuisine = selectedCuisine == null || r.cuisine.contains(selectedCuisine, ignoreCase = true)
            matchesQuery && matchesCuisine
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search biryani, pizza, restaurants...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("search_input")
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = vegOnly,
                        onClick = { vegOnly = !vegOnly },
                        label = { Text("Pure Veg") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuccessGreenSoft,
                            selectedLabelColor = SuccessGreen
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "RATING",
                        onClick = { sortBy = "RATING" },
                        label = { Text("Top Rated") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "FASTEST",
                        onClick = { sortBy = "FASTEST" },
                        label = { Text("Fastest") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortBy == "PRICE",
                        onClick = { sortBy = "PRICE" },
                        label = { Text("Price: Low-High") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCuisine == null,
                        onClick = { onSelectCuisine(null) },
                        label = { Text("All Cuisines") }
                    )
                }
                items(ALL_CUISINES) { c ->
                    FilterChip(
                        selected = selectedCuisine == c.name,
                        onClick = { onSelectCuisine(if (selectedCuisine == c.name) null else c.name) },
                        label = { Text(c.name) }
                    )
                }
            }
        }

        if (filteredRestaurants.isNotEmpty()) {
            item { Text("Restaurants (${filteredRestaurants.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
            items(filteredRestaurants, key = { "browse_${it.id}" }) { rest ->
                RestaurantCard(restaurant = rest, onClick = { onOpenRestaurant(rest.id) })
            }
        }

        item { Text("Dishes (${filteredDishes.size})", style = MaterialTheme.typography.titleMedium, color = MainText) }
        if (filteredDishes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryLavenderBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No dishes match your current filters. Try clearing cuisine or Pure Veg filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredDishes, key = { "dish_${it.id}" }) { dish ->
                DishHorizontalListCard(dish = dish, onAdd = { onAddDish(dish) })
            }
        }
    }
}

@Composable
private fun DishHorizontalListCard(dish: MenuItem, onAdd: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhiteBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumbnail(
                imageKey = dish.imageKey,
                imageUri = dish.imageUri,
                contentDescription = dish.name,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VegNonVegBadge(veg = dish.veg)
                    Text(dish.name, style = MaterialTheme.typography.titleSmall, color = MainText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(dish.description, style = MaterialTheme.typography.bodySmall, color = SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("₹${dish.priceInr}", style = MaterialTheme.typography.titleMedium, color = MainText)
                    if (dish.originalPriceInr > dish.priceInr) {
                        Text(
                            "₹${dish.originalPriceInr}",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = SecondaryText
                        )
                    }
                    Text("• ${dish.prepTimeMins}m", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                }
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Add", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RestaurantDetailScreen(
    restaurant: Restaurant,
    menuItems: List<MenuItem>,
    reviews: List<com.example.model.Review>,
    onBack: () -> Unit,
    onAddDish: (MenuItem) -> Unit,
    onOpenCart: () -> Unit,
    cartCount: Int
) {
    var vegOnly by remember { mutableStateOf(false) }
    val shownMenu = remember(menuItems, vegOnly) {
        if (vegOnly) menuItems.filter { it.veg } else menuItems
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                }
                if (cartCount > 0) {
                    Button(
                        onClick = onOpenCart,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
                    ) {
                        Text("View Cart ($cartCount)")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SecondaryLavenderBg,
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(restaurant.name, style = MaterialTheme.typography.headlineSmall, color = MainText)
                    Text("${restaurant.cuisine} • ${restaurant.addressLabel}", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("★ ${restaurant.rating} (${restaurant.reviewCount})", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                        Text("${restaurant.deliveryTimeMins} mins", style = MaterialTheme.typography.labelMedium, color = MainText)
                        Text("Min ₹${restaurant.minOrderInr}", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menu (${shownMenu.size})", style = MaterialTheme.typography.titleLarge, color = MainText)
                FilterChip(
                    selected = vegOnly,
                    onClick = { vegOnly = !vegOnly },
                    label = { Text("Veg Only") }
                )
            }
        }

        items(shownMenu, key = { it.id }) { dish ->
            DishHorizontalListCard(dish = dish, onAdd = { onAddDish(dish) })
        }

        if (reviews.isNotEmpty()) {
            item {
                HorizontalDivider(color = BorderGray)
                Spacer(Modifier.height(6.dp))
                Text("Customer Reviews", style = MaterialTheme.typography.titleMedium, color = MainText)
            }
            items(reviews.take(5), key = { it.id }) { rev ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WhiteBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rev.customerName, style = MaterialTheme.typography.titleSmall, color = MainText)
                            Text("★ ${rev.rating}/5", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                        }
                        Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOffersScreen(
    coupons: List<com.example.model.Coupon>,
    discountedItems: List<MenuItem>,
    restaurants: List<Restaurant>,
    onApplyCouponAndGoToCart: (com.example.model.Coupon) -> Unit,
    onOpenRestaurant: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Exclusive Food Offers", style = MaterialTheme.typography.headlineSmall, color = MainText)
            Text("Apply verified coupon codes during checkout.", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        items(coupons, key = { it.id }) { coupon ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(color = RoyalBlue, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                coupon.code,
                                color = WhiteBg,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(coupon.title, style = MaterialTheme.typography.titleMedium, color = WhiteBg)
                        Text(
                            "Up to ₹${coupon.maxDiscountInr} off • Min order ₹${coupon.minOrderInr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhiteBg.copy(alpha = 0.75f)
                        )
                    }
                    Button(
                        onClick = { onApplyCouponAndGoToCart(coupon) },
                        colors = ButtonDefaults.buttonColors(containerColor = WhiteBg, contentColor = DarkNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (discountedItems.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text("Discounted Dishes Today", style = MaterialTheme.typography.titleLarge, color = MainText)
            }
            items(discountedItems, key = { "offer_${it.id}" }) { dish ->
                val rest = restaurants.firstOrNull { it.id == dish.restaurantId }
                DishHorizontalListCard(
                    dish = dish,
                    onAdd = { if (rest != null) onOpenRestaurant(rest.id) }
                )
            }
        }
    }
}

@Composable
private fun DishCustomizationDialog(
    dish: MenuItem,
    onDismiss: () -> Unit,
    onConfirm: (String?, Long) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg,
        title = { Text("Customize ${dish.name}", style = MaterialTheme.typography.titleLarge, color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose an optional add-on:", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectedOption = null }
                ) {
                    RadioButton(selected = selectedOption == null, onClick = { selectedOption = null })
                    Text("Standard Preparation (₹${dish.priceInr})", style = MaterialTheme.typography.bodyMedium, color = MainText)
                }
                dish.customizations.forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedOption = opt }
                    ) {
                        RadioButton(selected = selectedOption == opt, onClick = { selectedOption = opt })
                        Text(opt, style = MaterialTheme.typography.bodyMedium, color = MainText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addOnPrice = selectedOption?.let { str ->
                        Regex("\\+₹(\\d+)").find(str)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 35L
                    } ?: 0L
                    onConfirm(selectedOption, addOnPrice)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg)
            ) {
                Text("Add to Cart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) }
        }
    )
}
