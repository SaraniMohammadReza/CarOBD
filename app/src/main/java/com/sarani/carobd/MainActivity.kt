package com.sarani.carobd

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
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
import com.sarani.carobd.ui.screens.connection.ConnectionCenterScreen
import com.sarani.carobd.ui.screens.connection.ConnectionCenterViewModel
import com.sarani.carobd.ui.screens.dashboard.DashboardScreen
import com.sarani.carobd.ui.screens.dashboard.DashboardViewModel
import com.sarani.carobd.ui.screens.diagnostics.DiagnosticsScreen
import com.sarani.carobd.ui.screens.diagnostics.DiagnosticsViewModel
import com.sarani.carobd.ui.screens.maintenance.MaintenanceScreen
import com.sarani.carobd.ui.screens.maintenance.MaintenanceViewModel
import com.sarani.carobd.ui.screens.owned.OwnedVehiclesScreen
import com.sarani.carobd.ui.screens.owned.OwnedVehiclesViewModel
import com.sarani.carobd.ui.screens.simulator.SimulatorSelectorScreen
import com.sarani.carobd.ui.screens.simulator.SimulatorSelectorViewModel
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
import com.sarani.carobd.core.storage.AppLanguage
import com.sarani.carobd.core.storage.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

sealed class AppDestination(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Home : AppDestination("home", R.string.nav_home, Icons.Default.Home)
    data object Dashboard : AppDestination("dashboard", R.string.nav_dashboard, Icons.Default.Speed)
    data object ConnectionCenter : AppDestination("connect", R.string.nav_connection_center, Icons.Default.Wifi)
    data object MyVehicle : AppDestination("my_vehicle", R.string.nav_my_vehicle, Icons.Default.DirectionsCar)
    data object Diagnostics : AppDestination("diagnostics", R.string.nav_diagnostics, Icons.Default.Build)
    data object Maintenance : AppDestination("maintenance", R.string.nav_maintenance, Icons.Default.Engineering)
    data object Parts : AppDestination("parts", R.string.nav_parts, Icons.Default.Category)
    data object Trip : AppDestination("trip", R.string.nav_trip, Icons.Default.Navigation)
    data object EcuProfiles : AppDestination("ecu_profiles", R.string.nav_ecu_profiles, Icons.Default.DirectionsCar)
    data object Settings : AppDestination("settings", R.string.nav_settings, Icons.Default.Settings)
    data object Terminal : AppDestination("terminal", R.string.nav_terminal, Icons.Default.Terminal)
    data object Simulator : AppDestination("simulator", R.string.nav_simulator, Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private val appPreferencesManager: AppPreferencesManager by inject()

    // §3: apply the persisted language in attachBaseContext (BEFORE super) so the
    // locale is baked into the Activity's resources for first composition — no race
    // with recreate, and no resource is read before the locale is set. The single
    // blocking DataStore read at attach replaces the old async lifecycleScope.apply()
    // that could run after super.onCreate.
    override fun attachBaseContext(newBase: Context) {
        val lang = runBlocking {
            try { appPreferencesManager.language.first() }
            catch (_: Exception) { AppLanguage.PERSIAN }
        }
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            org.koin.compose.KoinContext {
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
    val isOnboardingDone by appPreferencesManager.isOnboardingDone.collectAsState(initial = null)
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        com.sarani.carobd.ui.screens.splash.AutomotiveSplashScreen(
            onFinished = { showSplash = false }
        )
        return
    }

    if (isOnboardingDone == false) {
        com.sarani.carobd.ui.screens.onboarding.OnboardingScreen(
            onFinish = { setup ->
                scope.launch {
                    if (setup != null) {
                        appPreferencesManager.saveVehicleSetup(setup)
                    }
                    appPreferencesManager.setOnboardingDone(true)
                }
            }
        )
        return
    }

    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Dashboard,
        AppDestination.ConnectionCenter,
        AppDestination.MyVehicle,
        AppDestination.Diagnostics,
        AppDestination.Maintenance,
        AppDestination.Parts,
        AppDestination.Trip,
        AppDestination.EcuProfiles,
        AppDestination.Settings,
        AppDestination.Simulator,
        AppDestination.Terminal
    )

    val sessionState by org.koin.compose.koinInject<com.sarani.carobd.domain.repository.ObdRepository>().connectionState.collectAsState()
    var moreOpen by remember { mutableStateOf(false) }
    val primaryDestinations = listOf(AppDestination.Home, AppDestination.MyVehicle, AppDestination.Maintenance, AppDestination.Diagnostics)
    fun navigate(route: String) {
        if (currentRoute != route) navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = false
        }
    }
    if (moreOpen) AlertDialog(
        onDismissRequest = { moreOpen = false },
        title = { Text(stringResource(R.string.qa_more)) },
        text = { Column { destinations.filter { it !in primaryDestinations }.forEach { dest ->
            TextButton(modifier = Modifier.testTag("nav_" + dest.route), onClick = { moreOpen = false; navigate(dest.route) }) {
                Icon(dest.icon, contentDescription = null)
                Text(stringResource(dest.titleRes))
            }
        } } },
        confirmButton = { TextButton(onClick = { moreOpen = false }) { Text(stringResource(R.string.ui_cancel)) } }
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            if ((sessionState as? com.sarani.carobd.core.model.ConnectionState.Connected)?.isSimulator == true) {
                Text(stringResource(R.string.simulator_warning),
                    modifier = Modifier.fillMaxWidth().background(DarkCardBg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = NeonCyan, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkCardBg
            ) {
                primaryDestinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        modifier = Modifier.testTag("nav_" + dest.route),
                        onClick = { navigate(dest.route) },
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
                                fontSize = 12.sp,
                                maxLines = 2,
                                color = if (selected) NeonCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        )
                    )
                }
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_more"),
                    selected = currentRoute in destinations.filter { it !in primaryDestinations }.map { it.route },
                    onClick = { moreOpen = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                    label = { Text(stringResource(R.string.qa_more)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) {
                val vm: com.sarani.carobd.ui.screens.home.HomeViewModel = koinViewModel()
                com.sarani.carobd.ui.screens.home.HomeScreen(
                    viewModel = vm,
                    onNavigateToMyVehicle = { navigate(AppDestination.MyVehicle.route) },
                    onNavigateToMaintenance = { navigate(AppDestination.Maintenance.route) },
                    onNavigateToParts = { navigate(AppDestination.Parts.route) },
                    onNavigateToDiagnostics = { navigate(AppDestination.Diagnostics.route) },
                    onNavigateToLiveDashboard = { navigate(AppDestination.Dashboard.route) },
                    onNavigateToConnect = { navigate(AppDestination.ConnectionCenter.route) },
                    onNavigateToReports = { navigate("evidence") },
                    onNavigateToGuide = { navigate("guide") },
                    onNavigateToSettings = { navigate(AppDestination.Settings.route) }
                )
            }
            composable("guide") {
                val repo: com.sarani.carobd.core.vehicle.guide.GuideRepository = org.koin.compose.koinInject()
                com.sarani.carobd.ui.screens.guide.GuideScreen(
                    guideRepository = repo,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Dashboard.route) {
                val vm: DashboardViewModel = koinViewModel()
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToVehicleCatalog = {
                        navController.navigate(AppDestination.EcuProfiles.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
            composable(AppDestination.ConnectionCenter.route) {
                val vm: ConnectionCenterViewModel = koinViewModel()
                ConnectionCenterScreen(viewModel = vm)
            }
            composable(AppDestination.MyVehicle.route) {
                val vm: OwnedVehiclesViewModel = koinViewModel()
                OwnedVehiclesScreen(vm, onNavigateToMaintenance = { navController.navigate(AppDestination.Maintenance.route) }, onNavigateToParts = { navController.navigate(AppDestination.Parts.route) })
            }
            composable(AppDestination.Diagnostics.route) {
                val vm: DiagnosticsViewModel = koinViewModel()
                DiagnosticsScreen(
                    viewModel = vm,
                    onEvidence = { navController.navigate("evidence") },
                    onNavigateToConnect = { navController.navigate(AppDestination.ConnectionCenter.route) },
                    onNavigateToSimulator = { navController.navigate(AppDestination.Simulator.route) },
                    onNavigateToGuide = { navController.navigate("guide") }
                )
            }
            composable("monitor_tests") { com.sarani.carobd.ui.screens.evidence.MonitorTestsScreen(koinViewModel()) }
            composable("dtc_history") { com.sarani.carobd.ui.screens.evidence.DtcHistoryScreen(koinViewModel()) }
            composable("recordings") { com.sarani.carobd.ui.screens.recording.RecordingScreen(koinViewModel()) }
            composable("freeze_frame") { com.sarani.carobd.ui.screens.evidence.FreezeFrameScreen(koinViewModel()) }
            composable("vehicle_info") { com.sarani.carobd.ui.screens.evidence.VehicleInformationScreen(koinViewModel()) }
            composable("evidence") {
                com.sarani.carobd.ui.screens.evidence.EvidenceScreen(koinViewModel(), onVehicleInfo = { navController.navigate("vehicle_info") }, onFreezeFrame = { navController.navigate("freeze_frame") }, onRecordings = { navController.navigate("recordings") }, onHistory = { navController.navigate("dtc_history") }, onMonitorTests = { navController.navigate("monitor_tests") })
            }
            composable(AppDestination.Maintenance.route) {
                val vm: MaintenanceViewModel = koinViewModel()
                MaintenanceScreen(viewModel = vm)
            }
            composable(AppDestination.Parts.route) {
                val vm: com.sarani.carobd.ui.screens.parts.PartsCenterViewModel = koinViewModel()
                com.sarani.carobd.ui.screens.parts.PartsCenterScreen(
                    viewModel = vm,
                    onNavigateToMyVehicle = { navController.navigate(AppDestination.MyVehicle.route) }
                )
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
            composable(AppDestination.Simulator.route) {
                val vm: SimulatorSelectorViewModel = koinViewModel()
                SimulatorSelectorScreen(viewModel = vm, onBack = { navController.popBackStack() })
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



