package com.sarani.carobd.core.database

data class MotorcyclePinout(
    val brandName: String,
    val connectorType: String,
    val pinoutDescriptionFa: List<String>,
    val supportedModelsFa: String,
    val protocol: String
)

data class ChineseCarSpec(
    val brandName: String,
    val modelName: String,
    val ecuFamily: String,
    val protocol: String,
    val transmissionType: String,
    val cvtFluidTempPid: String,
    val specialNotesFa: String
)

object MotorcycleAndChineseSpecs {

    val MOTORCYCLE_PINOUTS = listOf(
        MotorcyclePinout(
            brandName = "یورو ۵ استاندارد ۶ پین (ISO 19689)",
            connectorType = "سوکت ۶ پین قرمز/سفید ضد آب",
            pinoutDescriptionFa = listOf(
                "پین ۱: CAN High (شبکه پرسرعت ۵۰۰k)",
                "پین ۲: CAN Low",
                "پین ۳: K-Line (پروتکل ISO 14230)",
                "پین ۴: بدنه / منفی شاسی (Ground)",
                "پین ۵: برق مثبت ۱۲ ولت دائم باتری (+12V Battery)",
                "پین ۶: مثبت سوییچ باز (Ignition Power)"
            ),
            supportedModelsFa = "بنلی ۱۸۰ و ۲۴۹S، کی‌تی‌ام دوک ۲۰۰ و ۲۵۰، سی‌اف موتو ۲۵۰، دایچی، وسپا یورو ۵",
            protocol = "ISO 15765-4 CAN 500k & KWP2000"
        ),
        MotorcyclePinout(
            brandName = "باجاج و کی‌تی‌ام (Bajaj Pulsar / Dominar)",
            connectorType = "سوکت ۶ پین مشکی",
            pinoutDescriptionFa = listOf(
                "پین ۲: خط دیتا K-Line ایسیو بوش",
                "پین ۴: منفی باتری (GND)",
                "پین ۶: مثبت ۱۲ ولت باتری (+12V)"
            ),
            supportedModelsFa = "پالس NS200، پالس RS200، دومینار ۲۴۹ و ۴۰۰، اونجر ۲۲۰ انژکتور",
            protocol = "Bosch MSE 3.0 / MSE 6.0 KWP2000"
        ),
        MotorcyclePinout(
            brandName = "تی‌وی‌اس آپاچی (TVS Apache RTR)",
            connectorType = "سوکت ۶ پین سفید",
            pinoutDescriptionFa = listOf(
                "پین ۱: مثبت ۱۲ ولت با فیوز ۵ آمپر",
                "پین ۳: خط تک‌سیمه K-Line ارتباط دیاگ",
                "پین ۵: بدنه اتصال منفی"
            ),
            supportedModelsFa = "آپاچی ۱۸۰، آپاچی ۲۰۰ چهار سوپاپ، تی‌وی‌اس راکس و ویگو",
            protocol = "Keihin / Continental KWP2000"
        ),
        MotorcyclePinout(
            brandName = "هوندا و طرح هوندا (Honda 4-Pin)",
            connectorType = "سوکت قرمز ۴ پین ضد آب هوندا",
            pinoutDescriptionFa = listOf(
                "پین ۱: خط K-Line عیب‌یابی Keihin",
                "پین ۲: منفی باتری (Ground)",
                "پین ۳: پالس چک موتور (MIL Signal)",
                "پین ۴: برق مثبت ۱۲ ولت"
            ),
            supportedModelsFa = "هوندا کلیک ۱۵۰ و ۱۶۰، ADV150، CB150، سی‌دی‌آی ۱۲۵ انژکتوری لایفان و کویر",
            protocol = "Honda PGM-FI ISO 9141"
        )
    )

    val CHINESE_CARS = listOf(
        ChineseCarSpec(
            brandName = "مدیران خودرو (MVM / Chery)",
            modelName = "تیگو ۷ و ۸ پرو / آریزو ۵ و ۶ توربو",
            ecuFamily = "Bosch MED17.8.10 / ME17.8.8 GDI",
            protocol = "ISO 15765-4 CAN 11bit 500kbps (CAN ID 0x7E0)",
            transmissionType = "گیربکس CVT9 و دوکلاچه تر (7DCT Wet)",
            cvtFluidTempPid = "2102",
            specialNotesFa = "خواندن دمای کلاچ دوگانه جهت جلوگیری از پیغام داغی گیربکس در ترافیک‌های سنگین تهران."
        ),
        ChineseCarSpec(
            brandName = "کرمان موتور (KMC / JAC)",
            modelName = "KMC J7 / T8 / K7 / جک S5 توربو",
            ecuFamily = "Delphi MT92 / Bosch MED17",
            protocol = "ISO 15765-4 CAN 11bit 500kbps",
            transmissionType = "گیربکس ۶ سرعته دوکلاچه DCT و دستی",
            cvtFluidTempPid = "221105",
            specialNotesFa = "پایش فشار ریل سوخت GDI تا ۱۵۰ بار و سنسور اکسیژن پهن‌باند جهت سلامت کاتالیزور."
        ),
        ChineseCarSpec(
            brandName = "بهمن موتور (Bahman)",
            modelName = "فیدلیتی پرایم و پرستیژ / دیگنیتی پرایم",
            ecuFamily = "Bosch ME17.8.8 Turbo",
            protocol = "ISO 15765-4 CAN 500kbps",
            transmissionType = "گیربکس ۶ سرعته خشک و ۷ سرعته تر",
            cvtFluidTempPid = "211A",
            specialNotesFa = "حساسیت بالا به بنزین کم‌کیفیت و بروز ناک؛ پایش زاویه ریتارد جرقه در سربالایی‌ها الزامی است."
        ),
        ChineseCarSpec(
            brandName = "ایران خودرو (IKCO)",
            modelName = "هایما S7 توربو / S5 توربو پلاس",
            ecuFamily = "Bosch ME17.8.8 / Delphi",
            protocol = "ISO 15765-4 CAN 500kbps",
            transmissionType = "گیربکس ۶ سرعته هیوندای پاور تک (HPT)",
            cvtFluidTempPid = "2101",
            specialNotesFa = "پایش دمای توربوشارژر و فشار منیفولد بوست تا ۱.۲ بار."
        )
    )
}
