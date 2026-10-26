package com.sarani.carobd.core.database

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
            )
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
            )
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
                "نقطه ضعف مشهور موتور XU7 سوختن واشر سرسیلندر به دلیل گرفتگی رادیاتور یا خرابی رله‌های فن است. دمای آب نباید از خط وسط (۹۰ درجه) بالاتر برود.",
                "هواگیری رادیاتور از شیر آلنی روی لوله خروجی هوزینگ ترموستات پس از هر بار تعویض آب یا ضد یخ الزامی است.",
                "سنسور استپر موتور و مپ سنسور هر ۲۰ هزار کیلومتر با اسپری انژکتورشور شستشو داده شود."
            )
        ),
        "ikco_tara" to VehicleSpecSheet(
            vehicleId = "ikco_tara",
            engineCode = "TU5 Plus (1.6L 16V CVVT - 113 HP)",
            engineOilGrade = "10W-40 SN یا 5W-40 تمام سنتتیک",
            engineOilCapacityLiters = 3.6f,
            recommendedOilBrands = listOf("بهران سوپر رانا 5W-40", "توتال کوارتز 9000", "ایرانول رونیا پلاس"),
            gearboxOilGrade = "اتوماتیک DAE: روغن AW-1 / گیربکس دستی: 75W-85",
            gearboxCapacityLiters = 2.1f,
            sparkPlugModel = "شمع سوزنی ایریدیوم پایه بلند بوش یا تورچ اصلی",
            sparkPlugGapMm = 0.85f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 134,
            brakeFluidType = "DOT 4 Low Viscosity (مناسب ESC / ESP)",
            coolantCapacityLiters = 6.2f,
            maintenanceTipsFa = listOf(
                "خودروی تارا دارای پمپ روغن متغیر (Variable Displacement Oil Pump) است و استفاده از روغن با استاندارد روز (SN) ضروری است.",
                "در مدل اتوماتیک ۶ سرعته DAE، واسکازین اختصاصی AW-1 بوده و به هیچ وجه نباید از روغن ATF Dexron III قدیمی استفاده کرد.",
                "سیستم ترمز مجهز به سامانه کنترل پایداری ESC بوش است و هواگیری آن حتماً باید با دستگاه دیاگ و پالس پمپ ABS انجام شود."
            )
        ),
        "saipa_pride_tiba" to VehicleSpecSheet(
            vehicleId = "saipa_pride_tiba",
            engineCode = "M13 / M15 (1.3L / 1.5L 8V - 71 / 87 HP)",
            engineOilGrade = "تیبا/ساینا/کوییک: 10W-40 SJ/SL / پراید قدیم: 20W-50",
            engineOilCapacityLiters = 3.4f,
            recommendedOilBrands = listOf("بهران تکتاز / پیشتاز 10W-40", "ایرانول صبا", "پارس پایا", "کاسترول GTX"),
            gearboxOilGrade = "75W-80 یا 75W-90 GL-4",
            gearboxCapacityLiters = 2.4f,
            sparkPlugModel = "ان‌جی‌کی BKR6E-11 / بوش FR7DC (پایه کوتاه فیلر ۱.۱)",
            sparkPlugGapMm = 1.1f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 107,
            brakeFluidType = "DOT 3 (زرد) یا DOT 4",
            coolantCapacityLiters = 5.0f,
            maintenanceTipsFa = listOf(
                "در موتور M15 تیبا و ساینا و کوییک فیلر شمع باید ۱.۱ میلی‌متر باشد؛ شمع‌های فیلر ریز باعث لرزش شدید دور آرام و افت شتاب می‌شوند.",
                "تسمه تایم در موتور قدیمی پراید در صورت پارگی سوپاپ کج نمی‌کرد، اما در موتور یورو ۴ جدید و موتور M15 تیبا پارگی تسمه تایم سوپاپ‌ها را کج می‌کند!",
                "استپر موتور و سنسور دریچه گاز (TPS) در ایسیوهای ساژم و زیمنس حساس هستند؛ فیش استپر را هنگام باز بودن سوییچ جدا نکنید."
            )
        ),
        "saipa_shahin" to VehicleSpecSheet(
            vehicleId = "saipa_shahin",
            engineCode = "M15-TC (1.5L 8V Turbocharger - 110 HP)",
            engineOilGrade = "5W-40 یا 10W-40 با استاندارد SN تمام سنتتیک توربو",
            engineOilCapacityLiters = 3.8f,
            recommendedOilBrands = listOf("بهران سوپر رانا پلاس 5W-40", "کاسترول اج 5W-40", "ایرانول سوپر رویال"),
            gearboxOilGrade = "75W-85 GL-4",
            gearboxCapacityLiters = 2.2f,
            sparkPlugModel = "شمع سرد توربوشارژر سوزنی ایریدیوم بوش FR7DI",
            sparkPlugGapMm = 0.75f,
            timingBeltIntervalKm = 50000,
            timingBeltTeeth = 107,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 6.0f,
            maintenanceTipsFa = listOf(
                "توربوشارژر به شدت به خنک‌کاری و روان‌کاری متکی است. پس از رانندگی پرشتاب، موتور را بلافاصله خاموش نکنید و اجازه دهید ۱ دقیقه درجا کار کند تا روغن در کارتریج توربو نسوزد.",
                "روغن موتور را به هیچ وجه دیرتر از ۵۰۰۰ کیلومتر تعویض نکنید تا مسیر لوله تغذیه روغن توربو رسوب نگیرد.",
                "استفاده از بنزین سوپر یا مکمل‌های افزاینده اکتان باکیفیت برای جلوگیری از پدیده ناک و شکستن پیستون در موتور توربو ضروری است."
            )
        ),
        "ikco_206_tu3_old" to VehicleSpecSheet(
            vehicleId = "ikco_206_tu3_old",
            engineCode = "TU3 JP (1.4L 8V - 75 HP)",
            engineOilGrade = "10W-40 SL/SM نیمه‌سنتتیک",
            engineOilCapacityLiters = 3.0f,
            recommendedOilBrands = listOf("توتال ۷۰۰۰", "بهران سوپر پیشتاز 10W-40", "ایرانول 12000"),
            gearboxOilGrade = "75W-80 GL-4",
            gearboxCapacityLiters = 2.0f,
            sparkPlugModel = "ایکوم Eyquem RFC58LZ2K / ان‌جی‌کی BKR6E",
            sparkPlugGapMm = 0.9f,
            timingBeltIntervalKm = 60000,
            timingBeltTeeth = 104,
            brakeFluidType = "DOT 4",
            coolantCapacityLiters = 5.8f,
            maintenanceTipsFa = listOf(
                "سوختن واشر سرسیلندر از سمت کارتل روغن (روغن‌ریزی از گوشه دینام) ایراد شایع موتورهای TU3 فرانسوی است.",
                "پروتکل ارتباطی ایسیو ساژم S2000 با باود ریت آهسته کار می‌کند؛ اتصال دیاگ ممکن است چند ثانیه بیشتر طول بکشد.",
                "فیلتر بنزین در زیر باک سمت شاگرد قرار دارد و هر ۱۵ هزار کیلومتر باید تعویض شود."
            )
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
            )
        )
    )

    fun getSpecSheet(vehicleId: String): VehicleSpecSheet {
        return SPECS[vehicleId] ?: SPECS["global_generic"]!!
    }
}
