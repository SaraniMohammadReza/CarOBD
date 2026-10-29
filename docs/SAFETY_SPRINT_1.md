# P0 safety baseline — sprint 1

Historical Sprint-1 record. Current verification and repairs are documented in SAFETY_SPRINT_2.md. Sprint 2 reproduced additional responder/transport-loss defects and repaired them; do not use this historical report as current acceptance evidence.

This document describes the repaired source baseline. It does not certify an APK or a vehicle/adapter combination.

## Physical command policy

All ELM diagnostic writes use the adapter's private typed dispatch. Public strings must first pass `SingleCommandParser` and `AuthorizedDiagnosticCommand.readOnly`. The parser rejects controls before normalization, limits input to 64 characters, and admits one allowlisted operation. Normal reads are structurally bounded modes 01, 02, 03, 06, 07, 09 and 0A; identity reads are ATI, ATRV, ATDP and ATDPN. Mode 22, arbitrary AT/STN/routing commands, unknown services and all write/control/security/programming operations are denied.

Physical Mode 04 is disabled in both repository and adapter. Only the dedicated simulator method can authorize 04, and the adapter independently requires a simulator transport. Replay has no physical transport delegate and ignores writes. A reconnect creates a new adapter and performs only controlled initialization and capability reads.

Internal initialization has a separate exact allowlist. Formatting and protocol commands require an explicit OK. J1939 code A produces ATSPA, not ATSPB. Unknown or mismatched protocol readback fails initialization; VEHICLE_PROFILE protocol mode is unavailable until it can be validated.

## Ownership and transport

A DiagnosticSession owns its adapter, reader, requests and single telemetry producer. Starts are idempotent. Explicit producer stop is terminal for that session; reconnect creates a fresh producer. Invalidation stops dispatch before shutdown; shutdown closes hardware, cancels and joins owned jobs, and clears repository telemetry. A transport-status observer sends no bus traffic and invalidates a lost session. Repository result publication checks session identity.

A canceled in-flight command permanently retires its stream. A subsequent command cannot consume late bytes from it; reconnect requires a fresh owner. Ordinary timeout retains bounded prompt quarantine. A missing prompt, write timeout, oversized response or stream error retires the session. Admission is limited to eight requests, response accumulation to 16 KiB, and dispatch spacing to at least 50 ms.

Bluetooth/Wi-Fi publish sockets before blocking connect so shutdown can close them. BLE requires completed notification subscription. USB requires an explicit identity and interface selection, matches all supplied fields, and rejects missing, ambiguous or wrong interfaces without fallback. USB partial writes fail; detach is observed before subsequent reads.

## Response and scan contract

The ELM envelope parser accepts compact/spaced, headerless, common compact 11-bit and 29-bit, and explicit header responses. Visible single-frame PCI is checked and removed. This is ELM-managed payload parsing: visible multi-frame/numbered/raw ISO-TP output fails closed. Raw ISO-TP production integration is not claimed.

Envelopes omit the positive service byte. DiagnosticScanRunner reconstructs it exactly once before the full-frame DTC decoder. Non-CAN DTCs have no assumed count byte; CAN framing is validated against count, actual codes and padding. A primary timeout/error never becomes positive from a partial buffer.

Scans retain each request/responder's outcome, raw lines and codes. Positive and negative evidence coexist; incomplete scans return an exception carrying the partial report. Different responders remain distinct and have generic ECU labels. Successful no-code results require valid positive evidence for all three requests. Legacy DTC persistence is disabled until its schema can retain these facts without fabricating category, protocol or MIL state.

## Verification and release restriction

Sprint 1 used a supplemental offline JVM runner with installed kotlinc, cached coroutines core/test 1.10.2, JUnit 4.13.2 and Hamcrest 1.3. The current runner reads project versions and requires coroutines 1.10.1; it accepts a CAROBD_JVM_LIB_DIR cache. It includes all pure core tests and Android-free session tests. Final supplemental results: **222 core/session tests passed** and **84 app unit tests passed**, with three session tests present in both runs. It does not download or change project dependencies; the project declares coroutines 1.10.1, so this is not verification of the exact Gradle-resolved dependency graph.

On 2026-10-29, direct Kotlin/Compose compilation also checked every app/core production Kotlin source against cached AndroidX libraries and the installed Android SDK. Only generated R identifiers were substituted. All app unit tests were additionally compiled/run against those real production classes. This does not verify Android resource processing, manifest merging, packaging, lint or the Gradle duplicate-class task. Duplicate app-local ServiceType and VehicleSpecSheet definitions found during this check were removed in favor of core models.

Historical Sprint-1 BUILD_ENVIRONMENT_BLOCKED: configured Gradle 9.8.1 was not installed completely. Sprint 2 found it fully cached, but the exact build is blocked acquiring the configured JDK 24 toolchain. A bounded attempt using cached Gradle 9.6.1 in offline mode timed out without executing tests; daemon configuration requests JDK 24. No full Android build, APK, lint or packaging result is claimed. No configured remote CI workflow was found.

REAL_VEHICLE_READ_ONLY_STATUS: NO_GO pending the exact Android full gate and controlled transport/adapter validation. The regression suite establishes software evidence, not a hardware certification. Raw ISO-TP, privileged commands and physical clearing remain unavailable.

Next validation gate: clean; app assembleDebug, testDebugUnitTest, checkDebugDuplicateClasses, lintDebug; core model/adapter/protocol/telemetry/vehicle tests. Then validate disconnect, cancellation and protocol readback on controlled adapters before a vehicle trial.
