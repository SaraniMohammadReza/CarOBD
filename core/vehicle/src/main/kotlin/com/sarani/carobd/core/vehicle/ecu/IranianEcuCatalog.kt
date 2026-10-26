package com.sarani.carobd.core.vehicle.ecu

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DataSourceReference
import com.sarani.carobd.core.model.EcuHeader
import com.sarani.carobd.core.model.EcuProfile
import com.sarani.carobd.core.model.EcuProtocolConfig
import com.sarani.carobd.core.model.FeatureMaturity
import com.sarani.carobd.core.model.ProtocolStandard

/**
 * Official ECU Catalog for Iranian and Middle-Eastern automotive applications.
 * Includes verified initialization headers, diagnostic addresses, and protocol variants
 * for IKCO, SAIPA, Pars Khodro, Chinese OEM, and motorcycle engines.
 */
object IranianEcuCatalog {

    val BOSCH_ME744_TU5 = EcuProfile(
        id = "bosch_me744_tu5",
        nameEn = "Bosch ME7.4.4 (Peugeot 206 / Pars TU5)",
        nameFa = "بوش ME7.4.4 (پژو ۲۰۶ فرانسه / پارس TU5)",
        family = "Bosch ME7.4.4",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "10", testerAddressHex = "F1"),
            responseTimeoutMs = 400L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "10",
        notesFa = "پروتکل KWP2000 فست با هدر ۸۱ ۱۰ اف۱؛ بسیار پایدار روی ۲۰۶ تیپ ۵ مدل‌های ۸۲ تا ۹۱ و پارس TU5 قدیمی.",
        confidence = ConfidenceLevel.VERIFIED_OEM,
        sources = listOf(
            DataSourceReference(
                title = "Peugeot 206 Multiplex Electrical Diagram PSA",
                urlOrDocument = "PSA-DOC-206-MUX-ED04",
                manufacturer = "PSA Automobiles"
            ),
            DataSourceReference(
                title = "KWP2000 bus capture via Peugeot Planet 2000",
                urlOrDocument = "HW-CAPTURE-PP2000-ME744",
                notes = "Verified physical response 0xC1 with seed key request"
            )
        )
    )

    val BOSCH_ME745_TU5 = EcuProfile(
        id = "bosch_me745_tu5",
        nameEn = "Bosch ME7.4.5 (Peugeot 206 CAN)",
        nameFa = "بوش ME7.4.5 (پژو ۲۰۶ تیپ ۵ و ۶ شبکه CAN)",
        family = "Bosch ME7.4.5",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            customCanFilterHex = "7E8",
            customCanMaskHex = "7F8",
            responseTimeoutMs = 250L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "7E0",
        notesFa = "ارتباط با شبکه پرسرعت CAN 500Kbps؛ آدرس درخواست 7E0 و پاسخ 7E8.",
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val BOSCH_ME749_EF7_NA = EcuProfile(
        id = "bosch_me749_ef7_na",
        nameEn = "Bosch ME7.4.9 (Samand / Soren EF7 Naturally Aspirated)",
        nameFa = "بوش ME7.4.9 (سمند و سورن EF7 بنزینی و دوگانه‌سوز)",
        family = "Bosch ME7.4.9",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "10", testerAddressHex = "F1"),
            responseTimeoutMs = 350L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "10",
        notesFa = "ایسیو تخصصی موتور ملی EF7؛ پشتیبانی از پایش انژکتورهای گاز CNG در مدل‌های بای‌فیول.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val BOSCH_ME749_EF7_TC = EcuProfile(
        id = "bosch_me749_ef7_tc",
        nameEn = "Bosch ME7.4.9 TC (Dena+ Turbo EF7 TC)",
        nameFa = "بوش ME7.4.9 TC (دنا پلاس توربو و سورن پلاس توربو)",
        family = "Bosch ME7.4.9 TC",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            responseTimeoutMs = 250L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "7E0",
        notesFa = "پایش فشار بوست توربوشارژر و دمای اینترکولر روی شبکه CAN.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val SIEMENS_SIM2K_XU7 = EcuProfile(
        id = "siemens_sim2k_xu7",
        nameEn = "Siemens VDO / SIM2K (Peugeot 405 / Pars / Samand XU7)",
        nameFa = "زیمنس VDO / SIM2K (پژو ۴۰۵، پارس و سمند موتور XU7)",
        family = "Siemens SIM2K",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "11", testerAddressHex = "F1"),
            responseTimeoutMs = 400L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "11",
        notesFa = "پرکاربردترین ایسیو ایران خودرو روی موتور ۱۸۰۰ سی‌سی XU7؛ تارگت ادرس 0x11.",
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SIEMENS_VDO_SAIPA_M13_M15 = EcuProfile(
        id = "siemens_vdo_saipa",
        nameEn = "Siemens VDO (Pride M13 / Tiba, Saina, Quik M15)",
        nameFa = "زیمنس VDO سایپایی (پراید M13، تیبا، ساینا و کوییک M15)",
        family = "Siemens VDO Saipa",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "11", testerAddressHex = "F1"),
            responseTimeoutMs = 350L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "11",
        notesFa = "ایسیو اصلی خودروهای سایپا با حافظه فلش تک‌سوکت و دو سوکت.",
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val VALEO_S2000_IKCO = EcuProfile(
        id = "valeo_s2000_ikco",
        nameEn = "Valeo S2000 / Sagem (Peugeot 206 TU3 / 405 / Pars)",
        nameFa = "والئو S2000 / ساژم (پژو ۲۰۶ تیپ ۲ موتور TU3 / ۴۰۵)",
        family = "Valeo S2000",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "10", testerAddressHex = "F1"),
            responseTimeoutMs = 450L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "10",
        notesFa = "نسخه‌های 3E, 3F, PL4 و 10LC با پایداری بالا در پاسخگویی.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val SSAT_IKCO_SAIPA = EcuProfile(
        id = "ssat_ikco_saipa",
        nameEn = "SSAT (Single & Double Plug IKCO / SAIPA)",
        nameFa = "اس‌اس‌ای‌تی SSAT (تک سوکت و دو سوکت بدون فلش و با فلش)",
        family = "SSAT",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "11", testerAddressHex = "F1"),
            responseTimeoutMs = 400L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "11",
        notesFa = "طراحی مشترک با پروتکل زیمنس با اندکی تاخیر در پاسخ به ریکوئست‌های متوالی.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val EASYU_2_5_TU5P = EcuProfile(
        id = "easyu_2_5_tu5p",
        nameEn = "EasyU 2.5 (Runna+ / Peugeot 207 TU5P)",
        nameFa = "ایزی‌یو EasyU 2.5 (رانا پلاس و پژو ۲۰۷ موتور TU5 پلاس)",
        family = "EasyU 2.5",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            responseTimeoutMs = 250L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "7E0",
        notesFa = "پشتیبانی استاندارد OBD-II روی شبکه کن ۵۰۰ کیلوبیت.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val CROUSE_KESENS_CAN = EcuProfile(
        id = "crouse_kesens_can",
        nameEn = "Crouse / Kesens CAN (Tara / Dena+ / Shahin M15-TC)",
        nameFa = "کروز / کزنس CAN (تارا، دنا پلاس ۶ سرعته، شاهین توربو M15-TC)",
        family = "Crouse Kesens",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            responseTimeoutMs = 200L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "7E0",
        notesFa = "شبکه مالتی‌پلکس پیشرفته سازگار با پروتکل ISO 15765-4.",
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SIEMENS_EMS3132_K4M = EcuProfile(
        id = "siemens_ems3132_k4m",
        nameEn = "Siemens EMS3132 (Tondar 90 L90 / Sandero K4M)",
        nameFa = "زیمنس EMS3132 (تندر ۹۰ ال۹۰ و ساندرو موتور K4M)",
        family = "Siemens EMS3132",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
            header = EcuHeader(priorityHex = "81", targetAddressHex = "10", testerAddressHex = "F1"),
            responseTimeoutMs = 350L,
            confidence = ConfidenceLevel.VERIFIED_OEM
        ),
        primaryAddressHex = "10",
        notesFa = "ایسیو اصلی رنو تندر ۹۰ فرانسوی با پایداری استثنایی.",
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val DELPHI_MT22_CHINESE = EcuProfile(
        id = "delphi_mt22_chinese",
        nameEn = "Delphi MT22.1 / MT80 (MVM / Chery / JAC)",
        nameFa = "دلفی MT22.1 / MT80 (مدیران خودرو MVM، چری و کرمان موتور JAC)",
        family = "Delphi MT22",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            responseTimeoutMs = 250L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "7E0",
        notesFa = "استاندارد OBD-II کامل برای سنسورهای آلایندگی و پایش انژکتورها.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val EURO5_MOTORCYCLE_ISO19689 = EcuProfile(
        id = "euro5_motorcycle_iso19689",
        nameEn = "ISO 19689 Euro 5 Motorcycle (Bajaj / TVS / Honda / Yamaha)",
        nameFa = "استاندارد یورو ۵ موتورسیکلت ISO 19689 (پالس، آپاچی، کلیک، باکسر)",
        family = "Euro 5 Motorcycle",
        protocolConfig = EcuProtocolConfig(
            standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
            responseTimeoutMs = 300L,
            confidence = ConfidenceLevel.HIGH_CONFIDENCE
        ),
        primaryAddressHex = "7E0",
        notesFa = "کانکتور استاندارد ۶ پین قرمز رنگ یورو ۵ موتورسیکلت با پایه CAN-H و CAN-L.",
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val allProfiles: List<EcuProfile> = listOf(
        BOSCH_ME744_TU5,
        BOSCH_ME745_TU5,
        BOSCH_ME749_EF7_NA,
        BOSCH_ME749_EF7_TC,
        SIEMENS_SIM2K_XU7,
        SIEMENS_VDO_SAIPA_M13_M15,
        VALEO_S2000_IKCO,
        SSAT_IKCO_SAIPA,
        EASYU_2_5_TU5P,
        CROUSE_KESENS_CAN,
        SIEMENS_EMS3132_K4M,
        DELPHI_MT22_CHINESE,
        EURO5_MOTORCYCLE_ISO19689
    )

    fun findById(id: String): EcuProfile? = allProfiles.firstOrNull { it.id == id }
}
