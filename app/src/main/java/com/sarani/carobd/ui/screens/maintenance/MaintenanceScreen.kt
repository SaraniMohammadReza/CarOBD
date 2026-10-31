package com.sarani.carobd.ui.screens.maintenance

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.calculator.UnitConverter
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceActionType
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.core.model.VehicleSpecSheet
import com.sarani.carobd.domain.reminder.SnoozeDuration
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.LocalUnitSystem
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val unitSystem = LocalUnitSystem.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Vehicle Header & Selector
            VehicleHeaderSection(
                selectedVehicle = state.selectedVehicle,
                allVehicles = state.vehicles,
                onSelectVehicle = { viewModel.onIntent(MaintenanceIntent.SelectVehicle(it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary KPI Counters
            MaintenanceKpiRow(
                overdueCount = state.overdueCount,
                dueSoonCount = state.dueSoonCount,
                okCount = state.okCount,
                unknownCount = state.unknownCount
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Tab Navigation
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = DarkCardBg,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = state.selectedTab == MaintenanceTab.SCHEDULES,
                    onClick = { viewModel.onIntent(MaintenanceIntent.SelectTab(MaintenanceTab.SCHEDULES)) },
                    text = {
                        Text(
                            stringResource(R.string.maintenance_tab_schedules),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedTab == MaintenanceTab.SCHEDULES) NeonCyan else TextMuted
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == MaintenanceTab.HISTORY,
                    onClick = { viewModel.onIntent(MaintenanceIntent.SelectTab(MaintenanceTab.HISTORY)) },
                    text = {
                        Text(
                            stringResource(R.string.maintenance_tab_history),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedTab == MaintenanceTab.HISTORY) NeonCyan else TextMuted
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == MaintenanceTab.SPECS,
                    onClick = { viewModel.onIntent(MaintenanceIntent.SelectTab(MaintenanceTab.SPECS)) },
                    text = {
                        Text(
                            stringResource(R.string.maintenance_tab_specs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.selectedTab == MaintenanceTab.SPECS) NeonCyan else TextMuted
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (state.selectedVehicle == null) {
                // Empty Garage State
                EmptyGarageState()
            } else {
                when (state.selectedTab) {
                    MaintenanceTab.SCHEDULES -> {
                        SchedulesTabContent(
                            schedules = state.schedules,
                            currentOdometerKm = state.selectedVehicle?.currentOdometerKm,
                            onInitDefaults = { viewModel.onIntent(MaintenanceIntent.InitializeDefaultSchedules) },
                            onAddSchedule = { viewModel.onIntent(MaintenanceIntent.OpenAddScheduleDialog) },
                            onRecordService = { viewModel.onIntent(MaintenanceIntent.OpenRecordServiceDialog(it)) },
                            onEditSchedule = { viewModel.onIntent(MaintenanceIntent.OpenEditScheduleDialog(it)) },
                            onDeleteSchedule = { viewModel.onIntent(MaintenanceIntent.RequestDeleteSchedule(it)) },
                            onSnoozeSchedule = { sched, dur -> viewModel.onIntent(MaintenanceIntent.SnoozeSchedule(sched.id, dur)) }
                        )
                    }
                    MaintenanceTab.HISTORY -> {
                        HistoryTabContent(
                            records = state.records,
                            onDeleteRecord = { viewModel.onIntent(MaintenanceIntent.RequestDeleteRecord(it)) }
                        )
                    }
                    MaintenanceTab.SPECS -> {
                        TechnicalSpecsView(specs = state.specSheet)
                    }
                }
            }
        }

        // Dialogs
        state.recordServiceDialog?.let { dialogModel ->
            RecordServiceDialog(
                model = dialogModel,
                onDismiss = { viewModel.onIntent(MaintenanceIntent.DismissRecordServiceDialog) },
                onUpdate = { viewModel.onIntent(MaintenanceIntent.UpdateRecordServiceDialog(it)) },
                onSubmit = { viewModel.onIntent(MaintenanceIntent.SubmitRecordService(it)) }
            )
        }

        state.editScheduleDialog?.let { editModel ->
            EditScheduleDialog(
                model = editModel,
                onDismiss = { viewModel.onIntent(MaintenanceIntent.DismissEditScheduleDialog) },
                onUpdate = { viewModel.onIntent(MaintenanceIntent.UpdateEditScheduleDialog(it)) },
                onSubmit = { viewModel.onIntent(MaintenanceIntent.SubmitSaveSchedule(it)) }
            )
        }

        state.deleteScheduleTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(MaintenanceIntent.DismissDeleteSchedule) },
                containerColor = DarkCardBg,
                title = { Text(stringResource(R.string.maintenance_delete_schedule), color = TextPrimary) },
                text = {
                    Text(
                        stringResource(R.string.maintenance_delete_schedule_confirm, target.titleFa.ifBlank { target.titleEn }),
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onIntent(MaintenanceIntent.ConfirmDeleteSchedule(target.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        Text(stringResource(R.string.my_vehicle_delete_title), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(MaintenanceIntent.DismissDeleteSchedule) }) {
                        Text(stringResource(R.string.ui_cancel), color = TextMuted)
                    }
                }
            )
        }

        state.deleteRecordTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(MaintenanceIntent.DismissDeleteRecord) },
                containerColor = DarkCardBg,
                title = { Text(stringResource(R.string.ui_delete), color = TextPrimary) },
                text = { Text(stringResource(R.string.maintenance_delete_record_confirm), color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onIntent(MaintenanceIntent.ConfirmDeleteRecord(target.id)) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        Text(stringResource(R.string.ui_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(MaintenanceIntent.DismissDeleteRecord) }) {
                        Text(stringResource(R.string.ui_cancel), color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
fun VehicleHeaderSection(
    selectedVehicle: OwnedVehicle?,
    allVehicles: List<OwnedVehicle>,
    onSelectVehicle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedVehicle?.displayName ?: stringResource(R.string.my_vehicle_empty),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (selectedVehicle != null) {
                val subText = listOfNotNull(
                    selectedVehicle.manufacturer,
                    selectedVehicle.model,
                    selectedVehicle.engine
                ).filter { it.isNotBlank() }.joinToString(" • ")
                if (subText.isNotBlank()) {
                    Text(
                        text = subText,
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Odometer Display
            selectedVehicle?.currentOdometerKm?.let { km ->
                val dist = UnitConverter.formatDistance(km.toDouble(), LocalUnitSystem.current)
                Box(
                    modifier = Modifier
                        .background(DarkBg, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${dist.first} ${dist.second}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Vehicle Switcher Button
            if (allVehicles.size > 1) {
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.maintenance_switch_vehicle),
                            tint = NeonCyan
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(DarkCardBg)
                    ) {
                        allVehicles.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = v.displayName,
                                        color = if (v.id == selectedVehicle?.id) NeonCyan else TextPrimary,
                                        fontWeight = if (v.id == selectedVehicle?.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onSelectVehicle(v.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceKpiRow(
    overdueCount: Int,
    dueSoonCount: Int,
    okCount: Int,
    unknownCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.maintenance_stat_overdue),
            count = overdueCount,
            color = if (overdueCount > 0) NeonRed else TextMuted
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.maintenance_stat_due_soon),
            count = dueSoonCount,
            color = if (dueSoonCount > 0) NeonAmber else TextMuted
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.maintenance_stat_ok),
            count = okCount,
            color = if (okCount > 0) NeonGreen else TextMuted
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.maintenance_stat_unscheduled),
            count = unknownCount,
            color = TextMuted
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Box(
        modifier = modifier
            .background(DarkCardBg, RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyGarageState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.maintenance_no_vehicles),
                color = TextPrimary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SchedulesTabContent(
    schedules: List<MaintenanceSchedule>,
    currentOdometerKm: Long?,
    onInitDefaults: () -> Unit,
    onAddSchedule: () -> Unit,
    onRecordService: (MaintenanceSchedule) -> Unit,
    onEditSchedule: (MaintenanceSchedule) -> Unit,
    onDeleteSchedule: (MaintenanceSchedule) -> Unit,
    onSnoozeSchedule: (MaintenanceSchedule, SnoozeDuration) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (schedules.isEmpty()) {
                Button(
                    onClick = onInitDefaults,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = DarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.maintenance_init_default_schedules), color = DarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "${schedules.size} items",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = onAddSchedule,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.maintenance_add_schedule), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.maintenance_no_schedules),
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        currentOdometerKm = currentOdometerKm,
                        onRecordService = { onRecordService(schedule) },
                        onEdit = { onEditSchedule(schedule) },
                        onDelete = { onDeleteSchedule(schedule) },
                        onSnooze = { onSnoozeSchedule(schedule, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: MaintenanceSchedule,
    currentOdometerKm: Long?,
    onRecordService: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (SnoozeDuration) -> Unit = {}
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val unitSystem = LocalUnitSystem.current
    val badgeColor = when (schedule.dueState) {
        DueState.OVERDUE, DueState.DUE -> NeonRed
        DueState.DUE_SOON -> NeonAmber
        DueState.OK -> NeonGreen
        DueState.UNKNOWN -> TextMuted
    }

    val badgeText = when (schedule.dueState) {
        DueState.OVERDUE -> stringResource(R.string.maintenance_badge_overdue)
        DueState.DUE -> stringResource(R.string.maintenance_badge_due)
        DueState.DUE_SOON -> stringResource(R.string.maintenance_badge_due_soon)
        DueState.OK -> stringResource(R.string.maintenance_badge_ok)
        DueState.UNKNOWN -> stringResource(R.string.maintenance_badge_unknown)
    }

    val lastKm = schedule.lastServiceKm
    val intervalKm = schedule.intervalKm
    val intervalMonths = schedule.intervalMonths

    // Progress calculation
    val progress = remember(schedule, currentOdometerKm) {
        if (lastKm != null && intervalKm != null && currentOdometerKm != null && intervalKm > 0) {
            val elapsed = currentOdometerKm - lastKm
            (1f - (elapsed.toFloat() / intervalKm.toFloat())).coerceIn(0f, 1f)
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (schedule.dueState == DueState.OVERDUE || schedule.dueState == DueState.DUE) NeonRed.copy(alpha = 0.5f) else DarkCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title, Badge, and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = schedule.titleFa.ifBlank { schedule.titleEn },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interval description
                    val intervalText = buildString {
                        if (intervalKm != null && intervalKm > 0) {
                            val dist = UnitConverter.formatDistance(intervalKm.toDouble(), unitSystem)
                            append("${dist.first} ${dist.second}")
                        }
                        if (intervalMonths != null && intervalMonths > 0) {
                            if (isNotEmpty()) append(" / ")
                            append("$intervalMonths mo")
                        }
                    }
                    if (intervalText.isNotBlank()) {
                        Text(
                            text = intervalText,
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (schedule.dueState == DueState.OVERDUE || schedule.dueState == DueState.DUE || schedule.dueState == DueState.DUE_SOON) {
                        Box {
                            IconButton(
                                onClick = { showSnoozeMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = stringResource(R.string.maintenance_snooze),
                                    tint = NeonAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSnoozeMenu,
                                onDismissRequest = { showSnoozeMenu = false },
                                modifier = Modifier
                                    .background(DarkCardBg)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.maintenance_snooze_1d), color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        showSnoozeMenu = false
                                        onSnooze(SnoozeDuration.ONE_DAY)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.maintenance_snooze_3d), color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        showSnoozeMenu = false
                                        onSnooze(SnoozeDuration.THREE_DAYS)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.maintenance_snooze_1w), color = TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        showSnoozeMenu = false
                                        onSnooze(SnoozeDuration.ONE_WEEK)
                                    }
                                )
                            }
                        }
                    }

                    // Mark Completed Button
                    Button(
                        onClick = onRecordService,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.maintenance_mark_completed), color = NeonCyan, fontSize = 11.sp)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = badgeColor,
                trackColor = DarkBg
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Next Due Distance & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val nextDueKm = schedule.nextDueKm
                val statusSubText = if (nextDueKm != null && currentOdometerKm != null) {
                    val remKm = nextDueKm - currentOdometerKm
                    if (remKm < 0) {
                        val formatted = UnitConverter.formatDistance((-remKm).toDouble(), unitSystem)
                        stringResource(R.string.s5_overdue, formatted.first, formatted.second)
                    } else {
                        val formatted = UnitConverter.formatDistance(remKm.toDouble(), unitSystem)
                        stringResource(R.string.s5_remaining, formatted.first, formatted.second)
                    }
                } else if (schedule.lastServiceKm == null) {
                    stringResource(R.string.s5_unrecorded)
                } else {
                    "—"
                }

                Text(
                    text = statusSubText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (nextDueKm != null) {
                    val formatted = UnitConverter.formatDistance(nextDueKm.toDouble(), unitSystem)
                    Text(
                        text = stringResource(R.string.s5_due, formatted.first, formatted.second),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    records: List<VehicleMaintenanceRecord>,
    onDeleteRecord: (VehicleMaintenanceRecord) -> Unit
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.maintenance_history_empty),
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
        val unitSystem = LocalUnitSystem.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                HistoryRecordCard(
                    record = record,
                    dateFormat = dateFormat,
                    onDelete = { onDeleteRecord(record) }
                )
            }
        }
    }
}

@Composable
fun HistoryRecordCard(
    record: VehicleMaintenanceRecord,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val unitSystem = LocalUnitSystem.current
    val dist = UnitConverter.formatDistance(record.performedKm.toDouble(), unitSystem)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.itemKey.replace("_", " ").uppercase(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${dist.first} ${dist.second}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date & Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(record.performedDateMs)),
                    color = TextMuted,
                    fontSize = 11.sp
                )

                if (record.costTomans > 0L) {
                    Text(
                        text = "%,d Toman".format(record.costTomans),
                        color = NeonAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Details: Service Center, Brand, Notes
            val details = listOfNotNull(
                record.serviceCenter?.let { "Center: $it" },
                record.partsBrand?.let { "Parts: $it" },
                record.partNumber?.let { "#$it" },
                record.notes
            ).filter { it.isNotBlank() }

            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                details.forEach { detail ->
                    Text(
                        text = detail,
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordServiceDialog(
    model: RecordServiceDialogModel,
    onDismiss: () -> Unit,
    onUpdate: (RecordServiceDialogModel) -> Unit,
    onSubmit: (RecordServiceDialogModel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        title = {
            Text(
                text = "${stringResource(R.string.maintenance_record_service)}: ${model.title}",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = model.performedKm,
                    onValueChange = { onUpdate(model.copy(performedKm = it)) },
                    label = { Text(stringResource(R.string.maintenance_performed_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.costTomans,
                    onValueChange = { onUpdate(model.copy(costTomans = it)) },
                    label = { Text(stringResource(R.string.maintenance_cost_tomans)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.serviceCenter,
                    onValueChange = { onUpdate(model.copy(serviceCenter = it)) },
                    label = { Text(stringResource(R.string.maintenance_service_center)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.partsBrand,
                    onValueChange = { onUpdate(model.copy(partsBrand = it)) },
                    label = { Text(stringResource(R.string.maintenance_parts_brand)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.notes,
                    onValueChange = { onUpdate(model.copy(notes = it)) },
                    label = { Text(stringResource(R.string.maintenance_notes)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (model.errorMessage != null) {
                    Text(
                        text = model.errorMessage,
                        color = NeonRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(model) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(stringResource(R.string.maintenance_mark_completed), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel), color = TextMuted)
            }
        }
    )
}

@Composable
fun EditScheduleDialog(
    model: EditScheduleDialogModel,
    onDismiss: () -> Unit,
    onUpdate: (EditScheduleDialogModel) -> Unit,
    onSubmit: (EditScheduleDialogModel) -> Unit
) {
    val isNew = model.scheduleId == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        title = {
            Text(
                text = if (isNew) stringResource(R.string.maintenance_add_schedule) else stringResource(R.string.maintenance_edit_schedule),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = model.titleFa,
                    onValueChange = { onUpdate(model.copy(titleFa = it)) },
                    label = { Text(stringResource(R.string.my_vehicle_name)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.intervalKm,
                    onValueChange = { onUpdate(model.copy(intervalKm = it)) },
                    label = { Text(stringResource(R.string.maintenance_interval_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.intervalMonths,
                    onValueChange = { onUpdate(model.copy(intervalMonths = it)) },
                    label = { Text(stringResource(R.string.maintenance_interval_months)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model.lastServiceKm,
                    onValueChange = { onUpdate(model.copy(lastServiceKm = it)) },
                    label = { Text(stringResource(R.string.maintenance_last_service_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (model.errorMessage != null) {
                    Text(
                        text = model.errorMessage,
                        color = NeonRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(model) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(stringResource(R.string.ui_save), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_cancel), color = TextMuted)
            }
        }
    )
}

@Composable
fun TechnicalSpecsView(specs: VehicleSpecSheet?) {
    if (specs == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.s5_parts_reference),
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SpecRowCard(
                title = stringResource(R.string.s5_oil),
                mainValue = stringResource(R.string.s5_oil_reference, specs.engineOilGrade, specs.engineOilCapacityLiters),
                subNotes = stringResource(R.string.s5_parts_reference)
            )
        }
        item {
            SpecRowCard(
                title = stringResource(R.string.s5_spark),
                mainValue = specs.sparkPlugModel,
                subNotes = stringResource(R.string.s5_gap_reference, specs.sparkPlugGapMm)
            )
        }
        item {
            SpecRowCard(
                title = stringResource(R.string.s5_belt),
                mainValue = stringResource(R.string.s5_belt_reference, specs.timingBeltIntervalKm, specs.timingBeltTeeth),
                subNotes = stringResource(R.string.s5_parts_reference)
            )
        }
        item {
            SpecRowCard(
                title = stringResource(R.string.s5_gear_oil),
                mainValue = stringResource(R.string.s5_oil_reference, specs.gearboxOilGrade, specs.gearboxCapacityLiters),
                subNotes = stringResource(R.string.s5_parts_reference)
            )
        }
        item {
            SpecRowCard(
                title = stringResource(R.string.s5_brake_coolant),
                mainValue = stringResource(R.string.s5_coolant_reference, specs.brakeFluidType, specs.coolantCapacityLiters),
                subNotes = stringResource(R.string.s5_parts_reference)
            )
        }
    }
}

@Composable
fun SpecRowCard(title: String, mainValue: String, subNotes: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = mainValue, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subNotes, color = TextPrimary.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
