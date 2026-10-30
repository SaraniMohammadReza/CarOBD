# CAROBD — AGENT CONTINUATION HANDOFF

This is the canonical, agent-independent continuation record. Read it before analysis. Historical reports remain intact; this file records the actual continuation and distinguishes verified source from work in progress. Never reset to an old report's HEAD.

## 1. Project Identity

CarOBD; `git@github.com:SaraniMohammadReza/CarOBD.git`; branch **feat/car-obd-development** only. Android/Kotlin/Compose application package `com.sarani.carobd`. Gradle modules `app`, `core:model`, `core:adapter`, `core:protocol`, `core:telemetry`, `core:vehicle`. Existing MVI/ViewModel/use-case/repository/session architecture; serialized typed adapter dispatch and one shared telemetry producer. No AGENTS.md found in the project/parent checkout on this inspection. Do not delegate this active mission to another agent.

## 2. Current Git State

Snapshot taken immediately before publishing this documentation checkpoint, 2026-10-30 23:20 +03:30:

- Local HEAD at latest source snapshot: `9ed09ea`.
- Remote HEAD after verified push: `9ed09ea`.
- Branch: `feat/car-obd-development`; source checkpoint pushed. The documentation commit containing this snapshot is necessarily above the sampled source hash; always inspect actual Git HEAD rather than resetting to this value.
- Staged diff empty before this file. Uncommitted source: runtime-admission guard/factory/repository regression and LAN permission UI test; report JSON exporter/shared escaping/history export UI/ViewModel/tests. These are **IN_PROGRESS**, not claimed as pushed/verified together yet.
- Uncommitted documentation: ARCHITECTURE.md, OBD_SUPPORT_MATRIX.md, docs/ANDROID_STUDIO_SETUP.md, docs/COMPETITOR_FEATURE_MATRIX.md, new docs/COMPETITOR_GAP_DEVELOPMENT.md.
- Pre-existing untracked `report.md` must remain unchanged and uncommitted. SHA256 `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325` verified again. It is not a build/source input.

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

Full details/path explanations: [overnight engineering record](OVERNIGHT_DEVELOPMENT_HANDOFF.md), [competitor continuation draft](COMPETITOR_GAP_DEVELOPMENT.md), [previous runtime QA](RUNTIME_QA_CONTINUATION.md). The draft may contain explicitly pending fields until final integration; this file is the current task authority.

## 4. Current Active Task

**VERIFIED: permission revocation guard**. New `core/adapter/src/main/kotlin/com/sarani/carobd/core/adapter/transport/AdmissionGuardedTransport.kt` rechecks the existing admission callback on connect/write and passive isConnected checks. Physical Classic/BLE/Wi-Fi are wrapped by `app/.../data/transport/TransportFactory.kt`; simulator/USB keep existing ownership. A revoked generation never revives on regrant. No new commands, permissions, reader or producer. Five new guard unit tests and an actual repository-session regression passed (`/tmp/carobd-overnight-revocation-tests.log`, exit0 11s; app136, adapter115). Updated RuntimeNavigation test exercises LAN permission outcomes as well as existing flows. Full Android run `/tmp/carobd-overnight-revocation-ui-final.log` is now VERIFIED: exit0,2m58s,8/8,zero failures/errors/skips. Permission-guard source checkpoint: `9ed09ea` (pushed); no need to repeat its successful tests without a change.

Important LAN finding: do not demand a separate dialog after Bluetooth grant. Official Android guidance says ACCESS_LOCAL_NETWORK shares NEARBY_DEVICES and may be granted without another prompt. An initial new test wrongly expected a deny button and failed; the corrected test records the actual path. Current log marker is `CAROBD_LAN_PERMISSION_FLOW=PREGRANTED_ON_REQUEST_NO_DIALOG`. It also verifies admission remains blocked if a request is not granted. Do not claim actual LAN denial/regrant or OS permission revocation from that pregrant run. No LAN/vehicle socket is opened by this test.

**VERIFIED: structured report export**. `core/model/.../DiagnosticReportExport.kt`, `JsonStrings.kt`, DiagnosticTranscript shared escaping, `app/.../ui/screens/evidence/DtcHistoryViewModel.kt`, DiagnosticReportCard/EvidenceScreen, EN/FA strings and new `DiagnosticReportExportViewModelTest`. Existing reports only; schema1 JSON, null identities, scope/source/category/responders/raw/outcomes and explicit unread services/no health assertion. SAF CreateDocument, write on IO, selection ID captured across picker recreation, failure never says Saved. Core JSON and ViewModel tests now pass: app139/model57/adapter115, exit0,9s (`/tmp/carobd-overnight-export-tests.log`). Independent Android JSONObject/output test and real SAF picker cancellation check passed: `/tmp/carobd-overnight-export-ui.log`, exit0,2m59s,9/9,zero failures/errors/skips. Actual SAF Save callback will additionally be exercised in the next privacy/export verification iteration; do not conflate cancellation with a successful provider write. Full integration gate remains to be refreshed after subsequent changes.

Do not repeat the earlier vehicle/report/dashboard implementation. Do not weaken assertions to hide failures. Keep permission and export checkpoints independently staged.

## 5. Remaining Priorities

- **P0 VERIFIED (software/emulator):** admission guard unit and repository tests plus Android8/8 pass; publish intended files independently of the unfinished export. Physical OS revocation remains REQUIRES_DEVICE/ADAPTER.
- **P0 REQUIRES_DEVICE/ADAPTER:** physical Bluetooth/BLE/USB/Wi-Fi revocation, CCCD/late callbacks, exact USB target/detach, network loss and sustained lifecycle. Existing software policies/tests are not physical certification. No physical operations authorized overnight.
- **P1 VERIFIED:** report JSON export unit/Android file/JSON tests and SAF cancellation pass; explicit provider Save verification is next. New review found unrestricted template OS cloud-backup rules; exclude private diagnostic data from automatic cloud backup without deleting local data.
- **P1 PARTIAL:** reports currently only03/07/0A; next combined scope must use existing typed readiness/02/09/06 readers and expected session token. No fake diagnostic depth selector.
- **P1 PARTIAL:** lazy/paged report details for large archives; more canonical sensors only after proven decoder/support; existing-widget style editing.
- **P2 NOT_IMPLEMENTED/PARTIAL:** manual fuel ledger, maintenance source confidence, alerts, full app backup, contextual help. Preserve measured/estimated distinction.
- **P3 DEFERRED:** HUD/parking/OEM read-only datasets. Dashcam/monetization out of scope; physical scripts/plugins/writes rejected.

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

Environment prefix for CLI: `JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12 ANDROID_HOME=/home/mohammad/Android/Sdk`.

Exact major gate (last fully verified at7ef5476):
```bash
./gradlew --offline clean :app:assembleDebug :app:testDebugUnitTest :app:checkDebugDuplicateClasses :app:lintDebug :core:model:test :core:adapter:test :core:protocol:test :core:telemetry:test :core:vehicle:test :app:assembleDebugAndroidTest --console=plain
```
Exit0,107 tasks executed,JVM441/0fail/0error/0skip,lint84warnings/0errors. Latest targeted guard tests: `./gradlew --offline :core:adapter:test :app:testDebugUnitTest --console=plain`, exit0,11s,app136+adapter115/0fail/0error/0skip. New JSON core tests were also run by the active command; inspect reports for final counts.

Emulator: official Pixel_10 API37 x86_64 Playstore16KiB image; isolated adb5038 because Studio/default adb5037 has separate instability. Check `adb -P5038 devices` and ro.kernel.qemu=1; never select a physical serial. Android command prefix additionally `ANDROID_ADB_SERVER_PORT=5038 ADB_SERVER_SOCKET=tcp:localhost:5038`; task `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.qaLocale=en`. Current active run log above; wait for completion. UTP fresh installation is required by permission/onboarding fixture. Preserve failed logs; do not call a no-device failure a product test PASS.

FA test uses emulator wm size900x2000/font_scale1.3, then restore wm size reset/font1.0. Previous FA and EN runs each8/8 passed; only RuntimeNavigation is the broad UI workflow, other methods are SQLite/applicationId checks. No physical device/adapter/vehicle PASS exists. Local evidence `/home/mohammad/.local/share/carobd-build/competitor-gap-qa/`; do not depend on it for source portability.

## 10. Known Limitations

Combined report readiness/MIL/02/09/06 absent; no diagnostic health verdict; code history is separate. Current report reader loads up to100 reports including raw evidence; lazy details remain desirable. Custom/OEM catalog is not proof of physical support. Real Android permission revocation/adapter behavior remain unverified. Company-machine cache is not the home cache and must not be assumed present. Existing Studio Device Manager/adb5037 issue is separate from verified Gradle Sync.

## 11. Exact Next Actions

1. Permission guard checkpoint is green and being published separately. Record its actual Git hash after push; do not redo the successful8/8 run absent changes. Continue the export ViewModel/Android tests, leaving physical admission validation explicitly separate.
2. Export targeted tests and Android9/9 now pass. Next: protect automatic OS cloud backup via the existing XML policies, test packaged policies, and exercise SAF Save/readback on simulator evidence. Do not enable cloud sync or claim full application backup/restore.
3. Run the exact fresh offline gate and final Android EN/FA where changed. Capture test counts, lint, APK SHA256/size and signature; update final documents without pending fields.
4. Run `./gradlew --offline -PcarobdCacheWarmup=true carobdWarmAllConfigurations --no-configuration-cache --console=plain`, then `./gradlew --offline help` in the same persistent user cache. Add/update docs/ANDROID_STUDIO_OFFLINE_SETUP.md, preserving existing setup evidence rather than duplicating contradictory instructions.
5. Continue safe high-value implementation within the available budget; update this file after each substantial checkpoint and at least every45min. Before ending, push everything verified, fetch and compare exact heads; leave only documented report.md unrelated.

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
Do not discard local changes to switch/pull. Open the Git root directory in Android Studio; use compatible Temurin24.0.2+12 for Gradle, Java/Kotlin target17, default persistent user Gradle home and installed SDK37/build-tools37.0.0. Install/resolve dependencies on that machine; no cache is in Git. See existing [Studio setup](ANDROID_STUDIO_SETUP.md). Read active task and latest evidence before any refactor.

## 13. Last Updated

2026-10-30 23:28 +03:30, Asia/Tehran workstation time. Requested window ends 2026-10-31 07:00 local or actual execution budget/blocker first; no artificial waiting. Final native sleep is authorized **only after** verified push, saved final report, completed build/tests, and confirmation this is the actual local workstation with suspend support. No sleep has been invoked. Never suspend a container/VM/SSH/shared host; report inability honestly. Sleep must be the last system action, with no later development scheduled.

### Latest verified checkpoint additions

`9ed09ea` (pushed): ongoing runtime admission guard, six new JVM regressions and Android8/8. Report export is introduced by the source commit publishing this handoff update: DiagnosticReportExport/JsonStrings plus shared transcript escaping; history ViewModel and Card/Screen SAF flow; EN/FA copy; three core and three ViewModel regressions; independent Android JSON persistence/readback; actual SAF cancellation. Targeted tests app139/model57/adapter115 exit0,9s; Android9/9 exit0,2m59s. No new dependencies or physical dispatch paths. Automatic OS backup templates are the next concrete privacy correction, not yet changed at this snapshot.

Host sleep preflight (no suspend performed): Linux/systemd on ext4, systemd-detect-virt=none, active local Wayland seat0 session42 Remote=no, Lenovo hardware, no SSH_CONNECTION, logind CanSuspend=yes. Recheck before the final action. Do not suspend until all verified source and final reports are pushed and all build/test tasks have finished.
