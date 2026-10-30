package com.sarani.carobd.data.db

/** Additive: old code history cannot establish a source identity or complete report. */
object DiagnosticReportMigration {
    val version4 = listOf(
        "CREATE TABLE diagnosis_reports (id TEXT PRIMARY KEY, connection_session_id TEXT, started_at INTEGER NOT NULL, finished_at INTEGER NOT NULL, vehicle_id TEXT, vehicle_label TEXT, profile_id TEXT, profile_label TEXT, simulator INTEGER NOT NULL, observed_protocol TEXT, adapter_identity TEXT)",
        "CREATE TABLE diagnosis_outcomes (report_id TEXT NOT NULL, position INTEGER NOT NULL, request TEXT NOT NULL, responder TEXT, outcome TEXT NOT NULL, detail TEXT, PRIMARY KEY(report_id, position))",
        "CREATE TABLE diagnosis_codes (report_id TEXT NOT NULL, outcome_position INTEGER NOT NULL, position INTEGER NOT NULL, code TEXT NOT NULL, category TEXT NOT NULL, responder TEXT, PRIMARY KEY(report_id, outcome_position, position))",
        "CREATE TABLE diagnosis_raw_lines (report_id TEXT NOT NULL, outcome_position INTEGER NOT NULL, position INTEGER NOT NULL, raw_line TEXT NOT NULL, PRIMARY KEY(report_id, outcome_position, position))"
    )
}
