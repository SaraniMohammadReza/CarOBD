package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class DiagnosticSessionReportTest {
    private fun report(rows: List<ResponderScanResult>) = DiagnosticSessionReport("id", null, 1, 2,
        null, null, null, null, true, null, null, DiagnosticSessionReport.outcomes(DiagnosticScanReport(rows)))

    @Test fun partialEvidenceKeepsCategoriesRespondersAndRawWithoutInventedIdentity() {
        val codes = listOf(DtcCategory.STORED, DtcCategory.PENDING).map {
            DiagnosticCode("P0133", "", "", "", "", category = it, responderAddressHex = "7E9")
        }
        val r = report(listOf(ResponderScanResult("03", "7E9", ScanOutcome.CODES, listOf(codes[0]), listOf("raw 03")),
            ResponderScanResult("07", "7E9", ScanOutcome.CODES, listOf(codes[1])),
            ResponderScanResult("0A", null, ScanOutcome.TIMEOUT)))
        assertEquals(ScanCompleteness.PARTIAL, r.completeness)
        assertEquals(2, r.codes.size)
        assertEquals(setOf("7E9"), r.responders)
        assertEquals(listOf("raw 03"), r.outcomes.first().rawLines)
        assertNull(r.vehicleId); assertNull(r.adapterIdentity); assertNull(r.observedProtocol)
    }

    @Test fun malformedAndEmptyAreNeverCompleteNoCodesReports() {
        assertEquals(ScanCompleteness.FAILED, report(emptyList()).completeness)
        val r = report(listOf("03", "07", "0A").map { ResponderScanResult(it, null, ScanOutcome.MALFORMED) })
        assertEquals(ScanCompleteness.FAILED, r.completeness)
        assertTrue(r.codes.isEmpty())
        assertTrue(r.responders.isEmpty())
    }

    @Test fun completeRequiresEveryExecutedCategoryAndNoFailedResponder() {
        val good = listOf("03", "07", "0A").map { ResponderScanResult(it, null, ScanOutcome.NO_CODES) }
        assertEquals(ScanCompleteness.COMPLETE, report(good).completeness)
        assertEquals(ScanCompleteness.PARTIAL, report(good.dropLast(1)).completeness)
        assertEquals(ScanCompleteness.PARTIAL, report(good + ResponderScanResult("03", "7E9", ScanOutcome.MALFORMED)).completeness)
    }
}
