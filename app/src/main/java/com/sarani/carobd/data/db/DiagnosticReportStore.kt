package com.sarani.carobd.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.sarani.carobd.core.model.*

/** SQLite transaction preserves even failed/partial scans independently of legacy success-only history. */
internal object DiagnosticReportStore {
    fun save(db: SQLiteDatabase, report: DiagnosticSessionReport) {
        db.beginTransaction()
        try {
            db.insertOrThrow("diagnosis_reports", null, ContentValues().apply {
                put("id", report.id); put("connection_session_id", report.connectionSessionId)
                put("started_at", report.startedAtMs); put("finished_at", report.finishedAtMs)
                put("vehicle_id", report.vehicleId); put("vehicle_label", report.vehicleLabel)
                put("profile_id", report.profileId); put("profile_label", report.profileLabel)
                put("simulator", if (report.simulator) 1 else 0)
                put("observed_protocol", report.observedProtocol); put("adapter_identity", report.adapterIdentity)
            })
            report.outcomes.forEachIndexed { index, row ->
                db.insertOrThrow("diagnosis_outcomes", null, ContentValues().apply {
                    put("report_id", report.id); put("position", index); put("request", row.request)
                    put("responder", row.responder); put("outcome", row.outcome.name); put("detail", row.detail)
                })
                row.codes.forEachIndexed { codeIndex, code ->
                    db.insertOrThrow("diagnosis_codes", null, ContentValues().apply {
                        put("report_id", report.id); put("outcome_position", index); put("position", codeIndex)
                        put("code", code.code); put("category", code.category.name); put("responder", code.responder)
                    })
                }
                row.rawLines.forEachIndexed { lineIndex, raw ->
                    db.insertOrThrow("diagnosis_raw_lines", null, ContentValues().apply {
                        put("report_id", report.id); put("outcome_position", index); put("position", lineIndex); put("raw_line", raw)
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun recent(db: SQLiteDatabase): List<DiagnosticSessionReport> = buildList {
        // Bounded screen window; no silent deletion of older evidence.
        db.query("diagnosis_reports", null, null, null, null, null, "started_at DESC, id DESC", "100").use { reports ->
            while (reports.moveToNext()) {
                val id = reports.text("id")!!
                val outcomes = buildList {
                    db.query("diagnosis_outcomes", null, "report_id=?", arrayOf(id), null, null, "position ASC").use { rows ->
                        while (rows.moveToNext()) {
                            val position = rows.text("position")!!
                            val args = arrayOf(id, position)
                            val codes = buildList {
                                db.query("diagnosis_codes", null, "report_id=? AND outcome_position=?", args, null, null, "position ASC").use { c ->
                                    while (c.moveToNext()) add(DiagnosticReportCode(c.text("code")!!, DtcCategory.valueOf(c.text("category")!!), c.text("responder")))
                                }
                            }
                            val raw = buildList {
                                db.query("diagnosis_raw_lines", null, "report_id=? AND outcome_position=?", args, null, null, "position ASC").use { c ->
                                    while (c.moveToNext()) add(c.text("raw_line")!!)
                                }
                            }
                            add(DiagnosticReportOutcome(rows.text("request")!!, rows.text("responder"), ScanOutcome.valueOf(rows.text("outcome")!!), codes, raw, rows.text("detail")))
                        }
                    }
                }
                add(DiagnosticSessionReport(id, reports.text("connection_session_id"), reports.getLong(reports.getColumnIndexOrThrow("started_at")),
                    reports.getLong(reports.getColumnIndexOrThrow("finished_at")), reports.text("vehicle_id"), reports.text("vehicle_label"),
                    reports.text("profile_id"), reports.text("profile_label"), reports.getInt(reports.getColumnIndexOrThrow("simulator")) == 1,
                    reports.text("observed_protocol"), reports.text("adapter_identity"), outcomes))
            }
        }
    }
    private fun Cursor.text(name: String): String? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }
}
