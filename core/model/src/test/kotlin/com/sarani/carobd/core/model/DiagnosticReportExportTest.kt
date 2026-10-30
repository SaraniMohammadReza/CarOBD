package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class DiagnosticReportExportTest {
    private fun report(outcomes: List<DiagnosticReportOutcome> = emptyList(), simulator: Boolean = true) = DiagnosticSessionReport(
        "saved-report", null, 10, 20, null, null, "profile", "Owner \"name\"\nفارسی", simulator, null, null, outcomes)

    @Test fun unknownIdentityAndUnreadServicesRemainExplicitlyUnknownInExport() {
        val json = DiagnosticReportExport.toJson(report())
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"scope\":\"DTC_03_07_0A\""))
        assertTrue(json.contains("\"observedProtocol\":null"))
        assertTrue(json.contains("\"vehicle\":{\"id\":null,\"label\":null}"))
        assertTrue(json.contains("\"completeness\":\"FAILED\""))
        assertTrue(json.contains("\"healthAssessment\":\"NOT_PERFORMED\""))
        assertTrue(json.contains("\"notCollected\":[\"READINESS_MIL\",\"MODE_02\",\"MODE_09\",\"MODE_06\"]"))
    }
    @Test fun partialCategoriesRespondersAndRawEvidenceAreNotCollapsedOrTruncated() {
        val rows = listOf(DtcCategory.STORED, DtcCategory.PENDING, DtcCategory.PERMANENT).mapIndexed { index, category ->
            DiagnosticReportOutcome(listOf("03", "07", "0A")[index], "7E9", ScanOutcome.CODES,
                listOf(DiagnosticReportCode("P0133", category, "7E9")), listOf("raw ".repeat(2000)))
        } + DiagnosticReportOutcome("03", null, ScanOutcome.TIMEOUT)
        val json = DiagnosticReportExport.toJson(report(rows, simulator = false))
        assertTrue(json.contains("\"completeness\":\"PARTIAL\""))
        assertTrue(json.contains("\"source\":\"PHYSICAL_READ_ONLY\""))
        assertTrue(json.contains("\"categorizedCodeCount\":3"))
        assertTrue(json.contains("\"identifiedResponderCount\":1"))
        for (category in listOf("STORED", "PENDING", "PERMANENT")) assertTrue(json.contains("\"category\":\"$category\""))
        assertTrue(json.contains("raw ".repeat(2000)))
        assertTrue(json.contains("\"outcome\":\"TIMEOUT\""))
    }
    @Test fun sharedJsonEscapingPreservesUnicodeAndEscapesEveryControlCharacter() {
        val controls = (0..31).map { it.toChar() }.joinToString("")
        val encoded = jsonStringLiteral("فارسی\\\"" + controls)
        assertTrue(encoded.startsWith("\"فارسی\\\\\\\""))
        for (n in 0..31) assertTrue(encoded.contains("\\u%04X".format(java.util.Locale.ROOT, n)))
        assertFalse(encoded.any { it.code < 32 })
        assertEquals("null", jsonStringLiteral(null))
        assertEquals("\"\"", jsonStringLiteral(""))
    }
}
