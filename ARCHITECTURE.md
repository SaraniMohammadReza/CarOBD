# CarOBD Architecture

Current source baseline: Sprint5, 2026-10-30. See [SAFETY_SPRINT_5.md](docs/SAFETY_SPRINT_5.md) for current gate evidence and limits; Sprint3 counts are historical. Software build readiness requires device/vehicle validation; physical Mode04 stays disabled.

## Modules and runtime ownership

- `core/model`: canonical protocol, command outcome, responder, connection profile, connection-test evidence and telemetry models.
- `core/protocol`: ELM envelope normalization, DTC/PID decoding, bounded capability discovery; separate raw ISO-TP/domain helpers are not integrated into live raw multi-frame dispatch.
- `core/adapter`: mandatory command authorization, ELM initialization/dispatch, scan aggregation, recording/replay infrastructure. STN helpers use the same deny-by-default command path.
- `core/telemetry`: capability-gated producer and trip calculations.
- `core/vehicle`: catalog/evidence metadata. Selecting catalog metadata does not modify live routing.
- `app`: physical drivers, isolated simulator, DiagnosticSession, repository, DataStore, Compose screens and foreground service.

`ObdRepositoryImpl` admits one connection attempt and serializes lifecycle changes. Each DiagnosticSession owns its transport, adapter, inbound reader, requests and one producer. Session requests and adapter admission each have a limit of eight. Disconnect invalidates ownership before waiting, closes transport, cancels/joins coroutine jobs, and clears repository telemetry. New connections use new transport/adapter instances. Low-level Android driver threads and platform callbacks still require DEVICE TEST REQUIRED validation.

## Mandatory diagnostic boundary

Product command paths enter private typed ELM dispatch through `AuthorizedDiagnosticCommand`. Single commands are limited to 64 ASCII characters; controls/separators and unknown operations are denied. Safe standard reads and exact identity reads are allowed. Initialization has a separate narrow configuration allowlist. Driver byte-array writes and USB line configuration are trusted implementation primitives, not alternative diagnostic APIs. The source inventory found one diagnostic `transport.write` call, in private ELM dispatch; RecordingTransport only delegates and ReplayTransport has no physical delegate.

Physical Mode 04, Mode 08, security, programming, manufacturer writes, arbitrary STPX, header/filter changes and user initialization commands are DISABLED FOR SAFETY. Only the dedicated simulator clear method authorizes local 04. Custom PID and privileged production dispatch are NOT IMPLEMENTED.

One command holds the wire mutex. Canceled commands retire the stream. Ordinary timeouts consume late bytes to a prompt within a bounded quarantine; missing prompt retires the stream. Ended/failed inbound flows retire ownership. Dispatch spacing uses monotonic elapsed time; coroutine deadlines bound quarantine. Response accumulation is capped at 16 KiB; the inbound channel is bounded. No state-changing retry exists.

## Connection truth and test execution

A socket or GATT link is not a vehicle connection. Initialization validates controlled configuration, ATI identity, positive Mode 01 PID 00 and observed protocol readback before Connected. Connected carries an explicit simulator flag. Transport loss invalidates the session; stale samples cannot publish into a replacement owner.

`ConnectionTestReport` retains explicit Pending/Running/Pass/Fail/Skipped events for transport, adapter, identity, protocol, ECU and capabilities. Evidence is recorded by the actual initialization executor rather than inferred from later UI state. Protocol readback happens after the handshake needed to trigger detection; stage completion can therefore be non-linear. Capabilities evidence is the validated initial PID 00 bitmap, not a claim that every ECU/PID was discovered.

Test Connection uses the same exclusive repository session path, starts no telemetry/foreground session, and closes before returning. The legacy ConnectionWizard state projection is deprecated and is not the executor or a verification source.

## Wire and scan semantics

ELM-managed compact/spaced responses, explicit CAN headers, common compact 11-bit headers, 18DA/18DB compact 29-bit headers and visible single-frame PCI are normalized. Colon-delimited explicit headers are accepted. Visible raw FF/CF and numbered multi-frame dumps fail; production raw ISO-TP support is NOT IMPLEMENTED.

Envelopes omit positive SID; DiagnosticScanRunner reconstructs it once in production. CAN DTC counts and padding are checked; non-CAN DTC pairs have no assumed count byte. Complete multi-line buffers retain positives alongside negative, malformed or NO DATA responder evidence. A primary timeout/transport error remains an error even if partial bytes resemble a positive response.

03/07/0A scans retain category, nullable responder, raw lines and per-responder outcomes. Identical codes from distinct ECUs remain distinct. Unknown responders are not assigned module roles. Only valid complete scans may report no codes; that does not establish vehicle health. Legacy DTC persistence is disabled until its schema can retain these facts without deriving MIL/protocol/category.

## Settings and persistence

Preferences and connection profiles use separate Preferences DataStores. Poll mode is eagerly collected into repository state and read by the producer per cycle. ECO/NORMAL/FAST add 2000/1000/500 ms after each cycle; they are not display refresh rates or guarantees of a fixed samples-per-second rate. Capability traversal has at most eight pages and stops unless extension bits authorize another page.

Record/replay models and exports are infrastructure only. A recording/session-list product workflow is NOT IMPLEMENTED; replay profiles are rejected rather than silently converted into a healthy simulator.


## Sprint4 evidence workflows

The foreground token, active profile and selected profile are distinct. Permission admission is checked at UI and transport boundaries. The foreground service acknowledges startup and reports token-bound failure; notification visibility is a conservative product requirement, not a claim that Android itself requires POST_NOTIFICATIONS to start every FGS.

Readiness, VehicleInformation, FreezeFrame and MonitorTests use the repository readEvidence boundary and typed per-responder outcomes. Multi-request reads bind to the captured session token. Unsupported capability is never assumed. Mode06 retains raw values without OEM scaling or pass/fail inference.

Every selected session transport is passively wrapped by RecordingTransport. RecordingController owns explicit capture, bounded canonical transcripts and atomic FileTranscriptStore snapshots. UI manages captures and SAF import/export. TranscriptReplayRunner creates an isolated ReplayTransport with no physical delegate and command-correlated responses. Replay retains recorded deadlines and bounded quarantine rather than replaying TX to hardware.

DTC database version2 adds nullable observed fields and legacy provenance without deleting old rows. DTC history shows unverified legacy data conservatively. LiveTelemetry retains per-PID measurement times and separates ATRV from0142. Units apply at display boundaries; evidence remains canonical. Manual and automatic trip capture share TripMetricEvidence. Database v3 stores nullable per-metric values, provenance, source, quality, time range, count, coverage and method; legacy rows remain unverified. Fuel estimates retain gasoline assumptions. Bench capture starts before admission using the same transcript and command pipeline.

Current Android Studio/cache, emulator QA and runtime UX evidence: [runtime continuation](docs/RUNTIME_QA_CONTINUATION.md). Physical vehicle validation remains separate.
