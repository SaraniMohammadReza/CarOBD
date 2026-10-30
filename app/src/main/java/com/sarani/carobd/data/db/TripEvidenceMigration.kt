package com.sarani.carobd.data.db

/** Additive v3: retain old numeric columns, but never promote their absent provenance. */
object TripEvidenceMigration {
    val version3 = listOf(
        "ALTER TABLE trip_records ADD COLUMN provenance TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'",
        "CREATE TABLE trip_metric_evidence (trip_id TEXT NOT NULL, metric TEXT NOT NULL, value REAL, provenance TEXT NOT NULL, quality TEXT NOT NULL, source TEXT, first_sample_ms INTEGER, last_sample_ms INTEGER, sample_count INTEGER NOT NULL, covered_ms INTEGER NOT NULL, method TEXT, PRIMARY KEY(trip_id, metric))",
        "CREATE INDEX dtc_records_session_idx ON dtc_records(session_id)",
        "CREATE INDEX trip_records_vehicle_time_idx ON trip_records(vehicle_id, start_time)"
    )
}
