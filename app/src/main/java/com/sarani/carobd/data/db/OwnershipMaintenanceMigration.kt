package com.sarani.carobd.data.db

/** Published v1-v7 remain immutable. Old records are deliberately not assigned to schedules. */
object OwnershipMaintenanceMigration {
    val version8 = listOf(
        "ALTER TABLE owned_vehicles ADD COLUMN archived INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE vehicle_maintenance_records ADD COLUMN schedule_id TEXT",
        "CREATE INDEX idx_maint_record_schedule ON vehicle_maintenance_records(schedule_id, performed_date, created_at)",
        "ALTER TABLE vehicle_maintenance_schedules ADD COLUMN baseline_km INTEGER",
        "ALTER TABLE vehicle_maintenance_schedules ADD COLUMN baseline_date INTEGER",
        "UPDATE vehicle_maintenance_schedules SET baseline_km=last_service_km, baseline_date=last_service_date",
        "CREATE TABLE maintenance_reminder_state (vehicle_id TEXT NOT NULL, schedule_id TEXT NOT NULL, snoozed_until INTEGER NOT NULL, basis_updated_at INTEGER NOT NULL, PRIMARY KEY(vehicle_id,schedule_id))"
    )
}
