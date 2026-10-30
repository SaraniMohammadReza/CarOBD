package com.sarani.carobd.data.db

/** Existing values remain on disk, but unobserved legacy claims are never promoted to evidence. */
object DtcHistoryMigration {
    val version2 = listOf(
        "ALTER TABLE diagnostic_sessions ADD COLUMN observed_mil INTEGER",
        "ALTER TABLE diagnostic_sessions ADD COLUMN observed_protocol TEXT",
        "ALTER TABLE diagnostic_sessions ADD COLUMN provenance TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'",
        "ALTER TABLE diagnostic_sessions ADD COLUMN connection_session_id TEXT",
        "ALTER TABLE dtc_records ADD COLUMN observed_mil INTEGER",
        "ALTER TABLE dtc_records ADD COLUMN observed_protocol TEXT",
        "ALTER TABLE dtc_records ADD COLUMN responder TEXT",
        "ALTER TABLE dtc_records ADD COLUMN provenance TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'"
    )
}
