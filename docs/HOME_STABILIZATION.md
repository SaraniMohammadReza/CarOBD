# Home stabilization

## HOME-R1 — safe reconciliation

Remote verified `d8b1f2d4bae7ef61e1b369ceb96ef3a8687082de`. Original home HEAD `5ed1d92017224dd2df1113b2d8d14a902049b7e2`. All 21 development files were preserved in LOCAL ONLY branch `home/ownership-preservation`, commit `73e0dd8`; this is an unverified WIP snapshot, not a green release. Unrelated report.md remains untracked and unchanged. Patch and untracked archive also exist outside Git in /tmp.

Development worktree `/home/mohammad/Desktop/CarOBD/CarOBD-home-dev` starts exactly at remote on `feat/car-obd-development`. No rebase/reset/clean or preservation-branch push. The company schema/model is canonical; the incompatible home v5 migration is never applied.

| Home file | Decision | Reason |
|---|---|---|
| `app/src/androidTest/java/com/sarani/carobd/DiagnosticReportPersistenceInstrumentedTest.kt` | OBSOLETE | Old DB v5 expectations superseded by company v7 and subsequent additive migrations. |
| `app/src/androidTest/java/com/sarani/carobd/DtcHistoryMigrationInstrumentedTest.kt` | OBSOLETE | Old DB v5 expectations superseded by company v7 and subsequent additive migrations. |
| `app/src/androidTest/java/com/sarani/carobd/OwnershipPersistenceInstrumentedTest.kt` | MERGE SELECTIVELY | Port test concepts (unknown/zero, recreation, routing isolation), adapting to canonical APIs; do not copy obsolete routes. |
| `app/src/androidTest/java/com/sarani/carobd/RuntimeNavigationInstrumentedTest.kt` | MERGE SELECTIVELY | Port test concepts (unknown/zero, recreation, routing isolation), adapting to canonical APIs; do not copy obsolete routes. |
| `app/src/main/java/com/sarani/carobd/MainActivity.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/core/di/AppModule.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/core/storage/VehicleSetupNumbers.kt` | KEEP ONLY ON PRESERVATION BRANCH | Fractional mileage is incompatible with canonical integer-km model; company localized parser retained. |
| `app/src/main/java/com/sarani/carobd/data/db/CarObdDatabaseHelper.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/db/OwnedVehicleStore.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/db/OwnershipMigration.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/repository/OwnershipRepository.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/ui/screens/dashboard/DashboardScreen.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnedFieldLabels.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnershipScreen.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnershipViewModel.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/res/values-fa/strings.xml` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/res/values/strings.xml` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/test/kotlin/com/sarani/carobd/OwnedVehicleInputTest.kt` | KEEP ONLY ON PRESERVATION BRANCH | Fractional mileage is incompatible with canonical integer-km model; company localized parser retained. |
| `core/model/src/main/kotlin/com/sarani/carobd/core/model/OwnedVehicle.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `core/model/src/test/kotlin/com/sarani/carobd/core/model/OwnedVehicleTest.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `docs/SPRINT_4_IMPLEMENTATION_REPORT.md` | KEEP ONLY ON PRESERVATION BRANCH | Historical unfinished home draft, not evidence for company implementation. |

No duplicate home architecture is ported. Home integration-test concepts will be adapted during stabilization. Source fixes begin after this reconciliation checkpoint. Physical diagnostic core is outside scope.

R1 validation: localization checker PASS (618 resources); offline lintDebug PASS, exit0, 1m13s. git diff --check PASS. No runtime verification claimed.

## HOME-R2 — explicit catalog identity and fitment

Owned-vehicle resolution now distinguishes UNKNOWN, ENGINE_REQUIRED, AMBIGUOUS and EXACT. Blank identity never discovers a default vehicle. Maintenance/Home/Parts consume only exact resolution; no legacy name-based fallback. Default schedules only take numeric intervals from safe field evidence; generic fields remain UNVERIFIED with null intervals. User schedule edits are USER_CUSTOM. Parts search intersects vehicle-compatible fitments. Targeted maintenance/parts JVM and core vehicle tests PASS (exit0); localization and diff checks run before publication. No runtime claim.

## HOME-R3/R5 — durable reminders and transactional history

SQLite v8 adds nullable record.schedule_id (old records never guessed), original manual baseline, persisted snooze bound to schedule revision, and owned-vehicle archive flag. Latest linked event by performed date drives schedule; editing/deleting it recomputes baseline, reverting to manual baseline only when all linked events are gone. Odometer is monotonic and is not rewound by historical corrections. Record completion writes history, baseline and odometer in one SQLite transaction; injected update failure rolls everything back.

Normal reminder reconciliation respects persisted snooze across reload/recreation. Completion invalidates the old snooze and schedules every recalculated schedule, including future time warnings. Mileage-only OK schedules keep their persisted facts but have no invented wall-clock trigger; owner odometer edits reevaluate. WorkManager timing is best effort, not exact. Worker rereads current vehicle/schedule/snooze and closes its helper.

Deletion policy: archive owner, clear selection, retain schedules/history under the original owner id, delete snooze metadata, cancel tagged work and visible schedule notifications. Archived vehicles cannot be selected or resurrected by stale edits; workers ignore them. Deleting a schedule unlinks retained history rather than guessing another association.

Validation: targeted Reminder/Maintenance JVM PASS; Android API37 emulator actual SQLite/repository/migration/WorkManager tests 11/11 PASS, exit0. Final follow-up reran 5 stabilization Android tests plus targeted JVM, exit0. Localization 619 entries PASS; diff check PASS. No physical adapter/vehicle operation.

## HOME-R4 — canonical onboarding and cold-start ownership

First-run setup now saves/selects canonical OwnedVehicleRepository only; it never writes legacy UserVehicleSetup or supplies sample engine/mileage/tank/fuel/price. Skip creates nothing. Shared draft parsing is used by both My Vehicle and onboarding; invalid supplied numbers fail, localized real zero remains zero, optional blanks remain null. Saved draft identity is stable across retries/recreation. Persistence failure cannot complete onboarding.

Home hydrates persistence as its first entry point, passes nullable odometer through unchanged, suppresses invented remaining mileage, observes maintenance revisions, and respects explicitly cleared selection. Maintenance and Parts also hydrate independently. Targeted Home/Onboarding/OwnedVehicle/Maintenance/Parts JVM PASS, exit0; UI runtime verification follows in R6. No physical diagnostic behavior changed.

## HOME-R6 — green stabilization gate and emulator evidence

Full offline gate PASS exit0, 1m05s: all six JVM modules, duplicate classes, both debug APKs and lint. Added actual UI smoke under API37 emulator emulator-5554 (sdk_gphone16k_x86_64): EN ownership flow PASS; FA/RTL 360dp/font130 ownership flow plus five database/reminder stabilization tests 6/6 PASS. Covered skip, canonical create with unknowns, real persisted Home hydration using a new repository, ambiguous 206 maintenance denial, unknown-engine search empty, explicit TU3 fitment, and unchanged diagnostic routing/disconnected state. Screenshots in machine-local home-stabilization-qa; tests are committed/reproducible. No physical hardware/vehicle validation. Earlier failed compile/lint attempts are retained in /tmp logs; final gate was rerun after fixes.

Resolution now also rejects ambiguous family prefixes (M15 does not imply M15 TC); only full codes or explicitly curated identity aliases may resolve. Legacy app/core/model/MaintenanceModels.kt is retained because MaintenanceDueCalculationTest still references it; it is not a duplicate of VehicleSpecSheet. No unsafe removal or physical diagnostic changes. Remaining UI polish and broad legacy navigation fixture modernization are separate from these targeted runtime checks.

## HOME-R6 follow-up — unknown is visible

Home no longer labels schedules with missing service evidence as up-to-date. Unknown parts context displays selection-required rather than a measured zero; Parts uses the same explanation. Vehicle header displays unknown odometer as — and preserves actual zero. Home targeted tests, English actual UI assertions and lint PASS (exit0, 1m06s); localization624 and diff check PASS. This completes visible truthfulness alongside nullable domain facts.
