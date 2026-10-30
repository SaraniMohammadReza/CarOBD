package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Independent Android JSON parser verifies exported persisted evidence, not a matching fake encoder. */
@RunWith(AndroidJUnit4::class)
class DiagnosticReportExportInstrumentedTest {
    @Test fun savedPartialReportExportsAsValidUtf8JsonWithExactRawEvidenceAndNullIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "export-${java.util.UUID.randomUUID()}.db"
        val destination = java.io.File(context.cacheDir, "report-${java.util.UUID.randomUUID()}.json")
        val raw = "7E9 bad\\quoted \"payload\"\r\n\tفارسی"
        val report = DiagnosticSessionReport("saved-report", "old-session", 10, 20, null, null, "profile", "Owner فارسی", true, null, null,
            listOf(DiagnosticReportOutcome("03", "7E9", ScanOutcome.MALFORMED, rawLines = listOf(raw)),
                DiagnosticReportOutcome("07", null, ScanOutcome.NO_CODES), DiagnosticReportOutcome("0A", null, ScanOutcome.TIMEOUT)))
        try {
            CarObdDatabaseHelper(context, name).use { it.saveDiagnosticReport(report) }
            val saved = CarObdDatabaseHelper(context, name).use { it.recentDiagnosticReports().single() }
            destination.bufferedWriter(Charsets.UTF_8).use { it.write(DiagnosticReportExport.toJson(saved)) }
            val json = JSONObject(destination.readText(Charsets.UTF_8))
            assertEquals(1, json.getInt("schemaVersion"))
            assertEquals("DTC_03_07_0A", json.getString("scope"))
            assertEquals("SIMULATOR", json.getString("source"))
            assertEquals("PARTIAL", json.getString("completeness"))
            assertTrue(json.getJSONObject("vehicle").isNull("id"))
            assertTrue(json.isNull("observedProtocol"))
            assertEquals("NOT_VERIFIED", json.getString("adapterAuthenticity"))
            assertEquals("NOT_PERFORMED", json.getString("healthAssessment"))
            val outcomes = json.getJSONArray("outcomes")
            assertEquals(3, outcomes.length())
            assertEquals(raw, outcomes.getJSONObject(0).getJSONArray("rawLines").getString(0))
            assertEquals("7E9", outcomes.getJSONObject(0).getString("responder"))
            assertTrue(outcomes.getJSONObject(1).isNull("responder"))
            assertEquals("TIMEOUT", outcomes.getJSONObject(2).getString("outcome"))
            assertEquals(4, json.getJSONArray("notCollected").length())
            assertEquals("Owner فارسی", json.getJSONObject("connectionProfile").getString("label"))
        } finally { context.deleteDatabase(name); destination.delete() }
    }
}
