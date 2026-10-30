package com.sarani.carobd.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.storage.UserVehicleSetup
import com.sarani.carobd.core.storage.VehicleSetupNumbers
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleOnboardingModal(
    onSetupSaved: (UserVehicleSetup) -> Unit
) {
    var isMotorcycle by remember { mutableStateOf(false) }
    val carMakes = androidx.compose.ui.res.stringArrayResource(com.sarani.carobd.R.array.s5_car_makes)
    val bikeMakes = androidx.compose.ui.res.stringArrayResource(com.sarani.carobd.R.array.s5_bike_makes)
    val fuelOptions = androidx.compose.ui.res.stringArrayResource(com.sarani.carobd.R.array.s5_fuel_options)
    var selectedMake by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var tankCapacityText by remember { mutableStateOf("") }
    var odometerText by remember { mutableStateOf("") }

    val tankCapacity = VehicleSetupNumbers.tankLitres(tankCapacityText)
    val odometer = VehicleSetupNumbers.odometerKm(odometerText)
    val tankInvalid = tankCapacityText.isNotBlank() && tankCapacity == null
    val odometerInvalid = odometerText.isNotBlank() && odometer == null

    val makesList = if (isMotorcycle) bikeMakes.toList() else carMakes.toList()

    AlertDialog(
        onDismissRequest = {},
        containerColor = DarkCardBg,
        title = {
            Column {
                Text(
                    text = stringResource(com.sarani.carobd.R.string.ui_onboarding_title),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(com.sarani.carobd.R.string.ui_onboarding_limits),
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Type selector: Car vs Motorcycle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!isMotorcycle) DarkBg else DarkBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (!isMotorcycle) NeonCyan else DarkCardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                isMotorcycle = false
                                selectedMake = ""
                                modelName = ""
                                tankCapacityText = ""

                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = if (!isMotorcycle) NeonCyan else TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.sarani.carobd.R.string.ui_car), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isMotorcycle) DarkBg else DarkBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (isMotorcycle) NeonCyan else DarkCardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                isMotorcycle = true
                                selectedMake = ""
                                modelName = ""
                                tankCapacityText = ""

                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (isMotorcycle) NeonCyan else TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.sarani.carobd.R.string.ui_motorcycle), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brand / Make row
                Text(stringResource(com.sarani.carobd.R.string.ui_make), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(makesList) { make ->
                        FilterChip(
                            selected = selectedMake == make,
                            onClick = { selectedMake = make },
                            label = { Text(make, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model name input
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(stringResource(com.sarani.carobd.R.string.ui_vehicle_model), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fuel type row
                Text(stringResource(com.sarani.carobd.R.string.ui_fuel), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fuelOptions.forEach { f ->
                        FilterChip(
                            selected = fuelType == f,
                            onClick = { fuelType = f },
                            label = { Text(f, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Numerical inputs (Tank capacity & Odometer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tankCapacityText,
                        onValueChange = { tankCapacityText = it },
                        isError = tankInvalid,
                        supportingText = if (tankInvalid) ({ Text(stringResource(com.sarani.carobd.R.string.ui_number_invalid)) }) else null,
                        label = { Text(stringResource(com.sarani.carobd.R.string.ui_tank_capacity), fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        isError = odometerInvalid,
                        supportingText = if (odometerInvalid) ({ Text(stringResource(com.sarani.carobd.R.string.ui_number_invalid)) }) else null,
                        label = { Text(stringResource(com.sarani.carobd.R.string.ui_odometer), fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val setup = UserVehicleSetup(
                        make = selectedMake,
                        model = modelName,
                        fuelType = fuelType,
                        tankCapacityLiters = tankCapacity,
                        odometerKm = odometer,
                        isMotorcycle = isMotorcycle
                    )
                    onSetupSaved(setup)
                },
                enabled = !tankInvalid && !odometerInvalid,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBg)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(com.sarani.carobd.R.string.ui_onboarding_save), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        }
    )
}
