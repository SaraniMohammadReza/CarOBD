package com.sarani.carobd.core.vehicle.parts

import com.sarani.carobd.core.model.AftermarketPartReference
import com.sarani.carobd.core.model.CompatiblePartItem
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DataSourceReference
import com.sarani.carobd.core.model.OemPartNumber
import com.sarani.carobd.core.model.PartCategory
import com.sarani.carobd.core.model.PartDefinition
import com.sarani.carobd.core.model.PartSpecification
import com.sarani.carobd.core.model.VehicleFitment

class DefaultPartsRepository(
    private val definitions: List<PartDefinition> = DEFAULT_DEFINITIONS,
    private val fitments: List<VehicleFitment> = DEFAULT_FITMENTS
) : PartsRepository {

    override fun getCategories(): List<PartCategory> =
        definitions.map { it.category }.distinct()

    override fun getPartDefinition(id: String): PartDefinition? =
        definitions.firstOrNull { it.id == id }

    override fun findCompatibleParts(
        vehicleMakeId: String,
        vehicleModelId: String,
        generationId: String?,
        engineId: String?,
        engineCode: String?,
        engineFamily: String?,
        modelYear: Int?,
        category: PartCategory?
    ): List<CompatiblePartItem> {
        return fitments.filter { fitment ->
            matchesFitment(
                fitment = fitment,
                vehicleMakeId = vehicleMakeId,
                vehicleModelId = vehicleModelId,
                generationId = generationId,
                engineId = engineId,
                engineCode = engineCode,
                engineFamily = engineFamily,
                modelYear = modelYear
            )
        }.mapNotNull { fitment ->
            val def = definitions.firstOrNull { it.id == fitment.partDefinitionId } ?: return@mapNotNull null
            if (category != null && def.category != category) {
                return@mapNotNull null
            }
            CompatiblePartItem(
                definition = def,
                fitment = fitment,
                confidence = fitment.confidence
            )
        }
    }

    override fun searchParts(query: String): List<CompatiblePartItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return emptyList()

        return fitments.mapNotNull { fitment ->
            val def = definitions.firstOrNull { it.id == fitment.partDefinitionId } ?: return@mapNotNull null

            val matchesDefinition = def.canonicalNameEn.lowercase().contains(trimmed) ||
                def.canonicalNameFa.contains(trimmed) ||
                def.descriptionEn.lowercase().contains(trimmed) ||
                def.descriptionFa.contains(trimmed)

            val matchesOem = fitment.oemNumbers.any { oem ->
                oem.partNumber.lowercase().replace(" ", "").contains(trimmed.replace(" ", "")) ||
                    oem.manufacturer.lowercase().contains(trimmed)
            }

            val matchesAftermarket = fitment.aftermarketReferences.any { aft ->
                aft.brand.lowercase().contains(trimmed) ||
                    aft.partNumber.lowercase().replace(" ", "").contains(trimmed.replace(" ", ""))
            }

            if (matchesDefinition || matchesOem || matchesAftermarket) {
                CompatiblePartItem(
                    definition = def,
                    fitment = fitment,
                    confidence = fitment.confidence
                )
            } else {
                null
            }
        }
    }

    private fun matchesFitment(
        fitment: VehicleFitment,
        vehicleMakeId: String,
        vehicleModelId: String,
        generationId: String?,
        engineId: String?,
        engineCode: String?,
        engineFamily: String?,
        modelYear: Int?
    ): Boolean {
        // 1. Make check
        if (fitment.vehicleMakeId.isNotBlank() &&
            !fitment.vehicleMakeId.equals(vehicleMakeId, ignoreCase = true)
        ) {
            return false
        }

        // 2. Model check
        if (fitment.vehicleModelId.isNotBlank() &&
            !fitment.vehicleModelId.equals(vehicleModelId, ignoreCase = true)
        ) {
            return false
        }

        // 3. Generation check (if specified in fitment)
        if (fitment.vehicleGenerationId != null && generationId != null &&
            !fitment.vehicleGenerationId.equals(generationId, ignoreCase = true)
        ) {
            return false
        }

        // 4. Strict Engine Constraints: Never guess across engine variants
        // If the fitment requires a specific engineId, the vehicle MUST provide a matching engineId
        val fitmentEngineId = fitment.engineId
        if (fitmentEngineId != null) {
            if (engineId == null || !fitmentEngineId.equals(engineId, ignoreCase = true)) {
                return false
            }
        }

        // If the fitment requires a specific engineCode, the vehicle MUST match
        val fitmentEngineCode = fitment.engineCode
        if (fitmentEngineCode != null) {
            if (engineCode == null || !engineCode.contains(fitmentEngineCode, ignoreCase = true)) {
                return false
            }
        }

        // If the fitment requires a specific engineFamily, the vehicle MUST match
        val fitmentEngineFamily = fitment.engineFamily
        if (fitmentEngineFamily != null) {
            if (engineFamily == null || !engineFamily.contains(fitmentEngineFamily, ignoreCase = true)) {
                return false
            }
        }

        // 5. Model Year Range check
        if (modelYear != null) {
            val minYear = fitment.minYear
            if (minYear != null && modelYear < minYear) {
                return false
            }
            val maxYear = fitment.maxYear
            if (maxYear != null && modelYear > maxYear) {
                return false
            }
        }

        return true
    }

    companion object {
        val DEFAULT_DEFINITIONS = listOf(
            PartDefinition(
                id = "part_spark_plug",
                category = PartCategory.SPARK_PLUG,
                canonicalNameEn = "Spark Plug",
                canonicalNameFa = "شمع موتور",
                descriptionEn = "Engine ignition spark plug with specified heat range and electrode gap.",
                descriptionFa = "شمع جرقه‌زن موتور با درجه حرارتی و دهانه مشخص.",
                isSafetyCritical = true,
                iconKey = "spark_plug"
            ),
            PartDefinition(
                id = "part_oil_filter",
                category = PartCategory.OIL_FILTER,
                canonicalNameEn = "Engine Oil Filter",
                canonicalNameFa = "فیلتر روغن موتور",
                descriptionEn = "Lubrication system filter for trapping combustion debris and metal particles.",
                descriptionFa = "فیلتر مدار روغن‌کاری برای تصفیه براده‌ها و دوده ناشی از احتراق.",
                isSafetyCritical = true,
                iconKey = "oil_filter"
            ),
            PartDefinition(
                id = "part_air_filter",
                category = PartCategory.AIR_FILTER,
                canonicalNameEn = "Air Intake Filter",
                canonicalNameFa = "فیلتر هوای موتور",
                descriptionEn = "Engine air intake filter preventing particulate ingestion.",
                descriptionFa = "فیلتر هوای ورودی به منیفولد موتور جهت جلوگیری از ورود گرد و خاک.",
                isSafetyCritical = false,
                iconKey = "air_filter"
            ),
            PartDefinition(
                id = "part_cabin_filter",
                category = PartCategory.CABIN_FILTER,
                canonicalNameEn = "Cabin Air Filter",
                canonicalNameFa = "فیلتر هوای اتاق",
                descriptionEn = "Ventilation and HVAC cabin air particulate filter.",
                descriptionFa = "فیلتر تهویه و سیستم سرمایش/گرمایش کابین سرنشینان.",
                isSafetyCritical = false,
                iconKey = "cabin_filter"
            ),
            PartDefinition(
                id = "part_fuel_filter",
                category = PartCategory.FUEL_FILTER,
                canonicalNameEn = "Fuel Filter",
                canonicalNameFa = "فیلتر بنزین",
                descriptionEn = "Inline fuel system particulate and sediment filter.",
                descriptionFa = "فیلتر بنزین خط سوخت‌رسانی جهت محافظت از سوزن‌های انژکتور.",
                isSafetyCritical = false,
                iconKey = "fuel_filter"
            ),
            PartDefinition(
                id = "part_timing_belt",
                category = PartCategory.TIMING_BELT,
                canonicalNameEn = "Timing Belt Kit",
                canonicalNameFa = "کیت تسمه تایم",
                descriptionEn = "Toothed camshaft timing drive belt and tensioner pulley.",
                descriptionFa = "تسمه دندانه‌دار تایمینگ میل‌سوپاپ به همراه هرزگرد و تسمه‌سفت‌کن.",
                isSafetyCritical = true,
                iconKey = "timing_belt"
            ),
            PartDefinition(
                id = "part_brake_pads_front",
                category = PartCategory.BRAKE_PADS,
                canonicalNameEn = "Front Brake Pads",
                canonicalNameFa = "لنت ترمز چرخ‌های جلو",
                descriptionEn = "Friction brake pads for front axle disc brakes.",
                descriptionFa = "لقمه لنت‌های اصطکاکی ترمز دیسکی چرخ‌های محور جلو.",
                isSafetyCritical = true,
                iconKey = "brake_disc"
            ),
            PartDefinition(
                id = "part_battery",
                category = PartCategory.BATTERY,
                canonicalNameEn = "12V Automotive Battery",
                canonicalNameFa = "باتری ۱۲ ولت استارت",
                descriptionEn = "Lead-acid starting, lighting and ignition 12V storage battery.",
                descriptionFa = "باتری اسیدی یا اتمی ۱۲ ولت جهت استارت موتور و برق خودرو.",
                isSafetyCritical = true,
                iconKey = "battery"
            ),
            PartDefinition(
                id = "part_engine_oil",
                category = PartCategory.ENGINE_OIL,
                canonicalNameEn = "Engine Lubricating Oil",
                canonicalNameFa = "روغن روان‌کننده موتور",
                descriptionEn = "Multi-grade internal combustion engine lubricating fluid.",
                descriptionFa = "روغن چنددرجه‌ای روان‌کاری قطعات متحرک درون موتور خودرو.",
                isSafetyCritical = true,
                iconKey = "engine_oil"
            ),
            PartDefinition(
                id = "part_coolant",
                category = PartCategory.COOLANT,
                canonicalNameEn = "Engine Coolant / Antifreeze",
                canonicalNameFa = "مایع خنک‌کننده و ضدیخ",
                descriptionEn = "Ethylene glycol engine thermal management coolant solution.",
                descriptionFa = "محلول اتیلن گلیکول خنک‌کننده مدار رادیاتور و ضدیخ موتور.",
                isSafetyCritical = true,
                iconKey = "coolant"
            ),
            PartDefinition(
                id = "part_transmission_fluid",
                category = PartCategory.TRANSMISSION_FLUID,
                canonicalNameEn = "Transmission Gear Fluid",
                canonicalNameFa = "روغن گیربکس و دیفرانسیل",
                descriptionEn = "Manual or automatic gearbox lubricant formulated for synchronizers or torque converters.",
                descriptionFa = "روغن دنده یا مایع هیدرولیک گیربکس اتوماتیک جهت روان‌کاری چرخ‌دنده‌ها.",
                isSafetyCritical = true,
                iconKey = "transmission"
            )
        )

        val DEFAULT_FITMENTS = listOf(
            // -------------------------------------------------------------
            // Renault Tondar 90 (K4M Engine) — VERIFIED OEM (Renault MR388)
            // -------------------------------------------------------------
            VehicleFitment(
                id = "fit_l90_spark_plug",
                partDefinitionId = "part_spark_plug",
                vehicleMakeId = "renault",
                vehicleModelId = "renault_l90",
                vehicleGenerationId = "gen_l90",
                engineId = "eng_k4m",
                engineCode = "K4M",
                engineFamily = "K-Type",
                minYear = 2007,
                maxYear = 2019,
                notesEn = "Pre-gapped to 0.9 mm; dual ground electrode recommended for K4M 16V.",
                notesFa = "دهانه شمع ۰.۹ میلی‌متر؛ شمع دو پلاتین برای موتور K4M توصیه می‌شود.",
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("Renault Logan Workshop Manual MR388", "MR388", "Renault"),
                oemNumbers = listOf(
                    OemPartNumber("77 00 500 168", "Renault", true),
                    OemPartNumber("224018651R", "Renault", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Eyquem", "RFC58LZ2E", "Standard OEM Supplier"),
                    AftermarketPartReference("Bosch", "FR7DC+", "Super Plus"),
                    AftermarketPartReference("NGK", "BKR6E", "V-Power")
                ),
                specifications = listOf(
                    PartSpecification("gap", "Electrode Gap", "فاصله دهانه شمع", "0.9", "mm", ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("thread", "Thread Size", "رزوه پیچ", "M14 x 1.25", null, ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("hex", "Hex Size", "آچار شمع", "16", "mm", ConfidenceLevel.VERIFIED_OEM)
                )
            ),
            VehicleFitment(
                id = "fit_l90_oil_filter",
                partDefinitionId = "part_oil_filter",
                vehicleMakeId = "renault",
                vehicleModelId = "renault_l90",
                vehicleGenerationId = "gen_l90",
                engineId = "eng_k4m",
                engineCode = "K4M",
                minYear = 2007,
                maxYear = 2019,
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("Renault Logan Workshop Manual MR388", "MR388", "Renault"),
                oemNumbers = listOf(
                    OemPartNumber("77 00 274 177", "Renault", true),
                    OemPartNumber("82 00 768 913", "Renault", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Purflux", "LS218", "OEM Supplier"),
                    AftermarketPartReference("Mann-Filter", "W 75/3", "Spin-on")
                ),
                specifications = listOf(
                    PartSpecification("thread", "Thread Size", "رزوه اتصال", "M20 x 1.5", null, ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("type", "Filter Type", "نوع فیلتر", "Spin-on Canister", null, ConfidenceLevel.VERIFIED_OEM)
                )
            ),
            VehicleFitment(
                id = "fit_l90_air_filter",
                partDefinitionId = "part_air_filter",
                vehicleMakeId = "renault",
                vehicleModelId = "renault_l90",
                vehicleGenerationId = "gen_l90",
                engineId = "eng_k4m",
                engineCode = "K4M",
                minYear = 2007,
                maxYear = 2019,
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("Renault Logan Workshop Manual MR388", "MR388", "Renault"),
                oemNumbers = listOf(
                    OemPartNumber("82 00 431 051", "Renault", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Mann-Filter", "C 18 114", "Panel Air Filter"),
                    AftermarketPartReference("Purflux", "A1150", "Panel Air Filter")
                )
            ),
            VehicleFitment(
                id = "fit_l90_timing_belt",
                partDefinitionId = "part_timing_belt",
                vehicleMakeId = "renault",
                vehicleModelId = "renault_l90",
                vehicleGenerationId = "gen_l90",
                engineId = "eng_k4m",
                engineCode = "K4M",
                minYear = 2007,
                maxYear = 2019,
                notesEn = "Mandatory 60,000 km or 4-year replacement. Interference engine: failure causes catastrophic valve bend.",
                notesFa = "تعویض الزامی در ۶۰,۰۰۰ کیلومتر یا ۴ سال. موتور تداخلی بوده و بریدن تسمه به سوپاپ‌ها آسیب جدی می‌زند.",
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("Renault Logan Workshop Manual MR388", "MR388", "Renault"),
                oemNumbers = listOf(
                    OemPartNumber("13 0C 175 29R", "Renault", true),
                    OemPartNumber("77 01 477 028", "Renault", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Gates", "K015438XS", "PowerGrip Kit with Tensioner"),
                    AftermarketPartReference("Continental Contitech", "CT988K1", "Timing Belt Kit")
                ),
                specifications = listOf(
                    PartSpecification("teeth", "Tooth Count", "تعداد دندانه‌ها", "132", null, ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("width", "Belt Width", "عرض تسمه", "27", "mm", ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("interval", "Replacement Interval", "دوره تعویض", "60,000", "km", ConfidenceLevel.VERIFIED_OEM)
                )
            ),
            VehicleFitment(
                id = "fit_l90_brake_pads_front",
                partDefinitionId = "part_brake_pads_front",
                vehicleMakeId = "renault",
                vehicleModelId = "renault_l90",
                vehicleGenerationId = "gen_l90",
                minYear = 2007,
                maxYear = 2019,
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("Renault Logan Workshop Manual MR388", "MR388", "Renault"),
                oemNumbers = listOf(
                    OemPartNumber("41 06 021 92R", "Renault", true),
                    OemPartNumber("77 01 208 265", "Renault", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("TRW", "GDB1334", "Front Axle Pad Set"),
                    AftermarketPartReference("Ferodo", "FDB845", "Premier")
                )
            ),

            // -------------------------------------------------------------
            // Peugeot 206 (TU5 Engine) — VERIFIED OEM (PSA Documentation)
            // -------------------------------------------------------------
            VehicleFitment(
                id = "fit_206_tu5_spark_plug",
                partDefinitionId = "part_spark_plug",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_206",
                vehicleGenerationId = "gen_206_ir",
                engineId = "eng_tu5",
                engineCode = "TU5",
                engineFamily = "TU",
                minYear = 2003,
                maxYear = 2022,
                notesEn = "Electrode gap 0.9 mm; strictly designed for 16-valve TU5 head.",
                notesFa = "دهانه شمع ۰.۹ میلی‌متر؛ شمع پایه بلند مناسب سرسیلندر ۱۶ سوپاپ TU5.",
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("PSA TU5JP4 Workshop Manual", "TU5JP4-TECH", "PSA Automobiles"),
                oemNumbers = listOf(
                    OemPartNumber("5960 F3", "Peugeot", true),
                    OemPartNumber("5960 57", "Peugeot", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Bosch", "FR8ME", "Super 4"),
                    AftermarketPartReference("Eyquem", "RFN52HZ", "OEM Supplier"),
                    AftermarketPartReference("NGK", "LFR6B", "Standard")
                ),
                specifications = listOf(
                    PartSpecification("gap", "Electrode Gap", "فاصله دهانه شمع", "0.9", "mm", ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("reach", "Thread Reach", "طول رزوه (پایه)", "26.5", "mm", ConfidenceLevel.VERIFIED_OEM)
                )
            ),
            VehicleFitment(
                id = "fit_206_tu5_timing_belt",
                partDefinitionId = "part_timing_belt",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_206",
                vehicleGenerationId = "gen_206_ir",
                engineId = "eng_tu5",
                engineCode = "TU5",
                engineFamily = "TU",
                minYear = 2003,
                maxYear = 2022,
                notesEn = "134 teeth belt. NEVER substitute with 104/108 teeth TU3 belt.",
                notesFa = "تسمه ۱۳۴ دندانه. به هیچ وجه نباید با تسمه ۱۰۴ یا ۱۰۸ دندانه TU3 اشتباه شود.",
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("PSA TU5JP4 Workshop Manual", "TU5JP4-TECH", "PSA Automobiles"),
                oemNumbers = listOf(
                    OemPartNumber("0816 G4", "Peugeot", true),
                    OemPartNumber("0816 E0", "Peugeot", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Gates", "5581XS", "PowerGrip 134T"),
                    AftermarketPartReference("Continental Contitech", "CT1065", "Timing Belt 134T")
                ),
                specifications = listOf(
                    PartSpecification("teeth", "Tooth Count", "تعداد دندانه‌ها", "134", null, ConfidenceLevel.VERIFIED_OEM),
                    PartSpecification("width", "Belt Width", "عرض تسمه", "25.4", "mm", ConfidenceLevel.VERIFIED_OEM)
                )
            ),
            VehicleFitment(
                id = "fit_206_tu5_oil_filter",
                partDefinitionId = "part_oil_filter",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_206",
                vehicleGenerationId = "gen_206_ir",
                engineId = "eng_tu5",
                engineCode = "TU5",
                minYear = 2003,
                maxYear = 2022,
                confidence = ConfidenceLevel.VERIFIED_OEM,
                source = DataSourceReference("PSA TU5JP4 Workshop Manual", "TU5JP4-TECH", "PSA Automobiles"),
                oemNumbers = listOf(
                    OemPartNumber("1109 AL", "Peugeot", true),
                    OemPartNumber("1109 AH", "Peugeot", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Purflux", "L358A", "Eco Filter Cartridge"),
                    AftermarketPartReference("Mann-Filter", "HU 711/51 x", "Element")
                ),
                specifications = listOf(
                    PartSpecification("type", "Filter Type", "نوع فیلتر", "Cartridge Insert", null, ConfidenceLevel.VERIFIED_OEM)
                )
            ),

            // -------------------------------------------------------------
            // Peugeot 206 (TU3 Engine) — COMMUNITY CONFIRMED (PSA Workshop)
            // -------------------------------------------------------------
            VehicleFitment(
                id = "fit_206_tu3_spark_plug",
                partDefinitionId = "part_spark_plug",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_206",
                vehicleGenerationId = "gen_206_ir",
                engineId = "eng_tu3",
                engineCode = "TU3",
                engineFamily = "TU",
                minYear = 2001,
                maxYear = 2022,
                notesEn = "Electrode gap 0.8 mm; short reach thread for 8-valve TU3 head. NOT interchangeable with TU5.",
                notesFa = "دهانه شمع ۰.۸ میلی‌متر؛ شمع پایه کوتاه مناسب ۸ سوپاپ TU3. با شمع TU5 متفاوت است.",
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("PSA TU3 Workshop Documentation", "TU3-TECH", "PSA Automobiles"),
                oemNumbers = listOf(
                    OemPartNumber("5962 5c", "Peugeot", false),
                    OemPartNumber("5960 58", "Peugeot", false)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Eyquem", "RFC58LZ", "Short Reach OEM"),
                    AftermarketPartReference("Bosch", "FR7DC+", "Super Plus 0.8mm"),
                    AftermarketPartReference("NGK", "BKR6E", "Standard")
                ),
                specifications = listOf(
                    PartSpecification("gap", "Electrode Gap", "فاصله دهانه شمع", "0.8", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("reach", "Thread Reach", "طول رزوه (پایه)", "19.0", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            ),
            VehicleFitment(
                id = "fit_206_tu3_timing_belt",
                partDefinitionId = "part_timing_belt",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_206",
                vehicleGenerationId = "gen_206_ir",
                engineId = "eng_tu3",
                engineCode = "TU3",
                engineFamily = "TU",
                minYear = 2001,
                maxYear = 2022,
                notesEn = "104/108 teeth belt for 8-valve single overhead cam. DO NOT mount on 16V TU5.",
                notesFa = "تسمه ۱۰۴ یا ۱۰۸ دندانه تک میل‌سوپاپ TU3. هرگز بر روی موتور ۱۶ سوپاپ TU5 نصب نشود.",
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("PSA TU3 Workshop Documentation", "TU3-TECH", "PSA Automobiles"),
                oemNumbers = listOf(
                    OemPartNumber("0816 71", "Peugeot", false)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Gates", "5449XS", "PowerGrip 104T"),
                    AftermarketPartReference("Continental Contitech", "CT718", "Timing Belt")
                ),
                specifications = listOf(
                    PartSpecification("teeth", "Tooth Count", "تعداد دندانه‌ها", "104/108", null, ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("width", "Belt Width", "عرض تسمه", "17", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            ),

            // -------------------------------------------------------------
            // Dena / Samand (EF7 NA Engine) — COMMUNITY CONFIRMED (IKCO)
            // -------------------------------------------------------------
            VehicleFitment(
                id = "fit_dena_ef7_spark_plug",
                partDefinitionId = "part_spark_plug",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_dena",
                vehicleGenerationId = "gen_dena",
                engineId = "eng_ef7_na",
                engineCode = "EF7 NA",
                engineFamily = "National Engine (EF)",
                minYear = 2014,
                notesEn = "Recommended gap 0.75-0.85 mm for CNG/petrol bi-fuel dual map compatibility.",
                notesFa = "دهانه توصیه شده ۰.۷۵ تا ۰.۸۵ میلی‌متر برای کارکرد بهینه روی گاز و بنزین.",
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("IKCO EF7 Service Manual", "EF7-MANUAL", "Iran Khodro"),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Torch", "K6RTC", "Standard OEM"),
                    AftermarketPartReference("Bosch", "FR8DC+", "Super Plus"),
                    AftermarketPartReference("Champion", "RC9YC4", "Copper Plus")
                ),
                specifications = listOf(
                    PartSpecification("gap", "Electrode Gap", "فاصله دهانه شمع", "0.75 - 0.85", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("reach", "Thread Reach", "طول رزوه (پایه)", "19.0", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            ),
            VehicleFitment(
                id = "fit_dena_ef7_timing_belt",
                partDefinitionId = "part_timing_belt",
                vehicleMakeId = "ikco",
                vehicleModelId = "ikco_dena",
                vehicleGenerationId = "gen_dena",
                engineId = "eng_ef7_na",
                engineCode = "EF7 NA",
                engineFamily = "National Engine (EF)",
                minYear = 2014,
                notesEn = "127 teeth timing belt. CVVT intake camshaft gear. 60,000 km replacement.",
                notesFa = "تسمه تایم ۱۲۷ دندانه به همراه سیستم متغیر سوپاپ CVVT. تعویض در ۶۰,۰۰۰ کیلومتر.",
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("IKCO EF7 Service Manual", "EF7-MANUAL", "Iran Khodro"),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Continental Contitech", "CT1115", "127 Teeth"),
                    AftermarketPartReference("Gates", "5627XS", "PowerGrip 127T")
                ),
                specifications = listOf(
                    PartSpecification("teeth", "Tooth Count", "تعداد دندانه‌ها", "127", null, ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("width", "Belt Width", "عرض تسمه", "25.4", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            ),

            // -------------------------------------------------------------
            // Saipa Quik / Saina (M15 Engine) — COMMUNITY CONFIRMED
            // -------------------------------------------------------------
            VehicleFitment(
                id = "fit_quik_m15_spark_plug",
                partDefinitionId = "part_spark_plug",
                vehicleMakeId = "saipa",
                vehicleModelId = "saipa_quik",
                engineCode = "M15",
                engineFamily = "M15",
                minYear = 2017,
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("Saipa M15 Service Guide", "M15-GUIDE", "Mega Motor"),
                aftermarketReferences = listOf(
                    AftermarketPartReference("NGK", "BPR6ES", "Standard 0.8mm"),
                    AftermarketPartReference("Torch", "F6RTC", "Standard OEM Supplier")
                ),
                specifications = listOf(
                    PartSpecification("gap", "Electrode Gap", "فاصله دهانه شمع", "0.8", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("thread", "Thread Size", "سایز رزوه", "M14 x 1.25", null, ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            ),
            VehicleFitment(
                id = "fit_quik_m15_timing_belt",
                partDefinitionId = "part_timing_belt",
                vehicleMakeId = "saipa",
                vehicleModelId = "saipa_quik",
                engineCode = "M15",
                engineFamily = "M15",
                minYear = 2017,
                confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
                source = DataSourceReference("Saipa M15 Service Guide", "M15-GUIDE", "Mega Motor"),
                oemNumbers = listOf(
                    OemPartNumber("410493", "Saipa", true)
                ),
                aftermarketReferences = listOf(
                    AftermarketPartReference("Gates", "5407XS", "PowerGrip 107T"),
                    AftermarketPartReference("Dongil", "107 RU 22", "Super Star")
                ),
                specifications = listOf(
                    PartSpecification("teeth", "Tooth Count", "تعداد دندانه‌ها", "107", null, ConfidenceLevel.COMMUNITY_CONFIRMED),
                    PartSpecification("width", "Belt Width", "عرض تسمه", "22", "mm", ConfidenceLevel.COMMUNITY_CONFIRMED)
                )
            )
        )
    }
}
