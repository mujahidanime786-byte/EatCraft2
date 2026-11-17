package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthRepository
import com.example.ui.components.TechnovaAnnouncementBar
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.MainText
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SecondaryLavenderBg
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.WhiteBg

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Silent auto sign-in on startup (separate GetGoogleIdOption request)
    LaunchedEffect(Unit) {
        AuthRepository.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on sign-in screen for interactive button */ }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBg)
            .navigationBarsPadding()
    ) {
        TechnovaAnnouncementBar()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MainText, fontWeight = FontWeight.ExtraBold)) {
                                append("CRAFT")
                            }
                            withStyle(SpanStyle(color = RoyalBlue, fontWeight = FontWeight.ExtraBold)) {
                                append("EATS")
                            }
                        },
                        fontSize = 24.sp
                    )
                    Surface(color = SecondaryLavenderBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "CodeCraft Multi-Tenant Platform",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Hero Card matching TECHNOVA aesthetic
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SecondaryLavenderBg,
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_food_1794908863452),
                            contentDescription = "Curated gourmet food delivery composition",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
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
                                text = "Sign in with Google to order from curated kitchens, manage partner restaurants, stream live GPS deliveries, or govern the multi-tenant platform.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    }
                }

                // 4 Connected Interfaces Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cards = listOf(
                        "Customer" to Icons.Filled.ShoppingBag,
                        "Kitchen" to Icons.Filled.Restaurant,
                        "Delivery" to Icons.Filled.DeliveryDining,
                        "Admin" to Icons.Filled.AdminPanelSettings
                    )
                    cards.forEach { (title, icon) ->
                        Surface(
                            color = WhiteBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RoyalBlueSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = title, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MainText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = ErrorRedSoft,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        AuthRepository.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                isLoading = false
                                errorMessage = "Sign-in was cancelled. Tap Sign in with Google to continue."
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = WhiteBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = WhiteBg,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
