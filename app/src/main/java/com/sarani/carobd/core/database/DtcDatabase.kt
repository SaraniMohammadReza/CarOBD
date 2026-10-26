package com.sarani.carobd.core.database

import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DtcSeverity
import com.sarani.carobd.core.model.DtcSystem

object DtcDatabase {

    private val DICTIONARY = mapOf(
        "P0300" to DiagnosticCode(
            code = "P0300",
            titleEn = "Random/Multiple Cylinder Misfire Detected",
            titleFa = "تشخیص خطای احتراق ناقص تصادفی/چندین سیلندر (میس‌فایر)",
            descriptionEn = "ECU detected irregular crankshaft acceleration indicating cylinder misfiring. Check spark plugs, ignition coils, and fuel injectors.",
            descriptionFa = "ایسیو کاهش شتاب میل‌لنگ ناشی از عدم احتراق در سیلندرها را ثبت کرده است. شمع‌ها، وایر/کوئل دوبل، انژکتورها و تراکم سیلندر را بررسی کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0301" to DiagnosticCode(
            code = "P0301",
            titleEn = "Cylinder 1 Misfire Detected",
            titleFa = "خطای عدم احتراق (میس‌فایر) در سیلندر ۱",
            descriptionEn = "Misfire detected in cylinder 1. Common causes: bad spark plug, faulty coil pack, clogged injector.",
            descriptionFa = "احتراق ناقص در سیلندر شماره ۱ رخ داده است. شمع سیلندر ۱، سوزن انژکتور ۱ و خروجی کوئل را چک کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0302" to DiagnosticCode(
            code = "P0302",
            titleEn = "Cylinder 2 Misfire Detected",
            titleFa = "خطای عدم احتراق (میس‌فایر) در سیلندر ۲",
            descriptionEn = "Misfire detected in cylinder 2.",
            descriptionFa = "عدم احتراق در سیلندر ۲ رخ داده است. کوئل، شمع و سوپاپ‌ها را بررسی کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0303" to DiagnosticCode(
            code = "P0303",
            titleEn = "Cylinder 3 Misfire Detected",
            titleFa = "خطای عدم احتراق (میس‌فایر) در سیلندر ۳",
            descriptionEn = "Misfire detected in cylinder 3.",
            descriptionFa = "عدم احتراق در سیلندر ۳ ثبت شد. شمع و سوزن انژکتور سیلندر ۳ نیاز به بررسی دارند.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0304" to DiagnosticCode(
            code = "P0304",
            titleEn = "Cylinder 4 Misfire Detected",
            titleFa = "خطای عدم احتراق (میس‌فایر) در سیلندر ۴",
            descriptionEn = "Misfire detected in cylinder 4.",
            descriptionFa = "عدم احتراق در سیلندر ۴ ثبت شد. وضعیت شمع و فیش کوئل سیلندر ۴ را تست کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0115" to DiagnosticCode(
            code = "P0115",
            titleEn = "Engine Coolant Temperature Circuit Malfunction",
            titleFa = "ایراد در مدار سنسور دمای مایع خنک‌کننده موتور (فشنگی آب)",
            descriptionEn = "ECU reports invalid voltage reading from ECT sensor. Fan may stay on continuously.",
            descriptionFa = "ولتاژ غیرعادی از سنسور دمای آب (مهره آب سبز/قهوه‌ای) دریافت می‌شود. در این حالت معمولاً فن خنک‌کننده در دور تند یکسره کار می‌کند.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0118" to DiagnosticCode(
            code = "P0118",
            titleEn = "Engine Coolant Temperature Circuit High Input",
            titleFa = "ولتاژ بیش از حد بالای مدار سنسور دمای آب (قطعی مدار)",
            descriptionEn = "Indicates open circuit or disconnected wiring to ECT sensor.",
            descriptionFa = "نشان‌دهنده قطعی سیم‌کشی یا سوکت سنسور دمای آب یا سوختن سنسور است.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0120" to DiagnosticCode(
            code = "P0120",
            titleEn = "Throttle Position Sensor Circuit Malfunction",
            titleFa = "ایراد در مدار سنسور موقعیت دریچه گاز (TPS)",
            descriptionEn = "Inconsistent throttle angle signal. Vehicle may hesitate during acceleration.",
            descriptionFa = "سیگنال نامتعارف از موقعیت پدال یا دریچه گاز ارسال می‌شود؛ همراه با کپ‌کردن و تاخیر در شتاب‌گیری خودرو.",
            severity = DtcSeverity.MODERATE
        ),
        "P0130" to DiagnosticCode(
            code = "P0130",
            titleEn = "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1)",
            titleFa = "ایراد در مدار سنسور اکسیژن بالادست (قبل از کاتالیزور)",
            descriptionEn = "Upstream oxygen sensor voltage is not switching properly. May cause high fuel consumption.",
            descriptionFa = "سنسور اکسیژن اول سیگنال صحیحی ارسال نمی‌کند و خودرو در حالت حلقه باز (Open-loop) کار کرده و مصرف بنزین شدیداً بالا می‌رود.",
            severity = DtcSeverity.MODERATE
        ),
        "P0135" to DiagnosticCode(
            code = "P0135",
            titleEn = "O2 Sensor Heater Circuit (Bank 1 Sensor 1)",
            titleFa = "قطعی یا خرابی مدار گرمکن سنسور اکسیژن اول",
            descriptionEn = "Heater element inside oxygen sensor failed or fuse blown.",
            descriptionFa = "المنت گرم‌کننده داخلی سنسور اکسیژن سوخته یا فیوز مربوط به گرمکن سنسور در جعبه فیوز کالسکه قطع شده است.",
            severity = DtcSeverity.INFORMATIONAL
        ),
        "P0171" to DiagnosticCode(
            code = "P0171",
            titleEn = "System Too Lean (Bank 1)",
            titleFa = "ترکیب سوخت و هوا بیش از حد رقیق است (هوای اضافی)",
            descriptionEn = "Too much air or not enough fuel. Check for vacuum leaks, faulty MAP sensor, low fuel pump pressure.",
            descriptionFa = "هوای ورودی به منیفولد بیش از حد سوخت پاشیده شده است. نشتی وکیوم مانیفولد هوا، گرفتگی فیلتر بنزین یا افت فشار پمپ بنزین را بررسی کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0172" to DiagnosticCode(
            code = "P0172",
            titleEn = "System Too Rich (Bank 1)",
            titleFa = "ترکیب سوخت و هوا بیش از حد غنی است (سوخت اضافی/خام‌سوزی)",
            descriptionEn = "Too much fuel entering engine. Check dripping injectors, high fuel pressure, dirty air filter.",
            descriptionFa = "سوخت بیش از حد تزریق شده و بوی خام‌سوزی استشمام می‌شود. چکه سوزن انژکتور، خرابی رگلاتور سوخت یا سنسور MAP مشکوک هستند.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0420" to DiagnosticCode(
            code = "P0420",
            titleEn = "Catalyst System Efficiency Below Threshold (Bank 1)",
            titleFa = "کاهش راندمان کاتالیزور به کمتر از حد مجاز",
            descriptionEn = "Catalytic converter degraded or failing to reduce emissions properly.",
            descriptionFa = "کاتالیزور اگزوز کارایی خود را از دست داده، فرسوده شده یا تخلیه شده است (روشن شدن چراغ چک در سرعت بالا).",
            severity = DtcSeverity.MODERATE
        ),
        "P0500" to DiagnosticCode(
            code = "P0500",
            titleEn = "Vehicle Speed Sensor (VSS) Malfunction",
            titleFa = "ایراد در مدار سنسور سرعت خودرو (کیلومترشمار)",
            descriptionEn = "Speedometer not reading or jumping. Check speed sensor gear on gearbox.",
            descriptionFa = "عقربه کیلومتر کار نمی‌کند یا خودرو هنگام ترمز شدید یا کلاچ گرفتن خاموش می‌شود. سنسور کیلومتر روی پوسته گیربکس را تعویض یا سرویس کنید.",
            severity = DtcSeverity.MODERATE
        ),
        "P0505" to DiagnosticCode(
            code = "P0505",
            titleEn = "Idle Air Control System Malfunction (Stepper Motor)",
            titleFa = "ایراد در سیستم تنظیم هوای دور آرام (استپر موتور / دریچه برقی)",
            descriptionEn = "Common issue on Iranian cars with Siemens & S2000 ECUs. Idle RPM fluctuates or engine stalls.",
            descriptionFa = "خطای بسیار شایع در پژو ۴۰۵، پارس و پراید. نوسان دور آرام، گاز هرز خوردن یا خاموش شدن در حالت درجا. استپر موتور نیاز به شستشو یا تعویض دارد.",
            severity = DtcSeverity.MODERATE
        ),
        "P0335" to DiagnosticCode(
            code = "P0335",
            titleEn = "Crankshaft Position Sensor 'A' Circuit Malfunction",
            titleFa = "ایراد در مدار سنسور موقعیت میل‌لنگ (سنسور دور موتور)",
            descriptionEn = "Engine will crank but not start, or stall intermittently. Check sensor tip for metal debris.",
            descriptionFa = "خودرو استارت می‌خورد اما روشن نمی‌شود یا ناگهان ریپ زده و خاموش می‌شود. نوک سنسور دور موتور را از براده آهن تمیز کرده یا سنسور را تعویض کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0340" to DiagnosticCode(
            code = "P0340",
            titleEn = "Camshaft Position Sensor Circuit Malfunction",
            titleFa = "ایراد در مدار سنسور موقعیت میل‌سوپاپ (میل‌بادامک)",
            descriptionEn = "Camshaft timing sensor error. Common on EF7 CVVT and TU5 engines.",
            descriptionFa = "خطای همزمانی تایم میل‌سوپاپ به خصوص در موتورهای TU5 و EF7 دارای سیستم زمان‌بندی متغیر CVVT.",
            severity = DtcSeverity.MODERATE
        ),
        "P0105" to DiagnosticCode(
            code = "P0105",
            titleEn = "Manifold Absolute Pressure (MAP) Sensor Circuit",
            titleFa = "ایراد در مدار سنسور فشار مطلق منیفولد هوا (سنسور مپ)",
            descriptionEn = "MAP sensor signal out of range. Check O-ring seal and manifold vacuum.",
            descriptionFa = "سنسور مپ مکش هوا را به درستی گزارش نمی‌کند. اورینگ سنسور روی منیفولد هوا یا فیش آن را بررسی کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0562" to DiagnosticCode(
            code = "P0562",
            titleEn = "System Voltage Low",
            titleFa = "افت ولتاژ سیستم برق خودرو (کمتر از ۱۱ ولت)",
            descriptionEn = "Alternator not charging or battery weak. Risk of engine shutdown.",
            descriptionFa = "دینام شارژ نمی‌کند یا تسمه دینام پاره/شل شده و باتری رو به تخلیه است. آفتامات دینام را چک کنید.",
            severity = DtcSeverity.CRITICAL
        ),
        "P0563" to DiagnosticCode(
            code = "P0563",
            titleEn = "System Voltage High",
            titleFa = "افزایش خطرناک ولتاژ سیستم برق (بیش از ۱۵ ولت)",
            descriptionEn = "Alternator voltage regulator shorted. Risk of ECU or battery damage.",
            descriptionFa = "آفتامات دینام برق اضافه تولید می‌کند که می‌تواند به ایسیو، رادیوپخش و باتری صدمه بزند.",
            severity = DtcSeverity.CRITICAL
        )
    )

    fun resolveCode(code: String): DiagnosticCode {
        val normalized = code.uppercase().trim()
        DICTIONARY[normalized]?.let { return it }

        // Generic fallback for any other valid code
        val system = when (normalized.firstOrNull()) {
            'C' -> DtcSystem.CHASSIS
            'B' -> DtcSystem.BODY
            'U' -> DtcSystem.NETWORK
            else -> DtcSystem.POWERTRAIN
        }

        return DiagnosticCode(
            code = normalized,
            titleEn = "Diagnostic Trouble Code $normalized",
            titleFa = "کد خطای تشخیصی $normalized",
            descriptionEn = "Manufacturer or SAE trouble code. Refer to vehicle workshop manual for detailed test procedure.",
            descriptionFa = "کد خطای استاندارد یا اختصاصی کارخانه سازنده. لطفاً جهت تست دقیق به راهنمای تعمیرات کارگاهی خودرو مراجعه کنید.",
            severity = DtcSeverity.MODERATE,
            system = system
        )
    }

    fun getAllPredefinedCodes(): List<DiagnosticCode> = DICTIONARY.values.toList()
}
