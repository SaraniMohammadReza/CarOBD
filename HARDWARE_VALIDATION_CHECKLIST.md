# CarOBD Hardware Validation Checklist

Current status: SOFTWARE_BUILD_GO_READ_ONLY__DEVICE_VALIDATION_REQUIRED. The exact Gradle Android gate passed; see `docs/SAFETY_SPRINT_5.md`. No device or vehicle validation is claimed. Physical Mode04 remains disabled.

## Build gate — required first

- [x] Configured Gradle 9.8.1 can acquire the configured JDK 24 toolchain.
- [x] `./gradlew clean` succeeds.
- [x] app assembleDebug, testDebugUnitTest, checkDebugDuplicateClasses and lintDebug succeed.
- [x] core model/adapter/protocol/telemetry/vehicle tests succeed.
- [x] Record current-run XML totals, lint result and APK path/hash. Do not reuse historical artifacts.

## DEVICE TEST REQUIRED — controlled bench, read-only

- [ ] Fresh-install runtime Bluetooth/notification permission flow, denial and revocation (new admission and request controls require bench validation).
- [ ] Classic: exact selected MAC, permission denial, disabled Bluetooth, RFCOMM failure/EOF, cancel blocked connect and disconnect during read.
- [ ] BLE: exact MAC, supported service/characteristics, CCCD submission/acknowledgement, missing descriptor, failed acknowledgement and disconnect followed by late callback. Confirm BLE selection never opens Classic.
- [ ] Wi-Fi: explicit endpoint, DNS/refused/timeout failures, socket EOF, disconnect during active write/read.
- [ ] USB: exact identity/interface, absent target with another device attached, VID/PID collision, wrong interface, denied permission, line-coding failure, partial write and detach. Confirm no fallback or guessed specialized driver support.
- [ ] Test Connection: six truthful evidence stages; failed ECU/protocol cannot pass; completion/cancellation closes test transport.
- [ ] Concurrent connect/test/terminal/scan and disconnect: one session, bounded admission, no old reply/result updates a replacement session.
- [ ] Poll-mode persistence/cadence and capability bounds under NO DATA, negative response, malformed reply and timeout.
- [ ] App background/foreground, notification disconnect, service destruction and process loss: verify actual Android lifecycle behavior; do not infer it from JVM tests.
- [ ] Capture permitted wire traffic and verify zero 04/08/write/security/programming transmissions from every normal path.

## VEHICLE TEST REQUIRED — only after build/bench gates

- [ ] Authorized read-only CAN and non-CAN handshake with valid PID 00 and observed protocol.
- [ ] Compare stored/pending/permanent scans to a reference tool; preserve responder identity and partial failures.
- [ ] Validate compact/spaced adapter output; visible unsupported raw multi-frame output must fail rather than fabricate results.
- [ ] Verify unknown/stale signals display unavailable and transport loss clears Connected promptly.
- [ ] Confirm actual bus load remains conservative and unsupported capability pages are not repeatedly polled.

## DISABLED FOR SAFETY / NOT IMPLEMENTED

Do not test physical Mode 04 re-enablement, Mode 08, programming/security, arbitrary manufacturer commands, custom header/PID packs or raw ISO-TP dispatch as part of this baseline. Physical clear remains disabled. No real clearing/readiness reset, raw ISO-TP, specialized USB drivers or vehicle-profile forced initialization is claimed validated.


## Sprint4 additional device checks (not executed)

- [ ] Android12/13+ first request, deny, permanently deny, Bluetooth disabled/unavailable, permission revocation while scanning/connected.
- [ ] Repeated Classic/BLE scans, lifecycle STOP, navigation and replacement cleanup; no surviving callback or duplicate scan.
- [ ] Foreground startup denial, disabled notification channel, notification permission revoke, service kill, rotation and stale token actions.
- [ ] BLE notify and indicate CCCD acknowledgement; delayed callback after reconnect; API33 modern writes plus legacy SDK branch.
- [ ] Exact USB target absent / denied / reattached; Wi-Fi connect timeout and EOF; actionable typed failure retained.
- [ ] Recording SAF JSON/CSV export/import, corrupted file, process recreation, active-capture stop/delete and offline replay with physical transport disconnected.
- [ ] Version1 database migration on Android SQLite; run DtcHistoryMigrationInstrumentedTest. Existing Python SQLite check is not an Android device result.
- [ ] EN/FA offline process recreation, RTL/LTR layouts and four theme palettes; units consistent across cards, bounds, histories and freeze frame.
- [ ] Measured/stale/unavailable dashboard states, ATRV versus0142, actual connected profile and simulator notification marker.

## Sprint4 vehicle checks (read-only, not executed)

- [ ] Recorded positive/negative/no-data/malformed readiness per ignition type.
- [ ] Mode09 actual CAN and supported legacy VIN, counts, padding, multiple responders and unsupported formats.
- [ ] Mode02 selected frame index/base page, unsupported PID and truncated value behavior.
- [ ] CAN Mode06 capability pages/raw records against authorized vendor evidence; do not assign pass/fail without unit/signedness/scaling mapping.
- [ ] DTC03/07/0A exact categories/responders and unknown MIL; zero stored DTC wording must not imply vehicle health.
- [ ] Live disconnect, timeout quarantine and replacement ownership under controlled read-only bench capture.

The current precise execution plan is [Sprint5 bench checklist](docs/SPRINT5_BENCH_CHECKLIST.md). Include actual Android v1/v2→v3 trip/history migrations and shared provenance recreation; no checkbox in a software build establishes device or vehicle PASS.
