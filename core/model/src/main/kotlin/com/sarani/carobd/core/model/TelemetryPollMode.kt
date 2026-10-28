package com.sarani.carobd.core.model

/**
 * Persisted telemetry polling cadence (§7). Replaces the placebo "refresh
 * rate" selector that was never persisted/wired. The actual polling scheduler
 * reads this value — UI redraw remains decoupled and may be faster/slower
 * than vehicle polling.
 *
 * Bounds are intentionally modest: no millisecond-level spam. The UI redraw
 * rate is independent of these and is not driven by this value.
 */
enum class TelemetryPollMode(
    val delayMs: Long,
    val titleEn: String,
    val titleFa: String
) {
    /** ~2s cadence — slowest, lowest bus load / battery impact. */
    ECO(delayMs = 2000L, titleEn = "Eco (2.0s)", titleFa = "صرفه\u200Cجویی (۲٫۰ث)"),
    /** ~1s cadence — default balance. */
    NORMAL(delayMs = 1000L, titleEn = "Normal (1.0s)", titleFa = "عادی (۱٫۰ث)"),
    /** ~0.5s cadence — fastest, for active diagnostics. */
    FAST(delayMs = 500L, titleEn = "Fast (0.5s)", titleFa = "سریع (۰٫۵ث)");

    companion object {
        /**
         * Safe parse from a stored name. Unknown/invalid names degrade to [NORMAL]
         * (§7: invalid value clamps safely) rather than crashing or spamming.
         */
        fun fromStored(name: String?): TelemetryPollMode =
            name?.takeIf { it.isNotBlank() }?.let { v ->
                values().find { it.name == v }
            } ?: NORMAL
    }
}
