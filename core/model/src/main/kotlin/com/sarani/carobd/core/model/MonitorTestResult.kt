package com.sarani.carobd.core.model

/** CAN Mode06 raw record. Unknown scaling must not become an invented unit or pass/fail. */
data class MonitorTestResult(
    val monitorId: Int, val testId: Int, val unitScalingId: Int,
    val rawMeasured: Int, val rawMinimum: Int, val rawMaximum: Int,
    val componentId: Int? = null, val measured: Double? = null, val minimum: Double? = null,
    val maximum: Double? = null, val unit: String? = null, val passed: Boolean? = null,
)
