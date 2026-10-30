package com.sarani.carobd

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Device execution required; JVM SQL checks do not substitute for this Android SQLite test. */
@RunWith(AndroidJUnit4::class)
class DtcHistoryMigrationInstrumentedTest {
    @Test fun versionOneRowsSurviveWithUnknownObservationFields() {
        SQLiteDatabase.create(null).use { db ->
            db.execSQL("CREATE TABLE diagnostic_sessions (id TEXT PRIMARY KEY, vehicle_id TEXT, start_time INTEGER, end_time INTEGER, dtc_count INTEGER, mil_active INTEGER, ecu_name TEXT, protocol TEXT)")
            db.execSQL("CREATE TABLE dtc_records (id TEXT PRIMARY KEY, session_id TEXT, code TEXT, title_en TEXT, title_fa TEXT, description_en TEXT, description_fa TEXT, severity TEXT, system TEXT, category TEXT, is_mil INTEGER, ecu_source TEXT, timestamp INTEGER)")
            db.execSQL("INSERT INTO diagnostic_sessions VALUES ('legacy','vehicle',1,2,1,1,'Engine ECU','Standard OBD-II')")
            db.execSQL("INSERT INTO dtc_records VALUES ('old','legacy','P0133','','','','','MODERATE','POWERTRAIN','STORED',1,'Engine ECU',2)")
            CarObdDatabaseHelper(InstrumentationRegistry.getInstrumentation().targetContext).onUpgrade(db, 1, 2)
            db.rawQuery("SELECT code, observed_mil, observed_protocol, responder, provenance FROM dtc_records", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("P0133", cursor.getString(0))
                assertTrue(cursor.isNull(1)); assertTrue(cursor.isNull(2)); assertTrue(cursor.isNull(3))
                assertEquals("LEGACY_UNVERIFIED", cursor.getString(4))
                assertFalse(cursor.moveToNext())
            }
        }
    }
}
