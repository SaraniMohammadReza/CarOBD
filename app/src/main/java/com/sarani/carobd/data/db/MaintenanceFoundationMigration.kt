package com.sarani.carobd.data.db

/**
 * Published v6 remains immutable; normalized per-vehicle maintenance schedules
 * and rich isolated service history are added in additive schema v7.
 */
object MaintenanceFoundationMigration {
    val version7 = listOf(
        """
        CREATE TABLE IF NOT EXISTS vehicle_maintenance_schedules (
            id TEXT PRIMARY KEY,
            vehicle_id TEXT NOT NULL,
            item_key TEXT NOT NULL,
            title_en TEXT NOT NULL,
            title_fa TEXT NOT NULL,
            action_type TEXT NOT NULL,
            interval_km INTEGER,
            interval_months INTEGER,
            last_service_km INTEGER,
            last_service_date INTEGER,
            next_due_km INTEGER,
            next_due_date INTEGER,
            provenance TEXT NOT NULL,
            is_custom INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS idx_maint_sched_vehicle ON vehicle_maintenance_schedules(vehicle_id)",
        """
        CREATE TABLE IF NOT EXISTS vehicle_maintenance_records (
            id TEXT PRIMARY KEY,
            vehicle_id TEXT NOT NULL,
            item_key TEXT NOT NULL,
            performed_km INTEGER NOT NULL,
            performed_date INTEGER NOT NULL,
            cost_tomans INTEGER NOT NULL DEFAULT 0,
            service_center TEXT,
            parts_brand TEXT,
            part_number TEXT,
            notes TEXT,
            receipt_uri TEXT,
            provenance TEXT NOT NULL,
            created_at INTEGER NOT NULL
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS idx_maint_records_vehicle ON vehicle_maintenance_records(vehicle_id)"
    )
}
