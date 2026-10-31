# Development checkpoint log — append only

## A — Architecture and safety reassessment

- Time: 2026-10-31 06:07 Asia/Tehran. Starting local and fetched remote:14f924ea60d3ff350537fae143faf334ecf79691. Ending commit is the publication containing this entry; its exact Git identity is appended in the next verified checkpoint.
- Objective: recover actual architecture/safety before product expansion. No source changes or deletions.
- Inspected: canonical handoff; MainActivity navigation; ObdRepository/DiagnosticSession/typed authorization architecture; ActiveVehicleRepository; CarObdDatabaseHelper; DiagnosticReportStore; MaintenanceMvi/Screen; VehicleProfileMvi; core VehicleSpecDatabase and app IranianCarSpecsDatabase.
- Findings: production storage is SQLiteOpenHelper v4, not Room. garage_vehicles has mandatory year/odometer/capacity and cannot represent honest unknowns. ActiveVehicleRepository resolves ECU catalog routing profiles, not owned physical vehicles. Maintenance uses global preference odometer/service map and unqualified generic intervals; selecting a profile does not isolate service history. Core/app specification catalogs contain inconsistent values and unsubstantiated OEM confidence; no newly verified fitment or manufacturer interval can be derived from them.
- Decisions: additive normalized ownership tables/repository, nullable metadata, independent active owner selection; preserve legacy data unassigned. No ownership selection may dispatch a vehicle command or silently choose ECU routing. Do not reuse unverified catalogs for fitment or automatic service advice.
- Documentation created: this log, SPRINT_4_IMPLEMENTATION_REPORT.md, SPRINT_4_TEST_EVIDENCE.md, SPRINT_4_FINAL_HANDOFF.md; canonical entry updated. Safety/transport dispatch unchanged.
- Verification: actual branch/status/log and git fetch/head comparison; source inspection; git diff --check before commit. No fresh JVM/build/Android/lint pass claimed. Historical464/10/10 baseline is not a fresh result.
- P0: preserve physical04/08/write denial, generation/cancellation/quarantine/transport invariants. Physical device/vehicle validation outstanding. P1: ownership isolation, service provenance, combined report/lazy archive. P2: fitment/catalog/guide/reminders/intro integration. P3: deferred HUD/parking; no physical writes.
- Next: additive ownership model/store migration, active vehicle repository, integrated manual vehicle add/edit/switch, SQLite recreation/upgrade/isolation tests; verify then push immediately.
# 2026-10-31 — Checkpoint B: additive owned-vehicle persistence

- Recovered remote `5ed1d92` in an isolated worktree because the original company checkout contained four preserved, superseded uncommitted edits.
- Added nullable `OwnedVehicle`; unknown owner facts remain null and validation rejects invalid year/odometer/tank/timestamps.
- Added additive SQLite v5 `owned_vehicles` and nullable active selection. No legacy `garage_vehicles`, history or diagnostic row is copied or reassigned.
- Added persistence CRUD/selection and `OwnedVehicleRepository`; ECU-routing `ActiveVehicleRepository` remains untouched.
- Verification: model tests, app JVM tests and Android-test APK assembly passed. Android SQLite test compiled but was not executed.
- Source checkpoint pushed: `5753ec848738e1ce1974941949e1ba9771d023ae`.
- Next: bilingual ownership management UI and navigation.

# 2026-10-31 — Checkpoint C: bilingual My Vehicle management

- Added a dedicated My Vehicle destination and offline Compose flow for empty/list/create/edit/select/delete, separate from ECU routing profiles.
- Added English and Persian resources for every new user-facing label, validation message and confirmation.
- Extended the published ownership schema additively to SQLite v6 for nullable fuel type and notes; v5 SQL remains immutable and no legacy row is assigned.
- Added an interface-backed ownership repository and MVI state/editor validation. English, Persian and Arabic digits are accepted for model year; optional blanks persist as null; edits preserve identity and creation time.
- Added JVM regressions for localized save/edit, validation, selection and deletion isolation. Expanded the Android SQLite lifecycle test for edit, multiple vehicles, reopen persistence and selected-vehicle deletion.
- Final software gate passed: `:core:model:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:checkDebugDuplicateClasses`, exit 0; 210 JVM tests, zero failures/errors/skips.
- Android runtime status: `NOT_VERIFIABLE_ON_BLUESTACKS`. The local Pie64 API 28 instance accepted app and test APK installation but became offline before instrumentation. Gradle connected testing was additionally unable to resolve the UTP additional-test-output artifact. This is infrastructure evidence, not a product failure or pass.
- No Bluetooth, BLE, USB, Wi-Fi adapter or vehicle validation was performed.
- Next: execute the compiled persistence/UI instrumentation on a stable supported emulator before treating Android runtime behavior as verified; then begin provenance-aware vehicle catalog/fitment foundation.
