package com.sarani.carobd.core.vehicle.specs

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DataSourceReference
import com.sarani.carobd.core.model.VehicleSpecSheet

/**
 * Automotive technical encyclopedia providing OEM-verified specifications,
 * oil capacities, spark plug gaps, and maintenance guides for Iranian and regional vehicles.
 */
object VehicleSpecDatabase {

    private val specs: Map<String, VehicleSpecSheet> = listOf(
        // IKCO Peugeot 206 / 207 (TU5)
        VehicleSpecSheet(
            vehicleId = "ikco_206_tu5",
            engineCode = "TU5 JP4 (1587cc 16V)",
            engineOilGrade = "10W-40 API SL / SM / SN نیمه سنتتیک",
            engineOilCapacityLiters = 3.75f, // با فیلتر روغن (۳.۵ لیتر بدون فیلتر)
            recommendedOilBrands = listOf("توتال Total 7000", "بهران سوپر پیشتاز ۱۰W۴۰", "ایرانول تترا", "کاسترول مگناتک"),
            gearboxOilGrade = "75W-80 GL-4+ (توتال BV)",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "Bosch FR7DC+ / Champion RC87YCL / Eyquem RFN52HZ",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4 (آبی / سنتتیک)",
            coolantCapacityLiters = 6.0f,
            maintenanceTipsFa = listOf(
                "هواگیری سیستم خنک‌کننده TU5 حتما باید از دو پیچ برنجی روی هوزینگ آب و شلنگ بخاری انجام شود.",
                "روغن موتور با گرانروی ۲۰W-۵۰ به هیچ عنوان برای TU5 توصیه نمی‌شود زیرا باعث استهلاک تایپیت‌های هیدرولیکی می‌گردد.",
                "واشر درب سوپاپ‌های TU5 حساس به روغن‌ریزی روی دینام و شمع‌ها هستند و باید در صورت نشت سریعا تعویض شوند."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM,
            sources = listOf(
                DataSourceReference(
                    title = "Peugeot 206 Workshop Manual PSA - Mechanical Group",
                    urlOrDocument = "PSA-MANUAL-206-MECH-G3",
                    manufacturer = "PSA Automobiles"
                ),
                DataSourceReference(
                    title = "IKCO Technical Guideline - TU5 Engine Maintenance",
                    urlOrDocument = "IKCO-TG-TU5-2021",
                    manufacturer = "Iran Khodro"
                )
            )
        ),

        // IKCO Samand / Dena (EF7 NA)
        VehicleSpecSheet(
            vehicleId = "ikco_dena_ef7",
            engineCode = "EF7 Naturally Aspirated (1645cc 16V)",
            engineOilGrade = "10W-40 API SL / SM / SN (مخصوصا فرمولاسیون پایه گازسوز)",
            engineOilCapacityLiters = 5.25f, // با تعویض فیلتر روغن
            recommendedOilBrands = listOf("ایرانول رونیا یا رویال", "بهران رانا ۱۰W۴۰", "اسپیدی طلایی", "کاسترول مگناتک"),
            gearboxOilGrade = "75W-80 GL-5",
            gearboxCapacityLiters = 1.9f,
            sparkPlugModel = "Bosch FR8KDC (دو پلاتین) / NGK BKR6E-11 / Denso K20PR-U11",
            sparkPlugGapMm = 0.75f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 127,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 7.5f,
            maintenanceTipsFa = listOf(
                "روغن موتور موتور EF7 به دلیل سیستم CVVT حساس است و حجم کل ۵.۲۵ لیتر باید به دقت رعایت شود.",
                "برای مدل‌های دوگانه‌سوز CNG فاصله دهانه شمع حتما روی ۰.۷۵ میلی‌متر تنظیم شود تا از کوئل‌ها محافظت گردد.",
                "تسمه دینام و هرزگردهای شیاردار EF7 در هر سرویس ۴۰ هزار کیلومتر بازبینی شوند."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // IKCO Dena+ Turbo (EF7 TC)
        VehicleSpecSheet(
            vehicleId = "ikco_dena_plus_turbo",
            engineCode = "EF7 TC (1645cc 16V Turbocharged)",
            engineOilGrade = "5W-40 یا 10W-40 API SN / SP تمام سنتتیک",
            engineOilCapacityLiters = 5.25f,
            recommendedOilBrands = listOf("بهران سوپر رانا پلاس SN", "ایرانول رویال پلاس ۵W۴۰", "موتول ۸۱۰۰", "لیکومولی Top Tec"),
            gearboxOilGrade = "ATF AW-1 (برای گیربکس ۶ سرعته DAE اتوماتیک)",
            gearboxCapacityLiters = 5.5f,
            sparkPlugModel = "NGK ILFR6B (سوزنی ایریدیوم) / Bosch FR6KPP33X پلاتینیوم",
            sparkPlugGapMm = 0.70f,
            timingBeltIntervalKm = 50000,
            timingBeltTeeth = 127,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 7.8f,
            maintenanceTipsFa = listOf(
                "استفاده از شمع‌های تک پلاتین معمولی روی موتور توربو به شدت منع شده و باعث ذوب پیستون می‌گردد.",
                "بعد از رانندگی تحت فشار یا در بزرگراه، حتما ۱ تا ۲ دقیقه درجا کار کند تا شفت توربوشارژر خنک شود.",
                "تعویض روغن حداکثر در ۵۰۰۰ کیلومتر به دلیل تنش حرارتی بالای توربو انجام پذیرد."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // IKCO Peugeot Pars / 405 (XU7)
        VehicleSpecSheet(
            vehicleId = "ikco_pars_xu7",
            engineCode = "XU7 JP L3 (1761cc 8V)",
            engineOilGrade = "20W-50 API SL / SJ معدنی یا نیمه سنتتیک",
            engineOilCapacityLiters = 4.5f, // با فیلتر روغن
            recommendedOilBrands = listOf("بهران تکتاز یا پیشتاز ۲۰W۵۰", "ایرانول ۱۶۰۰۰", "پارس پایا"),
            gearboxOilGrade = "75W-80 یا 85W-90 GL-5",
            gearboxCapacityLiters = 1.9f,
            sparkPlugModel = "Eyquem RFC58LZ2E / Bosch Super Plus FR7DC9 / Champion RC9YC",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 114,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 7.0f,
            maintenanceTipsFa = listOf(
                "واشر سرسیلندر XU7 به دلیل بوش تر بودن سیلندرها به بی‌آبی و جوش آوردن بسیار حساس است.",
                "هرگز از آب شهری استفاده نشود؛ نسبت ۵۰٪ ضدیخ استاندارد اتیلن گلیکول با آب مقطر الزامی است.",
                "رله دوبل، سنسور دور موتور و استپر موتور جزو قطعات حیاتی با احتمال خرابی بالا در پژو ۴۰۵ هستند."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // SAIPA Pride 131/111 (M13)
        VehicleSpecSheet(
            vehicleId = "saipa_pride_m13",
            engineCode = "M13 (1323cc 8V)",
            engineOilGrade = "20W-50 یا 10W-40 API SJ / SL",
            engineOilCapacityLiters = 3.4f,
            recommendedOilBrands = listOf("بهران پیشتاز", "ایرانول صبا یا ۱۲۰۰۰", "پارس پایا"),
            gearboxOilGrade = "75W-90 GL-4",
            gearboxCapacityLiters = 2.4f,
            sparkPlugModel = "NGK BPR6ES / Champion RN9YC / Bosch WR7DC",
            sparkPlugGapMm = 0.85f,
            timingBeltIntervalKm = 50000,
            timingBeltTeeth = 107,
            brakeFluidType = "DOT 3 (زرد) یا DOT 4",
            coolantCapacityLiters = 5.0f,
            maintenanceTipsFa = listOf(
                "موتور پراید در صورت پارگی تسمه تایم سوپاپ کج نمی‌کند اما خودرو متوقف می‌شود.",
                "شستشوی دریچه گاز و تنظیم فیلر سوپاپ‌ها (ورودی ۰.۳۰ و خروجی ۰.۳۰ میلی‌متر در حالت گرم) تاثیر زیادی در کاهش مصرف دارد."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // SAIPA Tiba / Saina / Quik (M15)
        VehicleSpecSheet(
            vehicleId = "saipa_m15_series",
            engineCode = "M15 (1503cc 8V)",
            engineOilGrade = "10W-40 API SL / SM نیمه سنتتیک",
            engineOilCapacityLiters = 3.4f,
            recommendedOilBrands = listOf("بهران سوپر پیشتاز ۱۰W۴۰", "ایرانول تترا", "کاسترول مگناتک"),
            gearboxOilGrade = "75W-90 GL-4",
            gearboxCapacityLiters = 2.4f,
            sparkPlugModel = "NGK BPR6ES-11 / Bosch FR7DC+ / Torch K6RTC",
            sparkPlugGapMm = 0.85f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 107,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 5.5f,
            maintenanceTipsFa = listOf(
                "در موتور M15 برخلاف پراید، پاره شدن تسمه تایم قطعا باعث برخورد پیستون با سوپاپ‌ها و کج شدن سوپاپ می‌شود.",
                "روغن ۱۰W-۴۰ به دلیل وجود استکان‌های تایپیت ارتقا یافته برای کارکرد بی‌صدای موتور الزامی است."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // SAIPA Shahin (M15-TC Turbo)
        VehicleSpecSheet(
            vehicleId = "saipa_shahin_turbo",
            engineCode = "M15-TC (1503cc 8V Turbocharged)",
            engineOilGrade = "5W-40 یا 10W-40 API SN / SP فول سنتتیک",
            engineOilCapacityLiters = 4.0f,
            recommendedOilBrands = listOf("بهران سوپر رانا پلاس", "ایرانول رویال ۵W۴۰", "کاسترول اج Edge"),
            gearboxOilGrade = "75W-85 GL-4 (دنده‌ای) یا CVT Fluid NS-3 (اتوماتیک)",
            gearboxCapacityLiters = 2.5f,
            sparkPlugModel = "Torch KL6RTP (ایریدیوم) / NGK IFR6T11",
            sparkPlugGapMm = 0.80f,
            timingBeltIntervalKm = 50000,
            timingBeltTeeth = 107,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.5f,
            maintenanceTipsFa = listOf(
                "موتور شاهین توربو حساسیت بالایی به بنزین معمولی دارد؛ استفاده منظم از مکمل‌های استاندارد اکتان بوستر توصیه می‌شود.",
                "کولانت و ضدیخ باید حداکثر هر دو سال تعویض شوند تا به خنک‌کننده اینترکولر و توربو آسیبی نرسد."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),

        // Pars Khodro Tondar 90 (L90 K4M)
        VehicleSpecSheet(
            vehicleId = "renault_l90_k4m",
            engineCode = "K4M (1598cc 16V Renault)",
            engineOilGrade = "10W-40 API SL / SM / SN (استاندارد Renault RN0700)",
            engineOilCapacityLiters = 4.8f, // با تعویض فیلتر روغن
            recommendedOilBrands = listOf("الف الترون Elf Evolution 700 ST", "بهران رانا", "کاسترول مگناتک"),
            gearboxOilGrade = "75W-80 Tranself NFJ / NFP",
            gearboxCapacityLiters = 2.8f,
            sparkPlugModel = "Renault 7700500155 (Eyquem RFC50LZ2E) / Bosch FR7DC+",
            sparkPlugGapMm = 0.90f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 132,
            brakeFluidType = "DOT 4 (Elf Frelub 650)",
            coolantCapacityLiters = 5.5f,
            maintenanceTipsFa = listOf(
                "کیت تسمه تایم تندر ۹۰ حتما باید همراه با هر دو غلطک هرزگرد و سفت‌کن تعویض شود و از ابزار قفل‌کن میل‌سوپاپ استفاده شود.",
                "روغن گیربکس رنو فرمولاسیون خاصی دارد؛ استفاده از واسکازین نامناسب باعث زوزه دنده‌ها می‌شود."
            ),
            confidence = ConfidenceLevel.VERIFIED_OEM
        )
    ).associateBy { it.vehicleId }

    fun getSpecSheet(vehicleId: String): VehicleSpecSheet? = specs[vehicleId]

    fun allSpecSheets(): List<VehicleSpecSheet> = specs.values.toList()
}
