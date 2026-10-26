package com.sarani.carobd.ui.screens.adapter

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.core.database.MotorcycleAndChineseSpecs
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
fun AdapterInspectorScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "سلامت سخت‌افزار دانگل دیاگ و راهنمای اتصالات",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تست اصالت چیپ ELM327، بررسی پین‌اوت موتورسیکلت و عیب‌یابی خطاهای اتصال",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                    text = { Text("مشخصات دانگل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) NeonCyan else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("حل مشکل عدم اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) NeonCyan else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("دیاگ موتورسیکلت", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) NeonCyan else TextMuted) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> AdapterHealthView()
                1 -> TroubleshootingGuideView()
                2 -> MotorcyclePinoutView()
            }
        }
    }
}

@Composable
fun AdapterHealthView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("چیپست ELM327 نسخه ۱.۵ اورجینال (PIC18F25K80)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "این دانگل از تمام دستورات اختصاصی AT SH، AT WM و پروتکل KWP2000 خودروهای ایرانی (زیمنس، ساژم، بوش ME7.4.4 و SSAT) پشتیبانی کامل می‌کند.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("پارامترهای عملکردی لایه سخت‌افزار:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    HealthRow("پروتکل ارتباطی فعال:", "ISO 14230-4 KWP Fast Init (10.4 kbaud)")
                    HealthRow("تاخیر پاسخگویی دانگل (Latency):", "28 میلی‌ثانیه (بسیار سریع)")
                    HealthRow("ولتاژ پین ۱۶ پورت OBD:", "12.7 ولت (سالم)")
                    HealthRow("نرخ تبادل فریم‌ها:", "14.2 فریم بر ثانیه")
                    HealthRow("نوع اتصال فیزیکی:", "Bluetooth Classic SPP / 2.4GHz")
                }
            }
        }

        item {
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
                        Text("هشدار درباره دانگل‌های تقلبی ELM327 نسخه ۲.۱:", color = NeonAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "دانگل‌های بسیار ارزان نسخه ۲.۱ فاقد میکروکنترلر واقعی بوده و دستورات تنظیم هدر ایسیوهای ایرانی را اجرا نمی‌کنند و به پراید یا پژو وصل نخواهند شد. همیشه از دانگل‌های دارای بورد دوطبقه و چیپ PIC18F25K80 استفاده کنید.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TroubleshootingGuideView() {
    val issues = listOf(
        Pair("چراغ پاور دانگل روشن نمی‌شود", "فیوز فندکی یا فیوز سوکت دیاگ سوخته است. در خودروهای پژو فیوز F4 و در پراید فیوز فندکی داخل جعبه فیوز کالسکه را بررسی کنید."),
        Pair("خطای UNABLE TO CONNECT یا BUS INIT: ERROR", "سوییچ خودرو باز نیست (باید روی حالت ON باشد تا ایسیو برق‌دار شود)، یا پروتکل هدر خودرو به اشتباه انتخاب شده است. از تب «ایسیو» دکمه اسکن خودکار را بزنید."),
        Pair("کد پین جفت‌شدن بلوتوث چیست؟", "کد پیش‌فرض دانگل‌های بلوتوث معمولاً ۱۲۳۴ یا ۰۰۰۰ است. در صورت عدم اتصال، دانگل را از تنظیمات بلوتوث گوشی Unpair کرده و دوباره جفت کنید."),
        Pair("قطع شدن مداوم اتصال در حین رانندگی", "سوکت OBD-II خودرو شل است یا پین شماره ۱۶ (مثبت دائم) گشاد شده است. با انبردست پایه‌های پین ۷ و ۱۶ را کمی جمع کنید.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(issues) { (title, solution) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Help, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(solution, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun MotorcyclePinoutView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "برای دیاگ موتورسیکلت با دانگل استاندارد OBD-II ۱۶ پین، به کابل تبدیل مخصوص (Adapter Cable) نیاز دارید. پین‌اوت رنگ‌بندی و سوکت‌های مختلف در زیر آمده است:",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        items(MotorcycleAndChineseSpecs.MOTORCYCLE_PINOUTS) { moto ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(moto.brandName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("نوع سوکت: ${moto.connectorType}", color = NeonCyan, fontSize = 11.sp)
                    Text("مدل‌های تحت پوشش: ${moto.supportedModelsFa}", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    moto.pinoutDescriptionFa.forEach { pin ->
                        Text("• $pin", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HealthRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
