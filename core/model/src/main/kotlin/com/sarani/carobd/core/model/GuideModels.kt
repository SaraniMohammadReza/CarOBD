package com.sarani.carobd.core.model

/**
 * Top-level sections organizing the CarOBD User Guide.
 */
enum class GuideSection(
    val titleEn: String,
    val titleFa: String
) {
    GETTING_STARTED("Getting Started", "شروع کار"),
    OWNERSHIP("My Vehicle & Specs", "خودروی من و مشخصات"),
    MAINTENANCE("Maintenance & Reminders", "سرویس و نگهداری"),
    PARTS("Parts & Compatibility", "قطعات و سازگاری"),
    CONNECTIVITY("OBD & Adapters", "اتصال و دیاگ"),
    DIAGNOSTICS("Diagnostics & DTCs", "عیب‌یابی و کد خطا"),
    TELEMETRY("Live Telemetry & Dashboard", "داده‌های زنده و داشبورد"),
    RECORDS_SETTINGS("Reports, Privacy & Settings", "گزارش‌ها، حریم خصوصی و تنظیمات")
}

enum class GuideAlertType {
    INFO,
    TIP,
    WARNING,
    CAUTION
}

data class GuideKeyValue(
    val keyEn: String,
    val keyFa: String,
    val valueEn: String,
    val valueFa: String
)

sealed interface GuideContentBlock {
    data class Paragraph(
        val textEn: String,
        val textFa: String
    ) : GuideContentBlock

    data class Alert(
        val type: GuideAlertType,
        val titleEn: String,
        val titleFa: String,
        val messageEn: String,
        val messageFa: String
    ) : GuideContentBlock

    data class BulletList(
        val titleEn: String? = null,
        val titleFa: String? = null,
        val itemsEn: List<String>,
        val itemsFa: List<String>
    ) : GuideContentBlock

    data class KeyValues(
        val titleEn: String? = null,
        val titleFa: String? = null,
        val entries: List<GuideKeyValue>
    ) : GuideContentBlock

    data class CodeBlock(
        val code: String,
        val captionEn: String? = null,
        val captionFa: String? = null
    ) : GuideContentBlock
}

data class GuideTopic(
    val id: String,
    val section: GuideSection,
    val titleEn: String,
    val titleFa: String,
    val summaryEn: String,
    val summaryFa: String,
    val blocks: List<GuideContentBlock>,
    val tags: List<String> = emptyList(),
    val relatedTopicIds: List<String> = emptyList()
)
