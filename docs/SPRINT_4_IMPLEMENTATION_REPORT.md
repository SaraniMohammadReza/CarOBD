# Sprint 4 — autonomous continuation and ownership expansion

Starting source14f924ea60d3ff350537fae143faf334ecf79691. Canonical entry: AGENT_CONTINUATION_HANDOFF.md. Checkpoint history: DEVELOPMENT_CHECKPOINT_LOG.md. Test results: SPRINT_4_TEST_EVIDENCE.md.

## A — reassessment

Actual modules: app,core:model/adapter/protocol/telemetry/vehicle. Production SQLiteOpenHelper v4 stores legacy garage/session/DTC/trip/maintenance and additive diagnostic report evidence. Existing ActiveVehicleRepository selects ECU routing presets; this must remain separate from an owner's physical vehicle identity. Existing MaintenanceViewModel uses global preferences and generic intervals, permitting cross-vehicle interpretation. Two hardcoded specification catalogs disagree (e.g. TU5 oil capacity) and have insufficient source/variant provenance for verified fitment or automatic intervals. No source repair yet; next checkpoint implements ownership foundation before new advice.

All physical read-only restrictions remain mandatory. Software verification cannot certify adapters/vehicles. No physical operation authorized. No new database, vendor data, copyrighted photos or OEM compatibility will be invented.

## Planned implementation sequence

1. Add nullable, persisted owner vehicles and explicit active selection, isolated from ECU routing and legacy unassigned data; integrated offline UI.
2. Per-vehicle custom maintenance schedules/history with date/mileage evaluation; replace global generic advice. Persistent non-exact reminders only with permission-aware real scheduling.
3. Provenance-aware normalized variant catalog/fitment. Seed only verified licensed sources; unknown remains unknown. Accurate neutral part illustrations if licensed photos unavailable.
4. Combined diagnostic evidence persistence/export and archive scale, preserving responder/session/raw/partial outcomes.
5. Offline bilingual searchable guide, ownership-first navigation and skippable/reduced-motion branded intro.
6. Complete integration/emulator RTL/offline/lint/gates and truthful final GitHub handoff.

## B — additive ownership persistence

Implemented a separate nullable owner identity rather than reusing ECU routing profiles or the legacy `garage_vehicles` defaults. `OwnedVehicle` retains unknown year/model/engine/odometer/tank values as null. SQLite v5 adds `owned_vehicles` and a nullable singleton selection without migrating or assigning any historical row. `OwnedVehicleRepository` exposes observable list/selection state and is registered in DI; it never reads or writes diagnostic routing selection. The next checkpoint is the bilingual ownership management UI.

These are remaining requirements, not implemented features. Update status with actual checkpoints; do not label foundation as complete product.
