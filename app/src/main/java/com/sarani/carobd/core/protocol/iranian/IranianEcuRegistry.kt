package com.sarani.carobd.core.protocol.iranian

import com.sarani.carobd.core.model.EcuProtocol
import com.sarani.carobd.core.model.ProtocolStandard
import com.sarani.carobd.core.model.VehicleProfile

object IranianEcuRegistry {

    // 1. Siemens VDO / Crouse (Pride, Tiba, Saina, Quick, 405, Pars, Samand)
    val PROTOCOL_SIEMENS = EcuProtocol(
        id = "siemens_kwp",
        nameEn = "Siemens VDO / Crouse (ISO 14230 KWP)",
        nameFa = "زیمنس VDO / کروز (ISO 14230 KWP)",
        standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
        targetAddressHex = "11",
        testerAddressHex = "F1",
        customHeaderCommand = "AT SH 81 11 F1",
        wakeUpMessageCommand = "AT WM 81 11 F1 81",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 5",
            "AT SH 81 11 F1",
            "AT SW 96",
            "AT WM 81 11 F1 81",
            "AT AT 1"
        ),
        notesFa = "مناسب برای پراید، تیبا، ساینا، کوییک و پژوهای مجهز به ایسیو زیمنس (سریع‌ترین نرخ تبادل)"
    )

    // 2. Bosch ME7.4.4 / ME7.4.5 (Peugeot 206 Type 5, 207, Pars TU5)
    val PROTOCOL_BOSCH_ME744 = EcuProtocol(
        id = "bosch_me744",
        nameEn = "Bosch ME 7.4.4 / 7.4.5 TU5 (KWP2000 / K-Line)",
        nameFa = "بوش ME 7.4.4 / 7.4.5 TU5 (پژو ۲۰۶ و پارس)",
        standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
        targetAddressHex = "10",
        testerAddressHex = "F1",
        customHeaderCommand = "AT SH 81 10 F1",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 5",
            "AT SH 81 10 F1",
            "AT AT 1",
            "AT ST 64"
        ),
        notesFa = "ایسیو مشهور پژو ۲۰۶ تیپ ۵ فرانسوی و پارس TU5 با موتور ۱۶ سوپاپ"
    )

    // 3. Bosch ME7.4.9 (Samand EF7, Dena, Dena+ Petrol & Bi-Fuel CNG)
    val PROTOCOL_BOSCH_ME749 = EcuProtocol(
        id = "bosch_me749",
        nameEn = "Bosch ME 7.4.9 EF7 (Bi-Fuel & Petrol)",
        nameFa = "بوش ME 7.4.9 موتور ملی EF7 (سمند و دنا دوگانه‌سوز/بنزینی)",
        standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
        customHeaderCommand = "AT SH 7E0",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 6",
            "AT SH 7E0",
            "AT CRA 7E8",
            "AT AT 1"
        ),
        notesFa = "پشتیبانی کامل از سنسور فشار ریل گاز CNG، دمای آب و سنسور اکسیژن دوم"
    )

    // 4. SSAT KWP (Single & Bi-Fuel Samand, Pars, 405)
    val PROTOCOL_SSAT = EcuProtocol(
        id = "ssat_kwp",
        nameEn = "SSAT K-Line (Single & Bi-Fuel IKCO)",
        nameFa = "اس‌اس‌ای‌تی (SSAT بنزینی و دوگانه پژو و سمند)",
        standard = ProtocolStandard.ISO_14230_4_KWP_FAST,
        targetAddressHex = "10",
        testerAddressHex = "F1",
        customHeaderCommand = "AT SH 81 10 F1",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 5",
            "AT SH 81 10 F1",
            "AT WM 81 10 F1 81",
            "AT IB 10",
            "AT ST 96"
        ),
        notesFa = "نیاز به تنظیم timing اختصاصی در دانگل ELM327 جهت پایداری اتصال"
    )

    // 5. Valeo S2000 / Sagem (Peugeot 206 Type 2/3, 405 Old)
    val PROTOCOL_VALEO_S2000 = EcuProtocol(
        id = "valeo_s2000",
        nameEn = "Valeo S2000 / Sagem PL4 (ISO 9141-2)",
        nameFa = "والئو S2000 / ساژم (پژو ۲۰۶ تیپ ۲ و ۴۰۵ قدیمی)",
        standard = ProtocolStandard.ISO_9141_2,
        customHeaderCommand = "AT SH 68 6A F1",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 3",
            "AT SH 68 6A F1",
            "AT AT 1"
        ),
        notesFa = "پروتکل ۵ باود آهسته K-Line برای خودروهای مدل ۸۲ تا ۸۹"
    )

    // 6. EasyU 2.5 / Crouse EcoMux CAN (Tara, Dena+, Quick S, Shahin)
    val PROTOCOL_EASYU_CAN = EcuProtocol(
        id = "easyu_can",
        nameEn = "EasyU 2.5 / Crouse CAN (IKCO & SAIPA Modern)",
        nameFa = "ایزی‌یو EasyU 2.5 / شبکه کن کروز (تارا، دنا پلاس، شاهین)",
        standard = ProtocolStandard.ISO_15765_4_CAN_11_500,
        customHeaderCommand = "AT SH 7E0",
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 6",
            "AT SH 7E0",
            "AT CAF 1",
            "AT AT 1"
        ),
        notesFa = "پروتکل شبکه پرسرعت CAN Bus 500kbps خودروهای پلتفرم جدید ایران‌خودرو و سایپا"
    )

    // 7. Generic Global Standard OBD-II
    val PROTOCOL_GENERIC_AUTO = EcuProtocol(
        id = "generic_auto",
        nameEn = "Generic OBD-II / EOBD (Auto Detect)",
        nameFa = "استاندارد جهانی OBD-II (تشخیص خودکار)",
        standard = ProtocolStandard.AUTO,
        initSequence = listOf(
            "AT E0",
            "AT L0",
            "AT SP 0",
            "AT AT 1"
        ),
        notesFa = "مناسب برای تمام خودروهای وارداتی، چینی و استاندارد جهانی پس از سال ۱۹۹۶"
    )

    val ALL_PROTOCOLS = listOf(
        PROTOCOL_GENERIC_AUTO,
        PROTOCOL_SIEMENS,
        PROTOCOL_BOSCH_ME744,
        PROTOCOL_BOSCH_ME749,
        PROTOCOL_SSAT,
        PROTOCOL_VALEO_S2000,
        PROTOCOL_EASYU_CAN
    )

    val PRESET_VEHICLES = listOf(
        VehicleProfile(
            id = "ikco_206_tu5",
            manufacturer = "Iran Khodro (ایران خودرو)",
            modelNameEn = "Peugeot 206 / 207 (TU5)",
            modelNameFa = "پژو ۲۰۶ و ۲۰۷ (موتور TU5)",
            engineName = "TU5 1.6L 16V",
            ecuName = "Bosch ME7.4.4 / ME7.4.5",
            ecuProtocol = PROTOCOL_BOSCH_ME744,
            isDefault = true
        ),
        VehicleProfile(
            id = "ikco_dena_ef7",
            manufacturer = "Iran Khodro (ایران خودرو)",
            modelNameEn = "Dena / Samand (EF7)",
            modelNameFa = "دنا / سمند (موتور ملی EF7)",
            engineName = "EF7 1.7L CVVT / TC",
            ecuName = "Bosch ME7.4.9 / EasyU",
            ecuProtocol = PROTOCOL_BOSCH_ME749
        ),
        VehicleProfile(
            id = "ikco_pars_xu7",
            manufacturer = "Iran Khodro (ایران خودرو)",
            modelNameEn = "Peugeot Pars / 405 (XU7 & TU5)",
            modelNameFa = "پژو پارس / ۴۰۵ (موتور XU7 و TU5)",
            engineName = "XU7 1.8L / TU5",
            ecuName = "Siemens VDO / SSAT",
            ecuProtocol = PROTOCOL_SIEMENS
        ),
        VehicleProfile(
            id = "ikco_tara",
            manufacturer = "Iran Khodro (ایران خودرو)",
            modelNameEn = "Tara / Dena+ Turbo MUX",
            modelNameFa = "تارا / دنا پلاس توربو اتوماتیک",
            engineName = "TU5P / EF7 TC",
            ecuName = "Bosch ME17 / EasyU 2.5",
            ecuProtocol = PROTOCOL_EASYU_CAN
        ),
        VehicleProfile(
            id = "saipa_pride_tiba",
            manufacturer = "SAIPA (سایپا)",
            modelNameEn = "Pride / Tiba / Saina / Quick",
            modelNameFa = "پراید / تیبا / ساینا / کوییک",
            engineName = "M13 / M15 1.5L 8V",
            ecuName = "Siemens VDO / Crouse / SSAT",
            ecuProtocol = PROTOCOL_SIEMENS
        ),
        VehicleProfile(
            id = "saipa_shahin",
            manufacturer = "SAIPA (سایپا)",
            modelNameEn = "SAIPA Shahin Turbo",
            modelNameFa = "سایپا شاهین توربو",
            engineName = "M15-TC 1.5L Turbo",
            ecuName = "Bosch ME17 / Crouse EcoMux",
            ecuProtocol = PROTOCOL_EASYU_CAN
        ),
        VehicleProfile(
            id = "ikco_206_tu3_old",
            manufacturer = "Iran Khodro (ایران خودرو)",
            modelNameEn = "Peugeot 206 (TU3 Old French)",
            modelNameFa = "پژو ۲۰۶ تیپ ۲ (فرانسوی مدل ۸۰-۸۹)",
            engineName = "TU3 1.4L 8V",
            ecuName = "Valeo S2000 / Sagem",
            ecuProtocol = PROTOCOL_VALEO_S2000
        ),
        VehicleProfile(
            id = "global_generic",
            manufacturer = "Universal / International",
            modelNameEn = "Universal OBD-II / EOBD Vehicle",
            modelNameFa = "خودروی استاندارد بین‌المللی OBD2",
            engineName = "Generic Gasoline / Hybrid",
            ecuName = "Standard ISO 15765-4 / ISO 14230",
            ecuProtocol = PROTOCOL_GENERIC_AUTO
        )
    )
}
