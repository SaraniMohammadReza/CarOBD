# Development checkpoint log â append only

## A â Architecture and safety reassessment

- Time: 2026-10-31 06:07 Asia/Tehran. Starting local and fetched remote:14f924ea60d3ff350537fae143faf334ecf79691. Ending commit is the publication containing this entry; its exact Git identity is appended in the next verified checkpoint.
- Objective: recover actual architecture/safety before product expansion. No source changes or deletions.
- Inspected: canonical handoff; MainActivity navigation; ObdRepository/DiagnosticSession/typed authorization architecture; ActiveVehicleRepository; CarObdDatabaseHelper; DiagnosticReportStore; MaintenanceMvi/Screen; VehicleProfileMvi; core VehicleSpecDatabase and app IranianCarSpecsDatabase.
- Findings: production storage is SQLiteOpenHelper v4, not Room. garage_vehicles has mandatory year/odometer/capacity and cannot represent honest unknowns. ActiveVehicleRepository resolves ECU catalog routing profiles, not owned physical vehicles. Maintenance uses global preference odometer/service map and unqualified generic intervals; selecting a profile does not isolate service history. Core/app specification catalogs contain inconsistent values and unsubstantiated OEM confidence; no newly verified fitment or manufacturer interval can be derived from them.
- Decisions: additive normalized ownership tables/repository, nullable metadata, independent active owner selection; preserve legacy data unassigned. No ownership selection may dispatch a vehicle command or silently choose ECU routing. Do not reuse unverified catalogs for fitment or automatic service advice.
- Documentation created: this log, SPRINT_4_IMPLEMENTATION_REPORT.md, SPRINT_4_TEST_EVIDENCE.md, SPRINT_4_FINAL_HANDOFF.md; canonical entry updated. Safety/transport dispatch unchanged.
- Verification: actual branch/status/log and git fetch/head comparison; source inspection; git diff --check before commit. No fresh JVM/build/Android/lint pass claimed. Historical464/10/10 baseline is not a fresh result.
- P0: preserve physical04/08/write denial, generation/cancellation/quarantine/transport invariants. Physical device/vehicle validation outstanding. P1: ownership isolation, service provenance, combined report/lazy archive. P2: fitment/catalog/guide/reminders/intro integration. P3: deferred HUD/parking; no physical writes.
- Next: additive ownership model/store migration, active vehicle repository, integrated manual vehicle add/edit/switch, SQLite recreation/upgrade/isolation tests; verify then push immediately.
# 2026-10-31 â Checkpoint B: additive owned-vehicle persistence

- Recovered remote `5ed1d92` in an isolated worktree because the original company checkout contained four preserved, superseded uncommitted edits.
- Added nullable `OwnedVehicle`; unknown owner facts remain null and validation rejects invalid year/odometer/tank/timestamps.
- Added additive SQLite v5 `owned_vehicles` and nullable active selection. No legacy `garage_vehicles`, history or diagnostic row is copied or reassigned.
- Added persistence CRUD/selection and `OwnedVehicleRepository`; ECU-routing `ActiveVehicleRepository` remains untouched.
- Verification: model tests, app JVM tests and Android-test APK assembly passed. Android SQLite test compiled but was not executed.
- Source checkpoint pushed: `5753ec848738e1ce1974941949e1ba9771d023ae`.
- Next: bilingual ownership management UI and navigation.

# 2026-10-31 â Checkpoint C: bilingual My Vehicle management

- Added a dedicated My Vehicle destination and offline Compose flow for empty/list/create/edit/select/delete, separate from ECU routing profiles.
- Added English and Persian resources for every new user-facing label, validation message and confirmation.
- Extended the published ownership schema additively to SQLite v6 for nullable fuel type and notes; v5 SQL remains immutable and no legacy row is assigned.
- Added an interface-backed ownership repository and MVI state/editor validation. English, Persian and Arabic digits are accepted for model year; optional blanks persist as null; edits preserve identity and creation time.
- Added JVM regressions for localized save/edit, validation, selection and deletion isolation. Expanded the Android SQLite lifecycle test for edit, multiple vehicles, reopen persistence and selected-vehicle deletion.
- Final software gate passed: `:core:model:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:checkDebugDuplicateClasses`, exit 0; 210 JVM tests, zero failures/errors/skips.
- Android runtime status: `NOT_VERIFIABLE_ON_BLUESTACKS`. The local Pie64 API 28 instance accepted app and test APK installation but became offline before instrumentation. Gradle connected testing was additionally unable to resolve the UTP additional-test-output artifact. This is infrastructure evidence, not a product failure or pass.
- No Bluetooth, BLE, USB, Wi-Fi adapter or vehicle validation was performed.
- Next: execute the compiled persistence/UI instrumentation on a stable supported emulator before treating Android runtime behavior as verified; then begin provenance-aware vehicle catalog/fitment foundation.

# 2026-10-31 â Checkpoint D: provenance-aware normalized vehicle catalog foundation

- Local timestamp: 2026-10-31 10:20 Asia/Tehran.
- Goal: Establish normalized, expandable, provenance-aware vehicle catalog foundation (makes, models, generations, engine families, transmissions, variants, specifications) without hardcoding or unverified guesses.
- BlueStacks / Android runtime investigation: Reconnected BlueStacks at 127.0.0.1:5555 (Pie64 API 28, x86_64). Installed debug and test APKs. Execution of `am instrument` triggered a native crash in BlueStacks HD-Player.exe (exception code 0xc0000409 in ucrtbase.dll). Corroborates previous status `NOT_VERIFIABLE_ON_BLUESTACKS`.
- Implementation:
  - Canonical entities in `core:model`: `VehicleMake`, `VehicleModel`, `VehicleGeneration`, `EngineVariant`, `TransmissionVariant`, `VehicleSpecification`, `VehicleVariant`, `ResolvedVehicleMatch`, `VehicleMarket`, `FuelType`, `AspirationType`, `TransmissionType`, `DriveLayout`. Retains `ConfidenceLevel.VERIFIED_OEM` and `DataSourceReference`.
  - In `core:vehicle`: `VehicleCatalogRepository` interface and `DefaultVehicleCatalogRepository` implementing structured lookup, English/Persian alias search, and fuzzy variant resolution with strict model year production bounds and engine family differentiation (e.g. 206 TU3 != 206 TU5; Dena EF7 NA != Dena+ EF7 TC).
  - Wired into `AppModule` DI and integrated with `OwnedVehiclesViewModel` via `findCatalogMatches` and `applyCatalogSelection` so owners can autofill verified specifications into their vehicle drafts without mandatory constraints.
- Regressions & Tests:
  - 6 new tests in `VehicleCatalogRepositoryTest`: make/model lookup, Persian/English alias search, engine differentiation, production year boundary gating, and provenance preservation.
  - 1 new ViewModel regression in `OwnedVehiclesViewModelTest`: catalog match autofill of verified specifications into draft.
  - Total full suite across all 6 modules: 477 JVM tests (app 152, core:adapter 115, core:model 59, core:protocol 125, core:telemetry 19, core:vehicle 7), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors, 99 warnings. Duplicate class check: passed. Both APK assemblies: passed.
- Next: Maintenance Center data model, per-vehicle service schedules, and provenance-backed intervals.

# 2026-10-31 â Checkpoint E: maintenance foundation, due engine, and additive SQLite v7

- Local timestamp: 2026-10-31 10:28 Asia/Tehran.
- Goal: Implement normalized maintenance foundation, time/mileage due calculation engine, per-vehicle schedule/history isolation, and additive SQLite v7 persistence.
- Implementation:
  - Canonical models in `core:model`: `MaintenanceCategory`, `MaintenanceActionType`, `MaintenanceIntervalProvenance`, `DueState`, `MaintenanceItemDefinition`, `MaintenanceSchedule`, `VehicleMaintenanceRecord`, and `DueCalculationResult`.
  - Built `MaintenanceDueEngine` with conservative worst-evidence calculation: mileage-based lead window (default 1,000 km) and date-based lead window (default 30 days); missing last-service facts remain `DueState.UNKNOWN` without fabricating good or overdue health.
  - Published v6 remains immutable; created additive `MaintenanceFoundationMigration` v7 introducing `vehicle_maintenance_schedules` and `vehicle_maintenance_records` tables with vehicle foreign-key indexes.
  - Updated `CarObdDatabaseHelper` to DATABASE_VERSION = 7; added CRUD operations and dynamic due state calculation.
  - Implemented `MaintenanceRepository` and `SQLiteMaintenanceRepository` with atomic `recordCompletedService` that writes immutable history, advances schedule intervals, and updates next-due mileage/date.
  - Registered `MaintenanceRepository` in `AppModule`.
- Regressions & Tests:
  - 3 new JVM regressions in `MaintenanceDueEngineTest` covering unknown initial states, mileage lifecycle (OK -> DUE_SOON -> DUE -> OVERDUE), and multi-evidence worst-state prioritization.
  - New Android test `MaintenancePersistenceInstrumentedTest` testing schema v7, vehicle isolation, dynamic due transitions, service completion lifecycle, and untouched legacy tables.
  - Total full suite across all 6 modules: 480 JVM tests (app 152, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 7), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors, 99 warnings. Duplicate class check: passed. Both APK assemblies: passed.
- Next: Maintenance Center UI and per-vehicle service reminder management.

# 2026-10-31 â Checkpoint F: forensic review and hardening of vehicle catalog provenance

- Local timestamp: 2026-10-31 10:41 Asia/Tehran.
- Goal: Perform a forensic review of the vehicle catalog provenance; downgrade unsubstantiated `VERIFIED_OEM` claims to honest `ConfidenceLevel.COMMUNITY_CONFIRMED`; purge synthesized document IDs; verify technical data isolation; add regression coverage.
- Findings:
  - Catalog records contained synthesized/fake document IDs (e.g. `PSA-ENG-TU5JP4`, `IKCO-TG-EF7TC-2020`) and were marked `ConfidenceLevel.VERIFIED_OEM` without bundled verified OEM-signed source documentation in the repository.
- Changes:
  - Replaced fake document identifiers with genuine document titles or empty `urlOrDocument` (e.g. real Renault Logan MR388 manual).
  - Downgraded all community-derived engines and variant specifications to honest `ConfidenceLevel.COMMUNITY_CONFIRMED`.
  - Verified technical values: TU3 (108 belt teeth, 3.25L oil), TU5 (134 belt teeth, 3.75L oil), EF7 NA (127 teeth, 0.75mm plug gap), EF7 TC (127 teeth, 0.70mm turbo plug gap, 50,000 km belt), Shahin M15TC (114 teeth, 4.0L oil), Tondar K4M (132 teeth, 4.8L oil).
  - Ensured unverified specifications (tire sizes, battery spec, brake fluid) remain strictly null rather than fabricated.
- Regressions Added:
  - `testUnverifiedRecordsCannotBeSilentlyPromotedToVerifiedOem`: asserts that community data cannot be marked `VERIFIED_OEM` and source documents do not contain synthesized fake IDs.
  - `testMissingSpecificationsRemainStrictlyNull`: asserts missing fields remain null.
  - `testEngineVariantDataCannotLeakAcrossVariants`: verifies strict isolation between engine variants (e.g. TU3 vs TU5 belt teeth and oil capacities; EF7 NA vs EF7 TC aspiration and plug gaps).
- Tests & Verification:
  - 10 tests passed in `:core:vehicle:test` (0 failures, 0 errors, 0 skipped).
  - Total full suite across all 6 modules: 483 JVM tests passed, 0 failures, 0 errors, 0 skipped.
- Next: Inspect AVD emulator availability for runtime testing, then implement Maintenance Center UI.

# 2026-10-31 â Checkpoint G: bilingual Maintenance Center UI, service history, and runtime target investigation

- Local timestamp: 2026-10-31 11:05 Asia/Tehran.
- Goal: Implement the bilingual Maintenance Center UI wired to `MaintenanceRepository`, `OwnedVehicleRepository`, `MaintenanceDueEngine`; provide schedule management and service completion recording; inspect AVD emulator availability.
- AVD Target Investigation:
  - Verified Android SDK tools: `emulator.exe` present; Windows Hypervisor Platform WHPX(10.0.19044) is installed and usable (`emulator -accel-check`).
  - Command-line tools located at `C:\Users\mohammadreza.sarani\AppData\Local\Android\Sdk\skiaparser\3\bin` (`sdkmanager.bat`, `avdmanager.bat`).
  - Initiated installation of `system-images;android-34;google_apis;x86_64`.
  - Machine Policy Blocker: Connection was aborted during the 1.4 GB download by company host software (`Warning: An error occurred while preparing SDK package: An established connection was aborted by the software in your host machine`). Multi-gigabyte image downloads are restricted on this workstation network.
- Changes:
  - `MaintenanceMvi.kt`: Connected `MaintenanceViewModel` to `MaintenanceRepository`, `OwnedVehicleRepository`, and `VehicleCatalogRepository`. Added MVI state tracking `vehicles`, `selectedVehicle`, `schedules`, `records`, `specSheet`, KPI counters (`overdueCount`, `dueSoonCount`, `okCount`, `unknownCount`), and dialog states. Added intents for `SelectVehicle`, `InitializeDefaultSchedules`, `SubmitRecordService`, `SubmitSaveSchedule`, `ConfirmDeleteSchedule`, and `ConfirmDeleteRecord`. Recording completed service advances next due and updates vehicle odometer if newer.
  - `MaintenanceScreen.kt`: Material3 bilingual Compose UI featuring vehicle header with switcher dropdown and odometer formatting, KPI summary counters, three tabs (Upcoming Schedules, Service History, Specifications), schedule cards with dynamic status badges (`OVERDUE`, `DUE`, `DUE_SOON`, `OK`, `UNKNOWN`), progress indicator, remaining/due distance, "Mark Completed" dialog, "Add/Edit Schedule" dialog, and confirmation dialogs.
  - `AppModule.kt`: Updated `MaintenanceViewModel` injection to provide `MaintenanceRepository`, `OwnedVehicleRepository`, and `VehicleCatalogRepository`.
  - `strings.xml` & `values-fa/strings.xml`: Added bilingual strings for tabs, badges, KPI stats, actions, dialog fields, and empty states.
- Regressions & Tests:
  - Created `MaintenanceViewModelTest.kt` with 6 unit tests:
    - `initialStateLoadsVehiclesAndSelectedVehicleSchedules`
    - `emptyGarageShowsNullSelectedVehicleAndZeroCounts`
    - `initializeDefaultSchedulesCreatesRecommendedItemsForVehicle`
    - `recordCompletedServiceAdvancesScheduleAndUpdatesVehicleOdometer`
    - `switchingVehiclesIsolatesMaintenanceSchedulesAndHistory`
    - `deleteSchedulePreservesExistingServiceHistory`
  - Total full suite across all 6 modules: 489 JVM tests (app 158, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 10), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors. Duplicate class check: passed. Both APK assemblies (`assembleDebug` and `assembleDebugAndroidTest`): passed.
- Next: Field-level vehicle catalog provenance hardening (Checkpoint H) followed by Maintenance Reminder Engine (Checkpoint I).

# 2026-10-31 â Checkpoint H: field-level and auditable vehicle data provenance

- Local timestamp: 2026-10-31 11:15 Asia/Tehran.
- Goal: Make vehicle catalog data provenance field-level and auditable; ensure each technical value independently tracks its source, confidence, inference status, verification date, and safety for automatic recommendations.
- Changes:
  - `Provenance.kt`: Added `ProvenanceSourceType` enum (`OEM_MANUAL`, `OEM_PARTS_CATALOG`, `AUTHORIZED_SERVICE_BULLETIN`, `RELIABLE_WORKSHOP_SECONDARY`, `COMMUNITY_DATA`, `USER_INPUT`, `UNKNOWN`) and `FieldProvenance<T>` data class with factory helpers (`oemVerified`, `communityConfirmed`, `unverified`) and `isOemVerified()`.
  - `VehicleCatalogModels.kt`: Extended `VehicleSpecification` with `fieldProvenance: Map<String, FieldProvenance<*>>` and typed query helpers `getFieldProvenance<T>(field)`, `isFieldSafeForRecommendation(field)`, `getFieldConfidence(field)`.
  - `DefaultVehicleCatalogRepository.kt`: Populated explicit `fieldProvenance` maps across variants:
    - Tondar 90 (K4M): fields backed by Renault Logan MR388 manual are `FieldProvenance.oemVerified(...)` with `isOemVerified() == true` and `isSafeForAutomaticRecommendation = true`.
    - Iranian platforms (206 TU3, 206 TU5, Dena EF7 NA, Dena+ EF7 TC, Shahin M15TC): community data mapped as `FieldProvenance.communityConfirmed(...)`; uncertain replacement intervals have `isSafeForAutomaticRecommendation = false` preventing default mandatory scheduling.
    - Missing specifications (tires, battery spec, brake fluid) remain strictly `null` and absent from provenance maps.
- Regressions & Tests:
  - Added 3 new unit tests in `VehicleCatalogRepositoryTest`:
    - `testFieldLevelProvenanceIsolation`: asserts that OEM verification on one field does not promote missing or community fields.
    - `testCommunityDataCannotTriggerOemLabel`: asserts community intervals cannot claim OEM verification.
    - `testUncertainIntervalCannotBeMandatoryOemSchedule`: asserts uncertain intervals cannot be marked safe for automatic mandatory recommendations while physical parts facts (tooth counts) are verified.
  - Total full suite across all 6 modules: 492 JVM tests (app 158, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 13), 0 failures, 0 errors, 0 skipped.
- Next: Maintenance Reminder Engine foundation (WorkManager, per-vehicle schedule isolation, date/mileage triggers).

# 2026-10-31 â Checkpoint I: modern maintenance reminder engine and snooze architecture

- Local timestamp: 2026-10-31 11:35 Asia/Tehran.
- Goal: Implement modern Android Maintenance Reminder Engine using WorkManager; guarantee per-vehicle and per-schedule isolation; support truthful date and mileage due notifications without phantom background odometer progression; support non-destructive snooze semantics; deliver bilingual UI actions.
- Changes:
  - Added `androidx.work:work-runtime-ktx:2.8.1` to `gradle/libs.versions.toml` and `app/build.gradle.kts` (resolved from local Gradle cache, 100% offline).
  - `MaintenanceReminderModels.kt`:
    - `SnoozeDuration` enum (`ONE_DAY`, `THREE_DAYS`, `ONE_WEEK`) with English and Persian localization.
    - `ReminderIdentity`: deterministic unique work name per vehicle and schedule (`schedule_reminder_${vehicleId}_${scheduleId}`), vehicle tag (`vehicle_reminders_${vehicleId}`), and notification IDs.
    - `MaintenanceNotificationFormatter`: truthful bilingual formatting for English and Persian; avoids synthesizing fake odometer progression or "0 km" when mileage is unrecorded.
  - `MaintenanceReminderScheduler.kt` & `WorkManagerReminderScheduler.kt`:
    - Implemented modern scheduling with `ExistingWorkPolicy.REPLACE` preventing duplicate work.
    - No exact alarm permission needed; schedules standard WorkManager reminders for target dates or immediate due alerts.
    - Supports per-schedule cancellation, per-vehicle bulk cancellation, and snooze without altering underlying schedule intervals or maintenance history.
  - `MaintenanceReminderWorker.kt`:
    - `CoroutineWorker` querying vehicle and schedule facts, re-verifying due state with `MaintenanceDueEngine`.
    - Explicit `POST_NOTIFICATIONS` runtime permission checks and `SecurityException` protection.
  - UI & ViewModel Integration:
    - `MaintenanceMvi.kt`: Wired `MaintenanceReminderScheduler` into `MaintenanceViewModel`. Added `MaintenanceIntent.SnoozeSchedule`. Schedules reminders on due items, cancels on schedule deletion, and reschedules on service completion.
    - `MaintenanceScreen.kt`: Added Snooze button and dropdown menu to `ScheduleCard` for overdue, due, and due soon items.
    - `strings.xml` & `values-fa/strings.xml`: Added bilingual snooze strings.
- Regressions & Tests:
  - Created `MaintenanceReminderEngineTest.kt` with 10 unit tests:
    - `testReminderIdentityWorkNameIsIsolatedPerVehicleAndSchedule`
    - `testNotificationFormatterTruthfulWhenMileageAndDateKnown`
    - `testNotificationFormatterTruthfulWhenOverdue`
    - `testNotificationFormatterTruthfulWhenMileageIsUnknown`
    - `testSnoozeDurations`
    - `testViewModelSchedulesReminderOnLoadWhenDue`
    - `testViewModelCancelsReminderWhenScheduleDeleted`
    - `testViewModelSnoozeIntentDispatchesToScheduler`
    - `testMultiVehicleSchedulerIsolation`
    - `testServiceCompletionReschedulesReminderWithUpdatedOdometer`
  - Total full suite across all 6 modules: 502 JVM tests (app 168, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 13), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors. Both APK assemblies (`assembleDebug` and `assembleDebugAndroidTest`): passed.
- Next: Priority 3 Service Item Imagery Foundation and visual asset integration.

# 2026-10-31 â Checkpoint J: service item imagery foundation and automotive vector assets

- Local timestamp: 2026-10-31 11:45 Asia/Tehran.
- Goal: Implement dedicated service item imagery foundation with safe original vector illustrations for automotive parts and categories, wire into Maintenance UI schedule and history views.
- Changes:
  - Created 11 crisp, safe, original 24x24 Android vector drawables in `app/src/main/res/drawable/`:
    - `ic_part_oil_can.xml` (Engine oil, general fluids)
    - `ic_part_oil_filter.xml` (Canister oil filter)
    - `ic_part_air_filter.xml` (Intake air filter panel)
    - `ic_part_cabin_filter.xml` (Cabin ventilation filter)
    - `ic_part_fuel_filter.xml` (Inline fuel filter)
    - `ic_part_spark_plug.xml` (Spark plug with electrode and insulator)
    - `ic_part_timing_belt.xml` (Pulleys and toothed timing belt)
    - `ic_part_brake_disc.xml` (Brake rotor disc and caliper)
    - `ic_part_battery.xml` (12V automotive battery with posts)
    - `ic_part_coolant.xml` (Radiator and coolant thermometer)
    - `ic_part_transmission.xml` (Gearbox transmission cogs)
  - `MaintenanceIconResolver.kt`:
    - Item key keyword matching (`oil_filter`, `air_filter`, `cabin_filter`, `fuel_filter`, `spark_plugs`, `timing_belt`, `brake_pads`, `battery`, `coolant`, `transmission`, `engine_oil`).
    - Fallback matching for `MaintenanceCategory` (`ENGINE`, `TRANSMISSION`, `BRAKES`, `FILTERS`, `FLUIDS`, `IGNITION`, `ELECTRICAL`).
    - Safe default fallback to oil can icon.
  - `MaintenanceScreen.kt`:
    - Updated `ScheduleCard` with a 38dp container showcasing the item vector drawable.
    - Updated `HistoryRecordCard` with a 32dp container showcasing the item vector drawable.
- Regressions & Tests:
  - Created `MaintenanceIconResolverTest.kt` with 3 unit tests verifying:
    - `testResolvesSpecificMaintenanceItemKeys`
    - `testResolvesCategoryFallbacksWhenItemKeyIsGeneric`
    - `testResolvesDefaultDrawableWhenUnknownAndNoCategory`
  - Total full suite across all 6 modules: 505 JVM tests (app 171, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 13), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors. Both APK assemblies: passed.
- Next: Priority 4 Service History UX improvements (edit/delete records, audit timestamps, export) followed by Priority 5 Ownership Home integration & catalog recommendations.

# 2026-10-31 â Checkpoint K: service history UX improvements, editing flow, and truthful export

- Local timestamp: 2026-10-31 11:55 Asia/Tehran.
- Goal: Implement complete service history editing flow preserving identity and creation timestamps; implement truthful service history export dialog with clipboard copying; deliver bilingual UI dialogs.
- Changes:
  - `MaintenanceMvi.kt`:
    - Added `EditRecordDialogModel` tracking `recordId`, `vehicleId`, `itemKey`, `performedKm`, `performedDateMs`, `costTomans`, `serviceCenter`, `partsBrand`, `partNumber`, `notes`, `provenance`, `createdAtMs`.
    - Added `editRecordDialog` and `exportText` state properties.
    - Added `OpenEditRecordDialog`, `DismissEditRecordDialog`, `UpdateEditRecordDialog`, `SubmitSaveRecord`, `ExportServiceHistory`, `DismissExportDialog` intents.
    - `SubmitSaveRecord`: validates mileage, constructs updated `VehicleMaintenanceRecord` preserving creation timestamp, calls `maintenanceRepository.saveRecord()`, updates vehicle odometer if newer, and refreshes UI.
    - `ExportServiceHistory`: formats truthful summary including vehicle name, current odometer, export date, record count, and per-record item, date, mileage, cost, parts brand/number, and notes.
  - `MaintenanceScreen.kt`:
    - Added Edit button on each `HistoryRecordCard` next to Delete.
    - Added "Export History" outlined action button to `HistoryTabContent` header.
    - Implemented `EditRecordDialog` with bilingual fields and validation.
    - Implemented `ExportHistoryDialog` displaying monospace formatted export with one-tap clipboard copy and confirmation toast.
  - `strings.xml` & `values-fa/strings.xml`: Added bilingual strings for edit record, export history, and clipboard confirmation.
- Regressions & Tests:
  - Added 2 new unit tests in `MaintenanceViewModelTest.kt`:
    - `editExistingRecordUpdatesFieldsAndPreservesIdentity`: verifies dialog population, field editing, persistence update, and identity/createdAtMs preservation.
    - `exportServiceHistoryFormatsTruthfulSummary`: verifies truthful formatting of vehicle identity, odometer, record facts, and dismiss behavior.
  - Total full suite across all 6 modules: 507 JVM tests (app 173, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 13), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors. Both APK assemblies: passed.
- Next: Priority 5 Ownership Home integration & catalog recommendations.

# 2026-10-31 â Checkpoint L: vehicle-specific maintenance recommendations and ownership home integration

- Local timestamp: 2026-10-31 12:05 Asia/Tehran.
- Goal: Connect vehicle catalog recommendations directly to maintenance schedules gated strictly by `isFieldSafeForRecommendation`; integrate ownership management directly with the Maintenance Center.
- Changes:
  - `MaintenanceMvi.kt`:
    - Updated `InitializeDefaultSchedules` to query `catalogRepository.findMatchingVariants()` for the active vehicle.
    - Gated timing belt interval and replacement schedules with `isFieldSafeForRecommendation("timingBeltIntervalKm")`:
      - For verified OEM vehicles with verified intervals (e.g. Tondar 90 K4M at 60,000 km), assigns `MaintenanceIntervalProvenance.OEM_RECOMMENDED`.
      - For community-confirmed vehicles (e.g. Dena Plus, Peugeot 206, Samand, Quick), preserves truthful `MaintenanceIntervalProvenance.VERIFIED_SECONDARY` without falsely claiming OEM verification.
  - `OwnedVehiclesScreen.kt`:
    - Displayed current odometer km in vehicle row when recorded.
    - Added direct "Maintenance Center" shortcut action button to navigate directly from the owned vehicle card to the Maintenance Center for that selected vehicle.
  - `MainActivity.kt`:
    - Connected `OwnedVehiclesScreen`'s `onNavigateToMaintenance` lambda to `navController.navigate(AppDestination.Maintenance.route)`.
- Regressions & Tests:
  - Added unit test in `MaintenanceViewModelTest.kt`:
    - `initializeDefaultSchedulesForOemVehicleAssignsOemProvenance`: verifies that OEM-verified vehicle (Tondar 90 K4M) receives OEM-gated timing belt interval and `OEM_RECOMMENDED` provenance, while community vehicles strictly receive `VERIFIED_SECONDARY`.
  - Total full suite across all 6 modules: 508 JVM tests (app 174, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 13), 0 failures, 0 errors, 0 skipped.
  - Lint: 0 errors. Both APK assemblies: passed.
- Next: Complete master prompt priorities and final continuation verification.

# 2026-10-31  Checkpoint M: parts and fitment domain foundation, verified catalog, and Parts Center UI

- Local timestamp: 2026-10-31 12:35 Asia/Tehran.
- Goal: Establish parts and fitment domain foundation, curate initial verified parts catalog, build bilingual Parts & Consumables Center UI, and resolve runtime verification status.
- Workstation Runtime Target Investigation:
  - Inspected Android SDK tooling and system images. Local directory \AppData\Local\Android\Sdk\system-images\android-34\google_apis\x86_64\ contains only an aborted 192-byte \.installData\ file from previously aborted attempts.
  - \emulator -list-avds\ returns 0 AVDs; \.android\avd\ directory is empty.
  - Corporate workstation network policy terminates multi-gigabyte system-image downloads with host connection abort (\An established connection was aborted by the software in your host machine\).
  - Per explicit instructions, documented honest status as **BLOCKED_BY_WORKSTATION**; stopped retrying large downloads to prevent session burn.
- Implementation:
  - Canonical entities in \core:model\ (\PartsModels.kt\):
    - \PartCategory\ enum: \ENGINE_OIL\, \OIL_FILTER\, \AIR_FILTER\, \CABIN_FILTER\, \FUEL_FILTER\, \SPARK_PLUG\, \TIMING_BELT\, \BRAKE_PADS\, \BATTERY\, \COOLANT\, \TRANSMISSION_FLUID\, \ACCESSORY_BELT\, \WIPER_BLADES\.
    - \PartDefinition\: id, category, canonicalNameEn, canonicalNameFa, descriptionEn, descriptionFa, isSafetyCritical, iconKey.
    - \OemPartNumber\: partNumber, manufacturer, isDirectOem, source, notes.
    - \AftermarketPartReference\: brand, partNumber, series, barcode, notes, source.
    - \PartSpecification\: key, labelEn, labelFa, value, unit, confidence, source.
    - \VehicleFitment\: id, partDefinitionId, vehicleMakeId, vehicleModelId, vehicleGenerationId, engineId, engineCode, engineFamily, minYear, maxYear, notesEn, notesFa, confidence, source, oemNumbers, aftermarketReferences, specifications.
    - \CompatiblePartItem\: aggregation with \isSafetyCritical\, \isOemVerified\, \primaryOemNumber\.
  - Repository in \core:vehicle\ (\PartsRepository.kt\ and \DefaultPartsRepository.kt\):
    - Strict fitment matching with engine mismatch rejection (never guess across engine variants).
    - Model year boundary enforcement.
    - Truthful confidence preservation (VERIFIED_OEM preserved for Renault MR388 Tondar 90 parts; COMMUNITY_CONFIRMED for IKCO/Saipa parts).
    - Initial verified parts dataset:
      - Renault Tondar 90 (K4M 16V): OEM spark plugs \77 00 500 168\ (0.9mm gap), oil filter \77 00 274 177\, air filter \82 00 431 051\, timing belt \13 0C 175 29R\ (132 teeth), front brake pads \41 06 021 92R\.
      - Peugeot 206 (TU5 16V): OEM spark plugs \5960 F3\ (0.9mm gap), timing belt \816 G4\ (134 teeth), oil filter \1109 AL\.
      - Peugeot 206 (TU3 8V): Spark plugs \5962 5c\ (0.8mm gap), timing belt \816 71\ (104/108 teeth, strictly separated from TU5).
      - IKCO Dena / Samand (EF7 NA): Spark plugs \K6RTC\ / \FR8DC+\, timing belt (127 teeth), oil filter.
      - Saipa Quik / Saina (M15): Spark plugs \BPR6ES\, timing belt \410493\ (107 teeth), oil filter.
  - UI in \pp\ (\PartsCenterMvi.kt\ and \PartsCenterScreen.kt\):
    - Bilingual English + Persian RTL.
    - Active vehicle header card showing displayName, trim, year, engine code, and formatted odometer.
    - Prominent technical disclaimer banner: *Generic technical illustration  verify exact part numbers on vehicle before purchase* / *????? ?????? ???  ??? ?? ???? ????? ??? ?? ????? ?? ???? ????? ????? ????*.
    - Search bar across part names, OEM numbers, and aftermarket brands.
    - Category filter chips with 24x24 automotive vector assets.
    - Compatible part cards showcasing safety-critical badge, confidence badge, primary OEM number with copy-to-clipboard button, alternative OEM numbers, aftermarket brand pills, technical specifications, and safety/fitment warnings.
    - Connected navigation route \AppDestination.Parts\ in \MainActivity.kt\, added "Parts" action button to \OwnedVehiclesScreen.kt\, and registered in \AppModule.kt\.
- Regressions & Tests:
  - 7 new unit tests in \PartsRepositoryTest.kt\: distinct categories, verified OEM specs, engine mismatch rejection (TU3 vs TU5 belt/plug separation), unknown engine safety rejection, model year boundary checks, confidence preservation, and number/brand search.
  - 5 new unit tests in \PartsCenterViewModelTest.kt\: initial state loading, category filtering, search across OEM/aftermarket parts, empty vehicle handling, clipboard copy effect/toast.
  - Total full suite across all 6 modules: **520 JVM tests** (app 179, core:adapter 115, core:model 62, core:protocol 125, core:telemetry 19, core:vehicle 20), **0 failures, 0 errors, 0 skipped**.
  - Lint: **0 errors**. Duplicate class check: **passed**.
  - Both APK assemblies: \ssembleDebug\ and \ssembleDebugAndroidTest\ **passed**.

# 2026-10-31  Checkpoint UI-0: VehicleSpecSheet runtime crash repair and Koin Compose context cleanup

- Local timestamp: 2026-10-31 14:15 Asia/Tehran.
- Goal: Fix confirmed runtime \NoSuchMethodError\ on \VehicleSpecSheet.<init>(...)\ constructor from \IranianCarSpecsDatabase.<clinit>\ and \MaintenanceViewModel\; clean up Koin Compose context warnings.
- Root Cause Analysis:
  - In \:core:model\ \MaintenanceModels.kt\, \VehicleSpecSheet\ was declared without \@JvmOverloads\. When default parameters \sources = emptyList()\ and \confidence = ConfidenceLevel.VERIFIED_OEM\ were introduced, callers expecting the 14-parameter constructor (such as \IranianCarSpecsDatabase\ when compiled against earlier signatures or in default call sites) failed with \java.lang.NoSuchMethodError\.
  - In addition, Compose tree in \MainActivity.kt\ lacked a top-level \KoinContext\ wrapper, emitting repeated \No Compose Koin context setup, taking default\ warnings.
- Fix:
  - Added \@JvmOverloads constructor\ to \VehicleSpecSheet\ in \MaintenanceModels.kt\, guaranteeing generation of the exact 14-parameter bytecode signature \<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;FLjava/util/List;Ljava/lang/String;FLjava/lang/String;FIILjava/lang/String;FLjava/util/List;)V\.
  - Updated \IranianCarSpecsDatabase.kt\ to explicitly supply \sources\ and \confidence\ on all entries.
  - Wrapped \MainActivity.kt\'s Compose root in \org.koin.compose.KoinContext { ... }\.
  - Added \IranianCarSpecsDatabaseTest.kt\ verifying that \IranianCarSpecsDatabase.<clinit>\ loads cleanly and that all constructor overloads instantiate without errors.
  - Executed \:app:clean :core:model:clean :app:assembleDebug\ to completely purge stale DEX/class artifacts.

### Checkpoint UI-1: Canonical Design System, Reusable Shared Components, Ownership Home, and Automotive Guide
- Commit: à713e- Scope & Implementation:
  - Canonical design system tokens in \CarObdDesignSystem.kt\ (Spacing, CornerRadius, Elevation, SemanticColors, CarObdSizes).
  - Reusable automotive components in \SharedCarObdComponents.kt\ (CarObdTopBar, VehicleContextHeader, SectionHeader, PrimaryFeatureCard, SecondaryActionCard, CompactStat, StatusChip, EmptyState, ErrorState, CarObdCard, CarObdPrimaryButton, CarObdSecondaryButton, ProvenanceBadge).
  - Ownership Home experience in \HomeMvi.kt\ and \HomeScreen.kt\ emphasizing My Vehicle, Maintenance, and Parts with truthful empty states (UNSCHEDULED, never fake OK).
  - Automotive User Guide & Encyclopedia in \DefaultGuideRepository.kt\ and \GuideScreen.kt\ with bilingual topics, safety notices, and search.
  - Verification: Full suite 520 JVM unit tests passed, 0 failures, verified on attached Samsung phone.

### Checkpoint UI-2: Dashboard Layout & Gauge Reconstruction, Trip Screen Clearance, Guided Connection Center, Disconnected Diagnostics Recovery, Simulator Polish & First-Run 3-Step Onboarding
- Scope & Implementation:
  - Dashboard Reconstruction (\DashboardScreen.kt\, \CircularGauge.kt\, \DigitalMetricCard.kt\, \HorizontalBarGauge.kt\): Eliminated 1-character-per-line vertical text collapse on 360dp screens by moving gauge titles outside/above dials and constraining readouts; grouped custom widgets into paired 2-column rows for MEDIUM/SMALL sizes and full width for LARGE; removed raw unstyled debug text; streamlined widget management into compact controls.
  - Trip Screen Clearance (\TripScreen.kt\): Wrapped loose unstyled warnings into structured info cards; added bottom clearance spacer to prevent bottom navigation bar overlap on real phone hardware.
  - Connection Center Rebuild (\ConnectionCenterScreen.kt\): Added prominent 100% Read-Only Safety Guarantee card; multi-step connection progress visualizer; explicit profile editor wizard with educational transport descriptions (Bluetooth Classic, BLE, Wi-Fi TCP with 192.168.0.10:35000 defaults, Simulator, USB Serial).
  - Diagnostics Disconnected Recovery (\DiagnosticsScreen.kt\): Added guided recovery state when disconnected with one-tap action buttons (Connect Adapter, Use Simulator, Learn OBD-II & DTC Guide) and alert dialog when tapping scan while disconnected.
  - Simulator Experience Polish (\SimulatorSelectorScreen.kt\): Added prominent synthetic diagnostic simulation explanation banner and inline connection controls.
  - First-Run Onboarding & Animated Splash (\AutomotiveSplashScreen.kt\, \OnboardingScreen.kt\, \MainActivity.kt\): Fast automotive headlights illumination reveal splash sequence; progressive 3-step onboarding (Overview, Iranian Vehicle Selection, Safety & Diagnostic Options) with smooth transition into Home.
  - Verification: 520 JVM unit tests passed (exit 0); full APK assembled and installed on attached device \9889db474842494948\; verified live on phone across onboarding, Home, Diagnostics, and Connection Center with 0 crashes.

## HOME-R1 — reconciliation

Home draft preserved locally at `73e0dd8` on `home/ownership-preservation`; development based on company `d8b1f2d` in sibling worktree. See HOME_STABILIZATION.md for every file decision. No duplicate ownership architecture ported. Removed unsupported 100% safety claims and invalid percent formatting; physical safety code unchanged. Correctness stabilization is now active; earlier completion/runtime claims are historical.
