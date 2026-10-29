# CarOBD Support Matrix

Current baseline: 2026-10-30. SOFTWARE VERIFIED refers to supplemental JVM/source checks, not Android packaging. BUILD VERIFIED is pending. DEVICE TEST REQUIRED and VEHICLE TEST REQUIRED apply to all physical diagnostic behavior. No VEHICLE_SAFE/FULLY_VALIDATED claim is made.

| Feature | Current status and scope |
|---|---|
| Single-command grammar / authorization | SOFTWARE VERIFIED; narrow mandatory read-only dispatch; unknown denied |
| Physical Mode 04 | DISABLED FOR SAFETY at repository and adapter; no re-enablement in Sprint 2 |
| Mode 08, ECU reset, security, write, programming, arbitrary STPX | DISABLED FOR SAFETY |
| STN helpers | Use normal authorization and are denied; hardware detection is not authorization |
| Bluetooth Classic / BLE | Source integration present; explicit MAC; BLE CCCD and generation gate unit tested; DEVICE TEST REQUIRED |
| Wi-Fi TCP | Explicit host/port; EOF/write failure closes link; DEVICE TEST REQUIRED |
| USB | Exact target/interface selection SOFTWARE VERIFIED; generic CDC line configuration must acknowledge; specialized serial drivers NOT IMPLEMENTED; DEVICE TEST REQUIRED |
| AUTO / manual protocol | 0–9/A selection and rejection/readback tests; A=ATSPA; B/C/unknown and vehicle-profile forcing unavailable |
| Mode 01 | Capability-gated baseline telemetry; extension-gated traversal through E0, max eight pages; unknown capability yields no baseline PID polling |
| Mode 03 / 07 / 0A | SOFTWARE VERIFIED production repository framing and stored/pending/permanent categories; complete/partial/failed scans preserve responders; 0A is permanent DTCs |
| Mode 02 / 06 / 09 | Structurally constrained reads may pass policy; decoder/domain helpers exist; complete product workflows are not claimed |
| Mode 05 | Not in the live dispatch allowlist |
| Physical clear UI | Transmission unavailable; simulator-only local clear uses a separate method |
| Multi-responder | Positive/negative/malformed/NO DATA evidence retained; unknown responder nullable, module roles not invented |
| ELM framing | Compact/spaced, headerless, supported CAN headers and single-frame PCI; malformed fails |
| Raw ISO-TP | Standalone helpers only; live FF/CF/numbered dumps fail; production integration NOT IMPLEMENTED |
| Custom PID / header routing | Live arbitrary service/header/payload configuration DISABLED FOR SAFETY |
| Connection Center | Explicit profile editors and disconnect control; source compiled; UI/device rendering and permission behavior require validation |
| Connection test | Real exclusive read-only executor; six stages retain evidence; closes test session; tested with production repository/fake physical transport |
| Poll mode | Persisted ECO/NORMAL/FAST; eager repository wiring; disk recreation and live pacing regression tests |
| Simulator | Healthy no-code, stored/pending/permanent, malformed, multi ECU/negative, disconnect, real delayed prompt, NO DATA/7F/timeout/bus errors; isolated from hardware |
| Record/replay | Standalone transcript/transport infrastructure; replay has no physical delegate; session-list/profile replay NOT IMPLEMENTED |
| DTC durable storage | Disabled pending truthful responder/category/protocol/MIL schema |
| Health engine / post-drive report | Not part of this baseline; no healthy-vehicle inference from absent DTCs |
