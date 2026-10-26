package com.sarani.carobd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.ui.screens.adapter.AdapterInspectorScreen
import com.sarani.carobd.ui.screens.dashboard.DashboardScreen
import com.sarani.carobd.ui.screens.dashboard.DashboardViewModel
import com.sarani.carobd.ui.screens.diagnostics.DiagnosticsScreen
import com.sarani.carobd.ui.screens.diagnostics.DiagnosticsViewModel
import com.sarani.carobd.ui.screens.maintenance.MaintenanceScreen
import com.sarani.carobd.ui.screens.maintenance.MaintenanceViewModel
import com.sarani.carobd.ui.screens.settings.SettingsScreen
import com.sarani.carobd.ui.screens.terminal.TerminalScreen
import com.sarani.carobd.ui.screens.terminal.TerminalViewModel
import com.sarani.carobd.ui.screens.trip.TripScreen
import com.sarani.carobd.ui.screens.trip.TripViewModel
import com.sarani.carobd.ui.screens.vehicles.VehicleProfileScreen
import com.sarani.carobd.ui.screens.vehicles.VehicleProfileViewModel
import com.sarani.carobd.ui.theme.CarObdTheme
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.TextMuted
import androidx.lifecycle.lifecycleScope
import com.sarani.carobd.core.storage.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

sealed class AppDestination(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Dashboard : AppDestination("dashboard", R.string.nav_dashboard, Icons.Default.Speed)
    data object Diagnostics : AppDestination("diagnostics", R.string.nav_diagnostics, Icons.Default.Build)
    data object Maintenance : AppDestination("maintenance", R.string.nav_maintenance, Icons.Default.Engineering)
    data object Trip : AppDestination("trip", R.string.nav_trip, Icons.Default.Navigation)
    data object EcuProfiles : AppDestination("ecu_profiles", R.string.nav_ecu_profiles, Icons.Default.DirectionsCar)
    data object Settings : AppDestination("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val appPreferencesManager: AppPreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            // Restore persisted locale before first composition (§Phase C). Single cached
            // DataStore read — no runBlocking / heavy IO on startup.
            try {
                val lang = appPreferencesManager.language.first()
                LocaleHelper.apply(this@MainActivity, lang)
            } catch (_: Exception) {
                /* non-fatal: keep system locale */
            }
            setContent {
                CarObdTheme(appPreferencesManager = appPreferencesManager) {
                    MainAppScaffold(appPreferencesManager = appPreferencesManager)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(appPreferencesManager: AppPreferencesManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val destinations = listOf(
        AppDestination.Dashboard,
        AppDestination.Diagnostics,
        AppDestination.Maintenance,
        AppDestination.Trip,
        AppDestination.EcuProfiles,
        AppDestination.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = DarkCardBg
            ) {
                destinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != dest.route) {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = null,
                                tint = if (selected) NeonCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(dest.titleRes),
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                color = if (selected) NeonCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Dashboard.route) {
                val vm: DashboardViewModel = koinViewModel()
                DashboardScreen(viewModel = vm)
            }
            composable(AppDestination.Diagnostics.route) {
                val vm: DiagnosticsViewModel = koinViewModel()
                DiagnosticsScreen(viewModel = vm)
            }
            composable(AppDestination.Maintenance.route) {
                val vm: MaintenanceViewModel = koinViewModel()
                MaintenanceScreen(viewModel = vm)
            }
            composable(AppDestination.Trip.route) {
                val vm: TripViewModel = koinViewModel()
                TripScreen(viewModel = vm)
            }
            composable(AppDestination.EcuProfiles.route) {
                val vm: VehicleProfileViewModel = koinViewModel()
                VehicleProfileScreen(viewModel = vm)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    appPreferencesManager = appPreferencesManager,
                    onNavigateToAdapterInspector = {
                        navController.navigate("adapter_inspector")
                    },
                    onResetOnboarding = {
                        scope.launch {
                            appPreferencesManager.setOnboardingDone(false)
                            navController.navigate(AppDestination.Dashboard.route)
                        }
                    }
                )
            }
            composable("adapter_inspector") {
                AdapterInspectorScreen()
            }
            composable("terminal") {
                val vm: TerminalViewModel = koinViewModel()
                TerminalScreen(viewModel = vm)
            }
        }
    }
}
