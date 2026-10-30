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
source = (root / 'app/src/main/java/com/sarani/carobd/data/db/DtcHistoryMigration.kt').read_text()
for sql in re.findall(r'"(ALTER TABLE [^"]+)"', source): db.execute(sql)
assert db.execute('SELECT code, category, observed_mil, observed_protocol, responder, provenance FROM dtc_records').fetchone() == ('P0133', 'STORED', None, None, None, 'LEGACY_UNVERIFIED')
assert db.execute('SELECT vehicle_id, observed_mil, observed_protocol, connection_session_id, provenance FROM diagnostic_sessions').fetchone() == ('vehicle', None, None, None, 'LEGACY_UNVERIFIED')
assert db.execute('SELECT COUNT(*) FROM dtc_records').fetchone()[0] == 1
print('PASS: production migration preserves legacy rows and marks unobserved fields unknown')
