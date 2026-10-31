package com.sarani.carobd.data.db

/** Additive ownership schema. Legacy garage/routing/history rows are intentionally untouched. */
object OwnedVehicleMigration {
    val version5 = listOf(
        """CREATE TABLE owned_vehicles (
            id TEXT PRIMARY KEY,
            display_name TEXT NOT NULL,
            manufacturer TEXT,
            model TEXT,
            generation TEXT,
            year INTEGER,
            variant TEXT,
            engine TEXT,
            engine_code TEXT,
            transmission TEXT,
            market TEXT,
            vin TEXT,
            license_plate TEXT,
            current_odometer_km INTEGER,
            tank_capacity_liters REAL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )""".trimIndent(),
        "CREATE INDEX owned_vehicles_updated_idx ON owned_vehicles(updated_at DESC)",
        """CREATE TABLE owned_vehicle_selection (
            singleton_id INTEGER PRIMARY KEY CHECK(singleton_id = 1),
            vehicle_id TEXT
        )""".trimIndent(),
        "INSERT INTO owned_vehicle_selection(singleton_id, vehicle_id) VALUES (1, NULL)",
    )
}
