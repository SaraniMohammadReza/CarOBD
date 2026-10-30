#!/usr/bin/env python3
"""Execute production v1 create SQL + v2 migration in SQLite; no Android runtime claim."""
import pathlib, re, sqlite3
root = pathlib.Path(__file__).resolve().parents[1]
helper = (root / 'app/src/main/java/com/sarani/carobd/data/db/CarObdDatabaseHelper.kt').read_text()
names = dict(re.findall(r'const val (TABLE_\w+) = "([^"]+)"', helper))
create = re.findall(r'"""\s*(CREATE TABLE .*?)"""', helper, re.S)
db = sqlite3.connect(':memory:')
for sql in create:
    for key, value in names.items(): sql = sql.replace('$' + key, value)
    db.execute(sql)
db.execute("INSERT INTO diagnostic_sessions VALUES ('legacy','vehicle',1,2,1,1,'Engine ECU','Standard OBD-II')")
db.execute("INSERT INTO dtc_records VALUES ('code','legacy','P0133','','','','','MODERATE','POWERTRAIN','STORED',1,'Engine ECU',2)")
for category in ['PENDING', 'PERMANENT']:
    db.execute("INSERT INTO dtc_records VALUES (?, 'legacy','P0133','','','','','MODERATE','POWERTRAIN',?,1,'Engine ECU',2)", (category, category))
source = (root / 'app/src/main/java/com/sarani/carobd/data/db/DtcHistoryMigration.kt').read_text()
for sql in re.findall(r'"(ALTER TABLE [^"]+)"', source): db.execute(sql)
assert db.execute('SELECT code, category, observed_mil, observed_protocol, responder, provenance FROM dtc_records').fetchone() == ('P0133', 'STORED', None, None, None, 'LEGACY_UNVERIFIED')
assert db.execute('SELECT vehicle_id, observed_mil, observed_protocol, connection_session_id, provenance FROM diagnostic_sessions').fetchone() == ('vehicle', None, None, None, 'LEGACY_UNVERIFIED')
assert db.execute('SELECT COUNT(*) FROM dtc_records').fetchone()[0] == 3
assert db.execute("SELECT COUNT(*) FROM dtc_records WHERE provenance='LEGACY_UNVERIFIED' AND observed_mil IS NULL AND observed_protocol IS NULL AND responder IS NULL").fetchone()[0] == 3
print('PASS: production migration preserves legacy rows and marks unobserved fields unknown')

# v3 uses the exact production migration, persists unknown values and is transactional.
trip_values = ('old-trip', 'vehicle', 10, 20, 10, 2.5, 80, 3000, 45, 0, 0, 0, 0, 0, 85, 80, 1)
db.execute('INSERT INTO trip_records VALUES (' + ','.join('?' for _ in trip_values) + ')', trip_values)
trip_source = (root / 'app/src/main/java/com/sarani/carobd/data/db/TripEvidenceMigration.kt').read_text()
trip_sql = re.findall(r'"((?:ALTER TABLE|CREATE TABLE|CREATE INDEX) [^"]+)"', trip_source)
for sql in trip_sql: db.execute(sql)
assert db.execute('SELECT distance_km, fuel_liters, provenance FROM trip_records').fetchone() == (2.5, 0.0, 'LEGACY_UNVERIFIED')
assert db.execute('SELECT COUNT(*) FROM trip_metric_evidence').fetchone()[0] == 0
# Actual file close/reopen, not an in-memory object recreation.
import tempfile
with tempfile.TemporaryDirectory() as directory:
    path = pathlib.Path(directory) / 'evidence.db'
    db.commit()
    disk = sqlite3.connect(path)
    db.backup(disk)
    disk.execute("INSERT INTO trip_metric_evidence VALUES ('old-trip','FUEL_USED',NULL,'UNAVAILABLE','UNAVAILABLE',NULL,NULL,NULL,0,0,NULL)")
    disk.commit(); disk.close()
    disk = sqlite3.connect(path)
    assert disk.execute('SELECT value, provenance, source, covered_ms FROM trip_metric_evidence').fetchone() == (None, 'UNAVAILABLE', None, 0)
    assert disk.execute('SELECT provenance FROM trip_records').fetchone()[0] == 'LEGACY_UNVERIFIED'
    disk.close()
# Interrupted upgrade must rollback rather than present a partially upgraded schema.
probe = sqlite3.connect(':memory:')
probe.execute('CREATE TABLE trip_records (id TEXT, vehicle_id TEXT, start_time INTEGER)')
probe.execute('BEGIN')
try:
    for sql in trip_sql: probe.execute(sql)
except sqlite3.OperationalError: # missing dtc table deliberately causes index creation to fail
    probe.rollback()
else:
    raise AssertionError('Expected migration failure')
assert 'provenance' not in [r[1] for r in probe.execute('PRAGMA table_info(trip_records)')]
assert not probe.execute("SELECT name FROM sqlite_master WHERE name='trip_metric_evidence'").fetchall()
print('PASS: v3 preserves legacy trip values without inventing evidence, file recreation and migration rollback')
