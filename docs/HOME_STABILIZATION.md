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

## Final HOME stabilization evidence — 2026-10-31

Verified application source: `cdfe14f355b596bddb8edb7d0ea222a208d1cd9a`. This publication changes documentation only; obtain delivery HEAD with git rev-parse HEAD. Development root is `/home/mohammad/Desktop/CarOBD/CarOBD-home-dev` on feat/car-obd-development. The original `/home/mohammad/Desktop/CarOBD/CarOBD` is intentionally on LOCAL ONLY home/ownership-preservation (`73e0dd8`); do not open that preserved draft as the active product or push that branch. report.md remains unchanged/untracked.

P1-A through P1-I are implemented: field-authoritative intervals, explicit engine resolution, vehicle-scoped parts search, durable revision-bound snooze, future time warnings without invented mileage, canonical optional onboarding, cold persistence hydration/visible unknowns, transactional linked history reconciliation, and archive plus reminder cancellation. Old history remains unlinked; manual baseline is retained as fallback.

Final exact offline full gate on the verified source: exit0,26s,102 tasks (3 executed/99 up-to-date); JVM549/0 failures/0 errors/0 skips: app199, model62, adapter115, protocol125, telemetry19, vehicle29. Lint0 errors/120 warnings; duplicate check and app/test APK assembly PASS. Localization624 unique matching resources PASS; existing host SQL checker PASS; diff check across d8b1f2d..source PASS. No lint baseline/suppression or dependency version change.

Actual selected Android suite12/12 PASS on emulator-5554, model sdk_gphone16k_x86_64, Android17/API37. Final run FA/RTL360dp/font130. English ownership smoke also passed; exact earlier source run included12/12 EN tests. A direct runner repeat of current-source ownership smoke passed1/1 FA, then actual am force-stop + am start reported COLD and XML confirmed persisted QA owner and unknown odometer after splash. UTP removes APKs after its run, so the exact built app/test APKs were explicitly installed for that final direct-run/cold-process check. No phone/adapter/vehicle operation. The broad legacy RuntimeNavigation fixture is not claimed freshly verified; it retains pre-redesign UI assumptions and needs separate modernization.

Debug APK: `/home/mohammad/Desktop/CarOBD/CarOBD-home-dev/app/build/outputs/apk/debug/app-debug.apk`; 24092041 bytes; SHA256 `a8aa0c3f32417d39432d450f17b6c5d10e0f5e8606d71b72e2ec5ba2f0156671`; apksigner PASS, v2 signature. APKs/cache/screenshots are not in Git. Reproducible new tests: OwnershipRuntimeSmokeTest and OwnershipMaintenanceStabilizationTest. Evidence logs are machine-local /tmp/carobd-home-final-full-gate.log and /tmp/carobd-home-direct-runtime.log; screenshots/XML under /home/mohammad/.local/share/carobd-build/home-stabilization-qa. Application has no dependency on these artifacts.

GRADLE_USER_HOME: /home/mohammad/.gradle
GRADLE_JDK: /home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12 (Temurin24.0.2+12; JVM target17)
ANDROID_SDK: /home/mohammad/Android/Sdk (android-37.0/build-tools37.0.0)
WRAPPER_CACHED: YES,9.8.1
PLUGIN_DEPENDENCIES_CACHED: YES
PROJECT_DEPENDENCIES_CACHED: YES,133 artifact configurations/2403 occurrences/56 metadata buckets,0 failures
OFFLINE_HELP_STATUS: PASS
OFFLINE_BUILD_STATUS: PASS including lint
ANDROID_STUDIO_SYNC_READY: environment PREPARED; no new GUI Sync claimed
REPOSITORY_ROUTING_USED: existing persistent ~/.gradle/init.d/carobd-repositories.gradle; inherited by CLI/Studio, no CLI-only init argument
MISSING_ARTIFACTS: NONE

Open the DEVELOPMENT root in Android Studio. In Settings > Build, Execution, Deployment > Build Tools > Gradle, select the Temurin24 path above as Gradle JDK; keep normal user Gradle home. Use the SDK path above. Daemon criteria also pin24. Do not confuse runtime24 with target17. Caches were populated by Gradle itself, never copied manually.

Remaining scope: independent Hermes review, broad legacy navigation-test modernization, calendar-aware maintenance intervals, notification delivery/permission bench behavior, catalog source quality, and adaptive Dashboard visual QA at320/360dp. The legacy app/core/model/MaintenanceModels.kt remains because tests reference it; no active compatibility code was deleted. Learn Your Car/encyclopedia/catalog expansion/new protocols are deferred. Physical command authorization/adapter/protocol/session/transport/telemetry/foreground source paths are unchanged from company base. No shutdown/suspend is part of this mission.
