package com.sarani.carobd.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.storage.UserVehicleSetup
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
fun OnboardingScreen(
    onFinish: (UserVehicleSetup?) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedVehicleName by remember { mutableStateOf("پژو 206") }
    var selectedMake by remember { mutableStateOf("پژو / ایران خودرو") }
    var selectedModel by remember { mutableStateOf("206") }
    var odometerText by remember { mutableStateOf("") }
    var displacement by remember { mutableStateOf(1.4f) }
    var tankCapacity by remember { mutableStateOf(50f) }

    val popularVehicles = listOf(
        VehicleOption("پژو 206 (TU3)", "پژو / ایران خودرو", "206 TU3", 1.4f, 50f),
        VehicleOption("پژو 206 (TU5)", "پژو / ایران خودرو", "206 TU5", 1.6f, 50f),
        VehicleOption("پژو پارس / 405 (XU7)", "پژو / ایران خودرو", "Pars XU7", 1.8f, 70f),
        VehicleOption("سمند / دنا (EF7)", "ایران خودرو", "Samand EF7", 1.7f, 66f),
        VehicleOption("تارا (TU5P)", "ایران خودرو", "Tara", 1.6f, 50f),
        VehicleOption("پراید (M13)", "سایپا", "Pride", 1.3f, 37f),
        VehicleOption("تیبا / ساینا / کوییک (M15)", "سایپا", "Quick / Saina", 1.5f, 41f),
        VehicleOption("شاهین (M15T توربو)", "سایپا", "Shahin", 1.5f, 47f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Skip Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step Dots Indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0..2) {
                            Box(
                                modifier = Modifier
                                    .size(if (step == i) 24.dp else 8.dp, 8.dp)
                                    .background(
                                        if (step == i) NeonCyan else DarkCardBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    TextButton(onClick = { onFinish(null) }) {
                        Text(stringResource(R.string.onboarding_btn_skip), color = TextMuted, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (step) {
                    0 -> {
                        // STEP 1: WELCOME & 4 PILLARS
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.onboarding_welcome_title),
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_welcome_subtitle),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }

                            PillarCard(
                                icon = Icons.Default.DirectionsCar,
                                title = stringResource(R.string.onboarding_feature_garage_title),
                                desc = stringResource(R.string.onboarding_feature_garage_desc),
                                tint = NeonCyan
                            )
                            PillarCard(
                                icon = Icons.Default.Engineering,
                                title = stringResource(R.string.onboarding_feature_maint_title),
                                desc = stringResource(R.string.onboarding_feature_maint_desc),
                                tint = NeonAmber
                            )
                            PillarCard(
                                icon = Icons.Default.Category,
                                title = stringResource(R.string.onboarding_feature_parts_title),
                                desc = stringResource(R.string.onboarding_feature_parts_desc),
                                tint = NeonGreen
                            )
                            PillarCard(
                                icon = Icons.Default.Security,
                                title = stringResource(R.string.onboarding_feature_diag_title),
                                desc = stringResource(R.string.onboarding_feature_diag_desc),
                                tint = NeonCyan
                            )
                        }
                    }

                    1 -> {
                        // STEP 2: SELECT IRANIAN VEHICLE
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = stringResource(R.string.onboarding_step_vehicle),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.onboarding_vehicle_select_prompt),
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                popularVehicles.forEach { opt ->
                                    val isSelected = selectedVehicleName == opt.displayName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedVehicleName = opt.displayName
                                            selectedMake = opt.make
                                            selectedModel = opt.model
                                            displacement = opt.displacement
                                            tankCapacity = opt.tank
                                        },
                                        label = {
                                            Text(opt.displayName, fontSize = 12.sp)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonCyan
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = odometerText,
                                onValueChange = { odometerText = it.filter { char -> char.isDigit() } },
                                label = { Text(stringResource(R.string.my_vehicle_odometer)) },
                                placeholder = { Text("85000") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    2 -> {
                        // STEP 3: SAFETY & OPTIONAL DIAGNOSTICS
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(R.string.onboarding_step_safety),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.connection_safety_title), color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = stringResource(R.string.connection_safety_desc),
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.onboarding_safety_note),
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (step < 2) {
                    Button(
                        onClick = { step++ },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_btn_next),
                            color = DarkBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val setup = UserVehicleSetup(
                                make = selectedMake,
                                model = selectedModel,
                                fuelType = "بنزین",
                                engineDisplacementLiters = displacement,
                                tankCapacityLiters = tankCapacity,
                                odometerKm = odometerText.toIntOrNull() ?: 50000,
                                fuelPriceTomans = 3000f,
                                isMotorcycle = false
                            )
                            onFinish(setup)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_btn_get_started),
                            color = DarkBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarCard(
    icon: ImageVector,
    title: String,
    desc: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

private data class VehicleOption(
    val displayName: String,
    val make: String,
    val model: String,
    val displacement: Float,
    val tank: Float
)
