package com.sarani.carobd.core.protocol.dtc

import com.sarani.carobd.core.model.DtcSeverity

data class DtcLookupEntry(
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val severity: DtcSeverity,
    val likelyCausesFa: List<String>,
    val diagnosticStepsFa: List<String>
)

/**
 * Diagnostic Trouble Code dictionary covering standard SAE J2012 codes and
 * Iranian vehicle specific DTCs (Bosch ME7.4.4 / ME7.4.9, Siemens, SSAT, Valeo S2000, EasyU).
 */
object DtcLookupEngine {

    private val database: Map<String, DtcLookupEntry> = listOf(
        // Fuel & Air Metering
        DtcLookupEntry(
            code = "P0100",
            titleEn = "Mass or Volume Air Flow Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور جریان هوا (MAF)",
            descriptionEn = "Mass or volume air flow sensor circuit malfunction detected by ECU.",
            descriptionFa = "سیگنال ارسالی از سنسور هوای ورودی به کامپیوتر خودرو قطع یا دارای اختلال است.",
            severity = DtcSeverity.MODERATE,
            likelyCausesFa = listOf("خرابی سنسور مپ/مف", "نشتی هوای منیفولد ورودی", "کثیفی المنت سنسور", "اتصالی یا قطعی سیم‌کشی"),
            diagnosticStepsFa = listOf("بررسی ولتاژ تغذیه ۵ ولت سنسور", "شستشوی سنسور با اسپری مخصوص MAF", "بررسی نشتی خرطومی هواکش")
        ),
        DtcLookupEntry(
            code = "P0101",
            titleEn = "Mass Air Flow Circuit Range/Performance Problem",
            titleFa = "خطای عملکرد خارج از محدوده سنسور هوا (MAF)",
            descriptionEn = "Mass air flow sensor output is out of expected operating range.",
            descriptionFa = "داده‌های دریافتی از سنسور هوا با میزان دور موتور و بار خودرو همخوانی ندارد.",
            severity = DtcSeverity.MODERATE,
            likelyCausesFa = listOf("آلودگی سنسور جریان هوا", "پارگی یا نشتی خرطومی هواکش", "گیر کردن سوپاپ EGR"),
            diagnosticStepsFa = listOf("تست سلامت سوپاپ EGR", "بررسی اورینگ‌های مانیفولد ورودی", "تست پارامتر حجم هوای منیفولد در دیاگ")
        ),
        DtcLookupEntry(
            code = "P0105",
            titleEn = "Manifold Absolute Pressure Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور فشار مطلق منیفولد (MAP)",
            descriptionEn = "Manifold Absolute Pressure (MAP) circuit electrical malfunction.",
            descriptionFa = "خطای الکتریکی در مدار سنسور مپ (فشار هوای مانیفولد ورودی).",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی سنسور مپ", "نشتی شلنگ‌های وکیوم منیفولد", "ایراد در تغذیه ۵ ولت سنسور"),
            diagnosticStepsFa = listOf("اندازه‌گیری ولتاژ پایه سیگنال در حالت سوئیچ باز (حدود ۴ تا ۴.۵ ولت)", "چک کردن فشار مکش در دور آرام (حدود ۳۰۰ تا ۳۵۰ میلی‌بار)")
        ),
        DtcLookupEntry(
            code = "P0106",
            titleEn = "MAP Sensor Range/Performance Problem",
            titleFa = "خطای محدوده یا کارکرد سنسور مپ (MAP)",
            descriptionEn = "MAP sensor signal does not correlate with throttle position and engine speed.",
            descriptionFa = "سیگنال سنسور مپ با زاویه دریچه گاز و دور موتور همخوانی ندارد.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("نشتی هوای شدید در منیفولد", "خرابی یا خطای اندازه‌گیری سنسور مپ", "ایراد در کالیبراسیون ECU"),
            diagnosticStepsFa = listOf("بررسی سلامت واشر منیفولد هوا", "تست صحت زاویه باز شدن دریچه گاز برقی یا مکانیکی")
        ),
        DtcLookupEntry(
            code = "P0110",
            titleEn = "Intake Air Temperature Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور دمای هوای ورودی (IAT)",
            descriptionEn = "Intake Air Temperature sensor circuit malfunction.",
            descriptionFa = "ایراد در ارسال سیگنال دمای هوای ورودی به موتور.",
            severity = DtcSeverity.INFORMATIONAL,
            likelyCausesFa = listOf("خرابی سنسور دمای هوا", "شل بودن سوکت سنسور", "خرابی یا قطعی دسته سیم"),
            diagnosticStepsFa = listOf("بررسی مقاومت NTC سنسور بر حسب اهم در دمای ۲۰ درجه سانتی‌گراد", "بررسی سوکت سنسور از نظر اکسیداسیون")
        ),
        DtcLookupEntry(
            code = "P0115",
            titleEn = "Engine Coolant Temperature Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور دمای مایع خنک‌کننده موتور (ECT)",
            descriptionEn = "Engine Coolant Temperature sensor circuit electrical fault.",
            descriptionFa = "اشکال در مدار سنسور دمای آب سبز/آبی که منجر به روشن ماندن مداوم دور تند فن می‌شود.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی فشنگی آب (سنسور دمای آب)", "کم بودن سطح مایع خنک‌کننده", "گیر کردن ترموستات", "قطعی سیم‌کشی به ECU"),
            diagnosticStepsFa = listOf("بررسی دمای نمایش داده شده در دیاگ", "چک کردن مقاومت سنسور در دمای کاری (حدود ۲۰۰ تا ۳۰۰ اهم در ۹۰ درجه)")
        ),
        DtcLookupEntry(
            code = "P0117",
            titleEn = "Engine Coolant Temperature Circuit Low Input",
            titleFa = "ولتاژ ورودی مدار سنسور دمای آب پایین (اتصال به بدنه)",
            descriptionEn = "Engine Coolant Temperature sensor circuit input voltage too low.",
            descriptionFa = "اتصال کوتاه سیم سنسور دمای آب به بدنه خودرو که ECU آن را به اشتباه داغ کردن موتور تلقی می‌کند.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("اتصال کوتاه سیم سنسور به بدنه", "سوختن سنسور دمای آب", "داغ کردن واقعی موتور"),
            diagnosticStepsFa = listOf("جدا کردن سوکت سنسور و بررسی رفع اتصالی", "بررسی سلامت سیم‌کشی روی سرسیلندر")
        ),
        DtcLookupEntry(
            code = "P0118",
            titleEn = "Engine Coolant Temperature Circuit High Input",
            titleFa = "ولتاژ ورودی مدار سنسور دمای آب بالا (قطعی مدار)",
            descriptionEn = "Engine Coolant Temperature sensor circuit input voltage too high (open circuit).",
            descriptionFa = "قطعی کامل مدار سنسور دمای آب که باعث بالا رفتن پاشش سوخت و بد استارت خوردن موتور در حالت گرم می‌شود.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("قطعی سیم یا جدا شدن سوکت سنسور دمای آب", "خرابی سنسور"),
            diagnosticStepsFa = listOf("بررسی پین‌های سوکت سنسور آب", "تست ولتاژ مرجع ۵ ولت با مولتی‌متر")
        ),
        DtcLookupEntry(
            code = "P0120",
            titleEn = "Throttle Position Sensor Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور موقعیت دریچه گاز (TPS)",
            descriptionEn = "Throttle / Pedal Position Sensor 'A' circuit malfunction.",
            descriptionFa = "ایراد الکتریکی در مقاومت متغیر زاویه دریچه گاز یا پدال گاز الکترونیکی.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی سنسور دریچه گاز (TPS)", "خوردگی چرخ‌دنده‌های دریچه گاز برقی", "خرابی سنسور پدال گاز"),
            diagnosticStepsFa = listOf("تست خطی بودن افزایش ولتاژ سنسور گاز از ۰.۵ تا ۴.۵ ولت بدون پرش", "شستشوی پولکی دریچه گاز")
        ),
        DtcLookupEntry(
            code = "P0130",
            titleEn = "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1)",
            titleFa = "اشکال در مدار سنسور اکسیژن بالا (قبل از کاتالیزور)",
            descriptionEn = "Upstream Oxygen Sensor circuit malfunction.",
            descriptionFa = "عدم ارسال داده یا اختلال ولتاژ در سنسور اکسیژن اول که منجر به افزایش چشمگیر مصرف سوخت می‌گردد.",
            severity = DtcSeverity.MODERATE,
            likelyCausesFa = listOf("سوختن سنسور اکسیژن اول", "خرابی المنت گرمکن سنسور", "نشتی اگزوز از واشر گلویی منیفولد"),
            diagnosticStepsFa = listOf("بررسی نوسان ولتاژ سنسور بین ۱۰۰ تا ۹۰۰ میلی‌ولت در دور آرام", "بررسی سلامت فیوز گرمکن سنسور")
        ),
        DtcLookupEntry(
            code = "P0133",
            titleEn = "O2 Sensor Circuit Slow Response (Bank 1 Sensor 1)",
            titleFa = "کندی پاسخگویی سنسور اکسیژن بالا",
            descriptionEn = "Upstream Oxygen Sensor circuit response time is degraded.",
            descriptionFa = "کندی در تغییرات ولتاژ سنسور اکسیژن ناشی از دوده گرفتن یا فرسودگی سنسور.",
            severity = DtcSeverity.MODERATE,
            likelyCausesFa = listOf("کهنگی یا کربن گرفتگی سنسور اکسیژن", "بنزین بی‌کیفیت یا اکتان نامناسب", "افت فشار پمپ بنزین"),
            diagnosticStepsFa = listOf("تست سرعت سوئیچینگ سنسور با اسکوپ یا داده لایو دیاگ", "بررسی شمع‌ها از نظر دوده زدن")
        ),
        DtcLookupEntry(
            code = "P0135",
            titleEn = "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 1)",
            titleFa = "اشکال در مدار گرمکن سنسور اکسیژن بالا",
            descriptionEn = "Upstream Oxygen Sensor heater element or circuit fault.",
            descriptionFa = "المنت حرارتی سنسور اکسیژن کار نمی‌کند و سنسور دیر به دمای کاری ۳۰۰ درجه می‌رسد.",
            severity = DtcSeverity.INFORMATIONAL,
            likelyCausesFa = listOf("سوختن فیوز مدار گرمکن", "سوختن المنت داخلی سنسور اکسیژن", "اکسیداسیون پین‌های سوکت"),
            diagnosticStepsFa = listOf("اندازه‌گیری مقاومت اهمی پایه‌های گرمکن سنسور (سفید-سفید، معمولا ۴ تا ۱۵ اهم)", "تست ولتاژ ۱۲ ولت سر سوکت گرمکن")
        ),
        DtcLookupEntry(
            code = "P0171",
            titleEn = "System Too Lean (Bank 1)",
            titleFa = "مخلوط سوخت و هوا بیش از حد رقیق است (فقر سوخت)",
            descriptionEn = "Fuel trim system has reached maximum enrichment limit (too much air or not enough fuel).",
            descriptionFa = "کامپیوتر خودرو پاشش بنزین را افزایش داده اما همچنان اکسیژن اضافی در اگزوز وجود دارد.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("نشتی هوا از شلنگ‌های وکیوم یا اورینگ منیفولد", "افت فشار پمپ بنزین یا گرفتگی فیلتر بنزین", "گرفتگی سوزن انژکتورها"),
            diagnosticStepsFa = listOf("تست فشار ریل سوخت با مانومتر (باید حدود ۳.۵ بار باشد)", "تست دود برای یافتن نشتی هوای منیفولد")
        ),
        DtcLookupEntry(
            code = "P0172",
            titleEn = "System Too Rich (Bank 1)",
            titleFa = "مخلوط سوخت و هوا بیش از حد غلیظ است (مصرف بالای سوخت)",
            descriptionEn = "Fuel trim system has reached maximum lean limit (too much fuel or not enough air).",
            descriptionFa = "پاشش سوخت بیش از حد یا کمبود هوا در سیلندر که باعث دود سیاه و بوی خام‌سوزی می‌شود.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("چکه کردن سوزن انژکتور", "خرابی رگولاتور فشار سوخت", "کثیفی شدید فیلتر هوا", "خرابی سنسور اکسیژن"),
            diagnosticStepsFa = listOf("بررسی نشتی انژکتورها روی دستگاه تست سوزن", "بررسی سلامت فیلتر هوای ورودی")
        ),
        DtcLookupEntry(
            code = "P0300",
            titleEn = "Random / Multiple Cylinder Misfire Detected",
            titleFa = "احتراق ناقص تصادفی یا چندگانه در سیلندرها (Misfire)",
            descriptionEn = "Random or multiple cylinder misfire detected by crankshaft speed variations.",
            descriptionFa = "احتراق ناقص که موجب لرزش شدید موتور و چشمک زدن چراغ چک می‌شود.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("فرسودگی شمع‌ها", "خرابی کوئل دوبل یا تکی", "افت فشار بنزین", "کمپرس پایین سیلندرها (واشر سرسیلندر)"),
            diagnosticStepsFa = listOf("تست کمپرس موتور با گیج کمپرس‌سنج", "بررسی فاصله دهانه و سلامت شمع‌ها و وایرها")
        ),
        DtcLookupEntry(
            code = "P0301",
            titleEn = "Cylinder 1 Misfire Detected",
            titleFa = "احتراق ناقص در سیلندر شماره ۱",
            descriptionEn = "Misfire detected in cylinder number 1.",
            descriptionFa = "اشکال در جرقه، سوخت یا کمپرس سیلندر شماره یک.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی شمع یا وایر سیلندر ۱", "خرابی کوئل", "گرفتگی یا قطعی سوزن انژکتور سیلندر ۱"),
            diagnosticStepsFa = listOf("جابجا کردن شمع سیلندر ۱ با ۲ و بررسی انتقال خطا", "تست مقاومت وایر سیلندر ۱")
        ),
        DtcLookupEntry(
            code = "P0335",
            titleEn = "Crankshaft Position Sensor Circuit Malfunction",
            titleFa = "اشکال در مدار سنسور موقعیت میل‌لنگ (سنسور دور موتور)",
            descriptionEn = "Crankshaft position sensor (CKP) circuit malfunction.",
            descriptionFa = "خرابی سنسور دور موتور که اصلی‌ترین سنسور روشن شدن و زمان‌بندی جرقه خودرو است.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("سوختن سنسور دور موتور", "چسبیدن براده آهن به نوک سنسور", "شکستگی دندانه‌های چرخ لنگر/فلایویل", "قطعی سیم به ECU"),
            diagnosticStepsFa = listOf("تمیز کردن نوک مغناطیسی سنسور دور موتور", "اندازه‌گیری مقاومت اهمی سنسور (معمولا بین ۳۰۰ تا ۹۰۰ اهم)", "بررسی فاصله هوایی سنسور تا فلایویل")
        ),
        DtcLookupEntry(
            code = "P0500",
            titleEn = "Vehicle Speed Sensor Malfunction",
            titleFa = "اشکال در عملکرد سنسور سرعت خودرو (سنسور کیلومتر)",
            descriptionEn = "Vehicle speed sensor (VSS) signal missing or implausible.",
            descriptionFa = "سنسور کیلومتر سرعت را صفر نشان می‌دهد و ممکن است خودرو هنگام ترمز خاموش شود.",
            severity = DtcSeverity.MODERATE,
            likelyCausesFa = listOf("خرابی سنسور کیلومتر روی گیربکس", "ساییده شدن چرخ‌دنده پلاستیکی کیلومتر", "قطعی سوکت یا سیم به آمپر"),
            diagnosticStepsFa = listOf("بررسی چرخش پینیون کیلومتر با دست و مشاهده حرکت عقربه", "تست ولتاژ ورودی و پالس خروجی با اسپیدومتر")
        ),
        DtcLookupEntry(
            code = "P0562",
            titleEn = "System Voltage Low",
            titleFa = "ولتاژ سیستم برق خودرو پایین است (زیر ۱۰.۵ ولت)",
            descriptionEn = "Main system voltage supplied to ECU is below threshold.",
            descriptionFa = "دینام شارژ نمی‌کند یا باتری فرسوده است؛ خطر خاموش شدن ناگهانی خودرو وجود دارد.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("عدم شارژ دینام", "برق‌دزدی شدید خودرو در حالت خاموش", "شل بودن یا بریدگی تسمه دینام"),
            diagnosticStepsFa = listOf("تست ولتاژ دوسر باتری در حالت روشن بودن موتور (باید بین ۱۳.۸ تا ۱۴.۴ ولت باشد)", "بررسی رگولاتور دینام")
        ),
        DtcLookupEntry(
            code = "P0601",
            titleEn = "Internal Control Module Memory Checksum Error",
            titleFa = "خطای چک‌سام حافظه داخلی کامپیوتر موتور (ECU)",
            descriptionEn = "Internal ROM checksum error in Engine Control Module.",
            descriptionFa = "خطای عدم تطابق حافظه فلش ECU معمولا ناشی از ریمپ غیر اصولی یا خرابی تراشه حافظه.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی یا به‌هم‌ریختگی حافظه فلش ECU", "ریمپ یا فلش ناموفق توسط تیونر", "خرابی سخت‌افزاری پردازنده ECU"),
            diagnosticStepsFa = listOf("فلش مجدد نرم‌افزار استاندارد کارخانه روی ECU با دستگاه پروگرامر", "تست سلامت تغذیه حافظه فلش")
        ),
        DtcLookupEntry(
            code = "U0100",
            titleEn = "Lost Communication With ECM/PCM 'A'",
            titleFa = "قطع ارتباط شبکه با کامپیوتر موتور (ECU)",
            descriptionEn = "Network communication with Engine Control Module lost.",
            descriptionFa = "ارتباط شبکه CAN یا ون بین یونیت موتور و سایر ماژول‌ها قطع شده و استارت نمی‌خورد.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی رله دوبل (رله اصلی انژکتور)", "سوختن فیوز اصلی ECU", "قطعی سیم ارتباطی شبکه به پشت آمپر و BSI/CCN"),
            diagnosticStepsFa = listOf("بررسی صدای پمپ بنزین و تقه رله دوبل هنگام باز کردن سوئیچ", "تست مقاومت خط CAN (حدود ۶۰ اهم بین پایه‌های ۶ و ۱۴ پورت OBD)")
        ),
        DtcLookupEntry(
            code = "P1600",
            titleEn = "ECU Power Latch / Relay Delay Relay Fault (Double Relay)",
            titleFa = "خطای پاورلچ ECU / رله دوبل انژکتور",
            descriptionEn = "ECU power latch relay circuit malfunction.",
            descriptionFa = "ایراد در تغذیه رله دوبل بسیار شایع در پژو ۴۰۵، پارس و پراید که باعث خاموشی ناگهانی می‌شود.",
            severity = DtcSeverity.CRITICAL,
            likelyCausesFa = listOf("خرابی رله دوبل", "خرابی پایه برق مداوم سوئیچ", "ضعف اتصال منفی باتری"),
            diagnosticStepsFa = listOf("تعویض رله دوبل با نمونه اورجینال", "بررسی پین‌های سوکت رله دوبل از نظر ذوب‌شدگی")
        )
    ).associateBy { it.code }

    /**
     * Looks up an entry by DTC string (e.g. "P0133" or "P0133-11").
     */
    fun find(code: String): DtcLookupEntry? {
        val cleanCode = code.trim().uppercase().substringBefore("-")
        return database[cleanCode]
    }

    /**
     * Search codes matching a query in English or Persian.
     */
    fun search(query: String): List<DtcLookupEntry> {
        val q = query.trim().lowercase()
        return database.values.filter {
            it.code.lowercase().contains(q) ||
            it.titleEn.lowercase().contains(q) ||
            it.titleFa.lowercase().contains(q) ||
            it.descriptionEn.lowercase().contains(q) ||
            it.descriptionFa.lowercase().contains(q)
        }
    }
}
