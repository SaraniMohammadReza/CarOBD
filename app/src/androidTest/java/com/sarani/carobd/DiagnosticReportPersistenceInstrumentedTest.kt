package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticReportPersistenceInstrumentedTest {
    private fun report(id: String = "report") = DiagnosticSessionReport(id, "session-1", 100, 200,
        null, null, "profile", "Owner profile", true, "ISO 15765-4", "Adapter claims ELM",
        listOf(DiagnosticReportOutcome("03", "7E8", ScanOutcome.CODES,
            listOf(DiagnosticReportCode("P0133", DtcCategory.STORED, "7E8")), listOf("7E8 43 01 01 33")),
            DiagnosticReportOutcome("03", "7E9", ScanOutcome.MALFORMED, rawLines = listOf("bad\nraw", "\"quoted\""), detail = "Truncated"),
            DiagnosticReportOutcome("07", null, ScanOutcome.NO_CODES),
            DiagnosticReportOutcome("0A", null, ScanOutcome.TIMEOUT)))

    @Test fun partialReportAndRawLinesSurviveActualSqliteRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "report-${java.util.UUID.randomUUID()}.db"
        try {
            CarObdDatabaseHelper(context, name).use { it.saveDiagnosticReport(report()) }
            CarObdDatabaseHelper(context, name).use {
                assertEquals(report(), it.recentDiagnosticReports().single())
                assertEquals(ScanCompleteness.PARTIAL, it.recentDiagnosticReports().single().completeness)
                assertTrue(it.diagnosticHistory().isEmpty())
                try { it.saveDiagnosticReport(report()); fail("Duplicate must fail") } catch (_: android.database.sqlite.SQLiteConstraintException) {}
                assertEquals(listOf(report()), it.recentDiagnosticReports())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun versionThreeUpgradeCreatesNoFabricatedReportsAndKeepsExistingHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "report-upgrade-${java.util.UUID.randomUUID()}.db"
        try {
            CarObdDatabaseHelper(context, name).use { helper ->
                helper.writableDatabase.apply {
                    for (table in listOf("diagnosis_raw_lines", "diagnosis_codes", "diagnosis_outcomes", "diagnosis_reports")) execSQL("DROP TABLE $table")
                    version = 3
                }
            }
            CarObdDatabaseHelper(context, name).use {
                assertEquals(4, it.readableDatabase.version)
                assertTrue(it.recentDiagnosticReports().isEmpty())
                it.saveDiagnosticReport(report())
            }
            CarObdDatabaseHelper(context, name).use { assertEquals(report(), it.recentDiagnosticReports().single()) }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun storageFailureRollsBackHeaderOutcomesAndCodesTogether() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "report-rollback-${java.util.UUID.randomUUID()}.db"
        try {
            CarObdDatabaseHelper(context, name).use { helper ->
                helper.writableDatabase.execSQL("CREATE TRIGGER reject_report_code BEFORE INSERT ON diagnosis_codes BEGIN SELECT RAISE(ABORT, 'test storage failure'); END")
                try { helper.saveDiagnosticReport(report()); fail("Expected failure") } catch (_: android.database.sqlite.SQLiteException) {}
                assertTrue(helper.recentDiagnosticReports().isEmpty())
                for (table in listOf("diagnosis_reports", "diagnosis_outcomes", "diagnosis_codes", "diagnosis_raw_lines")) {
                    helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
                }
                helper.writableDatabase.execSQL("DROP TRIGGER reject_report_code")
                helper.saveDiagnosticReport(report())
                assertEquals(report(), helper.recentDiagnosticReports().single())
            }
        } finally { context.deleteDatabase(name) }
    }
}
