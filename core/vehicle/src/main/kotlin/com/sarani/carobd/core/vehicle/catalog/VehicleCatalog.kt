package com.sarani.carobd.core.vehicle.catalog

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.EngineProfile
import com.sarani.carobd.core.model.FeatureMaturity
import com.sarani.carobd.core.model.TransmissionProfile
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.model.VehicleType
import com.sarani.carobd.core.vehicle.ecu.IranianEcuCatalog

/**
 * Verified catalog of Iranian, Chinese, and motorcycle vehicles with their primary and secondary ECUs.
 */
object VehicleCatalog {

    val PEUGEOT_206_TU5 = VehicleProfile(
        id = "ikco_206_tu5",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "Peugeot 206 Type 5 (TU5)",
        modelNameFa = "پژو ۲۰۶ تیپ ۵ (موتور TU5)",
        modelYears = "1382-1401",
        engine = EngineProfile(code = "TU5 JP4", displacementLiters = 1.587f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual MA5", speedCount = 5, fluidType = "75W-80 GL-4+", capacityLiters = 2.0f),
        primaryEcu = IranianEcuCatalog.BOSCH_ME744_TU5,
        alternativeEcus = listOf(IranianEcuCatalog.BOSCH_ME745_TU5, IranianEcuCatalog.VALEO_S2000_IKCO),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val PEUGEOT_206_TU3 = VehicleProfile(
        id = "ikco_206_tu3",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "Peugeot 206 Type 2 (TU3)",
        modelNameFa = "پژو ۲۰۶ تیپ ۲ (موتور TU3)",
        modelYears = "1380-1401",
        engine = EngineProfile(code = "TU3 A/JP", displacementLiters = 1.360f, cylinderCount = 4, valveCount = 8, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual MA5", speedCount = 5, fluidType = "75W-80 GL-4+", capacityLiters = 2.0f),
        primaryEcu = IranianEcuCatalog.VALEO_S2000_IKCO,
        alternativeEcus = listOf(IranianEcuCatalog.BOSCH_ME744_TU5),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val PEUGEOT_207_TU5P = VehicleProfile(
        id = "ikco_207_tu5p",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "Peugeot 207i MC / AT (TU5P)",
        modelNameFa = "پژو ۲۰۷i پلاس و اتوماتیک (موتور TU5 پلاس)",
        modelYears = "1400-1404",
        engine = EngineProfile(code = "TU5P", displacementLiters = 1.587f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Automatic 6AT DAE", speedCount = 6, fluidType = "ATF AW-1", capacityLiters = 5.5f),
        primaryEcu = IranianEcuCatalog.EASYU_2_5_TU5P,
        alternativeEcus = listOf(IranianEcuCatalog.CROUSE_KESENS_CAN),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val PEUGEOT_PARS_XU7 = VehicleProfile(
        id = "ikco_pars_xu7",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "Peugeot Pars (XU7 1.8L)",
        modelNameFa = "پژو پارس سال (موتور ۱۸۰۰ سی‌سی XU7)",
        modelYears = "1382-1401",
        engine = EngineProfile(code = "XU7 JP L3", displacementLiters = 1.761f, cylinderCount = 4, valveCount = 8, fuelType = "Gasoline", isBiFuelCng = false),
        transmission = TransmissionProfile(type = "Manual BE3", speedCount = 5, fluidType = "75W-80 GL-5", capacityLiters = 1.9f),
        primaryEcu = IranianEcuCatalog.SIEMENS_SIM2K_XU7,
        alternativeEcus = listOf(IranianEcuCatalog.SSAT_IKCO_SAIPA, IranianEcuCatalog.VALEO_S2000_IKCO),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SAMAND_DENA_EF7 = VehicleProfile(
        id = "ikco_dena_ef7",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "IKCO Dena / Samand (EF7 NA)",
        modelNameFa = "سمند و دنا معمولی (موتور ملی EF7 تنفس طبیعی)",
        modelYears = "1389-1403",
        engine = EngineProfile(code = "EF7 NA", displacementLiters = 1.645f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline / CNG", isBiFuelCng = true),
        transmission = TransmissionProfile(type = "Manual BE4", speedCount = 5, fluidType = "75W-80 GL-5", capacityLiters = 1.9f),
        primaryEcu = IranianEcuCatalog.BOSCH_ME749_EF7_NA,
        alternativeEcus = listOf(IranianEcuCatalog.CROUSE_KESENS_CAN, IranianEcuCatalog.SSAT_IKCO_SAIPA),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val DENA_PLUS_TURBO_EF7 = VehicleProfile(
        id = "ikco_dena_plus_turbo",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "IKCO Dena+ Turbo (EF7 TC)",
        modelNameFa = "دنا پلاس توربو شارژ (موتور EF7 توربو)",
        modelYears = "1397-1404",
        engine = EngineProfile(code = "EF7 TC", displacementLiters = 1.645f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline", isTurbocharged = true),
        transmission = TransmissionProfile(type = "Automatic 6AT DAE", speedCount = 6, fluidType = "ATF AW-1", capacityLiters = 5.5f),
        primaryEcu = IranianEcuCatalog.BOSCH_ME749_EF7_TC,
        alternativeEcus = listOf(IranianEcuCatalog.CROUSE_KESENS_CAN),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val IKCO_TARA_TU5P = VehicleProfile(
        id = "ikco_tara_tu5p",
        type = VehicleType.CAR,
        manufacturer = "Iran Khodro (IKCO)",
        modelNameEn = "IKCO Tara (TU5P)",
        modelNameFa = "تارا دنده‌ای و اتوماتیک (موتور TU5 پلاس)",
        modelYears = "1400-1404",
        engine = EngineProfile(code = "TU5P", displacementLiters = 1.587f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Automatic 6AT DAE", speedCount = 6, fluidType = "ATF AW-1", capacityLiters = 5.5f),
        primaryEcu = IranianEcuCatalog.CROUSE_KESENS_CAN,
        alternativeEcus = listOf(IranianEcuCatalog.EASYU_2_5_TU5P),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SAIPA_PRIDE_M13 = VehicleProfile(
        id = "saipa_pride_m13",
        type = VehicleType.CAR,
        manufacturer = "SAIPA",
        modelNameEn = "Saipa Pride 131 / 111 (M13)",
        modelNameFa = "سایپا پراید ۱۳۱ و ۱۱۱ (موتور ۱۳۰۰ سی‌سی M13)",
        modelYears = "1384-1399",
        engine = EngineProfile(code = "M13", displacementLiters = 1.323f, cylinderCount = 4, valveCount = 8, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual 5-Speed", speedCount = 5, fluidType = "75W-90 GL-4", capacityLiters = 2.4f),
        primaryEcu = IranianEcuCatalog.SIEMENS_VDO_SAIPA_M13_M15,
        alternativeEcus = listOf(IranianEcuCatalog.SSAT_IKCO_SAIPA),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SAIPA_TIBA_SAINA_QUIK_M15 = VehicleProfile(
        id = "saipa_m15_series",
        type = VehicleType.CAR,
        manufacturer = "SAIPA",
        modelNameEn = "Saipa Tiba / Saina / Quik (M15)",
        modelNameFa = "سایپا تیبا، ساینا و کوییک (موتور ۱۵۰۰ سی‌سی M15)",
        modelYears = "1389-1404",
        engine = EngineProfile(code = "M15", displacementLiters = 1.503f, cylinderCount = 4, valveCount = 8, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual 5-Speed", speedCount = 5, fluidType = "75W-90 GL-4", capacityLiters = 2.4f),
        primaryEcu = IranianEcuCatalog.SIEMENS_VDO_SAIPA_M13_M15,
        alternativeEcus = listOf(IranianEcuCatalog.SSAT_IKCO_SAIPA, IranianEcuCatalog.CROUSE_KESENS_CAN),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val SAIPA_SHAHIN_TURBO = VehicleProfile(
        id = "saipa_shahin_turbo",
        type = VehicleType.CAR,
        manufacturer = "SAIPA",
        modelNameEn = "Saipa Shahin (M15-TC Turbo)",
        modelNameFa = "سایپا شاهین توربو (موتور M15 توربوشارژ)",
        modelYears = "1400-1404",
        engine = EngineProfile(code = "M15-TC", displacementLiters = 1.503f, cylinderCount = 4, valveCount = 8, fuelType = "Gasoline", isTurbocharged = true),
        transmission = TransmissionProfile(type = "Manual 5MT / CVT", speedCount = 5, fluidType = "75W-85 GL-4", capacityLiters = 2.5f),
        primaryEcu = IranianEcuCatalog.CROUSE_KESENS_CAN,
        alternativeEcus = emptyList(),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val RENAULT_L90_K4M = VehicleProfile(
        id = "renault_l90_k4m",
        type = VehicleType.CAR,
        manufacturer = "Pars Khodro / Renault",
        modelNameEn = "Tondar 90 / Sandero (K4M 1.6L)",
        modelNameFa = "رنو تندر ۹۰ و ساندرو (موتور K4M ۱۶۰۰ سی‌سی)",
        modelYears = "1386-1398",
        engine = EngineProfile(code = "K4M 690", displacementLiters = 1.598f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual JH3 / Automatic DP2", speedCount = 5, fluidType = "75W-80 Tranself NFJ", capacityLiters = 2.8f),
        primaryEcu = IranianEcuCatalog.SIEMENS_EMS3132_K4M,
        alternativeEcus = emptyList(),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.VERIFIED_OEM
    )

    val CHERY_MVM_X22 = VehicleProfile(
        id = "chery_mvm_x22",
        type = VehicleType.CAR,
        manufacturer = "MVM / Chery",
        modelNameEn = "MVM X22 / Tiggo 2 (1.5L)",
        modelNameFa = "مدیران خودرو MVM X22 (موتور ۱۵۰۰ سی‌سی)",
        modelYears = "1396-1403",
        engine = EngineProfile(code = "ACTECO 1.5", displacementLiters = 1.497f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Automatic 4AT / CVT", speedCount = 4, fluidType = "ATF SP-III", capacityLiters = 4.5f),
        primaryEcu = IranianEcuCatalog.DELPHI_MT22_CHINESE,
        alternativeEcus = emptyList(),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val JAC_KMC_S5 = VehicleProfile(
        id = "jac_kmc_s5",
        type = VehicleType.CAR,
        manufacturer = "KMC / JAC",
        modelNameEn = "JAC S5 Turbo (2.0L / 1.5L TGDI)",
        modelNameFa = "جک S5 و KMC (موتور ۲۰۰۰ و ۱۵۰۰ توربو)",
        modelYears = "1394-1403",
        engine = EngineProfile(code = "HFC4GA3-1D", displacementLiters = 1.997f, cylinderCount = 4, valveCount = 16, fuelType = "Gasoline", isTurbocharged = true),
        transmission = TransmissionProfile(type = "Dual Clutch DCT 6-Speed", speedCount = 6, fluidType = "DCTF-1", capacityLiters = 5.0f),
        primaryEcu = IranianEcuCatalog.DELPHI_MT22_CHINESE,
        alternativeEcus = emptyList(),
        maturity = FeatureMaturity.PRODUCTION_IMPLEMENTED,
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val BAJAJ_PULSAR_NS200 = VehicleProfile(
        id = "moto_pulsar_ns200",
        type = VehicleType.MOTORCYCLE,
        manufacturer = "Bajaj",
        modelNameEn = "Bajaj Pulsar NS200 (Euro 5 FI)",
        modelNameFa = "موتورسیکلت باجاج پالس NS200 انژکتور یورو ۵",
        modelYears = "1397-1403",
        engine = EngineProfile(code = "DTS-i Triple Spark", displacementLiters = 0.199f, cylinderCount = 1, valveCount = 4, fuelType = "Gasoline"),
        transmission = TransmissionProfile(type = "Manual 6-Speed", speedCount = 6, fluidType = "20W-50 JASO MA2", capacityLiters = 1.2f),
        primaryEcu = IranianEcuCatalog.EURO5_MOTORCYCLE_ISO19689,
        alternativeEcus = emptyList(),
        maturity = FeatureMaturity.IMPLEMENTED_NEEDS_HARDWARE_VALIDATION,
        confidence = ConfidenceLevel.HIGH_CONFIDENCE
    )

    val allProfiles: List<VehicleProfile> = listOf(
        PEUGEOT_206_TU5,
        PEUGEOT_206_TU3,
        PEUGEOT_207_TU5P,
        PEUGEOT_PARS_XU7,
        SAMAND_DENA_EF7,
        DENA_PLUS_TURBO_EF7,
        IKCO_TARA_TU5P,
        SAIPA_PRIDE_M13,
        SAIPA_TIBA_SAINA_QUIK_M15,
        SAIPA_SHAHIN_TURBO,
        RENAULT_L90_K4M,
        CHERY_MVM_X22,
        JAC_KMC_S5,
        BAJAJ_PULSAR_NS200
    )

    fun findById(id: String): VehicleProfile? = allProfiles.firstOrNull { it.id == id }

    fun filterByType(type: VehicleType): List<VehicleProfile> = allProfiles.filter { it.type == type }

    fun search(query: String): List<VehicleProfile> {
        val q = query.trim().lowercase()
        return allProfiles.filter {
            it.manufacturer.lowercase().contains(q) ||
            it.modelNameEn.lowercase().contains(q) ||
            it.modelNameFa.lowercase().contains(q) ||
            it.engine.code.lowercase().contains(q)
        }
    }
}
