# CarOBD Architecture

> Status reflects the `feat/car-obd-development` branch at the current commit.
> "Software verified" marks are backed by `:app:testDebugUnitTest` and the
> `core:{model,adapter,protocol,telemetry,vehicle}:test` suites (see §53 gate).
> Anything marked **DEVICE TEST REQUIRED** / **VEHICLE TEST REQUIRED** has
> software-only coverage but has **not** been validated against real hardware.

## 1. Module structure

```
core/model     — single-source domain models: TransportType, ConnectionState,
                  ProtocolStandard (canonical, §3), DiagnosticCode/DtcSeverity,
                  FeatureMaturity, Provenance, VehicleProfile, DiagnosticTranscript.
core/protocol  — OBD wire parsers & engines: Obd2Command (Modes 01–0A matrix),
                  PidCodec, PidPollingPlan (capability-gated discovery), DtcParser
                  (protocol-aware DTC framing), ISO-TP reassembler, StnAdapter,
                  UdsEngine, AutoProtocolScanner.
core/adapter   — ELM/STN lifecycle: Elm327Adapter (deterministic command lifecycle,
                  late-prompt quarantine, command-session identity), Elm327BufferParser,
                  DtcScanValidator, ObdTransport interface + RecordingTransport +
                  ReplayTransport.
core/telemetry — live telemetry state machine: MEASURED / REAL_ZERO / UNKNOWN /
                  UNSUPPORTED / NOT_MEASURED / STALE / MALFORMED (§21).
core/vehicle   — Iranian vehicle catalog (Vehicle/Variant/Engine/Transmission/ECU/
                  DiagnosticProfile/PidPack/DtcPack) with evidence confidence
                  (ConfidenceLevel / DataSourceReference).
app            — UI (Jetpack Compose), transports (Bluetooth/BLE/Wi-Fi/USB/
                  Simulator), ObdRepositoryImpl, connection foreground service,
                  onboarding, Settings, Dashboard, Diagnostics, Maintenance,
                  Localization (LocaleHelper + AppPreferencesManager), Units.
```

## 2. Canonical single-source models (§3)

There is **one** `ProtocolStandard` (in `core/model`), one `LiveTelemetry`, one
`ConnectionState`, one `VehicleProfile`, one `DiagnosticCode`, one `DtcSeverity`,
one `ServiceReminderItem` — each declared exactly once. `app:checkDebugDuplicateClasses`
is part of the §53 gate to prevent the duplicate-FQCN class regressions seen in C0.

## 3. Transport vs protocol (§1, §45)

`ObdTransport` is the transport abstraction (Bluetooth Classic RFCOMM, BLE GATT,
Wi-Fi TCP socket, USB serial, Simulator). It is **distinct** from the vehicle
protocol (SAE J1850, ISO 9141-2, KWP, ISO 15765 CAN, J1939). `AutoProtocolScanner`
maps transport ↔ protocol. Classic ELM327 is **not** DoIP / J2534 / CAN FD.

## 4. Connection truth levels (§15)

```
TransportConnected  →  AdapterResponding  →  AdapterInitialized
  →  VehicleProtocolSelected  →  EcuResponding  →  ConnectedPhysical
```
`ConnectionState` (sealed) models each level distinctly; `Connected` carries
`isSimulator`, `responderCount`, adapter identity and reported protocol.
**Adapter connection alone is never reported as physical ECU connection.**
Physical ECU connection requires a positive `41 00` (Mode 01 PID 00) response.

## 5. ELM command lifecycle (§2)

`Elm327Adapter` serializes commands via `commandMutex` (one command in flight),
uses a bounded inbound channel, enforces command timeout, quarantines
post-timeout late bytes until the late `>` boundary, marks the stream
compromised if resync cannot be proven, refuses commands on a compromised
stream, and resets state on re-initialization/reconnect. Late responses from a
timed-out command A are **never** consumed by a later command B.

## 6. Wire / responder model (§24, §25)

`DiagnosticEvent` carries `Direction` (TX/RX/ERR), timing, `responder` (e.g.
`7E8`, `7E9` — never defaulted), raw `rawHex`, and a `DiagnosticOutcome`
(POSITIVE / NEGATIVE_RESPONSE / NO_DATA / TIMEOUT / ADAPTER_FAILURE /
MALFORMED / PARTIAL / UNSUPPORTED). Multi-ECU responses are kept per-responder;
`7E8`/`7E9`/`7EA` are not merged.

## 7. Record / replay (§38)

`RecordingTransport` decorates any `ObdTransport`, snapshotting TX/RX/ERR with
timing into a `DiagnosticTranscriptBuilder`; the parser stack is unchanged
(transparent `incomingBytes`). `ReplayTransport` re-emits a transcript's RX
frames as ASCII wire bytes so a recording replays through the **same** parsing
stack as a regression fixture. Export: JSON + CSV. Reload: schema-aware
`DiagnosticTranscript.fromJson`. **UI session list / simulator-scenario replay
UI is the remaining software step (not yet implemented).**

## 8. Safety boundaries (§32, §37)

- Mode 08: **disabled by default**, no arbitrary actuator writes, no
  manufacturer-specific writes.
- Raw terminal allowlist only: read-only PIDs + diagnostic AT commands.
  `04`, `08`, UDS writes, security access, flash/programming, immobilizer,
  key programming and memory writes are blocked from the terminal.
- Mode 04 clear requires explicit confirmation and a positive `0x44`
  acknowledgement; `7F`/`NO DATA`/timeout/wrong-SID are failures.
