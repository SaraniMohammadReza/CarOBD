package com.sarani.carobd.core.vehicle.guide

import com.sarani.carobd.core.model.GuideAlertType
import com.sarani.carobd.core.model.GuideContentBlock
import com.sarani.carobd.core.model.GuideKeyValue
import com.sarani.carobd.core.model.GuideSection
import com.sarani.carobd.core.model.GuideTopic

class DefaultGuideRepository : GuideRepository {

    private val topics: List<GuideTopic> = listOf(
        GuideTopic(
            id = "getting_started_adapter",
            section = GuideSection.GETTING_STARTED,
            titleEn = "Connecting Your OBD-II Adapter",
            titleFa = "اتصال دانگل دیاگ OBD-II",
            summaryEn = "Step-by-step instructions for pairing Bluetooth, BLE, Wi-Fi or USB ELM327 adapters.",
            summaryFa = "راهنمای گام‌به‌گام جفت‌سازی و اتصال انواع دانگل‌های بلوتوث، وای‌فای و USB.",
            blocks = listOf(
                GuideContentBlock.Paragraph(
                    textEn = "CarOBD connects to standard ELM327-compatible OBD-II adapters. Locate your vehicle's 16-pin DLC port, usually beneath the steering column or near the fuse box.",
                    textFa = "اپلیکیشن CarOBD با انواع دانگل‌های استاندارد سازگار با ELM327 کار می‌کند. پورت ۱۶ پین OBD خودرو معمولاً زیر فرمان، پشت قاب جعبه فیوز یا کنسول میانی قرار دارد."
                ),
                GuideContentBlock.Alert(
                    type = GuideAlertType.WARNING,
                    titleEn = "Turn Ignition ON",
                    titleFa = "سوئیچ را باز کنید",
                    messageEn = "The vehicle ignition must be ON (engine running or position II) for the ECU to respond to diagnostic queries.",
                    messageFa = "برای پاسخ‌دهی کامپیوتر خودرو (ECU)، سوئیچ خودرو باید باز باشد (حالت باز یا موتور روشن)."
                ),
                GuideContentBlock.KeyValues(
                    titleEn = "Supported Transports",
                    titleFa = "روش‌های اتصال پشتیبانی‌شده",
                    entries = listOf(
                        GuideKeyValue("Bluetooth Classic", "بلوتوث کلاسیک", "SPP 2.0/3.0 (e.g. OBDII / vLinker)", "پروتکل SPP با پین ۱۲۳۴ یا ۰۰۰۰"),
                        GuideKeyValue("Bluetooth Low Energy", "بلوتوث کم‌مصرف (BLE)", "GATT Characteristic (Android 12+)", "نیاز به دسترسی بلوتوث دستگاه‌های نزدیک"),
                        GuideKeyValue("Wi-Fi TCP", "وای‌فای شبکه", "TCP 192.168.0.10:35000", "اتصال به اکسس‌پوینت دانگل بدون اینترنت"),
                        GuideKeyValue("USB Serial", "کابل USB / OTG", "FTDI, CH340, CP2102, PL2303", "اتصال مستقیم سیمی پایدار")
                    )
                )
            ),
            tags = listOf("adapter", "bluetooth", "wifi", "usb", "elm327"),
            relatedTopicIds = listOf("diagnostics_readonly")
        ),
        GuideTopic(
            id = "ownership_vehicle_specs",
            section = GuideSection.OWNERSHIP,
            titleEn = "Managing Your Owned Vehicles",
            titleFa = "مدیریت مشخصات و پرونده خودروی من",
            summaryEn = "Track vehicle odometer, engine code, oil grade, and verified technical specifications.",
            summaryFa = "ثبت کیلومتر کارکرد، کد موتور، مشخصات روغن و دفترچه فنی خودرو.",
            blocks = listOf(
                GuideContentBlock.Paragraph(
                    textEn = "The My Vehicle section stores physical vehicles you own separately from diagnostic ECU communication profiles. Only enter factual data known from your vehicle registration or owner manual.",
                    textFa = "بخش «خودروی من» پرونده اختصاصی خودروهای شما را نگهداری می‌کند. مشخصات واقعی مانند کیلومتر کارکرد و نوع موتور را ثبت کنید تا محاسبات مصرفی‌ها دقیق انجام شود."
                ),
                GuideContentBlock.KeyValues(
                    titleEn = "Common Iranian Engine Codes",
                    titleFa = "کدهای رایج موتور خودروهای داخلی",
                    entries = listOf(
                        GuideKeyValue("TU5 / TU5P", "موتور TU5 و TU5 پلاس", "1.6L 16V DOHC (206, 207, Pars, Tara)", "۱.۶ لیتری ۱۶ سوپاپ — روغن 10W-40 یا 5W-40"),
                        GuideKeyValue("EF7 NA / TC", "موتور ملی EF7 و توربو", "1.7L 16V CVVT (Samand, Dena, Dena+)", "۱.۷ لیتری ۱۶ سوپاپ پایه گازسوز/بنزین — روغن 10W-40 SL/SM"),
                        GuideKeyValue("K4M", "موتور ال‌۹۰ / تندر", "1.6L 16V Renault (Tondar 90, Sandero)", "۱.۶ لیتری رنو — روغن 10W-40 RN0700"),
                        GuideKeyValue("M15 / M15TC", "موتور سایپا M15", "1.5L 8V (Tiba, Saina, Quik, Shahin TC)", "۱.۵ لیتری ۸ سوپاپ — روغن 10W-40 یا 20W-50")
                    )
                )
            ),
            tags = listOf("vehicle", "engine", "tu5", "ef7", "k4m", "odometer"),
            relatedTopicIds = listOf("maintenance_timing_belt", "parts_fitment")
        ),
        GuideTopic(
            id = "maintenance_timing_belt",
            section = GuideSection.MAINTENANCE,
            titleEn = "Timing Belt & Service Schedule Safety",
            titleFa = "ایمنی تسمه تایم و برنامه‌ریزی سرویس‌های دوره‌ای",
            summaryEn = "Crucial guidelines for interference engines and timing belt replacement intervals.",
            summaryFa = "نکات حیاتی درباره موتورهای تداخلی و فواصل ایمن تعویض تسمه تایم.",
            blocks = listOf(
                GuideContentBlock.Alert(
                    type = GuideAlertType.CAUTION,
                    titleEn = "Interference Engine Risk",
                    titleFa = "خطر برخورد سوپاپ با پیستون",
                    messageEn = "Engines such as TU5, EF7, and K4M are interference designs: a broken or jumped timing belt will bend valves and cause severe internal engine damage.",
                    messageFa = "موتورهای TU5، EF7 و K4M طراحی تداخلی دارند؛ در صورت پارگی تسمه تایم، سوپاپ‌ها با پیستون برخورد کرده و موتور آسیب سنگین می‌بیند."
                ),
                GuideContentBlock.Paragraph(
                    textEn = "Strictly adhere to verified replacement intervals. For severe urban operating conditions, inspect belts every 20,000 km and replace by 60,000 km or 3 to 4 years, whichever comes first.",
                    textFa = "در شرایط رانندگی شهری و ترافیک سنگین، توصیه می‌شود تسمه تایم حداکثر هر ۶۰٬۰۰۰ کیلومتر یا ۴ سال (هرکدام زودتر فرارسد) به همراه بلبرینگ تایم تعویض گردد."
                )
            ),
            tags = listOf("maintenance", "timing_belt", "engine", "safety"),
            relatedTopicIds = listOf("parts_fitment")
        ),
        GuideTopic(
            id = "parts_fitment",
            section = GuideSection.PARTS,
            titleEn = "Parts Compatibility & OEM Numbers",
            titleFa = "سازگاری قطعات یدکی و پارت‌نامبرهای کارخانه (OEM)",
            summaryEn = "How CarOBD verifies part compatibility using engine variants and OEM reference codes.",
            summaryFa = "روش تطبیق قطعات بر اساس کد دقیق موتور و شماره‌های فنی کارخانه.",
            blocks = listOf(
                GuideContentBlock.Paragraph(
                    textEn = "CarOBD never guesses fitment across different engine families. Parts are gated by verified OEM engineering references and community technical consensus.",
                    textFa = "در مرکز قطعات CarOBD، قطعات یدکی بدون حدس و گمان و فقط بر اساس کد دقیق موتور و کاتالوگ‌های معتبر (مانند رنو MR388 و ایساکو) پیشنهاد می‌شوند."
                ),
                GuideContentBlock.Alert(
                    type = GuideAlertType.TIP,
                    titleEn = "Verify Before Purchasing",
                    titleFa = "تطبیق فیزیکی پیش از خرید",
                    messageEn = "Always compare the OEM part number on your existing physical component with the numbers provided in the app before ordering.",
                    messageFa = "همیشه قبل از خرید، شماره فنی درج‌شده روی قطعه فابریک خودرو را با پارت‌نامبرهای پیشنهادی تطبیق دهید."
                )
            ),
            tags = listOf("parts", "oem", "filters", "spark_plugs"),
            relatedTopicIds = listOf("ownership_vehicle_specs")
        ),
        GuideTopic(
            id = "diagnostics_readonly",
            section = GuideSection.DIAGNOSTICS,
            titleEn = "Read-Only Diagnostic Safety",
            titleFa = "ایمنی عیب‌یابی فقط‌خواندنی (Read-Only)",
            summaryEn = "Why physical clear DTC (Mode 04) is denied and how read-only diagnostics protects your ECU.",
            summaryFa = "علت غیرفعال بودن پاک کردن فیزیکی خطای موتور و نحوه حفاظت از ECU.",
            blocks = listOf(
                GuideContentBlock.Alert(
                    type = GuideAlertType.CAUTION,
                    titleEn = "Diagnostic Safety Guard",
                    titleFa = "حفاظت سخت‌افزاری کامپیوتر خودرو",
                    messageEn = "Physical Mode 04 (Clear Diagnostic Trouble Codes) is strictly denied in this version to eliminate any risk of bus contention, reset during driving, or improper ECU reboot.",
                    messageFa = "دستور پاک کردن خطای فیزیکی (Mode 04) برای جلوگیری از اختلال در شبکه CAN و خاموش شدن ناخواسته خودرو غیرفعال است. رفع عیب باید به‌صورت فیزیکی انجام شود."
                ),
                GuideContentBlock.Paragraph(
                    textEn = "Clearing a trouble code without repairing the underlying mechanical or electrical fault does not fix the vehicle. Once the fault is repaired, the ECU will automatically extinguish the Check Engine Light after completion of the drive cycle.",
                    textFa = "پاک کردن کد خطا بدون رفع عیب واقعی، مشکل فنی را حل نمی‌کند. پس از رفع نقص فنی، کامپیوتر خودرو پس از چند سیکل رانندگی چراغ چک را خاموش خواهد کرد."
                )
            ),
            tags = listOf("diagnostics", "dtc", "safety", "readonly"),
            relatedTopicIds = listOf("getting_started_adapter")
        )
    )

    override fun getSections(): List<GuideSection> = GuideSection.entries.toList()

    override fun getAllTopics(): List<GuideTopic> = topics

    override fun getTopicsForSection(section: GuideSection): List<GuideTopic> =
        topics.filter { it.section == section }

    override fun getTopicById(id: String): GuideTopic? =
        topics.firstOrNull { it.id == id }

    override fun searchTopics(query: String): List<GuideTopic> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return topics
        return topics.filter { t ->
            t.titleEn.lowercase().contains(q) ||
                t.titleFa.contains(q) ||
                t.summaryEn.lowercase().contains(q) ||
                t.summaryFa.contains(q) ||
                t.tags.any { it.contains(q) }
        }
    }

    override fun getRelatedTopics(topicId: String): List<GuideTopic> {
        val topic = getTopicById(topicId) ?: return emptyList()
        return topic.relatedTopicIds.mapNotNull { getTopicById(it) }
    }
}
