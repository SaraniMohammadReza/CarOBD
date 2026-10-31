# CAROBD — AGENT CONTINUATION HANDOFF

This is the canonical, agent-independent continuation record. Read it before analysis. Historical reports remain intact; this file records the actual continuation and distinguishes verified source from work in progress. Never reset to an old report's HEAD.

## 1. Project Identity

CarOBD; `git@github.com:SaraniMohammadReza/CarOBD.git`; branch **feat/car-obd-development** only. Android/Kotlin/Compose application package `com.sarani.carobd`. Gradle modules `app`, `core:model`, `core:adapter`, `core:protocol`, `core:telemetry`, `core:vehicle`. Existing MVI/ViewModel/use-case/repository/session architecture; serialized typed adapter dispatch and one shared telemetry producer. No AGENTS.md found in the project/parent checkout on this inspection. Do not delegate this active mission to another agent.

## 2. Current Git State

Verified source snapshot: `1f327633e3375e1ea20d7d324c0f93f772694703`, equal to origin after push. Branch `feat/car-obd-development` only. Later commits containing this document necessarily sit above the sampled source hash; inspect actual HEAD rather than resetting to this snapshot.

Localized numeric input is verified and pushed; final integration is recorded in section4. Final documentation is published in ARCHITECTURE.md, OBD_SUPPORT_MATRIX.md, COMPETITOR_FEATURE_MATRIX.md, COMPETITOR_GAP_DEVELOPMENT.md, ANDROID_STUDIO_OFFLINE_SETUP.md and the safety/gaps documents. Staging is explicit per checkpoint.

Pre-existing untracked `report.md` must remain unchanged and uncommitted. SHA256 `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325` verified repeatedly. No dependency on it; no caches/SDK/JDK/APK enter Git. No master modification or force push.

## 3. Completed Engineering Work

All listed source checkpoints were tested and pushed immediately:

| Commit | Work / main files | Actual verification |
|---|---|---|
| `f657191` | docs/COMPETITOR_FEATURE_MATRIX.md: source-qualified competitor gap audit before implementation | Diff reviewed/check passed; docs only |
| `91795ef` | AppPreferencesManager, VehicleOnboardingModal, Maintenance MVI/screen: optional vehicle/fuel/odometer metadata, no sample Peugeot/TU5 or numeric defaults; existing owner data retained | App JVM suite, two added persistence/unknown tests |
| `a5dc754` | DiagnosticSessionReport; DiagnosticReportMigration/Store; CarObdDatabaseHelper v4; ObdRepositoryImpl; diagnostic/history UI | Partial/malformed/storage-failure regressions, actual SQLite recreation/upgrade/rollback; Android8/8 passed |
| `dabdc81` | TelemetryGraphHistory, LiveTelemetry.pidSupport, producer publication; Dashboard MVI/screen; SensorPicker; layout codec/preferences | Capability selection, bounded/gap-aware per-PID histories, layout order/style/size/recreation; complete offline clean gate, 107 executed tasks |
| `7ef5476` | SensorPicker scrolling/vertical size choices; plural report counts; accessibility show-on-screen test helper | Final exact offline gate exit0 1m24s, JVM441 zero failures/errors/skips, lint84 warnings/0errors; EN8/8 3m3s; FA/RTL900×2000/font130 8/8 3m7s |
| `d4c41d5` | ObdRepository.disconnectIfCurrent; repository lock; ConnectionForegroundService stale start/stop/disconnect handling; RuntimeNavigation test | Three new repository regressions; app135 tests passed; actual Android8/8 exit0 2m54s including stale service intents and current-token disconnect |
| `9ed09ea` | AdmissionGuardedTransport; TransportFactory; actual repository-session revocation regression; LAN UI test | App136/adapter115 JVM pass; Android8/8 exit0,2m58s; LAN shared-group pregrant observed, not explicit denial/regrant |
| `70704aa` | DiagnosticReportExport/JsonStrings; history ViewModel/Card/Screen SAF; core/VM/Android JSON tests | App139/model57/adapter115 pass; Android9/9 exit0,2m59s; SAF launch/cancel |
| `98953bb` | Backup XML exclusions; packaged-policy regression; actual DocumentsUI Save/readback | Android10/10 exit0,3m6s; no cloud backup/migration executed |
| `08ff3e8` | NotificationMeasurements; foreground saved units/language; four unit tests and Settings/notification UI assertion | App143 pass exit0,3s; Android10/10 exit0,3m11s; same session through imperial→metric |
| `1f32763` | VehicleSetupNumbers; onboarding validation/error support; four regressions and actual form acceptance/rejection/clear | App147 pass exit0,3s; FA10/10 exit0,3m22s; final full offline gate464 tests,lint84/0 |

Full details/path explanations: [overnight engineering record](OVERNIGHT_DEVELOPMENT_HANDOFF.md), [full development report](COMPETITOR_GAP_DEVELOPMENT.md), [previous runtime QA](RUNTIME_QA_CONTINUATION.md). The final report qualifies historical results separately from the current source gate; this file is the continuation authority.

## Active mission — 2026-10-31

Actual recovered starting local/remote HEAD: `14f924ea60d3ff350537fae143faf334ecf79691`, development branch. Only pre-existing report.md is untracked. Prior464 JVM/10EN/10FA results below are historical, not new test results. No AGENTS.md found. New mandatory product scope: ownership-first multiple manual vehicles, normalized catalog/fitment provenance, per-vehicle maintenance/history/reminders, offline bilingual guide and efficient skippable intro. Preserve all diagnostic safety. SQLiteOpenHelper is the actual database; do not invent Room/DAOs or replace storage unnecessarily.

Checkpoint A reassessment: see [implementation report](SPRINT_4_IMPLEMENTATION_REPORT.md), [append-only log](DEVELOPMENT_CHECKPOINT_LOG.md), [new test evidence](SPRINT_4_TEST_EVIDENCE.md), [current final handoff](SPRINT_4_FINAL_HANDOFF.md). Current task is additive ownership persistence and real offline vehicle management. Existing ECU routing profile is not a uniquely owned vehicle; do not change physical routing on owner-vehicle selection. Preserve old garage/preferences/diagnostic rows; unassigned historical data must not be guessed into a new vehicle.

Checkpoint B ownership persistence is pushed in `5753ec848738e1ce1974941949e1ba9771d023ae`: nullable `OwnedVehicle`, additive SQLite v5 tables/selection, repository state and DI. It deliberately does not copy legacy garage rows and does not touch ECU routing selection. Targeted verification passed: `:core:model:test :app:testDebugUnitTest :app:assembleDebugAndroidTest`, exit 0. The new Android SQLite lifecycle test compiled but was not executed because no emulator/device run was authorized in this checkpoint. Exact next task: ownership UI for list/create/edit/select/delete using `OwnedVehicleRepository`, bilingual resources, and navigation clearly separate from ECU Profiles.

Checkpoint C implements the bilingual My Vehicle destination and offline list/create/edit/select/delete flow, localized validation, interface-backed repository, and additive SQLite v6 nullable fuel/notes fields. Final software gate passed with 210 JVM tests, zero failures/errors/skips, both APK assemblies, lint and duplicate checks. Android instrumentation compiled but is `NOT_VERIFIABLE_ON_BLUESTACKS`: the local Pie64 API 28 instance accepted APK installation and then became offline before instrumentation; the connected Gradle path also lacked the UTP additional-test-output artifact. This is not a runtime pass or product failure.

Checkpoint D delivers the provenance-aware normalized vehicle catalog foundation: canonical entities in `:core:model` (VehicleMake, VehicleModel, VehicleGeneration, EngineVariant, TransmissionVariant, VehicleSpecification, VehicleVariant, ResolvedVehicleMatch, VehicleMarket, FuelType, AspirationType, TransmissionType, DriveLayout) retaining ConfidenceLevel.VERIFIED_OEM and DataSourceReference; `VehicleCatalogRepository` in `:core:vehicle` with DefaultVehicleCatalogRepository providing structured lookup, Persian/English alias searching, and fuzzy variant resolution respecting model year production boundaries and engine family differentiation (e.g. 206 TU3 != 206 TU5; Dena EF7 NA != Dena+ EF7 TC); AppModule DI registration and OwnedVehiclesViewModel integration. Complete full suite passed across all 6 modules: 477 JVM tests, 0 failures/errors/skips, lint 0 errors / 99 warnings, duplicate class check passed, both APKs assembled. BlueStacks investigation confirmed native crash (0xc0000409 in ucrtbase.dll) upon instrumentation call. Pushed in commit `a8ac89b`.

Checkpoint E implements the Maintenance Foundation, due engine, and additive SQLite v7 persistence: canonical models in `:core:model` (MaintenanceCategory, MaintenanceActionType, MaintenanceIntervalProvenance, DueState, MaintenanceItemDefinition, MaintenanceSchedule, VehicleMaintenanceRecord, DueCalculationResult); `MaintenanceDueEngine` with conservative worst-evidence evaluation; additive `MaintenanceFoundationMigration` v7 creating `vehicle_maintenance_schedules` and `vehicle_maintenance_records`; `CarObdDatabaseHelper` updated to v7 with dynamic due calculations; `MaintenanceRepository` and `SQLiteMaintenanceRepository` with atomic `recordCompletedService` history creation and interval advancing; `AppModule` registration; 3 new unit tests in `MaintenanceDueEngineTest` and new Android test `MaintenancePersistenceInstrumentedTest`. Complete full suite passed across all 6 modules: 480 JVM tests, 0 failures/errors/skips, lint 0 errors / 99 warnings, both APKs assembled. Pushed in commit `15d9688`.

Checkpoint F completes the forensic audit and hardening of Vehicle Catalog Provenance: purged synthesized document IDs; downgraded unsupported `VERIFIED_OEM` labels to truthful `ConfidenceLevel.COMMUNITY_CONFIRMED`; verified technical values across TU3, TU5, TU5P, EF7 NA, EF7 TC, M15TC, K4M; enforced null preservation for unmeasured specifications; added regressions preventing silent promotion to VERIFIED_OEM and verifying cross-variant engine data isolation. 483 JVM tests passed across all 6 modules. Pushed in commit `de6d35f`.

Checkpoint G delivers the bilingual Maintenance Center UI and per-vehicle service management: wired `MaintenanceViewModel` to `MaintenanceRepository`, `OwnedVehicleRepository`, and `VehicleCatalogRepository`; implemented complete Material3 bilingual Compose UI (`MaintenanceScreen.kt`) with active vehicle switcher, formatted odometer, KPI summary counters (Overdue, Due Soon, OK, Unscheduled), three tabs (Upcoming Schedules, Service History, Specifications), schedule cards with dynamic status badges (`OVERDUE`, `DUE`, `DUE_SOON`, `OK`, `UNKNOWN`), progress indicators, remaining/due distance, "Mark Completed" dialog with immutable history recording and vehicle odometer updating, "Add/Edit Schedule" dialog, and confirmation dialogs; added bilingual English and Persian strings; added 6 unit tests in `MaintenanceViewModelTest`. AVD runtime target inspection established that WHPX is usable, but downloading a 1.4 GB system image is blocked by host software aborting large connections. Complete full suite passed across all 6 modules: 489 JVM tests, 0 failures/errors/skips, lint 0 errors, both APKs assembled. Pushed in commit `0d6f75e`.

Checkpoint H hardens Vehicle Catalog data provenance to be field-level and auditable: added `ProvenanceSourceType` and `FieldProvenance<T>` with OEM/community factories; extended `VehicleSpecification` with `fieldProvenance` map and typed query helpers; mapped field provenance across all variants distinguishing genuine OEM manual references (Tondar 90 MR388) from community-derived data; prevented uncertain intervals from driving mandatory automatic recommendations; added 3 new regressions in `VehicleCatalogRepositoryTest`. 492 JVM tests passed across all 6 modules. Pushed in commit `63693c5`.

Checkpoint I implements the modern Android Maintenance Reminder Engine: added `androidx.work:work-runtime-ktx:2.8.1` offline; created `MaintenanceReminderModels.kt` with `SnoozeDuration`, `ReminderIdentity` (unique work name `"schedule_reminder_${vehicleId}_${scheduleId}"`, vehicle tag `"vehicle_reminders_${vehicleId}"`), and `MaintenanceNotificationFormatter` (truthful bilingual English/Persian formatting; never assumes background odometer progression; formats due date honestly without synthesizing fake numbers); created `MaintenanceReminderScheduler` interface and `WorkManagerReminderScheduler` implementation using `ExistingWorkPolicy.REPLACE` without requesting exact alarm permissions; created `MaintenanceReminderWorker` with `MaintenanceDueEngine` validation and explicit `POST_NOTIFICATIONS` runtime permission checks; wired into `MaintenanceViewModel` (`SnoozeSchedule` intent, cancellation on delete, automatic rescheduling on service completion or schedule edit); updated `MaintenanceScreen.kt` with Snooze button and dropdown menu on overdue/due/due-soon schedule cards; added bilingual strings. Added 10 unit tests in `MaintenanceReminderEngineTest.kt`. Full suite 502 JVM tests passing across all 6 modules, lint 0 errors, both APKs assembled. Pushed in commit `081758a`.

Checkpoint J delivers the Service Item Imagery Foundation: created 11 safe original 24x24 vector drawables (`ic_part_oil_can`, `ic_part_oil_filter`, `ic_part_air_filter`, `ic_part_cabin_filter`, `ic_part_fuel_filter`, `ic_part_spark_plug`, `ic_part_timing_belt`, `ic_part_brake_disc`, `ic_part_battery`, `ic_part_coolant`, `ic_part_transmission`); implemented `MaintenanceIconResolver` with keyword and category fallback mapping; integrated visual icons into `ScheduleCard` and `HistoryRecordCard` in `MaintenanceScreen.kt`. Added 3 unit tests in `MaintenanceIconResolverTest`. Full suite 505 JVM tests passing across all 6 modules, lint 0 errors, both APKs assembled. Pushed in commit `baccc65`.

Checkpoint K implements Service History UX improvements, editing flow, and export: added `EditRecordDialogModel`, state management, and intents for opening, editing, saving records, and exporting history; `SubmitSaveRecord` validates mileage, preserves record identity and creation timestamp, updates vehicle odometer if newer, and persists via `maintenanceRepository.saveRecord()`; `ExportServiceHistory` formats truthful multi-record summary with vehicle metadata and notes; updated `MaintenanceScreen.kt` with Edit button on `HistoryRecordCard`, "Export History" header action, `EditRecordDialog`, and `ExportHistoryDialog` with clipboard copy; added bilingual strings. Added 2 unit tests in `MaintenanceViewModelTest.kt`. Full suite 507 JVM tests passing across all 6 modules, lint 0 errors, both APKs assembled. Pushed in commit `cc9cbb3`.

Checkpoint L implements vehicle-specific maintenance recommendations and ownership home integration: gated timing belt and replacement recommendations with `isFieldSafeForRecommendation("timingBeltIntervalKm")`; assigns `OEM_RECOMMENDED` to verified OEM vehicles (Tondar 90 K4M at 60,000 km) and `VERIFIED_SECONDARY` to community vehicles (Dena, 206, Samand, Quick); integrated owned vehicle cards in `OwnedVehiclesScreen.kt` with formatted odometer display and direct "Maintenance Center" action navigating directly to the selected vehicle's maintenance center via `MainActivity.kt`. Added unit test in `MaintenanceViewModelTest.kt`. Full suite 508 JVM tests passing across all 6 modules, lint 0 errors, both APKs assembled.

Checkpoint M implements the Parts & Fitment domain foundation, verified parts catalog, and Parts & Consumables Center UI: evaluated workstation runtime test target and documented honest status as `BLOCKED_BY_WORKSTATION` due to corporate firewall network abortion on large downloads; created canonical `PartsModels.kt` in `:core:model` (`PartCategory`, `PartDefinition`, `OemPartNumber`, `AftermarketPartReference`, `VehicleFitment`, `PartSpecification`, `CompatiblePartItem`); created `PartsRepository` in `:core:vehicle` (`DefaultPartsRepository`) with engine mismatch rejection (never guess across engine variants), model year bounds, and confidence preservation; populated initial verified parts dataset (Tondar 90 K4M OEM references from Renault MR388, Peugeot 206 TU5 OEM, TU3 community confirmed, Dena EF7 NA, Saipa Quik M15); built bilingual English + Persian RTL `PartsCenterScreen.kt` and `PartsCenterMvi.kt` with active vehicle header card, prominent generic technical illustration notice, search across OEM/aftermarket parts, category filter chips with automotive vector assets, compatible part cards with safety-critical badge, confidence badge, primary OEM number with copy-to-clipboard button, alternative OEM numbers, aftermarket brand pills, technical specifications, and safety/fitment warnings; connected navigation route `AppDestination.Parts` in `MainActivity.kt`, added "Parts" action button to `OwnedVehiclesScreen.kt`, and registered in `AppModule.kt`. 12 new unit tests (7 in `PartsRepositoryTest.kt`, 5 in `PartsCenterViewModelTest.kt`). Total full suite across all 6 modules: 520 JVM tests, 0 failures, 0 errors, 0 skipped; 0 lint errors; both APK assemblies passed.

Checkpoint UI-0 fixes the runtime `NoSuchMethodError` for `VehicleSpecSheet.<init>(...)` constructor by adding `@JvmOverloads constructor` in `MaintenanceModels.kt`, explicitly supplying parameters in `IranianCarSpecsDatabase.kt`, adding `IranianCarSpecsDatabaseTest.kt` regression, wrapping Compose root in `org.koin.compose.KoinContext` in `MainActivity.kt` to clean up Koin context warnings, and executing a clean APK rebuild. Ready for UI/UX reconstruction.

Checkpoint UI-1 & UI-2 establishes the canonical CarOBD design system (`CarObdDesignSystem.kt`), reusable shared automotive components (`SharedCarObdComponents.kt`), ownership-focused Home experience (`HomeMvi.kt`, `HomeScreen.kt`), automotive knowledge guide (`GuideScreen.kt`, `DefaultGuideRepository.kt`), bilingual English and Persian localization, and wires Home as the primary product surface in `MainActivity.kt` and `AppModule.kt`. 525+ JVM unit tests passing across all 6 modules, lint 0 errors, assembleDebug clean, interactive device smoke test verified on Android 9 Pie (360dp RTL) with 0 crashes. Pushed to `feat/car-obd-development`. Ready for UI-3 (My Vehicle & Details).

New mission supersedes prior suspend instruction: final full native poweroff only after all completed work/reports pushed, fetched remote equality, clean tracked tree, unchanged report.md and no active builds/tests. Never shut down with unpushed important work. Do not claim mission/product completion while major scope remains incomplete.

## 4. Previous Verified Checkpoint

**VERIFIED source; next report scope NOT_STARTED.** Localized numeric metadata input is complete. VehicleSetupNumbers normalizes English/Persian/Arabic digits and Arabic decimal separator; rejects ambiguous grouping, exponent/nonfinite/overflow/fractional odometer input; blank stays null. VehicleOnboardingModal uses exactly the same parsed values for validation/save and highlights errors. Four parser regressions passed in app147/0failure/error/skip, exit0,3s (`/tmp/carobd-overnight-numeric-unit.log`). Complete FA/RTL900×2000/font130 instrumentation passed: Android10/10, zero failures/errors/skips, exit0,3m22s (`/tmp/carobd-overnight-numeric-fa-ui.log`). Android test sets Persian decimal, rejects grouping and clears back to optional blank.

Do not repeat earlier vehicle/report/dashboard/service/export/notification implementation. Do not weaken assertions to hide failures. Source checkpoint1f32763 is pushed. The fresh full offline gate and cache revalidation now pass; final EN instrumentation passed10/10,exit0,3m12s. No unfinished source changes. Next engineering task is the combined report scope in section11.

LAN nuance: ACCESS_LOCAL_NETWORK shares NEARBY_DEVICES and may grant without another dialog. Actual emulator marker was `PREGRANTED_ON_REQUEST_NO_DIALOG`. Earlier new test wrongly demanded a deny button; corrected to handle real permission states. No actual LAN denial/regrant or physical OS revocation is established, and no vehicle/LAN socket is opened by this UI test.

## 5. Remaining Priorities

- **P0 VERIFIED (software/emulator):** generation-locked foreground teardown and runtime admission retirement; no regrant resurrection. Physical OS revocation remains REQUIRES_DEVICE/ADAPTER.
- **P0 REQUIRES_DEVICE/ADAPTER:** Classic/BLE/USB/Wi-Fi revocation, CCCD/late callbacks, exact USB target/detach, network loss and sustained lifecycle. No physical operations authorized overnight.
- **P1 VERIFIED:** captured-report JSON export, real SAF cancellation and successful Save/readback; automatic OS cloud-backup exclusions. Actual cloud/OS migration behavior not executed; full app backup remains NOT_IMPLEMENTED.
- **P1 PARTIAL:** combine existing readiness/MIL/02/09/06 typed readers with persistent report and expected session identity. Report currently03/07/0A only. Preserve per-reader partial outcomes, no fake depth/health.
- **P1 PARTIAL:** lazy/paged report detail loading for large archives; more canonical sensors only after proven decoder/support; existing-widget style editing.
- **P2 NOT_IMPLEMENTED/PARTIAL:** manual fuel ledger, maintenance source confidence, alerts, full app backup/restore, contextual help. Preserve measured/estimated distinction.
- **P3 DEFERRED:** HUD/parking/OEM datasets. Dashcam/monetization outside scope; physical scripts/plugins/writes rejected.

See [remaining gaps](CAROBD_REMAINING_GAPS.md) and [safety status](CAROBD_SAFETY_STATUS.md).

## 6. Architecture Map

Paths below are relative to repository root; `app/...` means `app/src/main/java/com/sarani/carobd/`.

- UI/navigation: `app/.../MainActivity.kt` hosts NavHost; `app/.../ui/screens/`; components in `ui/components/`.
- MVI/use cases: `ui/screens/dashboard/DashboardMvi.kt`, `ui/screens/diagnostics/DiagnosticsMvi.kt`, `domain/usecase/TelemetryAndDtcUseCases.kt`, `domain/usecase/ConnectUseCases.kt`.
- Repositories/DI: `domain/repository/ObdRepository.kt`, `data/repository/ObdRepositoryImpl.kt`, `ActiveVehicleRepository.kt`, `core/di/AppModule.kt`.
- SQLite: `data/db/CarObdDatabaseHelper.kt`, `DtcHistoryMigration.kt` v2, TripEvidenceMigration v3, DiagnosticReportMigration/Store v4. Published old migration SQL must not be rewritten.
- Session: `data/session/DiagnosticSession.kt`, one owner/generation and bounded admission8. Foreground ownership `service/ConnectionForegroundService.kt`.
- Adapter: `core/adapter/src/main/kotlin/com/sarani/carobd/core/adapter/elm327/Elm327Adapter.kt`; single wire command, bounded16KiB response, cancellation retirement and timeout resynchronization.
- Authorization: `core/adapter/.../safety/AuthorizedDiagnosticCommand.kt`, CommandSafetyClassifier; mandatory adapter dispatch. No UI-only bypass.
- Transports: `app/.../data/transport/TransportFactory.kt`, BluetoothClassicTransport, BleTransport, WifiTcpTransport, UsbSerialTransport, IsolatedSimulatorTransport. New admission wrapper is in core/adapter/.../transport/.
- Parsers: `core/protocol/.../obd2/ElmResponseEnvelopeParser.kt`, `dtc/DtcParser`; `core/adapter/.../command/DiagnosticScanRunner.kt`.
- Telemetry: `core/telemetry/.../scheduler/TelemetryProducer.kt`; canonical plan `core/protocol/.../obd2/PidPollingPlan.kt`; TripComputer in core/telemetry/.../trip/. Widgets observe shared LiveTelemetry, never create a producer.
- Recording/replay: `app/.../data/recording/RecordingController.kt`; `core/adapter/.../transport/FileTranscriptStore.kt`, TranscriptReplayRunner. Replay has no physical delegate.
- Settings/permissions: `app/.../core/storage/AppPreferencesManager.kt`, LocaleHelper; `core/permissions/AndroidAccess.kt`, UI RuntimeAccessControls. No automatic connection/command retry after granting access.

## 7. Safety Invariants

Physical04 denied;08 blocked; security/write/programming/arbitrary manufacturer requests and unknown commands denied; narrow ASCII typed authorization at adapter boundary; one command in flight; one producer; request/cancellation/generation ownership; bounded admission; timeout/late-response quarantine; transport loss retires session; exact USB target/interface with no fallback; protocol mismatch rejection; BLE CCCD acknowledgement; simulator/replay isolation; conservative PID capability; unknown never zero; stale never current; unknown responder stays null; multi-responder and category evidence retained; malformed/timeout/no-data never a successful empty scan. No real vehicle testing authorized.

## 8. Build Environment

Persistent machine cache `/home/mohammad/.gradle` (GRADLE_USER_HOME unset, normal default). Wrapper9.8.1; AGP9.3.3; Kotlin/Compose compiler2.2.10; coroutines1.10.1; JVM target17; core JBR17.0.14 toolchain. Gradle runtime Temurin24.0.2+12 at `/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12`; daemon criteria also select equivalent Gradle-managed JDK24. Do not confuse runtime24 with target17.

SDK `/home/mohammad/Android/Sdk`; compile/target37, min24; actual package platforms;android-37.0, build-tools37.0.0. Persistent user init `/home/mohammad/.gradle/init.d/carobd-repositories.gradle` scopes same-coordinate Aliyun Google routing to CarObd and keeps Central/portal fallback; no CLI-only --init-script needed. Do not commit machine caches/JDK/SDK/credentials or change versions. Actual Studio offline imports succeeded earlier at14:38:40 and16:35:12 on Oct30. No new GUI sync is inferred from CLI success. Final all-config warm-up/offline help will be rerun after current work.

## 9. Test Evidence

Exact fresh offline gate (run after final source changes):

```bash
JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12 \
ANDROID_HOME=/home/mohammad/Android/Sdk \
./gradlew --offline clean :app:assembleDebug :app:testDebugUnitTest \
 :app:checkDebugDuplicateClasses :app:lintDebug :core:model:test :core:adapter:test \
 :core:protocol:test :core:telemetry:test :core:vehicle:test :app:assembleDebugAndroidTest --console=plain
```

Final fresh gate on1f32763: exit0,1m28s,107/107 tasks executed (`/tmp/carobd-overnight-final-offline-gate.log`). JVM464/0fail/error/skip: app147,model57,adapter115,protocol125,telemetry19,vehicle1. Lint84warnings/0errors, no suppressions; duplicate-class check and both app/test APK assemblies pass. apksigner verification exit0,v2 true; debug APK22,114,928 bytes,SHA256 f4f7a8a90c0eacfe6ac4155fb3a509902cf0284a6d3834ad92058e2dcfbcb65b. APK ignored, not in Git.

Offline cache warm-up exit0,3s:133 artifact-bearing configurations/2331 artifact occurrences/56 metadata buckets/0 failures; normal /home/mohammad/.gradle and Temurin24.0.2+12. Offline help exit0,921ms. Logs `/tmp/carobd-overnight-final-cache.log`, `/tmp/carobd-overnight-final-offline-help.log`. Existing Studio actual imports14:38:40 and16:35:12 remain the GUI proof; no new GUI sync claimed from CLI. Final FA10/10 exit0,3m22s,900×2000/font130; final EN10/10,exit0,3m12s (`/tmp/carobd-overnight-final-en-ui.log`). Resource checker448 keys and production SQL migration/recreation/rollback host checks pass.

Emulator: official Pixel_10 API37 x86_64 Playstore16KiB; isolated adb5038. Verify `adb -P5038 devices` and ro.kernel.qemu=1 before emulator operations; never select physical serial. Prefix Android task with `ANDROID_ADB_SERVER_PORT=5038 ADB_SERVER_SOCKET=tcp:localhost:5038`; task `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.qaLocale=en` (fa for Persian). FA uses wm900×2000/font1.3, then restore wm reset/font1.0. UTP fresh-install fixture is required by onboarding/permissions; do not clear owner device data.

Evidence logs are named in section3 and OVERNIGHT_DEVELOPMENT_HANDOFF.md; local screenshots/log copies `/home/mohammad/.local/share/carobd-build/competitor-gap-qa/` are optional QA evidence, not source dependencies. No physical device, adapter or vehicle PASS exists. Preserve failed logs; no-device infrastructure failure is not a product PASS.

## 10. Known Limitations

Combined report readiness/MIL/02/09/06 absent; no diagnostic health verdict; code history is separate. Current report reader loads up to100 reports including raw evidence; lazy details remain desirable. Custom/OEM catalog is not proof of physical support. Real Android permission revocation/adapter behavior remain unverified. Company-machine cache is not the home cache and must not be assumed present. Existing Studio Device Manager/adb5037 issue is separate from verified Gradle Sync.

## 11. Exact Next Actions

1. Fetch/switch/pull safely as section12; inspect actual HEAD/status and this document. No source task is half-implemented. Do not reset to a historical hash or rerun successful gates absent a change/environment issue.
2. Add one combined report scope through existing typed readiness/MIL/02/09/06 readers in ObdRepositoryImpl and the existing DiagnosticSession generation owner. Capture expected token before any read; preserve returned per-service partial outcomes, nullable identities/raw evidence, and explicit unread services. Do not auto-retry or manufacture completeness. Extend core model/store/UI/export schema intentionally and add cancellation/replacement/partial-failure regressions before exposing the scope.
3. Optimize DiagnosticReportStore.recent: currently reads up to100 full records plus raw evidence. Introduce lightweight summaries and lazy selected detail loading, keeping old records/export exact; test archive bounds/recreation/error paths.
4. Add editing of existing dashboard widget style using DashboardLayoutCodec and existing shared telemetry; preserve conservative decoder/support gating and 24-widget bound. No new producer or arbitrary polling.
5. Physical phone/adapter bench validation remains REQUIRES_DEVICE/ADAPTER; no vehicle action authorized by this handoff. Follow SPRINT5_BENCH_CHECKLIST.md only with explicit authorization. Preserve typed safety invariants and do not expand physical capability from software PASS.

After every coherent verified checkpoint: review diff, relevant tests, git diff --check, inspect status, explicit intended staging, commit and immediate push origin feat/car-obd-development. Never master/force. Update this file and preserve report.md. Full app backup, broader fuel/maintenance provenance and alerts remain later gaps.

## 12. Morning Resume Instructions

```bash
cd /path/to/CarOBD
cat docs/AGENT_CONTINUATION_HANDOFF.md
git status --short
git fetch origin
git switch feat/car-obd-development
git pull --ff-only origin feat/car-obd-development
git rev-parse HEAD origin/feat/car-obd-development
```
Do not discard local changes to switch/pull. Open the Git root directory in Android Studio; use compatible Temurin24.0.2+12 for Gradle, Java/Kotlin target17, default persistent user Gradle home and installed SDK37/build-tools37.0.0. Install/resolve dependencies on that machine; no cache is in Git. See existing [Studio setup](ANDROID_STUDIO_OFFLINE_SETUP.md). Read active task and latest evidence before any refactor.

## 13. Last Updated

2026-10-30 23:50 +03:30, Asia/Tehran. Requested window ends2026-10-31 07:00 or actual execution budget/blocker first; no artificial waiting. Final native sleep is authorized only after final report/source push, remote equality and completed builds/tests. No sleep has been invoked.

Host preflight: Linux/systemd on ext4; systemd-detect-virt=none; active local Wayland seat0 session42 Remote=no; Lenovo hardware; no SSH_CONNECTION; logind CanSuspend=yes. Recheck before final action. Never suspend a container/VM/SSH/shared host, never force through inhibitors, and do not perform later development after suspend invocation. Full chronological evidence remains in OVERNIGHT_DEVELOPMENT_HANDOFF.md.

Final delivery is a safe execution-budget checkpoint before07:00, not a claim that the entire roadmap or physical validation is complete. All source through1f32763 is tested and pushed; this documentation publication follows it. Final APK was reinstalled/launched on emulator-5554 only, leaving fresh Persian onboarding with no active diagnostic session. Final Git equality and clean tracked tree are checked after publication; exact delivery hash is in Git history/final chat. Requested suspend is only invoked after that verification and no active build/test. No further development is scheduled after suspend.

## HOME-R1 — reconciliation

Home draft preserved locally at `73e0dd8` on `home/ownership-preservation`; development based on company `d8b1f2d` in sibling worktree. See HOME_STABILIZATION.md for every file decision. No duplicate ownership architecture ported. Removed unsupported 100% safety claims and invalid percent formatting; physical safety code unchanged. Correctness stabilization is now active; earlier completion/runtime claims are historical.

## HOME-R2 — explicit catalog identity and fitment

Owned-vehicle resolution now distinguishes UNKNOWN, ENGINE_REQUIRED, AMBIGUOUS and EXACT. Blank identity never discovers a default vehicle. Maintenance/Home/Parts consume only exact resolution; no legacy name-based fallback. Default schedules only take numeric intervals from safe field evidence; generic fields remain UNVERIFIED with null intervals. User schedule edits are USER_CUSTOM. Parts search intersects vehicle-compatible fitments. Targeted maintenance/parts JVM and core vehicle tests PASS (exit0); localization and diff checks run before publication. No runtime claim.
