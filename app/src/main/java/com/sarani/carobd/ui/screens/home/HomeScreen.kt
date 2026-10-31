package com.sarani.carobd.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.ui.components.CarObdCard
import com.sarani.carobd.ui.components.EmptyState
import com.sarani.carobd.ui.components.PrimaryFeatureCard
import com.sarani.carobd.ui.components.SectionHeader
import com.sarani.carobd.ui.components.StatusChip
import com.sarani.carobd.ui.components.StatusChipType
import com.sarani.carobd.ui.components.VehicleContextHeader
import com.sarani.carobd.ui.design.CarObdSizes
import com.sarani.carobd.ui.design.CornerRadius
import com.sarani.carobd.ui.design.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMyVehicle: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToParts: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToLiveDashboard: () -> Unit,
    onNavigateToConnect: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // OBD Connection State Indicator Pill
                    val (connLabel, connType) = when (val c = state.connectionState) {
                        is ConnectionState.Connected -> {
                            if (c.isSimulator) {
                                stringResource(R.string.status_connected_sim) to StatusChipType.SUCCESS
                            } else {
                                stringResource(R.string.status_connected) to StatusChipType.SUCCESS
                            }
                        }
                        is ConnectionState.TransportConnecting,
                        is ConnectionState.TransportConnected,
                        is ConnectionState.AdapterResetting,
                        is ConnectionState.AdapterIdentifying,
                        is ConnectionState.AdapterConfiguring,
                        is ConnectionState.ProtocolDetecting,
                        is ConnectionState.EcuDiscovering,
                        is ConnectionState.CapabilityDiscovering,
                        is ConnectionState.DeviceSelected,
                        is ConnectionState.TransportScanning,
                        is ConnectionState.Retrying -> stringResource(R.string.status_connecting) to StatusChipType.INFO
                        is ConnectionState.Degraded -> stringResource(R.string.status_degraded) to StatusChipType.WARNING
                        is ConnectionState.Failure -> stringResource(R.string.status_error) to StatusChipType.CRITICAL
                        else -> stringResource(R.string.status_disconnected) to StatusChipType.NEUTRAL
                    }
                    StatusChip(
                        label = connLabel,
                        type = connType,
                        modifier = Modifier
                            .clickable { onNavigateToConnect() }
                            .padding(end = Spacing.compact)
                    )

                    // User Guide Contextual Shortcut
                    IconButton(onClick = onNavigateToGuide) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.nav_user_guide),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.standard),
            contentPadding = PaddingValues(vertical = Spacing.standard),
            verticalArrangement = Arrangement.spacedBy(Spacing.standard)
        ) {
            // 1. ACTIVE VEHICLE HERO CARD
            item {
                VehicleContextHeader(
                    vehicle = state.activeVehicle,
                    onSwitchVehicle = {
                        if (state.vehicles.isEmpty()) {
                            onNavigateToMyVehicle()
                        } else {
                            viewModel.onIntent(HomeIntent.ToggleVehicleSelector)
                        }
                    }
                )
            }

            // If no vehicles exist, show empty onboarding banner
            if (state.activeVehicle == null) {
                item {
                    EmptyState(
                        title = stringResource(R.string.my_vehicle_empty),
                        description = stringResource(R.string.maintenance_no_vehicles),
                        icon = Icons.Default.DirectionsCar,
                        actionLabel = stringResource(R.string.my_vehicle_add),
                        onActionClick = onNavigateToMyVehicle
                    )
                }
            } else {
                // 2. PRIMARY OWNERSHIP DESTINATIONS
                item {
                    SectionHeader(title = stringResource(R.string.home_section_ownership))
                }

                // Card A: My Vehicle
                item {
                    PrimaryFeatureCard(
                        title = stringResource(R.string.nav_my_vehicle),
                        subtitle = stringResource(R.string.home_my_vehicle_subtitle),
                        icon = Icons.Default.DirectionsCar,
                        onClick = onNavigateToMyVehicle,
                        badgeText = state.activeVehicle?.variant ?: state.activeVehicle?.engineCode,
                        badgeType = StatusChipType.INFO
                    )
                }

                // Card B: Maintenance Center
                item {
                    val maintSubtitle: String
                    val maintBadgeText: String
                    val maintBadgeType: StatusChipType

                    if (state.totalSchedulesCount == 0) {
                        // Truthful empty state: never claim "Everything is healthy"
                        maintSubtitle = stringResource(R.string.home_maintenance_no_schedules)
                        maintBadgeText = stringResource(R.string.status_unscheduled)
                        maintBadgeType = StatusChipType.NEUTRAL
                    } else if (state.overdueCount > 0) {
                        maintSubtitle = stringResource(R.string.status_overdue_badge, state.overdueCount)
                        maintBadgeText = stringResource(R.string.status_overdue_badge, state.overdueCount)
                        maintBadgeType = StatusChipType.CRITICAL
                    } else if (state.dueSoonCount > 0) {
                        maintSubtitle = stringResource(R.string.status_due_soon_badge, state.dueSoonCount)
                        maintBadgeText = stringResource(R.string.status_due_soon_badge, state.dueSoonCount)
                        maintBadgeType = StatusChipType.WARNING
                    } else if (state.nextServiceTitle != null && state.nextServiceRemainingKm != null) {
                        maintSubtitle = stringResource(
                            R.string.home_maintenance_next_service,
                            state.nextServiceTitle ?: "",
                            "%,d".format(state.nextServiceRemainingKm)
                        )
                        maintBadgeText = stringResource(R.string.status_up_to_date)
                        maintBadgeType = StatusChipType.SUCCESS
                    } else {
                        maintSubtitle = stringResource(R.string.home_maintenance_up_to_date)
                        maintBadgeText = stringResource(R.string.status_up_to_date)
                        maintBadgeType = StatusChipType.SUCCESS
                    }

                    PrimaryFeatureCard(
                        title = stringResource(R.string.nav_maintenance),
                        subtitle = maintSubtitle,
                        icon = Icons.Default.Engineering,
                        onClick = onNavigateToMaintenance,
                        badgeText = maintBadgeText,
                        badgeType = maintBadgeType
                    )
                }

                // Card C: Parts & Consumables
                item {
                    val partsSubtitle = if (state.compatiblePartsCount > 0) {
                        stringResource(R.string.home_parts_subtitle, state.compatiblePartsCount)
                    } else {
                        stringResource(R.string.home_parts_empty_subtitle)
                    }

                    val (partsBadge, partsBadgeType) = when (state.partsConfidence) {
                        ConfidenceLevel.VERIFIED_OEM -> stringResource(R.string.parts_oem_verified) to StatusChipType.SUCCESS
                        ConfidenceLevel.COMMUNITY_CONFIRMED -> stringResource(R.string.parts_community_confirmed) to StatusChipType.INFO
                        else -> stringResource(R.string.status_parts_badge, state.compatiblePartsCount) to StatusChipType.NEUTRAL
                    }

                    PrimaryFeatureCard(
                        title = stringResource(R.string.nav_parts),
                        subtitle = partsSubtitle,
                        icon = Icons.Default.Category,
                        onClick = onNavigateToParts,
                        badgeText = partsBadge,
                        badgeType = partsBadgeType
                    )
                }
            }

            // 3. SECONDARY DIAGNOSTICS & TELEMETRY SECTION
            item {
                SectionHeader(title = stringResource(R.string.home_section_diagnostics))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    SecondaryActionCard(
                        title = stringResource(R.string.nav_diagnostics),
                        subtitle = stringResource(R.string.home_diagnostics_subtitle),
                        icon = Icons.Default.Build,
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionCard(
                        title = stringResource(R.string.nav_dashboard),
                        subtitle = stringResource(R.string.home_live_gauges_subtitle),
                        icon = Icons.Default.Speed,
                        onClick = onNavigateToLiveDashboard,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    SecondaryActionCard(
                        title = stringResource(R.string.nav_connection_center),
                        subtitle = stringResource(R.string.home_connect_subtitle),
                        icon = Icons.Default.Wifi,
                        onClick = onNavigateToConnect,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionCard(
                        title = stringResource(R.string.evidence_title),
                        subtitle = stringResource(R.string.home_reports_subtitle),
                        icon = Icons.Default.Description,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. BOTTOM UTILITY & GUIDE SHORTCUTS
            item {
                SectionHeader(title = stringResource(R.string.home_section_resources))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    SecondaryActionCard(
                        title = stringResource(R.string.nav_user_guide),
                        subtitle = stringResource(R.string.home_guide_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToGuide,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionCard(
                        title = stringResource(R.string.nav_settings),
                        subtitle = stringResource(R.string.home_settings_subtitle),
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Vehicle Switcher Modal Dialog
    if (state.showVehicleSelector) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(HomeIntent.DismissVehicleSelector) },
            title = {
                Text(
                    text = stringResource(R.string.maintenance_switch_vehicle),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                    state.vehicles.forEach { v ->
                        val isSelected = v.id == state.activeVehicle?.id
                        Surface(
                            shape = CornerRadius.shapeMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onIntent(HomeIntent.SelectVehicle(v.id)) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.onIntent(HomeIntent.SelectVehicle(v.id)) }
                                )
                                Spacer(modifier = Modifier.width(Spacing.compact))
                                Column {
                                    Text(
                                        text = v.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val desc = listOfNotNull(v.manufacturer, v.model, v.year?.toString()).joinToString(" • ")
                                    if (desc.isNotBlank()) {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(HomeIntent.DismissVehicleSelector) }) {
                    Text(stringResource(R.string.ui_dismiss))
                }
            }
        )
    }
}

@Composable
private fun SecondaryActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarObdCard(
        modifier = modifier,
        shape = CornerRadius.shapeMedium,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.compact)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(CarObdSizes.iconMedium)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
