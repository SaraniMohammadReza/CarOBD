# Gemini Repository Rebuild Master Prompt — CarOBD

You are the principal Android architect, senior automotive diagnostics engineer, protocol implementer, data-governance owner, QA lead, and product engineer responsible for rebuilding the repository at `C:\Users\mohammadreza.sarani\AndroidStudioProjects\CarObd` into a truthful, testable automotive SuperApp.

This is an implementation assignment, not a planning-only exercise. Work autonomously in small verified increments. Inspect the live repository before every destructive choice because it may have changed since this audit. Preserve user work. Never use `git reset --hard`, discard unrelated changes, fabricate test evidence, or claim hardware compatibility without hardware evidence. If Git metadata is still absent, state that and keep a precise change ledger instead of pretending commits exist.

## 1. Product mission

Build an offline-first Android application that combines reliable generic OBD diagnostics, live data, configurable dashboards, adapter management, vehicle garage, trips, fuel, maintenance, driving analytics, logs/export, HUD, charts, searchable vehicle knowledge, evidence-governed Iranian vehicle/ECU packs, and an extension-ready architecture. It must be globally correct before it is locally broad. It must never show simulated, default, inferred, or unsupported data as measured vehicle truth.

The final product may eventually exceed Torque, Car Scanner, Infocar, OBD Auto Doctor, and AndrOBD in integrated ownership features and Iranian depth. Do not copy proprietary assets or reverse-engineered commercial databases. Respect AndrOBD's GPL license; learn from public behavior/tests, but do not transplant GPL code unless the repository intentionally adopts compatible licensing after an explicit decision.

## 2. Mandatory truth vocabulary

Every feature/data record/capability must use one of these states, stored and visible where relevant:

- `IMPLEMENTED_IN_SOFTWARE`: code and automated tests exist; no hardware claim.
- `SIMULATED_VERIFIED`: deterministic simulator/transcript tests pass.
- `HARDWARE_TEST_REQUIRED`: implementation exists but lacks accepted hardware evidence.
- `VEHICLE_VERIFIED`: a named adapter, app build, vehicle/ECU identity, transcript, tester, date, and assertions are recorded and reviewed.
- `UNSUPPORTED`: known not implemented or not available for this connection.
- `UNKNOWN`: not yet queried or evidence is insufficient.

Never translate `UNKNOWN` into zero, a plausible default, success, healthy, genuine, supported, or normal. Never say “works on all Iranian vehicles.” Never say an ECU is healthy merely because no DTC was returned. A simulator must be opt-in, visually persistent, isolated from real history by a source tag, and impossible to mistake for a real adapter.

## 3. Audited repository facts you must verify, then act on

The 2026-10-26 forensic baseline found 140 non-generated files, 93 production Kotlin files/12,467 physical lines, 9 test files/869 lines, and six modules: `:app`, `:core:model`, `:core:adapter`, `:core:protocol`, `:core:vehicle`, `:core:telemetry`.

Build baseline:

- `gradlew clean --warning-mode all`: passed.
- `gradlew assembleDebug --warning-mode all`: passed, 46 tasks, about 48 seconds.
- `gradlew testDebugUnitTest`: passed.
- `gradlew test`: 38 tests, zero failures/errors/skips.
- `gradlew lintDebug`: zero errors and 73 warnings: 42 unused resources, 8 newer-version, 8 typography ellipsis, 5 dependency, 5 modifier parameter, 2 AGP, and three single warnings.
- No adb device was attached; instrumentation was not executed. `app/src/androidTest/java/com/sarani/myapplication/ExampleInstrumentedTest.kt` asserts `com.sarani.myapplication`, not the real `com.sarani.carobd`, and is not meaningful coverage.
- Debug APK was 21,335,218 bytes.
- Toolchain was Java runtime 24.0.1, Gradle 9.8.1, AGP 9.3.3, Kotlin plugin 2.2.10, compile/target SDK 37, min SDK 24, Java target 17.
- The supplied directory was not a Git repository. Recheck; do not invent branch/commit history.

## 4. Immediate stop-ship defects

Fix these before adding product breadth.

### 4.1 Fabricated production results

- `app/src/main/java/com/sarani/carobd/ui/screens/dashboard/DashboardMvi.kt`: `OpenBluetoothDialog` inserts invented devices/MAC addresses (`OBDII ELM327 v1.5`, Viecar, Vgate, Galaxy Buds); `TriggerBtScan` only delays. Wire the real scanner or show an honest unavailable/permission state. No fake entries in production.
- `app/src/main/java/com/sarani/carobd/ui/screens/vehicles/VehicleProfileMvi.kt`: `performAutoScan()` delays through profiles and reports the currently selected protocol. Replace it with repository events from an evidence-based detection algorithm or disable it with `NOT_IMPLEMENTED`.
- `app/src/main/java/com/sarani/carobd/ui/screens/adapter/AdapterInspectorScreen.kt`: static claims of genuine PIC18F25K80, KWP support, 28 ms, 12.7 V, 14.2 fps, and full Iranian compatibility must be removed. Render only measured `AdapterReport` fields with per-field evidence/time and Unknown states.
- `app/src/main/java/com/sarani/carobd/core/model/LiveTelemetry.kt`: remove plausible nonzero defaults. Model values as absent/quality-tagged samples. UI placeholders must be `—`, not fabricated measurements.
- Remove/replace false marketing such as “100+ sensors”, “all protocols”, and the Persian no-DTC text saying the ECU is completely normal.
- Remove the unsafe troubleshooting instruction to squeeze OBD pins with pliers.

### 4.2 Split-brain model and duplicate stacks

The app and `core:model` define colliding FQCNs with incompatible shapes. Known collisions include `ConnectionState`, `DtcSeverity`, `DtcSystem`, `DiagnosticCode`, `ServiceType`, `ServiceReminderItem`, `VehicleSpecSheet`, `LiveTelemetry`, `ProtocolStandard`, and `VehicleProfile`. Select `core:model` as the canonical home, redesign it where necessary, migrate every call site, and delete app duplicates only after compile/tests prove migration. Add a build test that rejects duplicate FQCNs across modules.

Retire one of each duplicate generation:

- legacy `app/.../core/connectivity/*` versus `app/.../data/transport/*` and `core:adapter`;
- legacy `app/.../core/protocol/*` versus `core:protocol`;
- legacy static databases versus versioned data packs/Room;
- `TripLogger` versus `TripComputer`.

Do not leave deprecated code in production source “for reference.” Preserve useful tests by retargeting them to canonical contracts.

### 4.3 Connection validity

`core/adapter/.../Elm327Adapter.initialize()` currently ignores most initialization failures and treats only `UNABLE TO CONNECT` from `0100` as fatal. A prompt or syntactically parsed response is not proof of ECU communication. Require:

1. transport connected;
2. adapter reset/identity evidence;
3. required setup commands positively acknowledged or explicitly tolerated by capability policy;
4. selected/auto protocol reported;
5. at least one valid, service-correlated ECU response with correct bytes/header/source;
6. capability discovery complete enough for the requested feature.

Classify `NO DATA`, `STOPPED`, `?`, `CAN ERROR`, `BUS INIT...ERROR`, `UNABLE TO CONNECT`, buffer overflow, low voltage, timeout, malformed response, permission loss, and transport EOF separately. A response must carry raw text/bytes, parsed frames, adapter/ECU source, timestamps, elapsed time, warnings, and error category. Unknown protocol numbers must not default to CAN 11/500.

### 4.4 DTC correctness and safety

`ObdRepositoryImpl.scanDtcs()` passes the service response byte to `DtcParser.parseMode03Payload()`, although the parser expects payload after the service. The active simulator also inserts a fictional count byte. Establish one frame decoder that validates request/service correlation (`03`→`43`, `07`→`47`, `0A`→`4A`), strips transport/header/service exactly once, preserves ECU source, rejects odd/truncated pairs, and handles padding. Add regression transcripts proving `43 01 33 03 00` becomes exactly `P0133`, `P0300`, not `C0301` or other spurious codes.

Persist scans even when clean, with adapter/protocol/vehicle/ECU/build/source and raw transcript references. Preserve stored/pending/permanent categories and MIL semantics. Before Mode 04:

- require a real connected ECU and explicit user confirmation;
- explain that DTCs, freeze frames, and readiness may be cleared/reset;
- save a pre-clear scan and readiness snapshot;
- send one serialized clear request;
- require valid positive response `44` from intended ECU(s);
- wait appropriately, then rescan and record the result;
- never claim the MIL is off without subsequent evidence.

## 5. Target architecture and dependency rules

Create one directional architecture. The UI depends on domain contracts/state. Data implementations depend on domain and protocol/transport APIs. Protocol code must not import Android. Android transports must not parse OBD application payloads. No feature reaches a socket, GATT characteristic, `BluetoothAdapter`, or SQL helper directly.

Target logical modules/packages (split physically when stable):

- `core:model`: canonical immutable value objects, units, source/quality, typed errors, maturity/provenance.
- `core:common`: clocks, dispatchers, IDs, result primitives, logging redaction.
- `core:testing`: fake clock, scripted transport, transcript fixtures, test builders.
- `domain`: repository interfaces and use cases.
- `transport:api`: duplex byte-stream contract and transport events.
- `transport:android`: Classic Bluetooth, BLE, Wi-Fi/TCP, USB serial, permissions/network binding.
- `adapter:elm`: ELM/STN command actor, parser, initialization, capabilities, metrics.
- `protocol:obd`: SAE J1979/J1979-2 application services and data definitions.
- `protocol:isotp`, `protocol:uds`, `protocol:kwp`, `protocol:j1939`: operational clients only when their completeness gates pass.
- `data:database`: Room entities/DAOs/migrations.
- `data:packs`: signed/versioned schema and provenance registry.
- feature boundaries: connection, dashboard, diagnostics, garage, trips, fuel, maintenance, knowledge, settings, adapter, terminal, export, developer.

If creating all modules initially slows feedback, enforce the boundaries as packages/interfaces and extract incrementally. Add architecture tests for forbidden dependencies and duplicate classes.

Use one application-scoped `ConnectionCoordinator`/foreground-service owner and one per-session command actor. A repository must not launch hidden duplicate cold-flow polling jobs. StateFlow exposed to UI must be lifecycle-aware and derived from a single source.

## 6. Connection state machine

Implement an explicit sealed state machine with legal transitions and attempt/session IDs:

`Idle → PermissionRequired → Discovering → DeviceSelected → TransportConnecting → TransportConnected → AdapterResetting → AdapterIdentifying → AdapterConfiguring → ProtocolDetecting → EcuDiscovering → CapabilityDiscovering → Connected`.

Also support `Degraded`, `RetryScheduled`, `Disconnecting`, and typed `Failed(stage, cause, recoverability)`. Ignore stale callbacks using attempt IDs. Cancellation must close sockets/GATT/USB endpoints, stop collectors, cancel queued commands, and settle to Idle. Reconnect uses bounded exponential backoff with jitter and stops on permission/auth/configuration faults. UI must show the stage, evidence, and recovery action.

Use a `connectedDevice` foreground service for user-requested ongoing adapter communication and declare the Android 14+ service type and permissions correctly. Provide a notification with vehicle/adapter, connection state, and disconnect action. Do not use `dataSync` as a workaround. Decide whether Companion Device Manager improves initial pairing; it scans on the app's behalf but does not create the connection.

## 7. Android transport requirements

### Classic Bluetooth

Use injected `BluetoothManager/Adapter`, modern permissions, real bonded/discovered devices, SPP UUID/configurable UUID, connection timeout, ordered writes, bounded reads, EOF/error events, and proper close. No `SharedFlow.tryEmit` loss for protocol bytes. Pairing UX must be explicit; never invent devices.

### BLE

Build a serialized GATT operation queue. Do not mark connected until service discovery, characteristic selection, CCCD notification enablement, and required MTU/write-mode negotiation succeed. Handle status codes, bonding, timeouts, MTU, connection priority, characteristic write callbacks, indication/notification fragmentation, adapter-specific service profiles, reconnect, and Android API variants. Fixed 20-byte chunking is insufficient. Store BLE profiles as evidence-backed adapter definitions, not guesses.

### Wi-Fi

Validate endpoint and network, optionally bind the socket to the selected adapter network, use connect/read/write/idle timeouts, ordered I/O, EOF/error events, reconnect, and a bounded buffer. Make host/port configurable. Explain that Wi-Fi OBD can displace normal internet routing.

### USB

Add USB host support with device filters only after IDs are known, `UsbManager.requestPermission`, attach/detach receiver, and a maintained serial implementation supporting the required FTDI/CH340/CP210x/CDC families. Treat USB capability as hardware-specific. Test detach during command and service lifecycle. Do not claim support merely because `ObdTransport` comments mention USB.

## 8. Adapter engine

Use a coroutine actor/channel with bounded queue, one in-flight command, priorities, deadlines, cancellation, and fair scheduling. Capture command ID, normalized command, raw request/response, elapsed time, retries, prompt state, warnings, and source. Separate buffer framing from semantic interpretation.

Initialization policy must be capability-aware and transcript-tested. Probe conservatively using documented ELM327 and STN commands. `ATI` text such as “ELM327 v1.5” does not prove genuine silicon. `ATCAF1` support does not prove custom-header or J1939 support. Report observations (`commandAcknowledged`, measured voltage, measured latency distribution, STN command accepted), then derive a cautious quality assessment with reasons. Never assert a PIC model from command strings alone.

Implement adapter voltage as a timestamped adapter measurement, clearly distinct from ECU module voltage PID 0142. Validate parse range; show Unknown on malformed response.

## 9. Generic OBD implementation gate

Implement and test the complete product-critical generic sequence before proprietary data:

1. Discover responding ECUs and retain 11/29-bit source addresses.
2. Query supported PID pages `00/20/40/60/80/A0/C0/E0` as indicated by continuation bits, per ECU.
3. Build a capability graph by ECU/service/PID. Never poll unsupported PIDs.
4. Support Mode 01 live/current data with a table-driven `PidDefinition` (request, response service, byte length, formula, signedness, unit, bounds, precision, category, standard source).
5. Support Mode 02 freeze frame, including frame numbers and DTC association.
6. Support Mode 03 stored DTCs, Mode 07 pending, Mode 0A permanent, preserving category.
7. Support Mode 04 only through the safe workflow.
8. Support Mode 06 monitor results with unit/scaling/test identity and “not available” semantics.
9. Support Mode 09 VIN, CALID, CVN, ECU name where available, with ISO-TP/multi-message handling.
10. Decode PID 0101 MIL/DTC count/readiness and expose since-clear/current-cycle readiness where supported.

Mode 05/08 and in-use performance tracking may be later phases but must have explicit Unsupported states. Use current SAE J1979/J1979-DA licensed/authorized data; do not reconstruct copyrighted digital annexes from random websites.

The poll scheduler owns all live requests. It needs priority tiers, desired sampling periods, adapter throughput estimates, per-ECU queues, coalescing, backpressure, stale thresholds, and visible achieved Hz. Polling must pause for exclusive diagnostics/terminal commands and resume safely. Every sample includes monotonic/wall timestamps, PID/DID, ECU, numeric/text value, unit, `MEASURED|CALCULATED|INFERRED|SIMULATED`, freshness, and quality (`GOOD|STALE|MALFORMED|OUT_OF_RANGE|UNSUPPORTED`).

## 10. Advanced protocol gates

Do not call the existing helpers full implementations.

### ISO-TP / DoCAN

Key sessions by addressing tuple. Implement single/first/consecutive/flow-control frames, sequence validation, block size, STmin including sub-millisecond encodings where platform permits, N_As/N_Ar/N_Bs/N_Br/N_Cs/N_Cr timers, functional versus physical addressing, 11/29-bit and normal/extended/mixed addressing required by supported targets, payload-length bounds, cancellation, and structured errors. Add golden traces and fault injection for dropped/duplicate/out-of-order frames. CAN FD/escape length is a separate declared capability.

### KWP2000

Separate physical initialization from application framing. Implement only through hardware/adapter operations that can actually perform 5-baud or fast init; model keyword negotiation, addresses, checksum/length validation, P timing, keep-alive/tester-present, start/stop communication, positive/negative response correlation, and retries. A frame codec alone is not KWP support.

### UDS and OBDonUDS

Build an ISO-TP-backed client with session state, P2/P2* timers, tester present, NRC parsing including `0x78 responsePending`, DID schemas, DTC format identifier, and strict read-only default. Security access, routines, writes, coding, flash, and actuator functions are disabled behind an explicit expert/security policy and are out of initial release. Implement SAE J1979-2 separately from arbitrary OEM UDS; do not map every three-byte UDS DTC into classic five-character SAE form without the format identifier.

### J1939

Add 29-bit addressing, address claim awareness, PGN request, DM1/DM2, lamp states, SPN/FMI/OC, source address, and TP.CM/TP.DT BAM/RTS-CTS where needed. Test multiple source addresses. Do not treat `ATCAF` as proof of J1939 capability. `ProtocolStandard` and ELM `ATSP` mapping must agree; fix the current `AT SP A` versus `ATSPB` inconsistency using documented adapter protocol numbers.

### DoIP, WWH-OBD, J1708/J1587

Create roadmap contracts only until a product use case, standards access, transport, conformance fixtures, and hardware exist. DoIP requires UDP discovery, TCP routing activation/alive checks, logical addressing, payload validation, errors, and optional TLS policy. Names/enums do not count as support.

## 11. Persistence and offline-first data

Replace `CarObdDatabaseHelper` with Room using explicit migrations, exported schemas, foreign keys, indices, transactions, and migration tests. The local database is the source of truth. DataStore stores only small preferences (theme, language, units, selected garage vehicle, safe connection preferences).

Minimum entities/relations:

- `GarageVehicle`, vehicle/profile link, VIN/license plate encrypted or excluded from backup per policy;
- `AdapterDevice` and learned `ConnectionProfile`;
- `DiagnosticSession`, `Ecu`, `DtcObservation`, `FreezeFrame`, `ReadinessSnapshot`, raw transcript reference;
- `TelemetrySession`, chunk/file index, selected signals, summary;
- `Trip`, timestamped samples/location references, driving events and derived metrics;
- `FuelEntry`, `Expense`, `MaintenancePlan`, `MaintenanceRecord`, `Reminder`;
- dashboard pages/widgets/presets;
- `DataPack`, `SourceReference`, `VerificationRecord`, conflict record.

Do not store high-rate telemetry as one Room row per sample on the UI thread. Use buffered/chunked storage (documented binary/CSV/Parquet-like choice appropriate to Android) with Room metadata, bounded memory, crash-safe finalization, and export streaming. Raw diagnostic logs must be size/age bounded and redacted.

## 12. Iranian vehicle/ECU data governance

Current source is candidate input, not truth. `core/vehicle/.../VehicleCatalog.kt` has 14 profiles; `IranianEcuCatalog.kt` has 13; `VehicleSpecDatabase.kt` has 8; legacy app catalogs contain 8 presets, 7 protocols, 8 specs, 4 motorcycle pinouts, 4 Chinese records, 55 sensors, 20 DTC literals. Many are labelled `VERIFIED_OEM`/`PRODUCTION_IMPLEMENTED` without resolvable sources. Conflicts include TU5 oil 3.75 versus 3.25 L, EF7 5.25 versus 5.5 L, and Shahin 4.0 versus 3.8 L. Quarantine rather than choose silently.

Design a versioned, schema-validated data-pack format. Every fact/request definition must include:

- stable pack/record/version ID and applicability (make/model/year/engine/ECU HW/SW);
- protocol/address/init requirements;
- request/service/PID/DID, expected response, byte layout/formula/unit/range when applicable;
- feature maturity and confidence;
- source type, title, publisher, URL/document ID, edition/date/page/section, license;
- capture/test IDs and hashes, adapter, vehicle/ECU identity, result, reviewer;
- supersedes/conflicts links.

Source hierarchy: (1) standards/OEM documents; (2) adapter manufacturer documents; (3) reputable technical/open-source implementation with compatible license; (4) Iranian service/manual/diagnostic material; (5) community reports. Tier 4/5 can create candidates, not `VEHICLE_VERIFIED` facts without corroboration/hardware evidence.

Never ship placeholder requests like `IR_STEP`, `MC_GEAR`, or `CH_BOOST`. Never silently return RPM when a sensor ID is unknown; return NotFound. Reassess the unproven `01FE` transmission-temperature mapping and all `21xx/22xxxx` proprietary claims. ISO 19689 is connector-related evidence, not proof of the current universal red/white six-pin pinout, protocols, or brand list.

Build an internal pack validator and review screen. Production packs must be signed/checksummed, schema/version compatible, provenance complete, conflict-free or explicitly resolved, and covered by golden vectors. Community imports are sandboxed and visibly untrusted.

## 13. Product feature requirements

### Connection home

Show permission readiness, real devices, transport, stage, last verified profile, adapter evidence, ECU evidence, and actionable failures. Remember a successful connection profile only with confidence and allow reset. Simulator entry is separate and explicitly labelled.

### Garage/onboarding

Support multiple vehicles, default selection, optional VIN/plate/nickname, catalog or custom vehicle, language, units, privacy/backup choices, adapter association, and permission education. Do not require unnecessary permissions. Changing vehicle changes relevant packs/dashboards/maintenance without corrupting history.

### Dashboard

Implement persisted pages and a responsive grid; drag/drop, resize, duplicate/remove, edit, and presets. Widgets include numeric, gauge, bar, text/state, warning, chart, and calculated metrics. Only supported signals are selectable by default; advanced users can show unsupported definitions. Each widget shows source/quality/staleness. Include portrait/landscape/tablet layouts and an explicit low-distraction driving/HUD mode.

### Charts/recording

Timestamped bounded buffers, multiple series/axes where sensible, pause/resume, cursor, zoom/pan, min/max/average, gaps/stale markers, recording, replay, and streaming CSV export via Storage Access Framework. Sampling/display/render rates are separate. Avoid per-frame list copies.

### Diagnostics

Group by ECU and stored/pending/permanent. Provide raw/evidence detail, freeze frame, readiness, Mode 06, scan comparison/history, search, localized DTC explanations with provenance, and safe clear. Never offer a likely cause as a diagnosis; label it educational.

### Trips/driving/fuel

Unify trip ownership. Integrate distance by real timestamp, handle gaps, persist state, recover after process death, and keep sample buffers bounded. Fuel hierarchy: direct fuel-rate PID when valid; MAF stoichiometric calculation with fuel type; speed-density only with calibrated displacement/VE/IAT/MAP; otherwise Unknown. Every metric identifies method and confidence. GPS is optional and consented; support pause, map/replay only when location exists, and delete/export. Add fuel logbook and expense records independently of ECU estimates.

### Maintenance/knowledge

Persist odometer/date-based schedules per vehicle, history, cost/parts/notes, and WorkManager reminders. User/OEM intervals need provenance and override. Knowledge is offline searchable by vehicle/engine/ECU/system/DTC and clearly separates verified facts from community candidates.

### Adapter center/terminal/developer tools

Adapter center displays measured identity/capabilities/voltage/latency/throughput/firmware strings with raw evidence. Terminal must be reachable but gated as Expert, serialize through the same actor, distinguish AT/OBD/raw modes, show timestamps/raw frames, warn before write/clear/routine commands, and export redacted logs. Developer center provides state transitions, queues, scheduler rate, pack versions, sanitized transcript export, and simulator scenario selection.

### Settings/localization/accessibility

Persist system/light/dark themes, English/Persian locale, metric/imperial/US units, refresh policy, privacy, backups, and logging. Use resources, RTL-aware layouts, correct plural/number/date formatting, minimum readable text, content descriptions, focus order, touch targets, contrast, reduced motion, and large-font tests. `CarObdTheme` must honor its parameter instead of always selecting dark.

## 14. Simulator and test-fake rules

Keep exactly one deterministic simulator behind `transport:api`. Script scenarios such as healthy idle, drive cycle, DTC categories/freeze frame, unsupported PIDs, multi-ECU, slow adapter, split chunks, prompt fragmentation, `NO DATA`, CAN error, buffer full, timeout, disconnect/reconnect, malformed frames, low voltage, and ISO-TP sequence faults. Unknown commands return a deliberate unsupported response, not universal `OK`.

Production code must never instantiate a fake except from an explicit simulator selection/build flag. Fake devices do not appear in real discovery. Simulated records use a separate source and cannot satisfy hardware verification.

## 15. Dependencies and toolchain

Use the version catalog and stable mutually compatible versions available when you execute. At audit time official documentation showed AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Room 2.8.5, and newer Navigation/DataStore/Koin/coroutines than the repo. Do not update all at once. Establish a matrix for Gradle/AGP/JDK/Kotlin/Compose/KSP and upgrade in isolated green steps. AGP 9.4 requires Gradle 9.6+ and JDK 17; the current Gradle 9.8.1 is sufficient, but verify every plugin.

Add only justified dependencies, likely:

- Room runtime/KTX/compiler via compatible KSP and Room testing;
- WorkManager KTX;
- Kotlin serialization for owned pack/config formats;
- a maintained USB serial library if license and supported chipsets fit;
- Compose adaptive/window components;
- testing: Turbine or equivalent Flow assertions, MockK only where fakes are insufficient, Room migration helpers, Compose UI test, benchmark/macrobenchmark and baseline profile modules.

Avoid adding a network stack unless a concrete backend/data-pack sync phase needs it. Avoid a chart library if it cannot meet high-rate, RTL, accessibility, and licensing requirements. Record every dependency's purpose, version, license, transitive risk, and removal criteria. Remove unused Koin test/UI test dependencies only after usage analysis. Enable release shrinking/resource shrinking after keep-rule verification; create the referenced `proguard-rules.pro` or remove the invalid reference.

## 16. Testing strategy and required fixtures

Tests must prove behavior, not catalog counts or comments.

### Unit/property tests

- PID formulas at min/max/signed/bounds and malformed lengths;
- supported-PID continuation pages per ECU;
- exact DTC vectors for P/B/C/U, padding, multiple ECUs, UDS format identifiers, J1939 SPN/FMI;
- ELM prompt/echo/searching/header/chunk/parser cases;
- ISO-TP timers/flow/sequence/addresses;
- fuel/trip integration using fake monotonic clock and irregular intervals;
- state-machine legal/illegal/stale transitions;
- unit conversions and Persian/English formatting;
- pack schema, provenance, conflicts, signatures/hashes.

### Integration/transcript tests

Use a `ScriptedTransport` that fails on unexpected commands (never default `OK`). Assert exact command order, deadlines, retries, cancellation, raw response, and final state for ELM initialization, discovery, PID polling, diagnostics, clear, reconnect, and terminal arbitration. Store sanitized golden transcripts with provenance and expected parsed outputs.

### Database tests

Test every Room migration from each shipped schema, FK/cascade policy, clean scan/history, process restart, partial recording recovery, export/import roundtrip, corrupted pack rejection, and privacy deletion.

### UI/instrumentation

Fix package/runner, then test permission denial/retry, real-empty scan (no fake devices), simulator banner, connection stages/errors, no-data placeholders, dashboard edit/persistence, diagnostics/clear confirmation, RTL/LTR, theme, large fonts, navigation reachability, process recreation, and SAF export. Run on at least min SDK, a modern target device, phone/tablet, and API levels covering Bluetooth permission changes.

### Hardware matrix

Maintain a separate opt-in suite and evidence table for adapter model/firmware/transport/phone/API/vehicle/year/engine/ECU/protocol/test cases. Include genuine ELM/STN/OBDLink and representative clones, Classic/BLE/Wi-Fi/USB, CAN and K-Line vehicles, chunking/slow responses, ignition off, disconnect, and low voltage. Hardware failures do not get replaced with mocks; they remain `HARDWARE_TEST_REQUIRED`.

## 17. Security, privacy, and safety

Default to local-only. Minimize permissions; cap legacy location permissions at API 30 if Bluetooth is never used for location. Explain optional location. Add notification permission only when needed by the foreground service. Define retention and user deletion for VIN, plate, GPS, raw logs, adapter IDs, and exports. Exclude secrets/private identifiers from logs and backup; configure backup rules instead of leaving templates.

No arbitrary remote pack execution. Validate sizes/types/ranges, use signatures/hashes, prevent path traversal/zip bombs, and never evaluate formulas as general code. Terminal logs redact identifiers on share. Never include credentials or seed/key algorithms in public logs.

Write/actuator/security/programming commands are out of initial scope. Read-only is the default. Clear DTC is a separately audited exception. Any future active command requires a safety case, preconditions, expert consent, vehicle applicability, timeout/cancel/recovery, and hardware evidence.

## 18. Performance and reliability budgets

Define measurable budgets and test them: bounded command/sample/log queues; no unbounded channels/lists; no I/O/SQL on main; stable memory during a 60-minute session; dashboard rendering without per-frame allocation spikes; no duplicate pollers; graceful background/foreground transitions; bounded reconnect; acceptable battery/thermal behavior; and startup/database migration measurements. Report achieved PID rate rather than promising a fixed rate independent of adapter/vehicle.

## 19. Phased execution plan and gates

### Phase 0 — protect and reproduce

Inventory changes, re-run baseline, save machine-readable test/lint/dependency reports, add CI, and write architecture decision records. Gate: baseline results and environment are reproducible; no production behavior changed silently.

### Phase 1 — truth/safety

Remove fabricated UI/results, add source/quality/Unknown states, isolate simulator, fix DTC parsing and clear verification, replace unsafe/false strings. Gate: automated tests prove no production screen can emit fake scan, adapter, protocol, or telemetry results.

### Phase 2 — canonical core

Eliminate duplicate FQCNs/stacks, establish domain errors/state machine/contracts, move tests to canonical code. Gate: duplicate-class/architecture checks pass; clean build/test/lint; obsolete sources removed.

### Phase 3 — connection platform

Implement permissions, real discovery, robust Classic/BLE/Wi-Fi, USB when ready, adapter actor, foreground service, reconnect, and measured inspector. Gate: transcript/device tests for success and every failure class; no dropped bytes or stale callbacks.

### Phase 4 — generic OBD

ECU/capability discovery, scheduler, table-driven PIDs, DTC categories, readiness, freeze frame, Mode 06, VIN/vehicle info, multi-ECU. Gate: golden vectors, simulated end-to-end flow, clean scan persistence, no unsupported polling.

### Phase 5 — durable SuperApp foundation

Room/migrations, garage, connection profiles, settings, dashboard persistence, recording/export/backup. Gate: migration/process-death/roundtrip/privacy tests.

### Phase 6 — ownership features

Trips, GPS opt-in, fuel hierarchy, fuel log, maintenance/reminders, expenses, knowledge, charts/replay/HUD/driving mode. Gate: timestamp-correct calculations, persisted workflows, accessibility/device tests.

### Phase 7 — Iranian evidence packs

Schema, validator, source registry, quarantine existing data, reconcile conflicts, lab captures, reviewed release packs. Gate: no proprietary item without provenance/vector; hardware claims only from accepted evidence.

### Phase 8 — advanced protocols

Operational ISO-TP/UDS/KWP/J1939 in evidence-driven order; DoIP/WWH/J1708 only with justified scope. Gate: conformance/golden/hardware suites and truthful capability UI.

### Phase 9 — release hardening

Dependency/license/privacy/security review, minified release, baseline profiles, performance/battery runs, backup/restore, crash recovery, store policy and hardware matrix. Gate: all definition-of-done items below have artifacts.

At every phase: implement the smallest coherent slice, run focused tests, then `clean`, `assembleDebug`, all unit tests, lint, and relevant instrumentation. Do not accumulate a giant unverified rewrite.

## 20. Definition of done

Done means all of the following are proven, not asserted:

- one canonical model and one production stack;
- real discovery/permissions and honest connection state;
- ECU evidence required for Connected;
- supported-PID discovery controls UI/polling;
- correct multi-ECU DTC/readiness/freeze-frame/vehicle-info flows;
- safe verified clear with durable before/after evidence;
- no plausible defaults or fabricated production results;
- persistent garage/settings/dashboard/diagnostics/trips/fuel/maintenance;
- timestamp-correct fuel/trip math with method/confidence;
- bounded charts/recording, working SAF export/import/backup;
- reachable adapter center/terminal/developer tools with safety gates;
- English/Persian, RTL/LTR, units, accessibility and driving mode tested;
- evidence-governed Iranian packs with no unsupported proprietary fabrication;
- lifecycle/foreground/reconnect behavior tested;
- unit, transcript, migration, UI, integration, and hardware evidence matrix maintained;
- clean debug and minified release builds, acceptable lint with documented baselines only for justified warnings;
- dependency/license/privacy/security/performance reports delivered;
- all hardware-dependent items labelled accurately.

## 21. Mandatory progress and final reporting

Before coding, report verified repository status, dirty/untracked files, toolchain, baseline, and a file-level Phase 1 plan. During work, maintain:

- changed/added/deleted files and why;
- commands/tests and exact results;
- requirements completed with evidence links;
- risks/assumptions/open hardware items;
- data claims added/changed and their sources;
- dependency/license changes;
- current maturity labels.

At the end of each phase provide:

1. outcome, not activity;
2. file/class/function changes;
3. migrations and compatibility effects;
4. test/build/lint/instrumentation/hardware results with counts;
5. remaining false/unsupported claims found by search;
6. known limitations and next highest-risk slice.

Do not report the whole application complete because code compiles or simulator tests pass. Stop only when the requested phase gate is proven or when a concrete external dependency (hardware, licensed standard, authoritative OEM data, signing secret, or user product decision) blocks safe progress; describe exactly what evidence is missing.

## 22. Authoritative research starting points

Revalidate current editions/versions when working:

- Android: Bluetooth permissions and Companion Device Manager, USB host, foreground `connectedDevice` services, offline-first architecture, Room migrations, WorkManager, Compose/adaptive/accessibility, AGP compatibility.
- Automotive: SAE J1979/J1979-DA, SAE J1979-2 OBDonUDS, SAE J2012/J2012-DA, SAE J1850, SAE J1939-73/J1939-DA, ISO 9141-2, ISO 14230, ISO 14229, ISO 15765-2/-4, ISO 13400, ISO 27145, ISO 19689, and adapter documentation.
- Adapter primary documentation: ELM Electronics ELM327 datasheet and OBD Solutions STN family manuals.

Public standard landing pages establish scope but not every normative byte/timing requirement. Obtain legitimate access to the needed standards; do not fill gaps from memory or forums. Search Persian and English sources for Iranian data, cross-check exact claims, and preserve citations/page references and hardware captures.

Begin with Phase 0 and Phase 1. The first visible milestone is not “more features”; it is an app that never lies about what it measured, detected, supports, or verified.
