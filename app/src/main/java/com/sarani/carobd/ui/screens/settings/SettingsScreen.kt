package com.sarani.carobd.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.storage.AppLanguage
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.core.storage.AppThemeMode
import com.sarani.carobd.core.storage.LocaleHelper
import com.sarani.carobd.core.storage.UnitSystem
import kotlinx.coroutines.launch
import com.sarani.carobd.ui.theme.DarkBg
import com.sarani.carobd.ui.theme.DarkCardBg
import com.sarani.carobd.ui.theme.DarkCardBorder
import com.sarani.carobd.ui.theme.NeonAmber
import com.sarani.carobd.ui.theme.NeonCyan
import com.sarani.carobd.ui.theme.NeonGreen
import com.sarani.carobd.ui.theme.TextMuted
import com.sarani.carobd.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    appPreferencesManager: AppPreferencesManager,
    onNavigateToAdapterInspector: () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    // Real persisted settings (§Phase C): pickers read/write the DataStore via
    // AppPreferencesManager — no local-only remember state for theme/unit/language.
    val themeMode by appPreferencesManager.themeMode.collectAsState(AppThemeMode.AMOLED_BLACK)
    val unitSystem by appPreferencesManager.unitSystem.collectAsState(UnitSystem.METRIC)
    val language by appPreferencesManager.language.collectAsState(AppLanguage.PERSIAN)
    val isPersian = language == AppLanguage.PERSIAN
    var selectedRate by remember { mutableStateOf("15 Hz (Ultra Fast)") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Theme Customization & AMOLED Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تم و پوسته ظاهری (پشتیبانی از AMOLED)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AppThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { appPreferencesManager.setThemeMode(mode) } }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode.titleFa, color = if (themeMode == mode) NeonCyan else TextMuted, fontSize = 12.sp)
                            if (themeMode == mode) {
                                Text("فعال", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Unit System (Metric vs Imperial)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سیستم یکاها و واحدها (Units)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    UnitSystem.values().forEach { unit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { appPreferencesManager.setUnitSystem(unit) } }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(unit.titleFa, color = if (unitSystem == unit) NeonCyan else TextMuted, fontSize = 12.sp)
                            if (unitSystem == unit) {
                                Text("انتخاب شده", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Adapter & Motorcycle Inspector Navigation Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onNavigateToAdapterInspector() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("بررسی سخت‌افزار دانگل و پین‌اوت موتورسیکلت", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("تست اصالت چیپ ELM327 و کابل‌های تبدیل ۶ پین", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Text("مشاهده >", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Language Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.settings_language), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isPersian) "فارسی (Persian - پیش‌فرض)" else "English", color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    Switch(
                        checked = isPersian,
                        onCheckedChange = { nextIsPersian ->
                            val lang = if (nextIsPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH
                            scope.launch { appPreferencesManager.setLanguage(lang) }
                            LocaleHelper.applyAndRecreate(activity, lang)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }

            // Re-run Vehicle Profile Setup
            OutlinedButton(
                onClick = onResetOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تغییر مشخصات خودرو / موتورسیکلت (تنظیم مجدد پروفایل)")
            }

            // Architecture & Backend-Ready Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("مشخصات معماری سوپر اپلیکیشن", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• معماری ماژولار Clean Architecture + MVI\n• سیستم کشینگ آفلاین با DataStore Preferences\n• آماده اتصال به بک‌اند ابری (Cloud Sync Ready)\n• پشتیبانی از ۱۰۰+ سنسور خودرو و موتورسیکلت\n• پشتیبانی از خط K-Line و شبکه CAN تمام خودروهای داخلی، چینی و بین‌المللی",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
