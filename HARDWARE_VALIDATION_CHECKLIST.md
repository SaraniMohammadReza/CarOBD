# CarOBD Hardware Validation Checklist

Current status: NO_GO. Supplemental SOFTWARE VERIFIED checks do not establish Android BUILD VERIFIED, DEVICE VERIFIED or VEHICLE VERIFIED. No physical adapter/vehicle was used in this sprint.

## Build gate — required first

- [ ] Configured Gradle 9.8.1 can acquire the configured JDK 24 toolchain.
- [ ] `./gradlew clean` succeeds.
- [ ] app assembleDebug, testDebugUnitTest, checkDebugDuplicateClasses and lintDebug succeed.
- [ ] core model/adapter/protocol/telemetry/vehicle tests succeed.
- [ ] Record current-run XML totals, lint result and APK path/hash. Do not reuse historical artifacts.

## DEVICE TEST REQUIRED — controlled bench, read-only

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
