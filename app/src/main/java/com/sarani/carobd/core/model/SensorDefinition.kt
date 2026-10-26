package com.sarani.carobd.core.model

enum class SensorCategory(val titleFa: String, val titleEn: String) {
    ENGINE("موتور و احتراق", "Engine & Combustion"),
    FUEL_AIR("سوخت و هوای ورودی", "Fuel & Air Intake"),
    TEMPERATURE("دماها و حرارت", "Temperatures"),
    ELECTRICAL("برق و باتری", "Electrical & Voltage"),
    EMISSIONS("آلایندگی و اگزوز", "Emissions & Exhaust"),
    TRANSMISSION("گیربکس و انتقال قدرت", "Transmission & Drivetrain"),
    IRANIAN_ECU("اختصاصی خودروهای ایرانی", "Iranian ECU Proprietary"),
    MOTORCYCLE("موتورسیکلت و خودروهای چینی", "Motorcycle & Chinese ECUs")
}

enum class WidgetType(val titleFa: String) {
    CIRCULAR_GAUGE("گیج عقربه‌ای دایره‌ای"),
    DIGITAL_CARD("کارت دیجیتال"),
    BAR_GAUGE("گیج نواری افقی"),
    OSCILLOSCOPE_GRAPH("نمودار زنده نوسان‌نگار")
}

enum class WidgetSize(val spanCount: Int) {
    SMALL(1),      // Half row (compact)
    MEDIUM(1),     // Half row (standard)
    LARGE(2)       // Full width
}

data class DashboardWidgetConfig(
    val id: String,
    val sensorId: String,
    val widgetType: WidgetType = WidgetType.CIRCULAR_GAUGE,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val customColorHex: String? = null
)

data class SensorDefinition(
    val id: String,
    val pid: String,
    val nameEn: String,
    val nameFa: String,
    val shortName: String,
    val unitMetric: String,
    val unitImperial: String,
    val minValue: Float,
    val maxValue: Float,
    val category: SensorCategory,
    val descriptionFa: String,
    val formulaFormulaDescription: String = ""
)
