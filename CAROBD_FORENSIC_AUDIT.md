# CarOBD Forensic Audit

Audit date: 2026-10-26 (Asia/Tehran)  
Scope: the complete 140-file repository, excluding generated `build/` output. No production source was modified.

## A. Executive audit summary

CarOBD is a compiling prototype, not a trustworthy production diagnostic product. It has a promising modular skeleton, real partial Classic Bluetooth/BLE/Wi-Fi transports, an ELM327 command queue, several protocol codecs, a Compose UI, and deterministic simulators. Those assets are outweighed by a split-brain domain model, duplicate legacy/new stacks, fabricated UI results, incomplete connection validation, incorrect DTC parsing, non-persistent product features, unsupported catalog claims, and almost no end-to-end or hardware evidence.

The highest-risk issue is truthfulness. `DashboardMvi.kt` supplies four invented Bluetooth scan results. `VehicleProfileMvi.performAutoScan()` only delays and then reports the already-selected protocol as detected. `AdapterInspectorScreen.kt` always claims a genuine PIC18F25K80, 28 ms latency, 12.7 V, 14.2 fps, and full Iranian protocol support. `Elm327Adapter.initialize()` can accept `NO DATA`, timeout-like results, or unverified setup commands and still create a connected state. `LiveTelemetry` supplies realistic nonzero defaults that the UI can present as measured. These are not cosmetic defects; they invalidate diagnostic trust.

The codebase contains 93 production Kotlin files and 12,467 physical lines plus 9 test files and 869 lines. There are six Gradle modules: `app`, `core:model`, `core:adapter`, `core:protocol`, `core:vehicle`, and `core:telemetry`. The debug APK is 21,335,218 bytes. There is no Git metadata in the supplied directory, no CI, no Room, no WorkManager, no USB implementation, no foreground connected-device service, no data packs, and no authoritative provenance bundle.

Verdict: retain the protocol/transport ideas and selected pure codecs, but rebuild the runtime around one canonical model and a proof-driven state machine. Remove every fabricated production result before adding features. Hardware-dependent functionality must remain labelled `HARDWARE_TEST_REQUIRED` until transcript and vehicle evidence exists.

## B. Build/test baseline

| Check | Result | Evidence |
|---|---:|---|
| Repository state | Not a Git repository | `git status`, `git branch`, and `git log` all fail because `.git` is absent |
| Toolchain | Builds | Java 24.0.1 runtime; Gradle 9.8.1; AGP 9.3.3; Kotlin plugin 2.2.10; compile/target SDK 37; min SDK 24; JVM target 17 |
| `gradlew clean --warning-mode all` | Pass | ~2 s |
| `gradlew assembleDebug --warning-mode all` | Pass | ~48 s, 46 tasks |
| `gradlew testDebugUnitTest` | Pass | ~6 s |
| `gradlew test` | Pass | 38 tests, 0 failures/errors/skips, XML aggregate 1.865 s |
| `gradlew lintDebug` | Pass with debt | 73 warnings, 0 errors: 42 unused resources, 8 newer-version, 8 ellipsis typography, 5 dependency, 5 modifier parameter, 2 AGP, 1 plurals, 1 redundant label, 1 KTX |
| Instrumentation | Not run | `adb devices` returned no device/emulator. The sole boilerplate test expects the wrong package (`com.sarani.myapplication`) and would fail against `com.sarani.carobd`. |

Compile warnings include deprecated Bluetooth and BLE APIs, deprecated Compose tab/icon APIs, deprecated system-bar color APIs, native libraries that could not be stripped, and a Gradle API deprecated for Gradle 10. Release shrinking is disabled and `app/build.gradle.kts` references a missing `proguard-rules.pro`.

The catalog is stale/misaligned as of the audit date: AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Navigation 2.10.1/2.10.2-era lint guidance, Room 2.8.5, and newer DataStore/Koin/coroutines releases exist. Upgrades must be done as a compatibility matrix, not as blind version bumps.

## C. Architecture assessment

### What exists

- Compose single-activity app with Koin and MVI-like ViewModels.
- One app repository (`ObdRepositoryImpl`) and seven thin use cases.
- Five JVM core modules with models, adapter abstractions, codecs, catalogs, and telemetry helpers.
- DataStore for a single onboarding vehicle and a raw `SQLiteOpenHelper` with five tables.
- Partial real transports: RFCOMM/SPP, two BLE service families, and TCP Wi-Fi.

### Structural failures

1. Ten domain FQCNs are duplicated between `app/src/main/.../core/model` and `core/model`, including `ConnectionState`, `DiagnosticCode`, `DtcSeverity`, `DtcSystem`, `LiveTelemetry`, `ServiceType`, `ServiceReminderItem`, `VehicleSpecSheet`, `ProtocolStandard`, and `VehicleProfile`. Their shapes disagree. App and core class JARs both contain the colliding classes; the successful APK build therefore does not prove a coherent runtime type system.
2. There are two connection/protocol/database generations: legacy app `core/connectivity`, `core/protocol`, and `core/database`, versus the new core modules and app `data/transport`. Tests often exercise the legacy path rather than production.
3. Advanced `IsoTpReassembler`, `Kwp2000Engine`, `UdsEngine`, `J1939Engine`, `TelemetryScheduler`, and `BoundedRingBuffer` have no app production call sites. They are codecs/helpers, not implemented protocols.
4. `ObdRepositoryImpl` owns connection, initialization, polling, DTCs, fuel, trips, persistence, and profile selection. It is an untestable god object.
5. Two trip systems exist (`TripComputer` and `TripLogger`), and two database helpers are instantiated. UI/settings state is frequently local memory rather than repository state.
6. Raw SQLite has an empty `onUpgrade`, no migration tests, foreign keys, indices, constraints, or reactive queries. Diagnostic history has no read surface.
7. No service owns a connection beyond UI lifetime; no process/lifecycle recovery, reconnect policy, or foreground-service notification exists.

## D. Protocol coverage assessment

| Area | Actual state | Verdict |
|---|---|---|
| ELM327 | Serialized command execution and prompt parsing exist. Initialization ignores most setup failures and accepts weak ECU evidence. Prober over-infers capabilities. | Partial, unsafe |
| STN/OBDLink | Small wrapper/probe only; not integrated as a first-class runtime driver. | Skeleton |
| J1979 modes | Builders cover 01-0A; UI/runtime polls six Mode 01 PIDs and requests 03/07/0A. No complete 02/05/06/08/09/readiness/VIN flow. | Narrow subset |
| Supported PID discovery | `0100` is sent during init, but bitmaps do not gate polling or UI. | Not operational |
| DTC | `ObdRepositoryImpl` passes service bytes into a parser that expects post-service payload, creating spurious codes. Simulator invents a count byte. Clear does not verify `44`, preserve evidence, or rescan. | Incorrect/high risk |
| ISO-TP | Single in-memory segmentation/reassembly state; no address-keyed sessions, FC transmission, timers, BS/STmin, extended addressing, CAN FD, or surfaced errors. | Codec only |
| KWP2000 | Frame builder/parser only; no 5-baud/fast-init session, P timing, keep-alive, routing, or response validation. | Codec only |
| UDS | Request builders/minimal parser; no operational ISO-TP client, session control, NRC `0x78`, timing, security boundary, or DID schema. | Codec only |
| J1939 | CAN ID and DM payload decoder; no address claim, requests, BAM/RTS-CTS transport, scheduling, or adapter configuration. | Codec only |
| OBDonUDS/WWH-OBD/DoIP/J1708/J1587 | Names or no code. | Missing |
| Multi-ECU | No discovery/routing/capability graph. | Missing |

`PidCodec.cleanResponseHex()` can collect hexadecimal letters from textual noise and does not safely separate echo, headers, multi-ECU frames, or payloads. Manual profile application sends at most a header rather than a protocol/init/verify sequence. Legacy `AutoProtocolScanner` returns generic success after all candidates fail; the active UI scan is entirely simulated.

## E. Iranian support assessment

The repo contains useful taxonomy work but no defensible support proof. Counts are: 14 core vehicles, 13 core ECU profiles, 8 core spec sheets, 8 legacy vehicle presets, 7 legacy protocol profiles, 8 legacy spec sheets, 4 motorcycle pinout records, 4 Chinese vehicle records, 55 legacy sensor definitions, 20 legacy DTC literals, and 23 core DTC entries.

Core data labels many records `PRODUCTION_IMPLEMENTED` and `VERIFIED_OEM`, while most have no source or use opaque identifiers such as `PSA-DOC-206-MUX-ED04` rather than resolvable evidence. Public English/Persian research confirms that ECU families such as Bosch ME7.4.4 occur in the market, but it did not establish the repo's exact headers, init sequences, proprietary PIDs, formulas, pin allocations, or broad vehicle compatibility. Those claims must be demoted to `COMMUNITY_REPORTED` or `HARDWARE_TEST_REQUIRED` until primary evidence/transcripts exist.

Internal contradictions include TU5 oil capacity 3.75 L versus 3.25 L, EF7 5.25 L versus 5.5 L, and Shahin 4.0 L versus 3.8 L. Placeholder requests such as `IR_STEP`, `MC_GEAR`, and `CH_BOOST` have no executable request/formula. `01FE` is labelled transmission temperature without a vehicle-specific proof. `SensorCatalog.findById()` silently returns RPM for unknown IDs. ISO 19689 specifies a motorcycle diagnostic connector, not the repo's claimed universal six-pin color/protocol/pinout; those claims are not proven by the standard.

Required policy: each data fact needs a stable ID, value/unit, applicability, source URL/document/page, capture/test ID, author/reviewer, version, confidence, and status. Conflicting values remain visible and quarantined; they must never silently become verified.

## F. UI/product assessment

The app has eight screen composables and attractive dark automotive styling, but it is a static prototype. Terminal exists in navigation code but has no discoverable UI route. Six bottom destinations are cramped. Dashboard widgets are a list; size is ignored and there is no grid, page, drag/drop, resize, persistence, capability gating, or presets. Charts have about 35 untimestamped values and no pause, cursor, zoom, pan, multi-series, recording, export, or replay.

Maintenance starts at a hardcoded 115,000 km with six memory-only reminders. Settings toggles use `remember` and do not call DataStore; theme is always dark and locale switching is absent. Trip uses an unbounded list, fixed 0.1-second integration, a fabricated fuel formula, and an export preview that does not save. Garage is not wired to UI. The sensor picker shows unsupported entries. Accessibility is weak: many null content descriptions, 9.5-12sp text, no driving-distraction mode, and no adaptive layout.

Diagnostics lacks verified ECU identity, readiness, freeze frame, Mode 06, permanent-code semantics, multi-ECU grouping, provenance, scan history, and safe clear workflow. A Persian resource says a no-code result means the ECU is “completely normal”, which is diagnostically false. Troubleshooting tells users to manipulate vehicle connector pins with pliers; remove this unsafe instruction.

## G. Competitor gap assessment

| Capability | CarOBD | Market evidence |
|---|---|---|
| Real adapter discovery | Fabricated list in active UI | Mature apps connect to real Classic/BLE/Wi-Fi/USB adapters |
| Generic OBD breadth | Six live PIDs; broken DTC path | OBD Auto Doctor documents confirmed/pending/permanent DTCs, freeze frame, readiness, Modes 05/06, ECU information, 126+ PIDs |
| Dashboard | Memory-only vertical list | Car Scanner and Torque support user layouts/custom PIDs; Torque has a plugin/theme ecosystem |
| Recording/export | Fake preview, no durable files | AndrOBD supports save/load/replay/CSV and USB; Infocar exports logs and provides mapped driving records |
| Vehicle ownership | Single onboarding object | Infocar includes trips, driving scores, maintenance, and expenses |
| Extensions | None | Torque and AndrOBD expose plugin/custom data mechanisms |
| Iranian specialization | Large but unverified static claims | Potential differentiator only after evidence-driven packs and hardware validation |

Do not copy closed-source behavior or data. AndrOBD is useful for design/test comparison, but its GPL license must be respected; do not transplant GPL code into a differently licensed product without a deliberate legal decision.

## H. Most critical 20 defects/gaps

1. Fabricated adapter-health production screen.
2. Fabricated Bluetooth scan results and scan animation.
3. Fabricated protocol auto-detection and success message.
4. False-positive ELM initialization/connected state.
5. Incorrect DTC response slicing and simulator format.
6. Duplicate FQCN/domain model split across app and core.
7. Duplicate legacy/new connectivity, protocol, database, and trip stacks.
8. Real scanner exists but is not wired; Android permission UX is absent.
9. Nonzero telemetry defaults presented without quality/provenance.
10. No supported-PID/capability-driven poller; fixed sequential six-PID loop.
11. Cold telemetry flow can start duplicate poll loops; failures are swallowed.
12. BLE operations are unqueued and connection is declared before CCCD completion.
13. Classic/Wi-Fi incoming chunks can be dropped and disconnects are not state.
14. No USB transport despite interface comments and product ambition.
15. No foreground connected-device service/reconnect/lifecycle ownership.
16. Protocol engines are non-operational codecs and not integrated.
17. Raw persistence has no migrations/history reads; settings and product data do not persist.
18. Iranian/OEM values and proprietary PIDs lack auditable provenance and conflict internally.
19. Tests are happy-path/unit-heavy, often exercise obsolete code, and have no device/hardware/transcript gates.
20. Product claims (“100+ sensors”, universal support, “completely normal”) exceed executable, verified capability.

## I. Recommended target architecture

Use one canonical domain model and these dependency directions:

```text
app / feature:*  -> domain contracts <- data repositories
                                  ^
transport:android -> adapter engine -> protocol engines
                                  |
                    data-pack registry + Room
```

Recommended modules: `core:model`, `core:common`, `core:testing`, `domain`, `transport:api`, `transport:android`, `adapter:elm`, `protocol:obd`, `protocol:isotp`, `protocol:uds`, `protocol:kwp`, `protocol:j1939`, `data:database`, `data:packs`, and feature modules for connection, dashboard, diagnostics, garage, trips, maintenance, fuel, knowledge, settings, terminal, and developer tools. Start smaller if build overhead matters, but keep these boundaries in packages/contracts.

The connection state machine must distinguish permission, discovery, device selection, transport connection, adapter identity, adapter configuration, protocol detection, ECU evidence, capability discovery, connected/degraded/retrying, and failure. Every telemetry sample needs timestamp, ECU/source address, unit, quality, and provenance. One scheduler owns all commands with priority, deadlines, backpressure, retry classes, cancellation, and throughput metrics. Room is the local source of truth; DataStore only stores small preferences.

## J. Implementation priority

1. **Truth and safety gate:** delete/quarantine fabricated production results; introduce explicit Unknown/Unavailable/Simulated states; fix DTC parser boundary and clear verification.
2. **Coherence gate:** select the canonical core model, migrate callers, remove duplicate FQCNs and obsolete stacks, create compile-time architecture tests.
3. **Connection gate:** permissions, real discovery, robust transports, foreground service, state machine, adapter transcripts, reconnect, lifecycle tests.
4. **Generic OBD gate:** ECU discovery, supported PID bitmaps, Mode 01/02/03/04/06/07/09/0A, readiness, VIN, freeze frames, multi-ECU, durable scan sessions.
5. **Persistence/product gate:** Room migrations, garage, settings, dashboards, trips, maintenance, fuel, SAF export/backup.
6. **Iranian pack gate:** source registry, schema/signatures, quarantined candidate packs, lab captures, vehicle matrix. No proprietary request ships merely because it exists in the current source.
7. **Advanced protocol gate:** operational ISO-TP/UDS/KWP/J1939 behind conformance transcripts; DoIP/WWH/J1708 only when product evidence and hardware justify them.
8. **Release gate:** accessibility, driving mode, performance/battery benchmarks, security/privacy/licensing review, CI, device matrix, signed hardware evidence.

## Research sources

- Android: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [companion pairing](https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing), [USB host](https://developer.android.com/develop/connectivity/usb/host), [foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [offline-first architecture](https://developer.android.com/topic/architecture/data-layer/offline-first), [AGP compatibility](https://developer.android.com/build/releases/about-agp), [Compose setup/BOM](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [Room releases](https://developer.android.com/jetpack/androidx/releases/room).
- Standards: [SAE J1979](https://saemobilus.sae.org/standards/j1979_202505-e-e-diagnostic-test-modes), [SAE J1979-2](https://saemobilus.sae.org/standards/j1979-2_202104-e-e-diagnostic-test-modes-obdonuds), [SAE J1939-73](https://saemobilus.sae.org/standards/j1939-73_202609-application-layer-diagnostics), [SAE J1850](https://saemobilus.sae.org/standards/j1850_199007-class-b-data-communication-network-interface), [ISO 9141-2](https://www.iso.org/standard/16738.html), [ISO 14230-4](https://www.iso.org/standard/28826.html), [ISO 14229-1](https://committee.iso.org/standard/87962.html?browse=tc), [ISO 15765-2](https://www.iso.org/standard/84211.html), [ISO 15765-4](https://www.iso.org/standard/78384.html), [ISO 13400-2](https://www.iso.org/standard/13400-2), [ISO 27145-4](https://www.iso.org/standard/68571.html), [ISO 19689](https://www.iso.org/standard/66030.html).
- Adapters/competitors: [ELM327 datasheet](https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327DS.pdf), [STN1110 datasheet](https://www.scantool.net/scantool/downloads/313/stn1110_ds_f.pdf), [Torque Wiki](https://wiki.torque-bhp.com/view/Main_Page), [Car Scanner](https://www.carscanner.info/), [Infocar Play listing](https://play.google.com/store/apps/details?id=mureung.obdproject), [OBD Auto Doctor features](https://www.obdautodoctor.com/features/), [AndrOBD](https://github.com/fr3ts0n/AndrOBD).

Research limitation: most complete standards are paywalled. Public landing pages establish scope, not every byte/timing rule. No authoritative public source found in this pass proves the repository's exact Iranian proprietary headers, PIDs, formulas, or motorcycle pinouts. That absence is a reason to quarantine claims, not to infer them.
