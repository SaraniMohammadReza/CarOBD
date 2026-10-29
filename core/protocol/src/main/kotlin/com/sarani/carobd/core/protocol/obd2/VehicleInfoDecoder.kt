package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

object VehicleInfoDecoder {
    val implementedPids = listOf(1, 2, 3, 4, 5, 6, 9, 10)
    fun supported(envelope: DiagnosticWireResponse): Set<Int>? {
        val p = (envelope.result as? DiagnosticResult.Positive)?.payload ?: return null
        if (envelope.service != DiagnosticService.MODE_09 || p.size != 5 || p[0] != 0.toByte()) return null
        return (1..32).filter { pid -> p[1 + (pid - 1) / 8].toInt() and (0x80 ushr ((pid - 1) % 8)) != 0 }.toSet()
    }
    fun decode(pid: Int, envelopes: List<DiagnosticWireResponse>, legacyMessageCounts: Map<String?, Int> = emptyMap()): List<DiagnosticReading<VehicleInfoField>> {
        if (envelopes.isEmpty()) return listOf(DiagnosticReading(ReadingState.Malformed, detail = "Empty vehicle information evidence"))
        val out = mutableListOf<DiagnosticReading<VehicleInfoField>>()
        // Non-CAN VIN uses ordered four-byte chunks with an explicit sequence.
        // A null source is safe only for one unambiguous complete sequence.
        val legacy = if (pid == 2) envelopes.filter { e ->
            e.service == DiagnosticService.MODE_09 && (e.result as? DiagnosticResult.Positive)?.payload?.let { it.size == 6 && it[0] == 2.toByte() } == true
        }.groupBy { it.responderId } else emptyMap()
        val consumed = legacy.values.flatten().toSet()
        legacy.forEach { (source, frames) ->
            val indices = frames.map { it.payload[1].toInt() and 0xFF }
            val count = legacyMessageCounts[source]
            val bytes = frames.flatMap { it.payload.drop(2) }.toByteArray()
            val data = bytes.dropWhile { it == 0.toByte() }.toByteArray()
            val valid = count == 5 && indices == (1..5).toList() && bytes.size == 20 && bytes.take(3).all { it == 0.toByte() }
            val vin = if (valid) PidCodec.decodeVin(data) else null
            out += if (vin != null) DiagnosticReading(ReadingState.Success, VehicleInfoField(pid, listOf(vin), 1), frames)
                else DiagnosticReading(ReadingState.Malformed, evidence = frames, detail = "Invalid/ambiguous non-CAN VIN sequence or count")
        }
        envelopes.filter { it !in consumed }.forEach { e ->
            var r = EvidenceDecoders.outcome(e) { p ->
                require(e.service == DiagnosticService.MODE_09 && p.isNotEmpty() && (p[0].toInt() and 0xFF) == pid) { "Vehicle information SID/PID mismatch" }
                val bytes = p.drop(1).toByteArray()
                if (pid in listOf(1, 3, 5, 9)) {
                    require(bytes.size == 1 && bytes[0] != 0.toByte()) { "Invalid message count" }
                    VehicleInfoField(pid, listOf((bytes[0].toInt() and 0xFF).toString()))
                } else {
                    require(bytes.isNotEmpty()) { "Missing item count" }
                    val count = bytes[0].toInt() and 0xFF
                    require(count in 1..64) { "Invalid item count" }
                    val data = bytes.drop(1).toByteArray()
                    when (pid) {
                        2 -> { require(count == 1) { "VIN item count must be one" }; VehicleInfoField(pid, listOf(requireNotNull(PidCodec.decodeVin(data)) { "Malformed or partial VIN" }), count) }
                        4 -> { require(data.size == count * 16) { "CALID count/length mismatch" }; VehicleInfoField(pid, data.toList().chunked(16).map { text(it.toByteArray()) }, count) }
                        6 -> { require(data.size == count * 4) { "CVN count/length mismatch" }; VehicleInfoField(pid, PidCodec.decodeCvn(data).values, count) }
                        10 -> { require(data.size == count * 20) { "ECU name count/length mismatch" }; VehicleInfoField(pid, data.toList().chunked(20).map { text(it.toByteArray()) }, count) }
                        else -> throw IllegalArgumentException("Unsupported information PID")
                    }
                }
            }
            // Short positive data remains explicitly incomplete, with raw bytes.
            if (r.state == ReadingState.Malformed && e.result is DiagnosticResult.Positive && e.payload.size >= 2) {
                val count = e.payload[1].toInt() and 0xFF
                val expected = when (pid) { 2 -> if (count == 1) 17 else -1; 4 -> count * 16; 6 -> count * 4; 10 -> count * 20; else -> -1 }
                if (count in 1..64 && e.payload.size - 2 < expected) r = r.copy(state = ReadingState.Partial)
            }
            out += r
        }
        return out
    }
    private fun text(bytes: ByteArray): String {
        val end = bytes.indexOfFirst { it == 0.toByte() }.let { if (it < 0) bytes.size else it }
        require(bytes.drop(end).all { it == 0.toByte() }) { "Embedded padding" }
        require(bytes.take(end).all { (it.toInt() and 0xFF) in 0x20..0x7E }) { "Non-ASCII information" }
        return bytes.copyOfRange(0, end).toString(Charsets.US_ASCII).trimEnd(' ').also { require(it.isNotEmpty()) { "Empty information" } }
    }
}
