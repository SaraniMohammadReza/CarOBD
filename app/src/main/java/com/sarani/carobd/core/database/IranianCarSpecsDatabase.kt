package com.sarani.carobd.core.database

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DataSourceReference
import com.sarani.carobd.core.model.VehicleSpecSheet

object IranianCarSpecsDatabase {

    private val SPECS = mapOf(
        "ikco_206_tu5" to VehicleSpecSheet(
            vehicleId = "ikco_206_tu5",
            engineCode = "TU5 JP4 (1.6L 16V - 105 HP)",
            engineOilGrade = "10W-40 SL / SM یا SN نیمه‌سنتتیک",
            engineOilCapacityLiters = 3.25f, // با تعویض فیلتر روغن
            recommendedOilBrands = listOf("توتال ۷۰۰۰ (Total Quartz 7000)", "ایرانول رویال یا رونیا", "بهران سوپر پیشتاز ۱۰W-40", "کاسترول مگناتک"),
            gearboxOilGrade = "75W-80 GL-4 یا GL-4+",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "بوش FR7DE / ان‌جی‌کی BKR6E / ایکوم فرانسه (پایه کوتاه)",
            sparkPlugGapMm = 0.85f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4 (آبی / بدون رطوبت)",
            coolantCapacityLiters = 6.0f,
            maintenanceTipsFa = listOf(
                "تسمه تایم موتور TU5 به شدت حساس است؛ پاره شدن آن بلافاصله منجر به کج شدن سوپاپ‌ها و خسارت سنگین می‌شود. هر ۶۰ هزار کیلومتر حتماً به همراه هرزگردها تعویض گردد.",
                "روغن موتور حتماً با استاندارد 10W-40 ریخته شود؛ روغن‌های 20W-50 باعث به صدا افتادن استکان‌های تایپیت هیدرولیک و خرابی میل‌سوپاپ در سرما می‌شوند.",
                "سیستم خنک‌کننده TU5 نیاز به هواگیری دستی از طریق پیچ برنجی روی محفظه ترموستات و پیچ پلاستیکی لوله بخاری دارد."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "IKCO Technical Documentation - TU5 Maintenance",
                    urlOrDocument = "",
                    manufacturer = "Iran Khodro"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "ikco_dena_ef7" to VehicleSpecSheet(
            vehicleId = "ikco_dena_ef7",
            engineCode = "EF7 / TC (1.7L 16V CVVT - 115/150 HP)",
            engineOilGrade = "10W-40 SL/SM (توربو: 5W-40 یا 10W-40 SN تمام سنتتیک)",
            engineOilCapacityLiters = 5.5f, // موتور ملی کارتل بزرگی دارد
            recommendedOilBrands = listOf("کاسترول مگناتک 10W-40", "توتال 7000", "بهران سوپر رانا 5W-40", "اسپیدی طلایی پلاتینیوم"),
            gearboxOilGrade = "75W-80 GL-4 (گیربکس BE3 / BE4)",
            gearboxCapacityLiters = 1.9f,
            sparkPlugModel = "بوش FR8DE+ / ان‌جی‌کی سوزنی دوبل پلاتینوم (برای گازسوز و توربو)",
            sparkPlugGapMm = 0.75f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 127,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 7.5f,
            maintenanceTipsFa = listOf(
                "حجم روغن موتور EF7 با فیلتر دقیقاً ۵.۵ لیتر است. ریختن ۴ لیتر (اشتباه رایج اتوسرویس‌ها) باعث افت فشار روغن و سوختن شیر برقی CVVT و یاتاقان زدن می‌شود.",
                "شیر برقی CVVT (تایمینگ متغیر سوپاپ) به تمیزی روغن به شدت وابسته است؛ از فیلتر روغن اصلی پایه بلند سرکج استفاده کنید.",
                "در مدل‌های دوگانه‌سوز گازسوز و توربو، شمع حتماً فیلر ۰.۷۵ میلی‌متر با مقاومت حرارتی بالا باشد تا کپ و میس‌فایر رخ ندهد."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "IPCO / IKCO EF7 Technical Service Manual",
                    urlOrDocument = "",
                    manufacturer = "IPCO"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "ikco_pars_xu7" to VehicleSpecSheet(
            vehicleId = "ikco_pars_xu7",
            engineCode = "XU7 L3 / XU7P (1.8L 8V - 97 HP)",
            engineOilGrade = "20W-50 SL یا SJ (مدل‌های XU7P جدید: 10W-40)",
            engineOilCapacityLiters = 4.5f,
            recommendedOilBrands = listOf("بهران پیشتاز 20W-50", "ایرانول 16000", "پارس پایا 20W-50"),
            gearboxOilGrade = "75W-80 یا 80W-90",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "چمپیون RC9YC / ان‌جی‌کی BKR6E / بوش سوپر پلاس",
            sparkPlugGapMm = 0.8f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 114,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 7.0f,
            maintenanceTipsFa = listOf(
                "موتور XU7 شدیداً به هوا گرفتن سیستم خنک‌کننده و سوختن واشر سرسیلندر حساس است. سطح ضدیخ باید هفتگی چک شود.",
                "تعویض به موقع درب رادیاتور و ترموستات ۷۵ یا ۸۲ درجه برای جلوگیری از داغ کردن موتور ضروری است.",
                "تسمه تایم در ۶۰ هزار کیلومتر یا ۳ سال حتماً تعویض گردد."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "IKCO Technical Documentation - XU7 Maintenance",
                    urlOrDocument = "",
                    manufacturer = "Iran Khodro"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "ikco_tara" to VehicleSpecSheet(
            vehicleId = "ikco_tara",
            engineCode = "TU5P (1.6L 16V CVVT - 113 HP)",
            engineOilGrade = "10W-40 یا 5W-40 API SN",
            engineOilCapacityLiters = 3.8f,
            recommendedOilBrands = listOf("توتال 9000", "بهران رانا SN", "ایرانول رونیا پلاس", "کاسترول GTX"),
            gearboxOilGrade = "75W-80 GL-4+ (دستی) یا ATF AW-1 (اتوماتیک ۶ سرعته)",
            gearboxCapacityLiters = 1.9f,
            sparkPlugModel = "بوش FR8SE0 / سوزنی پایه بلند",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.2f,
            maintenanceTipsFa = listOf(
                "موتور TU5P مجهز به پمپ روغن متغیر و CVVT است؛ استفاده از روغن با استاندارد پایین باعث آسیب به تایپیت‌ها و ارور تایمینگ می‌شود.",
                "در نسخه اتوماتیک (گیربکس DAE)، روغن گیربکس در ۶۰ هزار کیلومتر با دستگاه ساکشن تعویض گردد."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "IKCO Tara Workshop Technical Documentation",
                    urlOrDocument = "",
                    manufacturer = "Iran Khodro"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "saipa_pride_tiba" to VehicleSpecSheet(
            vehicleId = "saipa_pride_tiba",
            engineCode = "M13 / M15 (1.3L / 1.5L 8V - 71/87 HP)",
            engineOilGrade = "10W-40 یا 20W-50 API SJ / SL",
            engineOilCapacityLiters = 3.4f,
            recommendedOilBrands = listOf("بهران رخش یا پیشتاز", "ایرانول 12000 یا صبا", "پارس پایا", "اسپیدی طلایی"),
            gearboxOilGrade = "75W-80 یا 75W-90 GL-4",
            gearboxCapacityLiters = 2.4f,
            sparkPlugModel = "ان‌جی‌کی BPR6ES / بوش WR7DC / رنو مکزیک",
            sparkPlugGapMm = 0.8f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 107, // سایپا تیبا / پراید انژکتور
            brakeFluidType = "DOT 3 یا DOT 4 (زرد یا آبی)",
            coolantCapacityLiters = 5.0f,
            maintenanceTipsFa = listOf(
                "در موتور M13 قدیمی پراید پاره شدن تسمه تایم سوپاپ کج نمی‌کند اما در موتور M15 تیبا/ساینا/کوییک به دلیل نسبت تراکم بالاتر پاره شدن تسمه باعث برخورد سوپاپ با پیستون می‌شود.",
                "فیلر سوپاپ‌ها هر ۴۰ هزار کیلومتر چک و تنظیم شود (موتور هیدرولیک نیست و نیاز به شمع و فیلرگیری منظم دارد)."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "Mega Motor M15 Technical Service Manual",
                    urlOrDocument = "",
                    manufacturer = "Mega Motor"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "saipa_shahin" to VehicleSpecSheet(
            vehicleId = "saipa_shahin",
            engineCode = "M15-TC Turbocharged (1.5L 8V Turbo - 110 HP)",
            engineOilGrade = "10W-40 یا 5W-40 API SN / SP تمام سنتتیک",
            engineOilCapacityLiters = 4.0f,
            recommendedOilBrands = listOf("بهران سوپر رانا 5W-40", "ایرانول رونیا SN", "کاسترول مگناتک استاپ-استارت", "توتال 9000"),
            gearboxOilGrade = "75W-80 GL-4 (گیربکس 5F18)",
            gearboxCapacityLiters = 2.2f,
            sparkPlugModel = "ایریدیوم توربوشارژ مقاومت حرارتی سرد (فیلر ۰.۷ میلی‌متر)",
            sparkPlugGapMm = 0.7f,
            timingBeltIntervalKm = 50000,
            timingBeltTeeth = 114,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.5f,
            maintenanceTipsFa = listOf(
                "به دلیل وجود توربوشارژر، بعد از رانندگی پرگاز یا طولانی بلافاصله خودرو را خاموش نکنید؛ ۳۰ ثانیه درجا کار کند تا دمای کارتل توربو پایین بیاید.",
                "روغن موتور حتماً از نوع SN توربو باشد؛ روغن‌های معدنی باعث سوختن شفت توربوشارژر می‌شوند."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "Mega Motor M15TC Service Sheet",
                    urlOrDocument = "",
                    manufacturer = "Mega Motor"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "ikco_206_tu3_old" to VehicleSpecSheet(
            vehicleId = "ikco_206_tu3_old",
            engineCode = "TU3 JP (1360cc 8V - 75 HP)",
            engineOilGrade = "20W-50 یا 10W-40 API SL",
            engineOilCapacityLiters = 3.25f,
            recommendedOilBrands = listOf("بهران پیشتاز", "توتال 5000 یا 7000", "ایرانول 16000"),
            gearboxOilGrade = "75W-80 GL-4+",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "ایکوم RFC58LZ / بوش FR7DC / چمپیون",
            sparkPlugGapMm = 0.8f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 108,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 5.8f,
            maintenanceTipsFa = listOf(
                "سوختن واشر سرسیلندر از سمت کارتل روغن (روغن‌ریزی از گوشه دینام) ایراد شایع موتورهای TU3 فرانسوی است.",
                "پروتکل ارتباطی ایسیو ساژم S2000 با باود ریت آهسته کار می‌کند؛ اتصال دیاگ ممکن است چند ثانیه بیشتر طول بکشد.",
                "فیلتر بنزین در زیر باک سمت شاگرد قرار دارد و هر ۱۵ هزار کیلومتر باید تعویض شود."
            ),
            sources = listOf(
                DataSourceReference(
                    title = "PSA TU3 Workshop Documentation",
                    urlOrDocument = "",
                    manufacturer = "PSA Automobiles"
                )
            ),
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
        ),
        "global_generic" to VehicleSpecSheet(
            vehicleId = "global_generic",
            engineCode = "Generic International Engine",
            engineOilGrade = "5W-30 / 10W-40 API SN / SP",
            engineOilCapacityLiters = 4.0f,
            recommendedOilBrands = listOf("Mobil 1", "Castrol Edge", "Shell Helix Ultra", "Liqui Moly"),
            gearboxOilGrade = "Manufacturer Specified ATF / MTF",
            gearboxCapacityLiters = 2.5f,
            sparkPlugModel = "Standard Iridium / Platinum Plug",
            sparkPlugGapMm = 1.0f,
            timingBeltIntervalKm = 80000,
            timingBeltTeeth = 120,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.5f,
            maintenanceTipsFa = listOf(
                "از روغن مطابق گرانروی توصیه شده در دفترچه راهنمای خودرو استفاده فرمایید.",
                "فیلتر هوا و فیلتر روغن در هر سرویس تعویض شوند."
            ),
            sources = emptyList(),
            confidence = ConfidenceLevel.UNKNOWN
        )
    )

    fun getSpecSheet(vehicleId: String): VehicleSpecSheet {
        return SPECS[vehicleId] ?: SPECS["global_generic"]!!
    }
}
