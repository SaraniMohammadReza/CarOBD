package com.sarani.carobd.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.model.ServiceReminderItem
import com.sarani.carobd.core.model.VehicleSpecSheet
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.NeonRed
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Vehicle Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "اطلاعات فنی و سرویس‌های ${state.activeProfile?.modelNameFa ?: "خودروی انتخاب‌شده"}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "موتور: ${state.specSheet?.engineCode ?: "—"}",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(DarkCardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${state.currentOdometerKm} km",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-tabs: 1. سرویس‌های دوره‌ای (Reminders) 2. مشخصات روغن و شمع (Specs)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCardBg,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("یادآور سرویس‌های دوره‌ای", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) NeonCyan else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("مشخصات فنی روغن و قطعات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) NeonCyan else TextMuted) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Tab 1: Service Reminders
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.reminders, key = { it.id }) { reminder ->
                        ServiceReminderCard(
                            reminder = reminder,
                            onResetClicked = { viewModel.onIntent(MaintenanceIntent.ResetService(reminder.id)) }
                        )
                    }
                }
            } else {
                // Tab 2: Vehicle Technical Spec Sheet
                state.specSheet?.let { TechnicalSpecsView(specs = it) }
            }
        }
    }
}

@Composable
fun ServiceReminderCard(
    reminder: ServiceReminderItem,
    onResetClicked: () -> Unit
) {
    val progress = reminder.progressPercent
    val isOverdue = reminder.isOverdue

    val barColor = when {
        isOverdue -> NeonRed
        progress < 0.25f -> NeonAmber
        else -> NeonGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, if (isOverdue) NeonRed.copy(alpha = 0.5f) else DarkCardBorder, RoundedCornerShape(12.dp))
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
                        imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Engineering,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = reminder.type.titleFa,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (reminder.notes.isNotBlank()) {
                            Text(text = reminder.notes, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = onResetClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("انجام شد", color = NeonCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = barColor,
                trackColor = DarkBg,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isOverdue) "منقضی شده! (${-reminder.remainingKm} km گذشته)" else "باقی‌مانده: ${reminder.remainingKm} km",
                    color = barColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "تعویض در: ${reminder.nextDueKm} km",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TechnicalSpecsView(specs: VehicleSpecSheet) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SpecRowCard(
                title = "روغن موتور توصیه شده",
                mainValue = "${specs.engineOilGrade} (${specs.engineOilCapacityLiters} لیتر با فیلتر)",
                subNotes = "برندهای معتبر: ${specs.recommendedOilBrands.joinToString("، ")}"
            )
        }
        item {
            SpecRowCard(
                title = "شمع موتور و فیلر مجاز",
                mainValue = specs.sparkPlugModel,
                subNotes = "فاصله دهانه شمع (فیلر): ${specs.sparkPlugGapMm} میلی‌متر"
            )
        }
        item {
            SpecRowCard(
                title = "تسمه تایم و دوره تعویض",
                mainValue = "${specs.timingBeltIntervalKm} کیلومتر (${specs.timingBeltTeeth} دندانه)",
                subNotes = "همراه با هرزگرد ثابت و ساعتی تعویض شود"
            )
        }
        item {
            SpecRowCard(
                title = "روغن گیربکس (واسکازین)",
                mainValue = "${specs.gearboxOilGrade} (${specs.gearboxCapacityLiters} لیتر)",
                subNotes = "هر ۴۰ تا ۵۰ هزار کیلومتر تعویض گردد"
            )
        }
        item {
            SpecRowCard(
                title = "روغن ترمز و خنک‌کننده",
                mainValue = "${specs.brakeFluidType} • مایع خنک‌کننده: ${specs.coolantCapacityLiters} لیتر",
                subNotes = "از ضدیخ با پایه آلی (OAT) با نسبت ۵۰/۵۰ استفاده شود"
            )
        }
        item {
            // Critical Tips Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نکات فنی و حیاتی نگهداری این خودرو", color = NeonAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    specs.maintenanceTipsFa.forEach { tip ->
                        Text(
                            text = "• $tip",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
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
