# CAROBD — AGENT CONTINUATION HANDOFF

This is the canonical, agent-independent continuation record. Read it before analysis. Historical reports remain intact; this file records the actual continuation and distinguishes verified source from work in progress. Never reset to an old report's HEAD.

## 1. Project Identity

CarOBD; `git@github.com:SaraniMohammadReza/CarOBD.git`; branch **feat/car-obd-development** only. Android/Kotlin/Compose application package `com.sarani.carobd`. Gradle modules `app`, `core:model`, `core:adapter`, `core:protocol`, `core:telemetry`, `core:vehicle`. Existing MVI/ViewModel/use-case/repository/session architecture; serialized typed adapter dispatch and one shared telemetry producer. No AGENTS.md found in the project/parent checkout on this inspection. Do not delegate this active mission to another agent.

## 2. Current Git State

Verified source snapshot: `08ff3e806009a6a3044ae1533eb5054d6f9c92d1`, equal to origin after push. Branch `feat/car-obd-development` only. Later commits containing this document necessarily sit above the sampled source hash; inspect actual HEAD rather than resetting to this snapshot.

The localized numeric-input checkpoint is active; see section4. Final integration documentation is being completed in ARCHITECTURE.md, OBD_SUPPORT_MATRIX.md, COMPETITOR_FEATURE_MATRIX.md, COMPETITOR_GAP_DEVELOPMENT.md, ANDROID_STUDIO_OFFLINE_SETUP.md and the safety/gaps documents. Staging is explicit per checkpoint.

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

Full details/path explanations: [overnight engineering record](OVERNIGHT_DEVELOPMENT_HANDOFF.md), [competitor continuation draft](COMPETITOR_GAP_DEVELOPMENT.md), [previous runtime QA](RUNTIME_QA_CONTINUATION.md). The draft may contain explicitly pending fields until final integration; this file is the current task authority.

## 4. Current Active Task

**VERIFIED: localized numeric metadata input**. VehicleSetupNumbers normalizes English/Persian/Arabic digits and Arabic decimal separator; rejects ambiguous grouping, exponent/nonfinite/overflow/fractional odometer input; blank stays null. VehicleOnboardingModal uses exactly the same parsed values for validation/save and highlights errors. Four parser regressions passed in app147/0failure/error/skip, exit0,3s (`/tmp/carobd-overnight-numeric-unit.log`). Complete FA/RTL900×2000/font130 instrumentation passed: Android10/10, zero failures/errors/skips, exit0,3m22s (`/tmp/carobd-overnight-numeric-fa-ui.log`). Android test sets Persian decimal, rejects grouping and clears back to optional blank.

Do not repeat earlier vehicle/report/dashboard/service/export/notification implementation. Do not weaken assertions to hide failures. Next publish this checkpoint, then fresh full offline gate, EN instrumentation, cache revalidation and final documentation.

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

Prior full gate on7ef5476: exit0,1m24s,107 executed tasks,JVM441/0fail/error/skip,lint84warnings/0errors. Subsequent targeted and Android results are in section3; latest localized parser app147 passed. Full integration will be refreshed before delivery.

Emulator: official Pixel_10 API37 x86_64 Playstore16KiB; isolated adb5038. Verify `adb -P5038 devices` and ro.kernel.qemu=1 before emulator operations; never select physical serial. Prefix Android task with `ANDROID_ADB_SERVER_PORT=5038 ADB_SERVER_SOCKET=tcp:localhost:5038`; task `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.qaLocale=en` (fa for Persian). FA uses wm900×2000/font1.3, then restore wm reset/font1.0. UTP fresh-install fixture is required by onboarding/permissions; do not clear owner device data.

Evidence logs are named in section3 and OVERNIGHT_DEVELOPMENT_HANDOFF.md; local screenshots/log copies `/home/mohammad/.local/share/carobd-build/competitor-gap-qa/` are optional QA evidence, not source dependencies. No physical device, adapter or vehicle PASS exists. Preserve failed logs; no-device infrastructure failure is not a product PASS.

## 10. Known Limitations

Combined report readiness/MIL/02/09/06 absent; no diagnostic health verdict; code history is separate. Current report reader loads up to100 reports including raw evidence; lazy details remain desirable. Custom/OEM catalog is not proof of physical support. Real Android permission revocation/adapter behavior remain unverified. Company-machine cache is not the home cache and must not be assumed present. Existing Studio Device Manager/adb5037 issue is separate from verified Gradle Sync.

## 11. Exact Next Actions

1. Localized metadata is green: app147 and FA10/10 passed. Publish intended checkpoint and record actual hash; no wire changes. Do not repeat successful FA tests without a new change.
2. Execute the exact fresh offline gate and final Android EN/FA where changed. Record actual counts/lint/APK hash/size/signature; no pending placeholders in final reports.
3. Run offline all-configuration warm-up then offline help in normal ~/.gradle. See ANDROID_STUDIO_OFFLINE_SETUP.md; do not move caches or put machine routing in project repositories.
4. Reconcile final documents, commit/push all verified work, fetch and verify exact local/remote heads; only unrelated report.md may remain untracked. Verify no active builds/tests before final native sleep.
5. Morning engineering: add one combined report scope via existing typed readers and expected generation guard; separately preserve partial failures and unread services. Then lazy archive details and existing-widget editing. Physical validation stays explicitly separate.

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

2026-10-30 23:44 +03:30, Asia/Tehran. Requested window ends2026-10-31 07:00 or actual execution budget/blocker first; no artificial waiting. Final native sleep is authorized only after final report/source push, remote equality and completed builds/tests. No sleep has been invoked.

Host preflight: Linux/systemd on ext4; systemd-detect-virt=none; active local Wayland seat0 session42 Remote=no; Lenovo hardware; no SSH_CONNECTION; logind CanSuspend=yes. Recheck before final action. Never suspend a container/VM/SSH/shared host, never force through inhibitors, and do not perform later development after suspend invocation. Full chronological evidence remains in OVERNIGHT_DEVELOPMENT_HANDOFF.md.
