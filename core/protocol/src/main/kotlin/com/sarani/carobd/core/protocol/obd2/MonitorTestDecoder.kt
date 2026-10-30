package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

/** CAN 9-byte records only. No OEM naming/scaling assumptions or unsigned pass/fail comparison. */
object MonitorTestDecoder {
    fun supported(page: Int, e: DiagnosticWireResponse): Set<Int>? {
        val p = (e.result as? DiagnosticResult.Positive)?.payload ?: return null
        if (e.service != DiagnosticService.MODE_06 || p.size != 5 || (p[0].toInt() and 0xFF) != page) return null
        return (1..32).filter { p[1 + (it - 1) / 8].toInt() and (0x80 ushr ((it - 1) % 8)) != 0 }.map { it + page }.toSet()
    }
    fun decode(mid: Int, e: DiagnosticWireResponse): DiagnosticReading<List<MonitorTestResult>> = EvidenceDecoders.outcome(e) { p ->
        require(e.service == DiagnosticService.MODE_06 && p.isNotEmpty() && p.size % 9 == 0) { "Invalid CAN Mode06 record length" }
        p.toList().chunked(9).map { record ->
            fun byte(index: Int) = record[index].toInt() and 0xFF
            fun word(index: Int) = byte(index) * 256 + byte(index + 1)
            require(byte(0) == mid) { "Unrelated monitor response" }
            MonitorTestResult(byte(0), byte(1), byte(2), word(3), word(5), word(7))
        }
    }
}
