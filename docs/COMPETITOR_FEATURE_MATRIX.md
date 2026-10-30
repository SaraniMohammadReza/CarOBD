# Competitor concepts and verified CarOBD gaps

Audited baseline: `3da2816`; latest source checkpoint: `1f32763`, 2026-10-30. Competitor columns describe concepts supplied by the owner, not independently verified current product editions or protocol support. No screenshot assets, proprietary copy, or layout are reused. `—` means not established by the supplied concepts, not that a competitor lacks the feature.

| Feature | InfoCar concept | Torque concept | CarOBD status | Existing implementation / remaining gap |
|---|---|---|---|---|
| Connection management | Connection action | Adapter settings | PARTIAL | Canonical profiles and Classic/BLE/Wi-Fi/USB/simulator; physical bench validation pending |
| Vehicle profiles | Registration | Explicit profile setup | PARTIAL | Catalog selection persisted; optional onboarding no longer injects a vehicle, displacement, tank, price or odometer. Existing owner metadata retained; localized Persian/Arabic numeric input accepted without guessing grouping. |
| Dashboard | Monitoring/dashboard | Realtime gauges | PARTIAL | Shared telemetry with quality; new selections require session capability and an implemented polling path. Catalog still contains disabled foundations. |
| Custom dashboard | Dashboard | Configurable gauges | PARTIAL | Add/remove/style selection, compact/full width and reorder persist (24-widget bound). Editing style on an existing widget remains a gap. |
| Graphs | Monitoring | Graphing | PARTIAL | Per-PID 120-point timestamp history, explicit missing/stale state, separate paths across gaps/rollback; seven polled Mode01 PIDs plus observed ATRV only. |
| DTC | Diagnosis | Fault codes | PARTIAL | Safe 03/07/0A with responder outcomes; stored/pending/permanent groups are visible. |
| DTC history | Recent diagnosis | — | PARTIAL | Explicit SAF schema1 JSON export of existing evidence, including source/unread services; persistent v4 DTC-scope reports now retain partial/failure/raw/source evidence and recent details; readiness/02/09/06 are explicitly not collected in these reports. |
| Diagnostic levels | Depth selector | — | NOT_IMPLEMENTED | Do not expose levels until their executed safe scope is real |
| Readiness | Proposed diagnosis scope; service not established | — | PARTIAL | Per-responder read-only evidence; not integrated into persistent reports |
| Freeze frame | Proposed diagnosis scope; service not established | — | PARTIAL | Selected frame/base page; no claim of complete frame enumeration |
| Mode09 | Proposed diagnosis scope; service not established | — | PARTIAL | VIN/CALID/CVN/name reads; not integrated into persistent reports |
| Mode06 | Proposed diagnosis scope; service not established | Test results concept | PARTIAL | Supported CAN raw monitor records; no invented OEM interpretation |
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
| Backup | — | Backup/restore | PARTIAL | Report/transcript exports only; OS cloud backup excluded, modern explicit device transfer eligible except permission history. Full app backup/restore not implemented. |
| Help | Guide/FAQ | — | PARTIAL | Existing help and evidence limitations; contextual coverage incomplete |
| Localization | — | — | PARTIAL | English/Persian resources and runtime checks; notification units/language follow saved settings; some catalog labels remain Persian |
| Physical clear | Clear | Clear | SAFETY_DISABLED | Physical 04 disabled; isolated simulator clear only |
| Writes/scripts/plugins | — | Scripts/plugins | SAFETY_DISABLED | Unknown commands, 08, security/programming and physical scripting denied |

## Selected work

Implemented this checkpoint: optional vehicle metadata, persistent DTC-scope reports and separate categories, capability-gated widget selection, layout persistence/reorder/resize, and bounded gap-aware per-PID graphs. Tests and device evidence are recorded in [development report](COMPETITOR_GAP_DEVELOPMENT.md).

Next: extend the report through existing typed readiness/02/09/06 readers with session identity guards, then broaden the canonical sensor plan only where decoding and support evidence are proven. Inspector/recording/trip improvements follow. Maintenance, alerts, backup and help remain later priorities.

A status is scoped to the stated software path. COMPLETE does not establish physical adapter/vehicle compatibility. New work must record targeted tests, full gate evidence and remaining device/vehicle checks. See [support matrix](../OBD_SUPPORT_MATRIX.md), [runtime QA baseline](RUNTIME_QA_CONTINUATION.md) and [architecture](../ARCHITECTURE.md).

Overnight software/emulator checkpoints also guard stale service actions and ongoing runtime admission. Structured report export and explicit Android backup policy do not expand diagnostic scope or establish vehicle safety. See [canonical handoff](AGENT_CONTINUATION_HANDOFF.md), [privacy](DATA_PRIVACY_AND_EXPORT.md) and [remaining gaps](CAROBD_REMAINING_GAPS.md).
