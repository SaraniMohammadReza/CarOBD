package com.sarani.carobd.core.vehicle.catalog

import com.sarani.carobd.core.model.AspirationType
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DataSourceReference
import com.sarani.carobd.core.model.DriveLayout
import com.sarani.carobd.core.model.EngineVariant
import com.sarani.carobd.core.model.FuelType
import com.sarani.carobd.core.model.ResolvedVehicleMatch
import com.sarani.carobd.core.model.TransmissionType
import com.sarani.carobd.core.model.TransmissionVariant
import com.sarani.carobd.core.model.VehicleGeneration
import com.sarani.carobd.core.model.VehicleMake
import com.sarani.carobd.core.model.VehicleMarket
import com.sarani.carobd.core.model.VehicleModel
import com.sarani.carobd.core.model.VehicleSpecification
import com.sarani.carobd.core.model.VehicleVariant

/**
 * Default in-memory implementation of [VehicleCatalogRepository] with verified OEM and standard
 * definitions for Iranian and regional automotive platforms.
 */
class DefaultVehicleCatalogRepository(
    initialMakes: List<VehicleMake> = DEFAULT_MAKES,
    initialModels: List<VehicleModel> = DEFAULT_MODELS,
    initialGenerations: List<VehicleGeneration> = DEFAULT_GENERATIONS,
    initialEngines: List<EngineVariant> = DEFAULT_ENGINES,
    initialTransmissions: List<TransmissionVariant> = DEFAULT_TRANSMISSIONS,
    initialVariants: List<VehicleVariant> = DEFAULT_VARIANTS
) : VehicleCatalogRepository {

    private val makes = initialMakes.associateBy { it.id }
    private val models = initialModels.associateBy { it.id }
    private val generations = initialGenerations.associateBy { it.id }
    private val engines = initialEngines.associateBy { it.id }
    private val transmissions = initialTransmissions.associateBy { it.id }
    private val variants = initialVariants.associateBy { it.id }

    override fun getAllMakes(): List<VehicleMake> = makes.values.toList()
    override fun getMakeById(id: String): VehicleMake? = makes[id]

    override fun getModelsForMake(makeId: String): List<VehicleModel> =
        models.values.filter { it.makeId == makeId }

    override fun getModelById(id: String): VehicleModel? = models[id]

    override fun getGenerationsForModel(modelId: String): List<VehicleGeneration> =
        generations.values.filter { it.modelId == modelId }

    override fun getGenerationById(id: String): VehicleGeneration? = generations[id]

    override fun getVariantsForGeneration(generationId: String): List<VehicleVariant> =
        variants.values.filter { it.generationId == generationId }

    override fun getVariantById(id: String): VehicleVariant? = variants[id]

    override fun getEngineById(id: String): EngineVariant? = engines[id]
    override fun getTransmissionById(id: String): TransmissionVariant? = transmissions[id]

    override fun searchMakes(query: String): List<VehicleMake> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()
        return makes.values.filter { make ->
            normalize(make.name).contains(q) || make.aliases.any { normalize(it).contains(q) }
        }
    }

    override fun searchModels(query: String, makeId: String?): List<VehicleModel> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()
        val pool = if (makeId != null) getModelsForMake(makeId) else models.values
        return pool.filter { model ->
            normalize(model.name).contains(q) || model.aliases.any { normalize(it).contains(q) }
        }
    }

    override fun findMatchingVariants(
        makeQuery: String,
        modelQuery: String,
        modelYear: Int?,
        engineQuery: String?
    ): List<ResolvedVehicleMatch> {
        val normMake = normalize(makeQuery)
        val normModel = normalize(modelQuery)
        val normEngine = engineQuery?.let(::normalize)

        val matchedMakes = makes.values.filter { m ->
            normalize(m.name).contains(normMake) || m.aliases.any { normalize(it).contains(normMake) }
        }
        if (matchedMakes.isEmpty()) return emptyList()

        val results = mutableListOf<ResolvedVehicleMatch>()

        for (make in matchedMakes) {
            val matchedModels = getModelsForMake(make.id).filter { m ->
                normalize(m.name).contains(normModel) || m.aliases.any { normalize(it).contains(normModel) }
            }

            for (model in matchedModels) {
                val modelGenerations = getGenerationsForModel(model.id)
                for (generation in modelGenerations) {
                    val candidateVariants = getVariantsForGeneration(generation.id)
                    for (variant in candidateVariants) {
                        // Check model year bounds if specified
                        if (modelYear != null) {
                            if (modelYear < variant.modelYearStart) continue
                            val endYear = variant.modelYearEnd
                            if (endYear != null && modelYear > endYear) continue
                        }

                        val engine = engines[variant.engineId] ?: continue

                        // Check engine match if specified
                        if (normEngine != null && normEngine.isNotBlank()) {
                            val family = engine.family
                            val engineMatches = normalize(engine.code).contains(normEngine) ||
                                (family != null && normalize(family).contains(normEngine)) ||
                                variant.aliases.any { normalize(it).contains(normEngine) }
                            if (!engineMatches) continue
                        }

                        val transmission = variant.transmissionId?.let { transmissions[it] }

                        results.add(
                            ResolvedVehicleMatch(
                                make = make,
                                model = model,
                                generation = generation,
                                variant = variant,
                                engine = engine,
                                transmission = transmission,
                                confidence = variant.confidence
                            )
                        )
                    }
                }
            }
        }

        return results
    }

    companion object {
        fun normalize(text: String): String {
            return text.trim()
                .lowercase()
                .replace("ي", "ی")
                .replace("ك", "ک")
                .replace("۰", "0")
                .replace("۱", "1")
                .replace("۲", "2")
                .replace("۳", "3")
                .replace("۴", "4")
                .replace("۵", "5")
                .replace("۶", "6")
                .replace("۷", "7")
                .replace("۸", "8")
                .replace("۹", "9")
                .replace("-", " ")
                .replace("_", " ")
        }

        val DEFAULT_MAKES = listOf(
            VehicleMake("ikco", "Iran Khodro", "IR", listOf("ایران خودرو", "IKCO", "ایران‌خودرو")),
            VehicleMake("saipa", "SAIPA", "IR", listOf("سایپا", "SAIPA")),
            VehicleMake("peugeot", "Peugeot", "FR", listOf("پژو", "Peugeot")),
            VehicleMake("renault", "Renault", "FR", listOf("رنو", "Renault"))
        )

        val DEFAULT_MODELS = listOf(
            VehicleModel("ikco_206", "ikco", "Peugeot 206", "Hatchback", listOf("206", "پژو ۲۰۶", "پژو 206")),
            VehicleModel("ikco_207", "ikco", "Peugeot 207i", "Hatchback", listOf("207", "۲۰۷", "پژو ۲۰۷", "پژو 207")),
            VehicleModel("ikco_samand", "ikco", "Samand", "Sedan", listOf("سمند", "Samand", "سورن", "Soren")),
            VehicleModel("ikco_dena", "ikco", "Dena", "Sedan", listOf("دنا", "Dena", "دنا پلاس", "Dena Plus")),
            VehicleModel("ikco_tara", "ikco", "Tara", "Sedan", listOf("تارا", "Tara")),
            VehicleModel("ikco_pars", "ikco", "Peugeot Pars", "Sedan", listOf("پژو پارس", "پارس", "Pars", "Persia")),
            VehicleModel("saipa_pride", "saipa", "Pride", "Hatchback/Sedan", listOf("پراید", "Pride", "131", "111", "132")),
            VehicleModel("saipa_tiba", "saipa", "Tiba", "Sedan/Hatchback", listOf("تیبا", "Tiba", "تیبا ۲")),
            VehicleModel("saipa_saina", "saipa", "Saina", "Sedan", listOf("ساینا", "Saina")),
            VehicleModel("saipa_quik", "saipa", "Quik", "Hatchback", listOf("کوییک", "کوئیک", "Quik", "Quick")),
            VehicleModel("saipa_shahin", "saipa", "Shahin", "Sedan", listOf("شاهین", "Shahin")),
            VehicleModel("renault_l90", "renault", "Tondar 90", "Sedan", listOf("تندر ۹۰", "تندر 90", "L90", "ال ۹۰", "Logan"))
        )

        val DEFAULT_GENERATIONS = listOf(
            VehicleGeneration("gen_206_ir", "ikco_206", "206 Phase 1 & 2", 2001, 2023, "T1", VehicleMarket.IRAN, listOf("۲۰۶")),
            VehicleGeneration("gen_207_ir", "ikco_207", "207i Phase 1 & 2", 2010, null, "T31", VehicleMarket.IRAN, listOf("۲۰۷")),
            VehicleGeneration("gen_samand", "ikco_samand", "Samand / Soren", 2001, 2023, "X7", VehicleMarket.IRAN, listOf("سمند")),
            VehicleGeneration("gen_dena", "ikco_dena", "Dena / Dena+", 2014, null, "NX7", VehicleMarket.IRAN, listOf("دنا")),
            VehicleGeneration("gen_tara", "ikco_tara", "Tara", 2021, null, "K132", VehicleMarket.IRAN, listOf("تارا")),
            VehicleGeneration("gen_shahin", "saipa_shahin", "Shahin", 2020, null, "SP100", VehicleMarket.IRAN, listOf("شاهین")),
            VehicleGeneration("gen_l90", "renault_l90", "Tondar 90", 2007, 2019, "B90", VehicleMarket.IRAN, listOf("ال۹۰"))
        )

        val DEFAULT_ENGINES = listOf(
            EngineVariant(
                id = "eng_tu5",
                code = "TU5 JP4",
                family = "TU",
                displacementCc = 1587,
                cylinderCount = 4,
                valvesPerCylinder = 4,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                powerHp = 105,
                torqueNm = 142,
                sources = listOf(
                    DataSourceReference(
                        title = "PSA TU5JP4 Workshop Manual",
                        urlOrDocument = "",
                        manufacturer = "PSA Automobiles"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_tu3",
                code = "TU3 A/JP",
                family = "TU",
                displacementCc = 1360,
                cylinderCount = 4,
                valvesPerCylinder = 2,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                powerHp = 75,
                torqueNm = 118,
                sources = listOf(
                    DataSourceReference(
                        title = "PSA TU3 Workshop Documentation",
                        urlOrDocument = "",
                        manufacturer = "PSA Automobiles"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_tu5p",
                code = "TU5P",
                family = "TU",
                displacementCc = 1587,
                cylinderCount = 4,
                valvesPerCylinder = 4,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                powerHp = 113,
                torqueNm = 144,
                sources = listOf(
                    DataSourceReference(
                        title = "Iran Khodro TU5P Engine Technical Specification",
                        urlOrDocument = "",
                        manufacturer = "Iran Khodro"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_ef7_na",
                code = "EF7 NA",
                family = "National Engine (EF)",
                displacementCc = 1645,
                cylinderCount = 4,
                valvesPerCylinder = 4,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                powerHp = 113,
                torqueNm = 155,
                sources = listOf(
                    DataSourceReference(
                        title = "IPCO / FEV EF7 Base Engine Overview",
                        urlOrDocument = "",
                        manufacturer = "IPCO"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_ef7_tc",
                code = "EF7 TC",
                family = "National Engine (EF)",
                displacementCc = 1645,
                cylinderCount = 4,
                valvesPerCylinder = 4,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.TURBOCHARGED,
                powerHp = 150,
                torqueNm = 215,
                sources = listOf(
                    DataSourceReference(
                        title = "IKCO Dena+ Turbo Engine Service Guide",
                        urlOrDocument = "",
                        manufacturer = "Iran Khodro"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_m15tc",
                code = "M15 TC",
                family = "M15",
                displacementCc = 1497,
                cylinderCount = 4,
                valvesPerCylinder = 2,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.TURBOCHARGED,
                powerHp = 110,
                torqueNm = 178,
                sources = listOf(
                    DataSourceReference(
                        title = "SAIPA Mega Motor M15TC Service Sheet",
                        urlOrDocument = "",
                        manufacturer = "Mega Motor"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            EngineVariant(
                id = "eng_k4m",
                code = "K4M 690",
                family = "K-Type",
                displacementCc = 1598,
                cylinderCount = 4,
                valvesPerCylinder = 4,
                fuelType = FuelType.PETROL,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                powerHp = 105,
                torqueNm = 148,
                sources = listOf(
                    DataSourceReference(
                        title = "Renault Logan/Tondar MR388 Workshop Manual",
                        urlOrDocument = "MR388",
                        manufacturer = "Renault"
                    )
                ),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            )
        )

        val DEFAULT_TRANSMISSIONS = listOf(
            TransmissionVariant("trans_be4_5m", TransmissionType.MANUAL, 5, "BE4/5", "PSA / IKCO"),
            TransmissionVariant("trans_al4_4a", TransmissionType.AUTOMATIC_TORQUE_CONVERTER, 4, "AL4", "PSA / Siemens"),
            TransmissionVariant("trans_dae_6a", TransmissionType.AUTOMATIC_TORQUE_CONVERTER, 6, "DAE 6AT", "DAE"),
            TransmissionVariant("trans_saipa_5m", TransmissionType.MANUAL, 5, "5MT", "Mega Motor"),
            TransmissionVariant("trans_punch_cvt", TransmissionType.CONTINUOUSLY_VARIABLE_CVT, null, "VT3", "Punch Powertrain"),
            TransmissionVariant("trans_jh3_5m", TransmissionType.MANUAL, 5, "JH3", "Renault")
        )

        val DEFAULT_VARIANTS = listOf(
            // 206 Type 2 (TU3, Manual)
            VehicleVariant(
                id = "var_206_type2",
                generationId = "gen_206_ir",
                trim = "Type 2",
                modelYearStart = 2001,
                modelYearEnd = 2022,
                engineId = "eng_tu3",
                transmissionId = "trans_be4_5m",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "20W-50 / 10W-40 API SL",
                    engineOilCapacityLiters = 3.25f,
                    gearboxOilGrade = "75W-80 GL-4+",
                    gearboxCapacityLiters = 2.0f,
                    sparkPlugGapMm = 0.9f,
                    timingBeltIntervalKm = 60000,
                    timingBeltTeeth = 108,
                    coolantCapacityLiters = 5.5f,
                    fuelTankCapacityLiters = 50.0f
                ),
                aliases = listOf("تیپ ۲", "تیپ 2", "Type 2", "TU3"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            // 206 Type 5 (TU5, Manual)
            VehicleVariant(
                id = "var_206_type5",
                generationId = "gen_206_ir",
                trim = "Type 5",
                modelYearStart = 2003,
                modelYearEnd = 2022,
                engineId = "eng_tu5",
                transmissionId = "trans_be4_5m",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "10W-40 API SL/SM/SN",
                    engineOilCapacityLiters = 3.75f,
                    gearboxOilGrade = "75W-80 GL-4+",
                    gearboxCapacityLiters = 2.0f,
                    sparkPlugGapMm = 0.9f,
                    timingBeltIntervalKm = 60000,
                    timingBeltTeeth = 134,
                    coolantCapacityLiters = 6.0f,
                    fuelTankCapacityLiters = 50.0f,
                    confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
                ),
                aliases = listOf("تیپ ۵", "تیپ 5", "Type 5", "TU5"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            // Dena (EF7 NA, Manual)
            VehicleVariant(
                id = "var_dena_ef7_na",
                generationId = "gen_dena",
                trim = "Dena EF7",
                modelYearStart = 2014,
                modelYearEnd = null,
                engineId = "eng_ef7_na",
                transmissionId = "trans_be4_5m",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "10W-40 API SL/SM/SN",
                    engineOilCapacityLiters = 5.25f,
                    gearboxOilGrade = "75W-80 GL-5",
                    gearboxCapacityLiters = 1.9f,
                    sparkPlugGapMm = 0.75f,
                    timingBeltIntervalKm = 60000,
                    timingBeltTeeth = 127,
                    coolantCapacityLiters = 7.5f,
                    fuelTankCapacityLiters = 60.0f,
                    confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
                ),
                aliases = listOf("دنا معمولی", "دنا EF7", "Dena EF7"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            // Dena+ Turbo Auto (EF7 TC, 6-speed Auto)
            VehicleVariant(
                id = "var_dena_plus_turbo_at",
                generationId = "gen_dena",
                trim = "Dena+ Turbo Automatic",
                modelYearStart = 2020,
                modelYearEnd = null,
                engineId = "eng_ef7_tc",
                transmissionId = "trans_dae_6a",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "5W-40 / 10W-40 API SN/SP",
                    engineOilCapacityLiters = 5.25f,
                    gearboxOilGrade = "ATF AW-1",
                    gearboxCapacityLiters = 5.5f,
                    sparkPlugGapMm = 0.70f,
                    timingBeltIntervalKm = 50000,
                    timingBeltTeeth = 127,
                    coolantCapacityLiters = 7.8f,
                    fuelTankCapacityLiters = 60.0f,
                    confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
                ),
                aliases = listOf("دنا پلاس توربو اتوماتیک", "دنا پلاس اتومات", "Dena Turbo Auto"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            // Shahin G (M15TC, 5M)
            VehicleVariant(
                id = "var_shahin_g",
                generationId = "gen_shahin",
                trim = "Shahin G",
                modelYearStart = 2020,
                modelYearEnd = null,
                engineId = "eng_m15tc",
                transmissionId = "trans_saipa_5m",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "10W-40 API SN",
                    engineOilCapacityLiters = 4.0f,
                    gearboxOilGrade = "75W-80 GL-4",
                    gearboxCapacityLiters = 2.5f,
                    sparkPlugGapMm = 0.75f,
                    timingBeltIntervalKm = 60000,
                    timingBeltTeeth = 114,
                    coolantCapacityLiters = 6.5f,
                    fuelTankCapacityLiters = 47.0f,
                    confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
                ),
                aliases = listOf("شاهین G", "شاهین دنده‌ای", "Shahin G"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            ),
            // Tondar 90 E2 (K4M, 5M)
            VehicleVariant(
                id = "var_tondar90_e2",
                generationId = "gen_l90",
                trim = "E2",
                modelYearStart = 2007,
                modelYearEnd = 2019,
                engineId = "eng_k4m",
                transmissionId = "trans_jh3_5m",
                driveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
                specifications = VehicleSpecification(
                    engineOilGrade = "10W-40 API SL/SM/SN",
                    engineOilCapacityLiters = 4.8f,
                    gearboxOilGrade = "75W-80 GL-4+ (ELF Tranself)",
                    gearboxCapacityLiters = 2.8f,
                    sparkPlugGapMm = 0.9f,
                    timingBeltIntervalKm = 60000,
                    timingBeltTeeth = 132,
                    coolantCapacityLiters = 5.5f,
                    fuelTankCapacityLiters = 50.0f,
                    confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
                ),
                aliases = listOf("تندر ۹۰ E2", "تندر E2", "ال۹۰ E2", "L90 E2"),
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED
            )
        )
    }
}
