package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppPortal
import com.example.model.LiveLocation
import com.example.model.OrderStatus
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
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhiteBg

@Composable
fun TechnovaAnnouncementBar(
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkNavy,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = "Free delivery",
                    tint = WhiteBg,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Free Delivery Over ₹199",
                    color = WhiteBg,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = "Freshness guarantee",
                    tint = WhiteBg,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "100% Fresh Kitchens",
                    color = WhiteBg,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Filled.HeadsetMic,
                    contentDescription = "24/7 support",
                    tint = WhiteBg,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "24/7 Help",
                    color = WhiteBg,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun TechnovaTopHeader(
    activePortal: AppPortal,
    cartCount: Int,
    onSearchClick: () -> Unit,
    onPortalSwitcherClick: () -> Unit,
    onCartClick: () -> Unit,
    showCart: Boolean = true,
    showSearch: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(WhiteBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MainText, fontWeight = FontWeight.ExtraBold)) {
                            append("CRAFT")
                        }
                        withStyle(SpanStyle(color = RoyalBlue, fontWeight = FontWeight.ExtraBold)) {
                            append("EATS")
                        }
                    },
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
                Surface(
                    color = RoyalBlueSoft,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable(onClick = onPortalSwitcherClick)
                        .testTag("portal_badge_button")
                ) {
                    Text(
                        text = activePortal.label,
                        color = RoyalBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSearch) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("header_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search restaurants and dishes",
                            tint = MainText
                        )
                    }
                }
                IconButton(
                    onClick = onPortalSwitcherClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("header_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Account and workspace switcher",
                        tint = MainText
                    )
                }
                if (showCart) {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("header_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = RoyalBlue, contentColor = WhiteBg) {
                                        Text(cartCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "Shopping cart",
                                tint = MainText
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = BorderGray, thickness = 1.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalSwitcherSheet(
    currentPortal: AppPortal,
    userName: String,
    userEmail: String,
    isAdmin: Boolean,
    onSelectPortal: (AppPortal) -> Unit,
    onOpenSupport: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WhiteBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RoyalBlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = RoyalBlue,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, style = MaterialTheme.typography.titleMedium, color = MainText)
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                if (isAdmin) {
                    Surface(color = DarkNavy, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "ADMIN",
                            color = WhiteBg,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderGray)
            Text(
                "Switch Connected Ecosystem Interface",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText
            )

            val portals = listOf(
                Triple(AppPortal.CUSTOMER, "Browse restaurants, order & track live GPS", Icons.Filled.ShoppingBag),
                Triple(AppPortal.RESTAURANT, "Manage kitchen menu, incoming & active orders", Icons.Filled.Storefront),
                Triple(AppPortal.DELIVERY, "Claim pickups, stream GPS & track earnings", Icons.Filled.DeliveryDining),
                Triple(AppPortal.SUPER_ADMIN, "Multi-tenant clients, approvals, audit logs & map", Icons.Filled.AdminPanelSettings)
            )

            portals.forEach { (portal, subtitle, icon) ->
                val enabled = portal != AppPortal.SUPER_ADMIN || isAdmin
                val selected = currentPortal == portal
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) RoyalBlueSoft else WhiteBg,
                    border = BorderStroke(1.dp, if (selected) RoyalBlue else BorderGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            onSelectPortal(portal)
                            onDismiss()
                        }
                        .testTag("switch_portal_${portal.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = portal.label,
                            tint = if (!enabled) SecondaryText else if (selected) RoyalBlue else MainText
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = portal.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (!enabled) SecondaryText else MainText
                            )
                            Text(
                                text = if (!enabled) "Requires Super Admin privileges" else subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        if (selected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = RoyalBlue)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenSupport()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Text("24/7 Support", color = MainText)
                }
                Button(
                    onClick = {
                        onDismiss()
                        onSignOut()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("sign_out_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = WhiteBg)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun FoodThumbnail(
    imageKey: String,
    imageUri: String? = null,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!imageUri.isNullOrBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        val resId = when (imageKey) {
            "pizza" -> R.drawable.img_dish_pizza_1794908864142
            "promo_dark" -> R.drawable.img_promo_dark_1794908863756
            "hero_food" -> R.drawable.img_hero_food_1794908863452
            else -> R.drawable.img_dish_biryani_1794908863543
        }
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
fun VegNonVegBadge(veg: Boolean, modifier: Modifier = Modifier) {
    val color = if (veg) SuccessGreen else ErrorRed
    Box(
        modifier = modifier
            .size(16.dp)
            .border(1.5.dp, color, RoundedCornerShape(3.dp))
            .background(WhiteBg, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun RatingRow(rating: Double, reviewCount: Long, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        repeat(5) { idx ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (idx < rating.toInt()) MainText else BorderGray,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = "($reviewCount)",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )
    }
}

@Composable
fun OrderStatusBadge(statusRaw: String, modifier: Modifier = Modifier) {
    val status = OrderStatus.fromRaw(statusRaw)
    val (bg, fg) = when (status) {
        OrderStatus.DELIVERED -> SuccessGreenSoft to SuccessGreen
        OrderStatus.CANCELLED, OrderStatus.REJECTED -> Color(0xFFFDECEC) to ErrorRed
        OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING -> Color(0xFFFEF6E7) to WarningAmber
        else -> RoyalBlueSoft to RoyalBlue
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderStatusTimeline(statusRaw: String, modifier: Modifier = Modifier) {
    val steps = listOf(
        "PLACED" to "Placed",
        "ACCEPTED" to "Accepted",
        "PREPARING" to "Preparing",
        "DRIVER_ASSIGNED" to "Assigned",
        "PICKED_UP" to "Picked Up",
        "ON_THE_WAY" to "On Way",
        "DELIVERED" to "Delivered"
    )
    val rankMap = mapOf(
        "PLACED" to 0,
        "ACCEPTED" to 1,
        "PREPARING" to 2,
        "READY_FOR_PICKUP" to 2,
        "DRIVER_ASSIGNED" to 3,
        "REACHED_RESTAURANT" to 3,
        "PICKED_UP" to 4,
        "ON_THE_WAY" to 5,
        "DELIVERED" to 6
    )
    val currentRank = rankMap[statusRaw] ?: -1

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, (_, label) ->
                val done = currentRank >= idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (done) RoyalBlue else BorderGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = label,
                                tint = WhiteBg,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (done) MainText else SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SalesBarChartCanvas(
    values: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (values.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val count = values.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.52f).coerceAtMost(40.dp.toPx())
            values.forEachIndexed { idx, (_, amount) ->
                val ratio = (amount.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                val barHeight = size.height * ratio
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = RoyalBlue,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            values.forEach { (label, amount) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("₹$amount", style = MaterialTheme.typography.labelSmall, color = MainText)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                }
            }
        }
    }
}

@Composable
fun LiveRouteMapCanvas(
    restaurantLat: Double,
    restaurantLng: Double,
    deliveryLat: Double,
    deliveryLng: Double,
    liveLocation: LiveLocation?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SecondaryLavenderBg,
        border = BorderStroke(1.dp, BorderGray),
        modifier = modifier.fillMaxWidth().height(150.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                val start = Offset(24f, size.height * 0.7f)
                val end = Offset(size.width - 24f, size.height * 0.3f)

                // Route line
                drawLine(
                    color = RoyalBlue.copy(alpha = 0.35f),
                    start = start,
                    end = end,
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    cap = StrokeCap.Round
                )

                // Restaurant marker
                drawCircle(color = DarkNavy, radius = 14f, center = start)
                drawCircle(color = WhiteBg, radius = 6f, center = start)

                // Customer marker
                drawCircle(color = SuccessGreen, radius = 14f, center = end)
                drawCircle(color = WhiteBg, radius = 6f, center = end)

                // Live GPS driver position interpolated cleanly from real lat/lng bounds
                if (liveLocation != null) {
                    val latSpan = (deliveryLat - restaurantLat).takeIf { kotlin.math.abs(it) > 0.0001 } ?: 0.01
                    val lngSpan = (deliveryLng - restaurantLng).takeIf { kotlin.math.abs(it) > 0.0001 } ?: 0.01
                    val tLat = ((liveLocation.latitude - restaurantLat) / latSpan).toFloat().coerceIn(0f, 1f)
                    val tLng = ((liveLocation.longitude - restaurantLng) / lngSpan).toFloat().coerceIn(0f, 1f)
                    val t = ((tLat + tLng) / 2f).coerceIn(0.05f, 0.95f)
                    val riderPos = Offset(
                        x = start.x + (end.x - start.x) * t,
                        y = start.y + (end.y - start.y) * t
                    )
                    drawCircle(color = RoyalBlue.copy(alpha = 0.2f), radius = 26f, center = riderPos)
                    drawCircle(color = RoyalBlue, radius = 13f, center = riderPos)
                    drawCircle(color = WhiteBg, radius = 5f, center = riderPos)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Kitchen (${String.format("%.4f", restaurantLat)}, ${String.format("%.4f", restaurantLng)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
                Text(
                    "Drop (${String.format("%.4f", deliveryLat)}, ${String.format("%.4f", deliveryLng)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
        }
    }
}
