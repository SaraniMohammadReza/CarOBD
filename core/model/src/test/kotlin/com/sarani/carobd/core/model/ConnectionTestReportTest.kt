package com.sarani.carobd.core.model
import org.junit.Assert.*
import org.junit.Test
class ConnectionTestReportTest {
    @Test fun failureCannotInventEarlierPasses() {
        val report = ConnectionTestReport(false).record(ConnectionTestEvidence(ConnectionTestStage.ECU, ConnectionTestStatus.FAIL, "NO DATA")).finishFailure("No ECU")
        assertFalse(report.passed)
        assertTrue(report.stages.none { it.status == ConnectionTestStatus.PASS })
        assertEquals(ConnectionTestStatus.FAIL, report.stages.first { it.stage == ConnectionTestStage.ECU }.status)
    }
    @Test fun completedEvidenceSurvivesFailure() {
        val report = ConnectionTestReport(true).record(ConnectionTestEvidence(ConnectionTestStage.TRANSPORT, ConnectionTestStatus.PASS, "Simulator open"))
            .record(ConnectionTestEvidence(ConnectionTestStage.ADAPTER, ConnectionTestStatus.RUNNING, "ATZ"))
            .finishFailure("Timeout")
        assertEquals("Simulator open", report.stages[0].evidence)
        assertEquals(ConnectionTestStatus.PASS, report.stages[0].status)
        assertEquals(ConnectionTestStatus.FAIL, report.stages[1].status)
        assertTrue(report.stages.drop(2).all { it.status == ConnectionTestStatus.SKIPPED })
    }
}
