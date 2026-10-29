# Safety verification and Sprint 2 — 2026-10-30

REAL_VEHICLE_READ_ONLY_STATUS: **NO_GO**. The exact Android build gate did not execute tasks successfully. No APK, device or vehicle validation is claimed.

## Starting reality

Started on clean `feat/car-obd-development` at `38393e197136a6779e38497e00bc418e2a9dddbc`. Fetch confirmed remote equality and all eight Sprint-1 commits. Origin is `git@github.com:SaraniMohammadReza/CarOBD.git`. Historical master object `0cf39bd48984a2a0cb49a035def1726ff5b89514` remains a commit; master was not changed or pushed.

## Adversarial results

| Sprint-1 claim | Result and evidence |
|---|---|
| Single-command grammar | VERIFIED: direct adapter corpus covers injected CR/LF/CRLF, all ASCII controls, NUL, TAB, semicolon, Unicode whitespace, oversized, odd/nonhex and unknown operations |
| Mandatory authorization | VERIFIED for existing diagnostic paths: one private typed ELM dispatch write; STN helper, raw, routing and direct adapter attacks produce zero unsafe writes |
| Physical 04 / 08 / writes/security/programming | VERIFIED: lower adapter guard plus repository/raw guards; dedicated simulator clear remains isolated; no re-enablement |
| Cancellation / replacement stream | VERIFIED and strengthened: canceled partial replies, fresh generation receiving old/new bytes, concurrent reads, quarantine cancellation and bounded admission |
| Timeout quarantine | VERIFIED; changed to coroutine deadline rather than wall-clock arithmetic; monotonic spacing avoids wall-clock changes affecting traffic pacing |
| Compact ELM normalization | REPAIRED_AGAIN: colon headers and responder-tagged error lines; compact/spaced/headerless/11-bit/29-bit tests and malformed conversion probes |
| DTC wire contract | VERIFIED through actual repository for non-CAN categories; malformed scans fail; CAN/compact/mixed evidence tested through actual buffer parser |
| Multi-responder evidence | REPAIRED_AGAIN: complete buffer with two positive responders and a NO DATA responder previously collapsed into one error; now every outcome survives |
| Session/producer ownership | REPAIRED_AGAIN: watchdog previously missed link loss before first observation; concurrent connection admission and request admission now bounded; disconnect cancels pending connection |
| Transport loss | VERIFIED in software; ended inbound flows now also retire even if the transport flag is stale; real physical callbacks/EOF remain DEVICE TEST REQUIRED |
| USB targeting | VERIFIED pure selection; selected-interface line coding now must acknowledge, rather than ignoring configuration failure; retired USB generations cannot reopen. Platform permission/detach behavior remains DEVICE TEST REQUIRED |
| Manual protocol | VERIFIED 0–9/A exact commands and readbacks, A=ATSPA; rejection/mismatch/unknown fail; B/C rejected by validation |
| BLE readiness/routing | REPAIRED_AGAIN: closed-generation gate blocks late CCCD revival; submission/link alone never ready, failed/unrequested acknowledgement fails. Factory/Dashboard retain BLE type. Actual Android callbacks remain DEVICE TEST REQUIRED |

Before fixes, the focused new regression run executed seven tests and reproduced two failures: mixed complete-buffer evidence was reduced from three responders to one, and early transport loss left the owner open. These failures were repaired, then the expanded suites passed. Source review additionally identified the BLE revival and legacy endpoint/configuration gaps described above.

The byte-array driver interfaces are trusted low-level implementation APIs. No alternate production diagnostic caller was found. RecordingTransport delegates the adapter's bytes; ReplayTransport has no physical delegate and ignores writes. Legacy ObdConnection/AutoProtocolScanner has no concrete hardware implementation or live consumer. STN detection does not authorize STPX/filter/baud helpers.

## Read-only product work

Connection Center now edits Classic, BLE, Wi-Fi, USB and simulator targets, persists/selects profiles, and exposes Disconnect/cancel. Unsupported custom BLE settings, transcript profiles and vehicle-profile protocol forcing fail explicitly. No hidden device/port fallback is offered. Legacy autoReconnect metadata is retained for compatibility, is not offered as an active feature, and new editor saves disable it.

Test Connection is an actual exclusive repository execution. Initialization emits explicit transport, adapter, identity, protocol, ECU and initial-capability evidence. It does not infer stage Pass from a snackbar or conflated StateFlow trace. Positive PID 00 plus observed protocol are required. It closes its session and starts no telemetry/foreground service. Evidence remains visible after completion/failure, with simulator identity explicit. The old state projection is deprecated.

Preferences use the actual DataStore implementation. A test saves ECO to disk, closes the store scope, recreates the store/manager/repository, and proves eager loading without observing repository poll state; FAST updates reach the consumer. A separate producer test proves cycle timing changes (0, 2000, 2500 ms) without additional capability discovery or producers. Settings exposes ECO/NORMAL/FAST; these are post-cycle delays, not UI refresh promises.

Simulator healthy has valid complete no-code fixtures. Stored/pending/permanent scenarios initialize successfully and return only their designated category. Malformed and multi-ECU scenarios retain failures. Disconnect is explicit. Late Prompt returns from write before a separately owned delayed reply arrives and is exercised through the production adapter's timeout quarantine. Unknown scenario IDs fail instead of silently becoming healthy.

Diagnostics cancels loading truthfully and cannot label cancellation as successful no-code; repeated scan/clear requests are suppressed while an operation is active. Clear acknowledgement text describes simulator-local behavior.

## Verification actually executed

- Supplemental core/session JUnit: **237 passed**, zero failures, zero ignored tests. Includes all five core modules' tests plus four app session tests.
- Supplemental app JUnit: **97 passed**, zero failures, zero ignored tests. The same four session tests occur in both runs; totals must not be summed without removing overlap.
- Supplemental Kotlin/Compose production compilation: all app/core Kotlin sources, successful with warnings. Uses installed Kotlin 2.4.21, Android SDK 37.0, cached/extracted AndroidX dependencies and generated resource identifier substitutes. It does not validate the project's Kotlin 2.2.10/AGP dependency graph, Android resources, manifests, packaging, lint or actual platform behavior.
- Runner now reads project coroutine/JUnit versions; both suites used **coroutines 1.10.1**. Missing core/test jars were downloaded once from Maven Central into a temporary directory outside the repository. No project dependencies, AGP, Kotlin or Gradle versions were changed. The former 1.10.2 substitution is not used for these results.
- Reproduce core checks: `CAROBD_JVM_LIB_DIR=<directory containing exact missing jars> python3 tools/check_safety_jvm.py`. It also searches Gradle and Maven caches, downloads nothing, and identifies itself as supplemental.

## Exact Android build: BUILD_ENVIRONMENT_BLOCKED

Gradle **9.8.1** is fully cached now. The initial direct wrapper invocation failed because the executable bit was absent; that source-controlled mode was fixed. A bounded `bash ./gradlew clean --console=plain` and the complete requested app/core gate were then attempted with the exact project wrapper. **Both exited 1 before task execution**, unable to acquire the configured JDK 24 toolchain download lock held by an existing process. That process/lock was not killed or deleted. No project toolchain/version downgrade or CI workaround was introduced.

Gradle test totals/failures/errors/skipped: **not available; tests did not execute**. App lint and duplicate-class task: **not executed successfully**. No current-run APK exists at `app/build/outputs/apk/debug/app-debug.apk`. Supplemental totals above are not Gradle totals. UI rendering/interaction on Android is not verified by source compilation.

## Exact remaining acceptance work

1. Restore configured JDK 24 provisioning, then run clean and the full app/core Gradle gate; retain XML/lint/duplicate-class results and current APK/hash.
2. Perform controlled bench validation for all physical transports, permission failures, disconnect/cancellation/late callbacks and actual Android service/background lifecycle. USB specialized serial drivers and raw ISO-TP remain unavailable rather than guessed.
3. Only after these gates, perform an authorized restricted read-only vehicle validation with reference scans and bus-load traces. Physical 04 and privileged commands stay disabled.

No hardware test, vehicle health conclusion or vehicle safety certification is implied by these software results.
