package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

object FreezeFrameDecoder {
    private val metrics = mapOf(0x04 to (1 to "%"), 0x05 to (1 to "°C"), 0x0B to (1 to "kPa"),
        0x0C to (2 to "rpm"), 0x0D to (1 to "km/h"), 0x0F to (1 to "°C"), 0x10 to (2 to "g/s"), 0x11 to (1 to "%"))
    fun command(pid: Int, frame: Int): String {
        require(pid in 0..255 && frame in 0..255)
        return "02%02X%02X".format(pid, frame)
    }
    fun supported(frame: Int, envelope: DiagnosticWireResponse): Set<Int>? {
        val p = (envelope.result as? DiagnosticResult.Positive)?.payload ?: return null
        if (envelope.service != DiagnosticService.MODE_02 || p.size != 6 || p[0] != 0.toByte() || (p[1].toInt() and 0xFF) != frame) return null
        return (1..32).filter { pid -> p[2 + (pid - 1) / 8].toInt() and (0x80 ushr ((pid - 1) % 8)) != 0 }.toSet()
    }
    fun decode(pid: Int, frame: Int, envelope: DiagnosticWireResponse): DiagnosticReading<FreezeFrameValue> {
        require(pid in 0..255 && frame in 0..255)
        val metric = metrics[pid]
        val r = EvidenceDecoders.outcome(envelope) { p ->
            require(envelope.service == DiagnosticService.MODE_02 && p.size >= 2 && (p[0].toInt() and 0xFF) == pid && (p[1].toInt() and 0xFF) == frame) { "Freeze-frame SID/PID/index mismatch" }
            val data = p.drop(2).toByteArray()
            if (metric != null) require(data.size == metric.first) { "Freeze-frame value length mismatch" }
            require(data.isNotEmpty()) { "Empty freeze-frame value" }
            FreezeFrameValue(pid, frame, metric?.let { PidCodec.decodeMode01Metric(pid, data) }, metric?.second,
                data.joinToString("") { "%02X".format(it) })
        }
        return if (metric == null && r.state == ReadingState.Success) r.copy(state = ReadingState.Unsupported, detail = "Raw field; decoder unavailable") else r
    }
}
