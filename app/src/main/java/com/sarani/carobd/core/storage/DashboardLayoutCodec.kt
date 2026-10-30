package com.sarani.carobd.core.storage

import com.sarani.carobd.core.model.*

/** Local preference schema, not a command or sensor-definition importer. */
internal object DashboardLayoutCodec {
    const val MAX_WIDGETS = 24
    private val id = Regex("[A-Za-z0-9_-]{1,80}")
    fun encode(widgets: List<DashboardWidgetConfig>): String {
        require(widgets.size <= MAX_WIDGETS && widgets.map { it.id }.distinct().size == widgets.size)
        return widgets.joinToString("\n") {
            require(id.matches(it.id) && id.matches(it.sensorId))
            require(it.customColorHex == null || Regex("#[A-Fa-f0-9]{6}([A-Fa-f0-9]{2})?").matches(it.customColorHex))
            listOf(it.id, it.sensorId, it.widgetType.name, it.widgetSize.name, it.customColorHex.orEmpty()).joinToString("|")
        }
    }
    fun decode(text: String): List<DashboardWidgetConfig>? = runCatching {
        require(text.length <= 16_384)
        val rows = if (text.isEmpty()) emptyList() else text.lines().map { row ->
            val parts = row.split('|')
            require(parts.size == 5)
            DashboardWidgetConfig(parts[0], parts[1], WidgetType.valueOf(parts[2]), WidgetSize.valueOf(parts[3]), parts[4].takeIf { it.isNotEmpty() })
        }
        encode(rows) // Apply the same bounds and identifier validation on read.
        rows
    }.getOrNull()
}
