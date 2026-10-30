# Competitor concepts and verified CarOBD gaps

Baseline: `3da2816`, 2026-10-30. Competitor columns describe concepts supplied by the owner, not independently verified current product editions or protocol support. No screenshot assets, proprietary copy, or layout are reused. `—` means not established by the supplied concepts, not that a competitor lacks the feature.

| Feature | InfoCar concept | Torque concept | CarOBD status | Existing implementation / remaining gap |
|---|---|---|---|---|
| Connection management | Connection action | Adapter settings | PARTIAL | Canonical profiles and Classic/BLE/Wi-Fi/USB/simulator; physical bench validation pending |
| Vehicle profiles | Registration | Explicit profile setup | PARTIAL | Catalog selection persisted; onboarding still injects sample metadata and must be repaired |
| Dashboard | Monitoring/dashboard | Realtime gauges | PARTIAL | Shared telemetry with quality; catalog and defaults exceed proven support |
| Custom dashboard | Dashboard | Configurable gauges | PARTIAL | Add/remove/style/size exist; capability admission and complete layout UX need work |
| Graphs | Monitoring | Graphing | PARTIAL | Chart exists; timestamped gaps and unavailable values need verification |
| DTC | Diagnosis | Fault codes | PARTIAL | Safe 03/07/0A with responder outcomes; category presentation can improve |
| DTC history | Recent diagnosis | — | PARTIAL | Persistent code history; not a complete diagnosis report |
| Diagnostic levels | Depth selector | — | NOT_IMPLEMENTED | Do not expose levels until their executed safe scope is real |
| Readiness | Diagnosis | Diagnostic evidence | PARTIAL | Per-responder read-only evidence; not integrated into persistent reports |
| Freeze frame | Diagnosis | Diagnostic evidence | PARTIAL | Selected frame/base page; no claim of complete frame enumeration |
| Mode09 | Vehicle data | Vehicle data | PARTIAL | VIN/CALID/CVN/name reads; not integrated into persistent reports |
| Mode06 | Engine diagnosis | Test results | PARTIAL | Supported CAN raw monitor records; no invented OEM interpretation |
| Manufacturer data | Manufacturer section | Extra sensors | FOUNDATION | Catalog is not verified physical compatibility |
| Adapter inspector | Connection details | Adapter status | PARTIAL | Session evidence exists; no ATI authenticity claim or invented counters |
| Trips | Driving record/log | Logging | PARTIAL | Persistent measured/derived/legacy evidence with coverage; no GPS assumption |
| Driving style | Style view | — | PARTIAL | Objective manual-log heuristics only; no invented scores |
| Fuel | Fuel | Fuel gauges | PARTIAL | Labelled MAF estimate; no manual tank-to-tank ledger |
| Maintenance | Maintenance log | — | PARTIAL | User odometer/service records; interval provenance needs expansion |
| Recording | Driving log | Logging/upload | PARTIAL | Bounded passive recording, SAF export/import; no cloud upload |
| Replay | — | — | COMPLETE | Local transcript replay without physical delegate; bounded validated import |
| Custom PID | Manufacturer data | Extra PIDs | FOUNDATION | No arbitrary physical command/formula dispatch |
| Alerts | — | Alarms | NOT_IMPLEMENTED | No threshold-alert product yet |
| HUD | HUD | Dash installation | NOT_IMPLEMENTED | Deferred behind diagnostic correctness |
| Parking | Parking | — | NOT_IMPLEMENTED | Deferred optional location feature |
| Dashcam | Dashcam | — | OUT_OF_SCOPE | No camera recording this sprint |
| Backup | — | Backup/restore | NOT_IMPLEMENTED | Transcript export is not a full application backup |
| Help | Guide/FAQ | — | PARTIAL | Existing help and evidence limitations; contextual coverage incomplete |
| Localization | — | — | PARTIAL | English/Persian resources and runtime checks; some catalog labels remain Persian |
| Physical clear | Clear | Clear | SAFETY_DISABLED | Physical 04 disabled; isolated simulator clear only |
| Writes/scripts/plugins | — | Scripts/plugins | SAFETY_DISABLED | Unknown commands, 08, security/programming and physical scripting denied |

## Selected work

1. Repair truthfulness regressions first: explicit vehicle metadata must not be supplied by sample defaults; no unknown graph value may become zero.
2. Build a persistent diagnosis report using existing session admission/parser paths. Keep partial outcomes, source identity and responders; never relabel the old code-only history as a complete report.
3. Improve category UX, then capability-driven dashboard admission and bounded graph segments. All widgets must consume the existing producer.
4. Improve inspector/recording/trip surfaces only after those foundations are verified. Maintenance, alerts, backup and help remain later priorities.

A status is scoped to the stated software path. COMPLETE does not establish physical adapter/vehicle compatibility. New work must record targeted tests, full gate evidence and remaining device/vehicle checks. See [support matrix](../OBD_SUPPORT_MATRIX.md), [runtime QA baseline](RUNTIME_QA_CONTINUATION.md) and [architecture](../ARCHITECTURE.md).
