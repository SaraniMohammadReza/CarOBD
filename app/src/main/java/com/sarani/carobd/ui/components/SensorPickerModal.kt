package com.sarani.carobd.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.database.SensorCatalog
import com.sarani.carobd.core.model.DashboardWidgetConfig
import com.sarani.carobd.core.model.SensorCategory
import com.sarani.carobd.core.model.SensorDefinition
import com.sarani.carobd.core.model.WidgetSize
import com.sarani.carobd.core.model.WidgetType
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary
import java.util.UUID

@Composable
fun SensorPickerModal(
    onWidgetAdded: (DashboardWidgetConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SensorCategory?>(null) }
    var selectedSensor by remember { mutableStateOf<SensorDefinition?>(null) }
    var selectedWidgetType by remember { mutableStateOf(WidgetType.CIRCULAR_GAUGE) }
    var selectedWidgetSize by remember { mutableStateOf(WidgetSize.MEDIUM) }

    val filteredSensors = SensorCatalog.ALL_SENSORS.filter { s ->
        val matchesCategory = selectedCategory == null || s.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                s.nameFa.contains(searchQuery, ignoreCase = true) ||
                s.nameEn.contains(searchQuery, ignoreCase = true) ||
                s.shortName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(com.sarani.carobd.R.string.ui_sensor_picker), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(com.sarani.carobd.R.string.ui_sensor_search), fontSize = 11.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
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

                // Category Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(com.sarani.carobd.R.string.ui_all), fontSize = 10.sp) }
                        )
                    }
                    items(SensorCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.titleFa, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sensor List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredSensors, key = { it.id }) { sensor ->
                        val isChosen = selectedSensor?.id == sensor.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isChosen) DarkBg else DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.dp, if (isChosen) NeonCyan else DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedSensor = sensor }
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sensor.nameFa, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${sensor.nameEn} • ${sensor.category.titleFa}", color = TextMuted, fontSize = 10.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(sensor.unitMetric, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Widget Display Style (عقربه‌ای، کارت، نواری، گراف)
                Text(stringResource(com.sarani.carobd.R.string.ui_widget_type), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(WidgetType.values()) { type ->
                        FilterChip(
                            selected = selectedWidgetType == type,
                            onClick = { selectedWidgetType = type },
                            label = { Text(type.titleFa, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Widget Size (عرض نیمه یا تمام‌عرض)
                Text(stringResource(com.sarani.carobd.R.string.ui_widget_size), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedWidgetSize == WidgetSize.MEDIUM,
                        onClick = { selectedWidgetSize = WidgetSize.MEDIUM },
                        label = { Text(stringResource(com.sarani.carobd.R.string.ui_widget_half), fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedWidgetSize == WidgetSize.LARGE,
                        onClick = { selectedWidgetSize = WidgetSize.LARGE },
                        label = { Text(stringResource(com.sarani.carobd.R.string.ui_widget_full), fontSize = 10.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = selectedSensor ?: return@Button
                    val config = DashboardWidgetConfig(
                        id = UUID.randomUUID().toString(),
                        sensorId = s.id,
                        widgetType = selectedWidgetType,
                        widgetSize = selectedWidgetSize
                    )
                    onWidgetAdded(config)
                },
                enabled = selectedSensor != null,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = DarkBg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(com.sarani.carobd.R.string.ui_widget_add), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextMuted)
            }
        }
    )
}
