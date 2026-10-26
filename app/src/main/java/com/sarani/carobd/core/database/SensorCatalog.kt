package com.sarani.carobd.core.database

import com.sarani.carobd.core.model.SensorCategory
import com.sarani.carobd.core.model.SensorDefinition

object SensorCatalog {

    val ALL_SENSORS: List<SensorDefinition> = listOf(
        // ==================== ENGINE & SPEED ====================
        SensorDefinition("rpm", "010C", "Engine RPM", "دور موتور", "RPM", "RPM", "RPM", 0f, 8000f, SensorCategory.ENGINE, "سرعت چرخش میل‌لنگ موتور"),
        SensorDefinition("speed", "010D", "Vehicle Speed", "سرعت خودرو", "SPD", "km/h", "mph", 0f, 260f, SensorCategory.ENGINE, "سرعت حرکت اندازه‌گیری شده از سنسور کیلومتر یا چرخ‌ها"),
        SensorDefinition("engine_load", "0104", "Calculated Engine Load", "بار محاسبه شده موتور", "LOAD", "%", "%", 0f, 100f, SensorCategory.ENGINE, "درصد بار وارده بر اساس مکش هوا و گشتاور"),
        SensorDefinition("throttle_pos", "0111", "Throttle Position", "موقعیت زاویه دریچه گاز", "TPS", "%", "%", 0f, 100f, SensorCategory.ENGINE, "میزان باز بودن پولکی دریچه گاز یا پدال گاز برقی"),
        SensorDefinition("rel_throttle_pos", "0145", "Relative Throttle Position", "زاویه نسبی دریچه گاز", "R-TPS", "%", "%", 0f, 100f, SensorCategory.ENGINE, "زاویه نسبی به دور آرام"),
        SensorDefinition("timing_advance", "010E", "Timing Advance", "آوانس جرقه‌زنی سیلندر ۱", "ADV", "°", "°", -64f, 64f, SensorCategory.ENGINE, "زاویه آوانس ریتارد شمع نسبت به نقطه مرگ بالا"),
        SensorDefinition("abs_engine_load", "0143", "Absolute Engine Load", "بار مطلق موتور", "A-LOAD", "%", "%", 0f, 100f, SensorCategory.ENGINE, "لود مطلق موتور برای کنترل سوخت"),
        SensorDefinition("run_time", "011F", "Engine Run Time", "زمان روشن بودن موتور", "RUN", "ثانیه", "sec", 0f, 36000f, SensorCategory.ENGINE, "مدت زمان سپری شده از لحظه استارت موتور"),
        SensorDefinition("pedal_pos_d", "0149", "Accelerator Pedal Pos D", "موقعیت پدال گاز سنسور ۱", "APP-D", "%", "%", 0f, 100f, SensorCategory.ENGINE, "سیگنال ولتاژ اول پدال گاز الکترونیکی"),
        SensorDefinition("pedal_pos_e", "014A", "Accelerator Pedal Pos E", "موقعیت پدال گاز سنسور ۲", "APP-E", "%", "%", 0f, 100f, SensorCategory.ENGINE, "سیگنال ولتاژ دوم پدال گاز جهت تایید ایمنی"),
        SensorDefinition("actuator_throttle", "014C", "Commanded Throttle Actuator", "فرمان ایسیو به دریچه گاز", "TAC", "%", "%", 0f, 100f, SensorCategory.ENGINE, "درصد بازشدن فرمان داده شده به موتور الکتریکی دریچه گاز"),

        // ==================== FUEL & AIR INTAKE ====================
        SensorDefinition("maf", "0110", "Mass Air Flow (MAF)", "جرم هوای ورودی به موتور", "MAF", "g/s", "lb/min", 0f, 250f, SensorCategory.FUEL_AIR, "حجم هوای عبوری از سنسور ایرفلومتر"),
        SensorDefinition("map", "010B", "Intake Manifold Pressure (MAP)", "فشار مطلق منیفولد هوا (سنسور مپ)", "MAP", "kPa", "psi", 0f, 255f, SensorCategory.FUEL_AIR, "فشار هوای داخل منیفولد (اندازه‌گیری وکیوم در موتورهای اتمسفر و بوست در توربو)"),
        SensorDefinition("stft_b1", "0106", "Short Term Fuel Trim Bank 1", "تنظیم سوخت کوتاه‌مدت (STFT)", "STFT1", "%", "%", -100f, 100f, SensorCategory.FUEL_AIR, "اصلاح لحظه‌ای پاشش انژکتورها بر اساس سنسور اکسیژن"),
        SensorDefinition("ltft_b1", "0107", "Long Term Fuel Trim Bank 1", "تنظیم سوخت بلندمدت (LTFT)", "LTFT1", "%", "%", -100f, 100f, SensorCategory.FUEL_AIR, "حافظه میان‌مدت غلظت سوخت ایسیو برای جبران خطای فرسایش"),
        SensorDefinition("stft_b2", "0108", "Short Term Fuel Trim Bank 2", "تنظیم سوخت کوتاه‌مدت بانک ۲", "STFT2", "%", "%", -100f, 100f, SensorCategory.FUEL_AIR, "اصلاح پاشش سمت شاگرد در موتورهای خورجینی V6/V8"),
        SensorDefinition("ltft_b2", "0109", "Long Term Fuel Trim Bank 2", "تنظیم سوخت بلندمدت بانک ۲", "LTFT2", "%", "%", -100f, 100f, SensorCategory.FUEL_AIR, "تنظیم بلندمدت بانک دوم سیلندرها"),
        SensorDefinition("fuel_pressure", "010A", "Fuel Rail Pressure (Low)", "فشار ریل سوخت باک", "F-PRS", "kPa", "psi", 0f, 765f, SensorCategory.FUEL_AIR, "فشار ارسالی پمپ بنزین داخل باک"),
        SensorDefinition("fuel_rail_direct", "0123", "Fuel Rail Pressure (Direct/GDI)", "فشار ریل تزریق مستقیم GDI", "FRP-D", "MPa", "psi", 0f, 200f, SensorCategory.FUEL_AIR, "فشار بسیار بالای پمپ مکانیکی در موتورهای تزریق مستقیم GDI"),
        SensorDefinition("fuel_level", "012F", "Fuel Tank Level", "سطح سوخت باک بنزین", "FUEL", "%", "%", 0f, 100f, SensorCategory.FUEL_AIR, "درجه شناور باک بنزین خودرو"),
        SensorDefinition("baro_pressure", "0133", "Barometric Pressure", "فشار هوای محیط اتمسفر", "BARO", "kPa", "inHg", 50f, 110f, SensorCategory.FUEL_AIR, "فشار جو در ارتفاعات (تنظیم ارتفاع ایسیو در سربالایی‌ها)"),
        SensorDefinition("fuel_rate", "015E", "Engine Fuel Rate", "نرخ مصرف بنزین در ساعت", "L/h", "L/h", "gal/h", 0f, 50f, SensorCategory.FUEL_AIR, "حجم لیتر بنزین مصرفی خودرو در هر ساعت کارکرد"),
        SensorDefinition("equiv_ratio", "0144", "Fuel-Air Equivalence Ratio (Lambda)", "ضریب نسبت سوخت/هوا (لاندا)", "LAMBDA", "λ", "λ", 0.5f, 2.0f, SensorCategory.FUEL_AIR, "نسبت استوکیومتریک بنزین (۱.۰ = ۱۴.۷ به ۱)"),

        // ==================== TEMPERATURES ====================
        SensorDefinition("coolant_temp", "0105", "Engine Coolant Temp (ECT)", "دمای آب خنک‌کننده موتور", "COOL", "°C", "°F", -40f, 140f, SensorCategory.TEMPERATURE, "دمای مایع خنک‌کننده ارسالی از مهره آب فشنگی"),
        SensorDefinition("intake_temp", "010F", "Intake Air Temp (IAT)", "دمای هوای ورودی منیفولد", "IAT", "°C", "°F", -40f, 100f, SensorCategory.TEMPERATURE, "دمای هوای ورودی از فیلتر هوا"),
        SensorDefinition("ambient_temp", "0146", "Ambient Air Temp", "دمای هوای محیط بیرون", "AAT", "°C", "°F", -40f, 70f, SensorCategory.TEMPERATURE, "دمای هوای خارج از اتاق خودرو"),
        SensorDefinition("engine_oil_temp", "015C", "Engine Oil Temperature", "دمای روغن موتور", "EOT", "°C", "°F", -40f, 160f, SensorCategory.TEMPERATURE, "حرارت روغن داخل کارتل یا فیلتر"),
        SensorDefinition("trans_oil_temp", "01FE", "Transmission Fluid Temp", "دمای واسکازین گیربکس اتوماتیک", "TFT", "°C", "°F", -40f, 150f, SensorCategory.TEMPERATURE, "دمای روغن گیربکس اتوماتیک AL4 / DAE / CVT"),
        SensorDefinition("cat_temp_b1s1", "013C", "Catalyst Temp B1S1", "دمای کاتالیزور بانک ۱ اول", "CAT1", "°C", "°F", 0f, 1000f, SensorCategory.TEMPERATURE, "حرارت هسته سرامیکی کاتالیزور اگزوز"),
        SensorDefinition("cat_temp_b1s2", "013E", "Catalyst Temp B1S2", "دمای کاتالیزور بانک ۱ دوم", "CAT2", "°C", "°F", 0f, 1000f, SensorCategory.TEMPERATURE, "دمای خروجی منبع کاتالیزور"),

        // ==================== ELECTRICAL & BATTERY ====================
        SensorDefinition("module_voltage", "0142", "Control Module Voltage", "ولتاژ باتری / دینام", "VOLT", "V", "V", 8f, 18f, SensorCategory.ELECTRICAL, "ولتاژ برق تغذیه ایسیو (۱۲.۶ ولت خاموش، ۱۴.۲ ولت شارژ دینام)"),
        SensorDefinition("hybrid_battery", "015B", "Hybrid Battery Remaining", "شارژ باتری هیبرید", "HV-BAT", "%", "%", 0f, 100f, SensorCategory.ELECTRICAL, "میزان شارژ پک باتری ولتاژ بالا در خودروهای هیبریدی"),
        SensorDefinition("elm_pin16_volt", "ATRV", "OBD Pin 16 Voltage", "ولتاژ پین ۱۶ دیاگ", "PIN16", "V", "V", 0f, 18f, SensorCategory.ELECTRICAL, "ولتاژ خط مستقیم باتری روی سوکت OBD-II"),

        // ==================== EMISSIONS & EXHAUST ====================
        SensorDefinition("o2_b1s1", "0114", "O2 Sensor Bank 1 Sensor 1", "ولتاژ سنسور اکسیژن اول (بالادست)", "O2-1", "V", "V", 0f, 1.25f, SensorCategory.EMISSIONS, "نوسان ولتاژ بین ۰.۱ (رقیق) تا ۰.۹ (غنی) ولت"),
        SensorDefinition("o2_b1s2", "0115", "O2 Sensor Bank 1 Sensor 2", "ولتاژ سنسور اکسیژن دوم (پایین‌دست)", "O2-2", "V", "V", 0f, 1.25f, SensorCategory.EMISSIONS, "بررسی سلامت تصفیه کاتالیزور (حدود ۰.۴۵ ولت ثابت)"),
        SensorDefinition("o2_b2s1", "0118", "O2 Sensor Bank 2 Sensor 1", "سنسور اکسیژن بانک ۲ اول", "O2-B2S1", "V", "V", 0f, 1.25f, SensorCategory.EMISSIONS, "سنسور اکسیژن اول منیفولد شاگرد"),
        SensorDefinition("evap_purge", "012E", "Commanded Evaporative Purge", "درصد بازشدن شیر کنیستر", "PURGE", "%", "%", 0f, 100f, SensorCategory.EMISSIONS, "تخلیه بخارات بنزین از فیلتر زغالی به مانیفولد"),
        SensorDefinition("dist_mil_on", "0121", "Distance with MIL On", "مسافت طی شده با چراغ چک روشن", "MIL-KM", "km", "miles", 0f, 65535f, SensorCategory.EMISSIONS, "کیلومتر رانندگی از زمان بروز خطا و روشن شدن چک"),
        SensorDefinition("dist_cleared", "0131", "Distance Since Codes Cleared", "مسافت از زمان ریست خطاها", "CLR-KM", "km", "miles", 0f, 65535f, SensorCategory.EMISSIONS, "کیلومتر طی شده پس از پاک‌کردن حافظه ایسیو"),
        SensorDefinition("warmups_cleared", "0130", "Warm-ups Since Codes Cleared", "تعداد گرم‌شدن موتور پس از ریست", "WARM", "بار", "cycles", 0f, 255f, SensorCategory.EMISSIONS, "چرخه‌های رانندگی جهت تست آماده‌سازی معاینه فنی"),

        // ==================== IRANIAN PROPRIETARY ECUS ====================
        SensorDefinition("stepper_steps", "IR_STEP", "Stepper Motor Steps", "گام استپر موتور دور آرام", "STEP", "گام", "steps", 0f, 200f, SensorCategory.IRANIAN_ECU, "میزان باز بودن ژیگلور استپر در پژو و پراید (حالت استاندارد: ۳۵ تا ۵۰ گام درجا)"),
        SensorDefinition("cng_tank_press", "IR_CNG_P", "CNG Tank Pressure", "فشار مخزن گاز طبیعی CNG", "CNG-T", "Bar", "psi", 0f, 250f, SensorCategory.IRANIAN_ECU, "فشار داخل کپسول گاز سمند EF7 و پژو دوگانه‌سوز"),
        SensorDefinition("cng_rail_press", "IR_CNG_R", "CNG Rail Pressure", "فشار ریل گاز CNG", "CNG-R", "Bar", "psi", 0f, 10f, SensorCategory.IRANIAN_ECU, "فشار خروجی رگلاتور گاز CNG به انژکتورها"),
        SensorDefinition("fan_low_relay", "IR_FAN_L", "Low Speed Cooling Fan", "رله فن خنک‌کننده دور کند", "FAN-L", "وضعیت", "state", 0f, 1f, SensorCategory.IRANIAN_ECU, "فعال شدن فن دور کند در دمای ۸۹ الی ۹۲ درجه"),
        SensorDefinition("fan_high_relay", "IR_FAN_H", "High Speed Cooling Fan", "رله فن خنک‌کننده دور تند", "FAN-H", "وضعیت", "state", 0f, 1f, SensorCategory.IRANIAN_ECU, "فعال شدن دور تند در دمای ۹۷ درجه یا با کولر"),
        SensorDefinition("knock_retard", "IR_KNOCK", "Knock Retard Angle", "ریتارد زاویه جرقه در اثر ضربه ناک", "KNK-R", "°", "°", 0f, 15f, SensorCategory.IRANIAN_ECU, "کاهش زاویه آوانس توسط ایسیو برای دفع پدیده خودسوزی و ضربه سوخت نامرغوب"),
        SensorDefinition("ac_pressure", "IR_AC_P", "A/C Refrigerant Pressure", "فشار گاز کولر در مدار", "AC-PRS", "Bar", "psi", 0f, 30f, SensorCategory.IRANIAN_ECU, "فشار سوئیچ سه مرحله‌ای گاز کولر R134a"),
        SensorDefinition("cvvt_cam_angle", "IR_CVVT", "CVVT Camshaft Advance Angle", "زاویه متغیر میل‌سوپاپ CVVT", "CVVT", "°", "°", 0f, 45f, SensorCategory.IRANIAN_ECU, "چرخش دنده میل‌سوپاپ هوا توسط شیر برقی در موتور EF7 و TU5P"),
        SensorDefinition("brake_pedal_sw", "IR_BRK_SW", "Brake Pedal Switch Status", "سوئیچ استپ ترمز", "BRK", "وضعیت", "state", 0f, 1f, SensorCategory.IRANIAN_ECU, "سیگنال فشرده شدن پدال ترمز به ایسیو و بوستر"),
        SensorDefinition("clutch_pedal_sw", "IR_CLT_SW", "Clutch Pedal Switch", "میکروسوئیچ پدال کلاچ", "CLT", "وضعیت", "state", 0f, 1f, SensorCategory.IRANIAN_ECU, "قطع دور موتور هنگام کلاچ‌گیری برای تعویض نرم دنده"),

        // ==================== MOTORCYCLE & CHINESE ECUS ====================
        SensorDefinition("moto_gear_pos", "MC_GEAR", "Motorcycle Gear Position", "دنده فعلی گیربکس موتورسیکلت", "GEAR", "دنده", "gear", 0f, 6f, SensorCategory.MOTORCYCLE, "نمایش شماره دنده (N, 1, 2, 3, 4, 5, 6) در موتور سیکلت‌های انژکتوری"),
        SensorDefinition("moto_side_stand", "MC_STAND", "Side Stand Switch", "سنسور جک بغل موتورسیکلت", "STAND", "وضعیت", "state", 0f, 1f, SensorCategory.MOTORCYCLE, "سوئیچ ایمنی قطع جرقه هنگام باز بودن جک"),
        SensorDefinition("moto_tip_over", "MC_TOS", "Tip-Over / Roll Sensor", "سنسور واژگونی موتورسیکلت", "TOS", "°", "°", 0f, 90f, SensorCategory.MOTORCYCLE, "سنسور سنجش زاویه موتورسیکلت برای خاموش کردن پمپ بنزین در تصادف"),
        SensorDefinition("chinese_turbo_boost", "CH_BOOST", "Turbocharger Boost Pressure", "فشار بوست توربوشارژر", "BOOST", "Bar", "psi", 0f, 2.5f, SensorCategory.MOTORCYCLE, "فشار هوای فشرده توربو در خودروهای جک، تیگو و هایما"),
        SensorDefinition("chinese_cvt_temp", "CH_CVT_T", "CVT Transmission Fluid Temp", "دمای روغن گیربکس CVT چینی", "CVT-T", "°C", "°F", 0f, 140f, SensorCategory.MOTORCYCLE, "دمای حیاتی روغن گیربکس ضریب متغیر CVT مدیران خودرو و چری")
    )

    fun findById(id: String): SensorDefinition {
        return ALL_SENSORS.firstOrNull { it.id == id } ?: ALL_SENSORS.first()
    }
}
