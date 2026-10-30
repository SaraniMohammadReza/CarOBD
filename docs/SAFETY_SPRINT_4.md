# CarOBD Autonomous Sprint 4 Continuation

Date: 2026-10-30. Development branch: `feat/car-obd-development`.
Continuation start: `cae1700de3c64429abc43e37d72ace946517162e`.
Whole Sprint4 baseline: `f3024d75183650e5ccc3111ddc483e2f1390d4e2`.
Historical master object: `0cf39bd48984a2a0cb49a035def1726ff5b89514` (not modified).

## Resumed work and production paths

At continuation entry, the previous phase commits were e459a17 (permissions/ownership), 88abe36 (readiness), 088cfe5 (Mode09) and cae1700 (Mode02). Recording work was in progress. The existing transport/model architecture was retained rather than replaced.

- Recording: production session transport → passive RecordingTransport → canonical DiagnosticTranscriptBuilder → RecordingController → atomic FileTranscriptStore → session list/detail/delete and SAF JSON/CSV export/import. Repository tests verify real command CR and raw RX/prompt retention alongside parsed outcomes. Capture begins only when explicitly started; initialization before that point is not retroactively recorded. TX records the host write attempt, not proof of a bus transmission or ECU acceptance.
- Replay: TranscriptReplayRunner creates ReplayTransport with no physical delegate. Commands must match expected recorded TX and read-only admission. RX timing is command-relative; timeouts, quarantine, disconnect and responder evidence remain distinguishable. It never sends recorded TX to Bluetooth/BLE/Wi-Fi/USB. Imports validate bounds, schema, elapsed ordering and structural strings before assigning a fresh local ID.
- DTC history: complete production scans persist exact category/responder, nullable observed MIL/protocol and provenance. Version2 additive migration retains rows while exposing legacy guessed fields as unverified/unknown. DTC presence is not MIL evidence. Failed/incomplete scans are not recorded as complete. Persistence failure remains a history warning rather than converting diagnostic failure into success.
- Dashboard: ATRV adapter supply voltage is separate from0142 control-module voltage. Per-PID observation times preserve stale evidence without making failed reads fresh; unknown values display an em dash and true measured zero remains zero. Observed protocol/active profile/actual simulator state are distinct from selected metadata. Recording state is visible.

## Product behavior and limits

[Current feature matrix](../OBD_SUPPORT_MATRIX.md) is authoritative for scope. Readiness, Mode09, Mode02 and CAN Mode06 have repository/use-case/ViewModel/UI flows through typed evidence envelopes. Discovery capability, unsupported, malformed, no-data, negative, partial, disconnected, timeout and adapter failure are not collapsed into numeric zero or a healthy result. Multi-read operations are bound to the session token.

Readiness requires exactly four status bytes and preserves ignition type. Mode09 validates VIN count/layout and character data, fixed CALID/name widths and binary CVN groups. Known incomplete CVN bytes remain Partial. Unknown responders remain nullable. Non-CAN VIN has strict sequence/count checks; other legacy information layouts remain partial. CAF1 numbered Mode06/09 assembly validates length and sequence; live raw FF/CF is still unsupported.

Mode02 reads an explicitly selected frame0–255 and advertised base PID page. It does not claim enumeration of every stored frame or extended PID page. Missing second bytes cannot produce numeric metrics.

Mode06 is CAN-only after observed protocol/capability evidence. Nine-byte MID/TID/UASID/value/min/max records retain raw16-bit values. Component, unit, scaling, signedness and pass/fail remain unknown without verified mapping. Capability failures from other responders remain evidence. Mode05 is not implemented; source evidence and coherent product work remain required. No actuator control was added.

Four persisted theme palettes are consumed by existing screens. Units convert display values/bounds/history without changing diagnostic evidence. EN/FA preference is restored from disk and locale applied before Activity creation. Both languages stay in the bundle for offline switching. Common diagnostic, connection, terminal, trip, maintenance, onboarding and notification controls use resources; underlying technical/catalog metadata is not fully localized. RTL/LTR rendering and process death still require a phone.

Profile timeout limits100–30000ms are validated and consumed by connection watchdog/command bounds. Individual telemetry requests can use shorter deadlines. Auto-reconnect remains unimplemented and its control is hidden. Poll ECO/NORMAL/FAST and persisted pacing tests are retained. Exact selected USB targets and strict profile decoding fail closed; unknown transports/protocols and overflowed ports cannot silently become simulator/AUTO.

Manual TripLogger integrates measured cadence, marks limited coverage and leaves unmeasured fuel unknown. Legacy automatic TripComputer/SQLite trip rows still have zero-fuel sentinel and heuristic score limitations; they are not verified fuel or health-report evidence. A provenance-aware automatic-trip migration is a remaining P1 item. Health engine, post-drive report and arbitrary custom PID product remain unimplemented. Catalog maintenance specifications and vehicle/adapter metadata require source-specific validation; ATI strings do not establish authenticity.

## Platform and safety

Runtime permission states include first request, denied, permanently denied, unavailable, unsupported and Bluetooth off. Classic/BLE discovery is bounded and stopped on lifecycle exit; replacement waits for prior cleanup. BLE subscription acknowledgement, notification/indication properties and API33 callback/write handling are implemented. Device-specific Android behavior remains unexecuted.

Foreground startup/ack/failure and notification actions carry the session token. Stale service callbacks cannot close a replacement session. Notification visibility is a conservative product admission rule; Android's POST_NOTIFICATIONS policy itself does not universally require that permission merely to start an FGS. Disabled visibility produces an actionable failure rather than a silently unowned long-running session. Notifications identify simulator sessions. EOF reasons survive disconnect cleanup.

Mandatory adapter command authorization, control-character injection denial, one in-flight command, cancellation isolation, timeout quarantine, generation ownership, one producer, capability truth, nullable responders, non-CAN P0133, exact category and per-responder evidence remain covered by regression suites. Physical Mode04, Mode08, arbitrary STPX and security/write/programming remain disabled. No automatic state-changing retry or physical replay delegate exists.

## Build reproduction

Committed versions unchanged: Gradle9.8.1, AGP9.3.3, Kotlin/Compose2.2.10, coroutines1.10.1, compile/target SDK37, minSDK24, JVM target17. Launcher/daemon uses JDK24; core compiler uses JBR17. Root Kotlin JVM plugin removal requested by the user remains preserved.

Local Google Maven routing needs the external init script below; it mirrors the same coordinates and makes no dependency/version substitutions. No build downgrade was used.

```bash
export JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12
export ANDROID_HOME=/home/mohammad/Android/Sdk
./gradlew --init-script /home/mohammad/.local/share/carobd-build/sprint3-evidence/carobd-s3-repositories.gradle clean --console=plain
./gradlew --init-script /home/mohammad/.local/share/carobd-build/sprint3-evidence/carobd-s3-repositories.gradle \
  :app:assembleDebug :app:testDebugUnitTest :app:checkDebugDuplicateClasses :app:lintDebug \
  :core:model:test :core:adapter:test :core:protocol:test :core:telemetry:test :core:vehicle:test \
  --console=plain
```

An interrupted attempt is not counted as a successful gate. The final clean gate results and APK hash are recorded below after completion. Android migration instrumentation is compiled separately; no connected instrumentation or vehicle test is claimed. `python3 tools/check_dtc_history_migration.py` exercises actual migration SQL with SQLite and passes, preserving legacy rows and unknown observed fields.

## Sources and interpretation

- [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions): runtime CONNECT/SCAN and legacy location admission informed platform guards.
- [Foreground background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) and [service types](https://developer.android.com/develop/background-work/services/fgs/service-types): ownership/start failures remain explicit; token checks are application protections.
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission): platform notification permission is distinct from the app's stricter persistent-session visibility admission.
- [ELM327 datasheet](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf): ELM output and numbered vehicle-information examples informed bounded framing. This does not establish universal vehicle layouts.
- [Mitsubishi vendor monitor documentation](https://mitsubishitechinfo.com/data/ELL/2017/13/HTML/M113115550572300USA.htm) and [Snap-on service-mode manual](https://www.snapon.com/DiagnosticsManuals/Help/OBDII/Content/Common/Help%20Topics/OBD/OBDII%20Service%20Modes.htm): interpretation is vehicle/monitor dependent. [ShinyLib Mode06 format](https://shinylib.net/obd/mode06/) was an implementation reference for CAN record structure, not normative certification or authority for OEM scaling. No external implementation code was copied.

## Required next validation

Run the [hardware checklist](../HARDWARE_VALIDATION_CHECKLIST.md) on supported Android API levels, permission states and actual transports. Run the version1 database instrumentation migration, capture/recreate/import/export/replay flows and offline EN/FA layouts. Then validate read-only ECU readiness, Mode09 layouts, selected Mode02 frames, Mode06 raw records, multi-responder categories, disconnect and quarantine with recorded evidence. No blanket vehicle compatibility, fuel accuracy, adapter authenticity or health claim follows from software tests.

## Final fresh software gate

Source checkpoint: `1caea636d695af7b5e0565cdd9372c8ccd4068fd` (subsequent documentation-only commit does not change the APK). Clean succeeded, then all nine requested tasks succeeded in1m22s,71 tasks executed, exit0. The interrupted earlier attempt was discarded and clean was rerun before this successful gate.

| Module | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
|app|112|0|0|0|
|core:model|44|0|0|0|
|core:adapter|108|0|0|0|
|core:protocol|121|0|0|0|
|core:telemetry|13|0|0|0|
|core:vehicle|1|0|0|0|
|Total|399|0|0|0|

Lint:0 errors,88 warnings. Inspected categories:51 unused resources,15 typography ellipsis,8 dependency update suggestions,7 ModifierParameter,3 UseKtx, and one each RedundantLabel, AndroidGradlePluginVersion, PluralsCandidate, ObsoleteSdkInt. No permission/Bluetooth/FGS/security lint error was suppressed. The former app-bundle locale warning was resolved by retaining both language resources. Remaining warnings are not device validation. Coroutine test opt-in/compiler and dependency Unsafe/Gradle deprecation warnings remain separate from lint; dependency versions were not changed for cosmetic suggestions.

APK: `app/build/outputs/apk/debug/app-debug.apk`,21,916,440 bytes.
SHA256: `cbdf95c330aa14a515d08083158a7fa9b22d374df91b90971a02caeac6785d54`.
Fresh file time:2026-10-30 13:38:53 +03:30. aapt confirms package `com.sarani.carobd`, minSDK24, targetSDK37; apksigner verification succeeds with the Android Debug certificate. This is a debug build, not a production-signing result.

Local evidence directory: `/home/mohammad/.local/share/carobd-build/sprint4-evidence/` contains clean/full-gate logs, fresh unit XML, lint XML/HTML/SARIF, APK inspection and a machine-readable summary. The generated APK is not committed. User `report.md` remains untracked and unchanged (SHA256 `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325`).

Read-only status: `SOFTWARE_BUILD_GO_READ_ONLY__DEVICE_VALIDATION_REQUIRED`. No known failing software gate remains. Unimplemented/partial features above are explicitly excluded from compatibility and health claims. Highest remaining risk is Android lifecycle/permission/transport behavior and ECU framing/timing that have not been exercised on real hardware.
