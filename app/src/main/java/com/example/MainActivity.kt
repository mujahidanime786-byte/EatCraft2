package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthRepository
import com.example.data.CatalogAndCustomerRepository
import com.example.data.OperationsAndAdminRepository
import com.example.model.AppPortal
import com.example.ui.components.PortalSwitcherSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerPortalScreen
import com.example.ui.screens.DeliveryPartnerScreen
import com.example.ui.screens.RestaurantPartnerScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.theme.CraftEatsTheme
import com.example.viewmodel.CraftEatsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CraftEatsTheme {
                CraftEatsRoot()
            }
        }
    }
}

@Composable
fun CraftEatsRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by AuthRepository.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates state reactively */ })
    } else {
        key(user.uid) {
            val isAdmin = user.isEmailVerified && user.email == AuthRepository.BOOTSTRAP_ADMIN_EMAIL
            val vm: CraftEatsViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        CraftEatsViewModel(
                            customerRepo = CatalogAndCustomerRepository(db),
                            opsRepo = OperationsAndAdminRepository(db),
                            currentUserId = user.uid,
                            currentUserEmail = user.email ?: "user@crafteats.app",
                            isAdmin = isAdmin
                        )
                    }
                }
            )
            AuthenticatedEcosystemHost(viewModel = vm)
        }
    }
}

@Composable
private fun AuthenticatedEcosystemHost(viewModel: CraftEatsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val activePortal by viewModel.activePortal.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showPortalSwitcher by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { outerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
            when (activePortal) {
                AppPortal.CUSTOMER -> CustomerPortalScreen(
                    viewModel = viewModel,
                    onOpenPortalSwitcher = { showPortalSwitcher = true }
                )

                AppPortal.RESTAURANT -> RestaurantPartnerScreen(
                    viewModel = viewModel,
                    onOpenPortalSwitcher = { showPortalSwitcher = true }
                )

                AppPortal.DELIVERY -> DeliveryPartnerScreen(
                    viewModel = viewModel,
                    onOpenPortalSwitcher = { showPortalSwitcher = true }
                )

                AppPortal.SUPER_ADMIN -> SuperAdminScreen(
                    viewModel = viewModel,
                    onOpenPortalSwitcher = { showPortalSwitcher = true }
                )
            }
        }
    }

    if (showPortalSwitcher) {
        PortalSwitcherSheet(
            currentPortal = activePortal,
            userName = profile?.displayName ?: "CraftEats User",
            userEmail = viewModel.currentUserEmail,
            isAdmin = viewModel.isAdmin,
            onSelectPortal = { viewModel.switchPortal(it) },
            onOpenSupport = {
                viewModel.switchPortal(AppPortal.CUSTOMER)
                viewModel.postMessage("Open the Account tab to submit a 24/7 support ticket.")
            },
            onSignOut = {
                AuthRepository.signOut(credentialManager, scope)
            },
            onDismiss = { showPortalSwitcher = false }
        )
    }
}
