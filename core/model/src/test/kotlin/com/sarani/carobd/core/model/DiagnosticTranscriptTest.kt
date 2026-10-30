package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §38 transcript model: JSON/CSV export, reload-from-JSON round trip, and the
 * diagnostic outcome vocabulary are all software-verifiable with no hardware.
 */
class DiagnosticTranscriptTest {

    private fun sample(): DiagnosticTranscript {
        val md = TranscriptMetadata(
            profileName = "BT-Elm",
            transportType = TransportType.BLUETOOTH_CLASSIC,
            target = "00:1D:A5:68:37:5B",
            adapterIdentity = "ELM327 v1.5",
            vehicleProtocol = "ISO 15765-4 CAN 11/500",
            startedAtMs = 1_000_000L
        )
        val b = DiagnosticTranscriptBuilder(md)
        b.recordConnect()
        b.recordTx("01 00")
        b.recordRx(
            rawHex = "41 00 BE 3F B8 11",
            responder = "7E8",
            outcome = DiagnosticOutcome.POSITIVE,
            protocolMetadata = mapOf("sid" to "01")
        )
        b.recordTx("03")
        b.recordRx("7F 03 12", responder = "7E8", outcome = DiagnosticOutcome.NEGATIVE_RESPONSE)
        b.recordErr("UNABLE TO CONNECT")
        b.recordDisconnect()
        return b.toTranscript()
    }

    @Test
    fun jsonRoundTripPreservesMetadataAndEvents() {
        val t = sample()
        val json = t.toJson()

        assertTrue("metadata present", json.contains("\"profileName\":\"BT-Elm\""))
        assertTrue("transport preserved", json.contains("\"transport\":\"BLUETOOTH_CLASSIC\""))
        assertTrue("negative outcome preserved", json.contains("\"outcome\":\"NEGATIVE_RESPONSE\""))
        assertTrue("err direction preserved", json.contains("\"dir\":\"ERR\""))
        assertTrue("responder preserved", json.contains("\"responder\":\"7E8\""))

        val rt = DiagnosticTranscript.fromJson(json)
        assertEquals(t.metadata.profileName, rt.metadata.profileName)
        assertEquals(t.metadata.transportType, rt.metadata.transportType)
        assertEquals(t.metadata.target, rt.metadata.target)
        assertEquals(t.metadata.adapterIdentity, rt.metadata.adapterIdentity)
        assertEquals(t.metadata.vehicleProtocol, rt.metadata.vehicleProtocol)
        assertEquals(t.metadata.startedAtMs, rt.metadata.startedAtMs)
        assertEquals(t.events.size, rt.events.size)
        assertEquals(t.events, rt.events) // structural equality incl. timestamps + meta maps
    }

    @Test
    fun csvExportHasHeaderAndOneRowPerEvent() {
        val csv = sample().toCsv()
        assertTrue(csv.startsWith("direction,elapsedMs,command,rawHex,responder,outcome,protocolMetadata"))
        val dataRows = csv.lines().filter { it.isNotBlank() }.size - 1 // drop header
        assertEquals(sample().events.size, dataRows)
    }

    @Test
    fun emptyMetadataRoundTrips() {
        val md = TranscriptMetadata("p", TransportType.SIMULATOR)
        val b = DiagnosticTranscriptBuilder(md)
        b.recordTx("0100")
        b.recordRx("41 00 BE 3F B8 11")
        val rt = DiagnosticTranscript.fromJson(b.toTranscript().toJson())
        assertEquals(2, rt.events.size)
        assertEquals(b.toTranscript().events, rt.events)
    }

    @Test
    fun outcomeCategoriesCoverAllSections() {
        // §24 outcome categories are the vocabulary the recorder/parser uses.
        val expected = setOf(
            DiagnosticOutcome.POSITIVE,
            DiagnosticOutcome.NEGATIVE_RESPONSE,
            DiagnosticOutcome.NO_DATA,
            DiagnosticOutcome.TIMEOUT,
            DiagnosticOutcome.ADAPTER_FAILURE,
            DiagnosticOutcome.MALFORMED,
            DiagnosticOutcome.PARTIAL,
            DiagnosticOutcome.UNSUPPORTED,
            DiagnosticOutcome.RAW
        )
        assertEquals(expected, DiagnosticOutcome.entries.toSet())
    }
}
