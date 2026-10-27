# CarOBD Support Matrix

Status legend (§56):

- **IMPLEMENTED** — code present; **SOFTWARE VERIFIED** by unit test / compile.
- **UNIT TESTED** — covered by a test in `core:{model,adapter,protocol,telemetry,vehicle}:test` or `:app:testDebugUnitTest`.
- **SIMULATOR VERIFIED** — reproducible via the deterministic simulator.
- **UI INTEGRATED** — reachable from the app UI.
- **DEVICE TEST REQUIRED** — software-complete but not validated on real hardware.
- **VEHICLE TEST REQUIRED** — needs a real vehicle/E CU.
- **DISABLED FOR SAFETY** — intentionally off / gated.
- **NOT IMPLEMENTED** — architecture only / absent.

| Feature | Status | Notes |
|---|---|---|
| Bluetooth Classic | SOFTWARE VERIFIED • UI INTEGRATED • DEVICE TEST REQUIRED | `BluetoothClassicTransport` (RFCOMM), `BluetoothDeviceScanner`, runtime permission flow, system-pairing guidance. Physical ELM via BT not yet verified. |
| Bluetooth LE | SOFTWARE VERIFIED • UI INTEGRATED • DEVICE TEST REQUIRED | `BleTransport` GATT RX/TX characteristics, AUTO mode; advanced UUID override isolated. Clone adapters unverified. |
| Wi-Fi TCP | SOFTWARE VERIFIED • UI INTEGRATED • DEVICE TEST REQUIRED | `WifiTcpTransport`, configurable host/port/timeout. Defaults are suggestions only. |
| USB Serial | SOFTWARE VERIFIED • UI INTEGRATED • DEVICE TEST REQUIRED | `UsbSerialTransport`, permission-state UI, VID/PID. |
| Simulator | IMPLEMENTED • UNIT TESTED • SIMULATOR VERIFIED | `IsolatedSimulatorTransport` deterministic scenarios (§39): healthy, NO DATA, 7F, timeout, late prompt, BUS INIT/CAN ERROR, multi-ECU, ISO-TP error, disconnect. Always visibly SIMULATOR. |
| ELM initialization | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | ATI/ATRV/ATDP/ATDPN sequence, STN detection (`StnAdapter`), echo handling, reset. |
| STN detection | IMPLEMENTED • UNIT TESTED | Positive-detection gating; STN/STPX only after detection. |
| Protocol selection | IMPLEMENTED • UI INTEGRATED • SIMULATOR VERIFIED | Auto / Profile / Manual (0–C). ISO 15765 CAN 11/29 × 250/500; J1932/J1850; ISO 9141-2; KWP slow/fast. |
| Vehicle profile application | IMPLEMENTED • UNIT TESTED • VEHICLE TEST REQUIRED | Profile drives recommended protocol + full ordered init (header, filters, SAFE PID pack) before ECU handshake. |
| Mode 01 | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | `0100/0120/.../01A0` capability map, extension bit respected (§22). |
| Mode 02 (freeze frame) | IMPLEMENTED (matrix) • PARTIAL • VEHICLE TEST REQUIRED | Frame index + PID + raw + decoded + responder preserved in `DtcParser`. Full end-to-end freeze-frame fetch pending. |
| Mode 03 (stored DTC) | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | Protocol-aware framing (non-CAN `43 01 33 ...` vs CAN counted) (§26). |
| Mode 04 (clear) | IMPLEMENTED • UNIT TESTED • UI INTEGRATED • SAFETY-GATED | Confirmation required; positive `0x44`; `7F`/`NO DATA`/timeout/wrong-SID = failure. |
| Mode 05 | IMPLEMENTED (matrix) • VEHICLE TEST REQUIRED | O2 sensor monitor (`O2_MONITOR`, `0x45`) in `Obd2Command`/`PidCodec`. |
| Mode 06 | IMPLEMENTED (matrix) • VEHICLE TEST REQUIRED | Monitor results (monitorId/testId/componentId/measured/min/max/passed/unit). |
| Mode 07 (pending DTC) | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | Pending-DTC framing. |
| Mode 08 | DISABLED FOR SAFETY | Defined in matrix (§32) but no actuator control exposed. |
| Mode 09 | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | VIN (0902), CALID (0903/0904), CVN (0905/0906), ECU name (090A), in-use perf (0907/090B). Binary CVN handled; partial CVN = PARTIAL/MALFORMED. |
| Mode 0A | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | Vehicle info (0900/0901). |
| Readiness | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | Mode 01 PID 01: MIL, DTC count, supported/complete/incomplete monitors (§10 tests). |
| Negative responses (7F) | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | `EcuRejectionException` preserves requested service + NRC + responder (§27). |
| Multi-ECU | IMPLEMENTED • UNIT TESTED • VEHICLE TEST REQUIRED | Responder identity survives transport → parser → ISO-TP → domain → UI → logging → export. `ECU <id>` for unknown responders. |
| ISO-TP | IMPLEMENTED • UNIT TESTED • VEHICLE TEST REQUIRED | ELM-managed (AT RX/TX filters) + app-managed reassembly per responder (SF/FF/CF, rollover, FF interruption, timeout, stale expiry, padding) (§34). |
| K-Line (9141-2 / KWP) | IMPLEMENTED (matrix) • VEHICLE TEST REQUIRED | ISO 9141-2, KWP slow/fast in `ObdProtocol` + profile hints (§35). |
| Custom PID | IMPLEMENTED (architecture) • VEHICLE TEST REQUIRED | `CustomPidDefinition` (ATSH/ATCRA, formula, bit range, unit, valid range, provenance, confidence). No guessed Iranian proprietary PIDs. |
| Record / Replay | IMPLEMENTED • UNIT TESTED • SIMULATOR VERIFIED (infra) | `RecordingTransport` + `ReplayTransport` + `DiagnosticTranscript` JSON/CSV (§38). **Session-list UI = NOT IMPLEMENTED.** |
| Logging / debug console | IMPLEMENTED (infra) • UI PARTIAL | Transcript capture + export (§38). In-app console screen (§40) partially wired; full filter/copy/export UI pending. |
| Localization | IMPLEMENTED • UNIT TESTED • SOFTWARE VERIFIED | Persian (RTL) / English (LTR); persistence; LocaleHelper lifecycle; string-resource migration for Dashboard/Settings/Connection/Adapter/Vehicle/Diagnostics/Simulator/Errors. Not: DTC/PID/hex/AT/VIN/CALID/protocol identifiers. |
| Units | IMPLEMENTED • SOFTWARE VERIFIED | km/h·mph, °C·°F, km·miles, L/100km·mpg(valid data only). Raw ECU values keep canonical SI. |
| Dashboard | IMPLEMENTED • UNIT TESTED • UI INTEGRATED | Status strip + numeric cards (compact, J7-class). Unknown = `—` never `0`. |
| Settings | IMPLEMENTED • UI INTEGRATED | Sections: App & Display, Connection & Adapter, Vehicle & Profile, Diagnostics & Data, Logging/Record/Replay, Advanced/Developer, Safety & Privacy, About/Version. All items have a persisted runtime effect. |
| Health engine / post-drive report | IMPLEMENTED (arch) • NOT IMPLEMENTED (UI) | Evidence rule engine (coolant/charging/fuel-trim/etc) pending UI/report wiring. |
