package com.sarani.carobd.ui.screens.owned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.OwnedVehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OwnedVehiclesScreen(
    viewModel: OwnedVehiclesViewModel,
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToParts: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.my_vehicle_title)) }) },
        floatingActionButton = { FloatingActionButton(modifier = Modifier.testTag("owned_vehicle_add"), onClick = viewModel::add) { Icon(Icons.Default.Add, null) } }) { padding ->
        if (state.vehicles.isEmpty()) Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(stringResource(R.string.my_vehicle_empty))
        } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.vehicles, key = { it.id }) { vehicle -> VehicleRow(vehicle, vehicle.id == state.selectedId,
                { viewModel.select(vehicle.id) }, { viewModel.edit(vehicle) }, { viewModel.requestDelete(vehicle) },
                { viewModel.select(vehicle.id); onNavigateToMaintenance() },
                { viewModel.select(vehicle.id); onNavigateToParts() }) }
        }
    }
    state.editor?.let { VehicleEditorDialog(it, state.error, viewModel::update, viewModel::save, viewModel::cancelEdit) }
    state.deleteCandidate?.let { vehicle -> AlertDialog(onDismissRequest = viewModel::cancelDelete,
        title = { Text(stringResource(R.string.my_vehicle_delete_title)) },
        text = { Text(stringResource(R.string.my_vehicle_delete_message, vehicle.displayName)) },
        confirmButton = { TextButton(modifier = Modifier.testTag("owned_vehicle_delete_confirm"), onClick = viewModel::confirmDelete) { Text(stringResource(R.string.ui_delete)) } },
        dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.ui_cancel)) } }) }
}

@Composable private fun VehicleRow(
    v: OwnedVehicle,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMaintenance: () -> Unit = {},
    onParts: () -> Unit = {}
) {
    ElevatedCard(Modifier.fillMaxWidth().testTag("owned_vehicle_${v.id}").clickable(onClick = onSelect)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(v.displayName, style = MaterialTheme.typography.titleMedium)
                    val details = listOfNotNull(v.manufacturer, v.model, v.year?.toString()).joinToString(" · ")
                    Text(details.ifBlank { stringResource(R.string.my_vehicle_unknown_details) })
                    if (v.currentOdometerKm != null) {
                        Text("${v.currentOdometerKm} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (selected) AssistChip(onClick = {}, label = { Text(stringResource(R.string.my_vehicle_selected)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onMaintenance) { Icon(Icons.Default.Engineering, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.nav_maintenance)) }
                TextButton(onClick = onParts) { Icon(Icons.Default.Category, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.nav_parts)) }
                TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Text(stringResource(R.string.ui_edit)) }
                TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Text(stringResource(R.string.ui_delete)) }
            }
        }
    }
}

@Composable private fun VehicleEditorDialog(d: OwnedVehicleDraft, error: OwnedVehicleEditorError?, update: (OwnedVehicleDraft) -> Unit, save: () -> Unit, cancel: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text(stringResource(if (d.id == null) R.string.my_vehicle_add else R.string.my_vehicle_edit)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(R.string.my_vehicle_name, d.displayName) { update(d.copy(displayName = it)) }
            Field(R.string.my_vehicle_manufacturer, d.manufacturer) { update(d.copy(manufacturer = it)) }
            Field(R.string.my_vehicle_model, d.model) { update(d.copy(model = it)) }
            Field(R.string.my_vehicle_generation, d.generation) { update(d.copy(generation = it)) }
            Field(R.string.my_vehicle_year, d.year, KeyboardType.Number) { update(d.copy(year = it)) }
            Field(R.string.my_vehicle_variant, d.variant) { update(d.copy(variant = it)) }
            Field(R.string.my_vehicle_engine, d.engine) { update(d.copy(engine = it)) }
            Field(R.string.my_vehicle_engine_code, d.engineCode) { update(d.copy(engineCode = it)) }
            Field(R.string.my_vehicle_transmission, d.transmission) { update(d.copy(transmission = it)) }
            Field(R.string.my_vehicle_fuel_type, d.fuelType) { update(d.copy(fuelType = it)) }
            Field(R.string.my_vehicle_market, d.market) { update(d.copy(market = it)) }
            Field(R.string.my_vehicle_vin, d.vin) { update(d.copy(vin = it)) }
            Field(R.string.my_vehicle_plate, d.licensePlate) { update(d.copy(licensePlate = it)) }
            Field(R.string.my_vehicle_odometer, d.odometer, KeyboardType.Number) { update(d.copy(odometer = it)) }
            Field(R.string.my_vehicle_tank, d.tankCapacity, KeyboardType.Decimal) { update(d.copy(tankCapacity = it)) }
            Field(R.string.my_vehicle_notes, d.notes) { update(d.copy(notes = it)) }
            error?.let { Text(stringResource(when(it) { OwnedVehicleEditorError.NAME_REQUIRED -> R.string.my_vehicle_error_name; OwnedVehicleEditorError.YEAR_INVALID -> R.string.my_vehicle_error_year; OwnedVehicleEditorError.ODOMETER_INVALID -> R.string.my_vehicle_error_odometer; OwnedVehicleEditorError.TANK_INVALID -> R.string.my_vehicle_error_tank; OwnedVehicleEditorError.SAVE_FAILED -> R.string.my_vehicle_error_save }), color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(modifier = Modifier.testTag("owned_vehicle_save"), onClick = save) { Text(stringResource(R.string.ui_save)) } },
        dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.ui_cancel)) } })
}

@Composable private fun Field(label: Int, value: String, keyboard: KeyboardType = KeyboardType.Text, changed: (String) -> Unit) {
    OutlinedTextField(value, changed, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, singleLine = label != R.string.my_vehicle_notes,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard))
}
