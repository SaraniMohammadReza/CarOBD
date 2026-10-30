# CarOBD Sprint 5 support matrix

Current software scope, 2026-10-30. See [Sprint 5 evidence and limitations](docs/SAFETY_SPRINT_5.md). Software tests do not establish physical adapter or vehicle compatibility. Every physical feature requires DEVICE_REQUIRED and VEHICLE_REQUIRED validation. Physical Mode04 remains disabled.

| Feature | Status | Evidence / limits |
|---|---|---|
| Canonical command admission | SOFTWARE_IMPLEMENTED | Single bounded command; controls and unknown commands denied at mandatory adapter boundary |
| Physical Mode04 / Mode08 / security / write / programming | SAFETY_DISABLED | No terminal, replay or STN bypass; simulator-local clear only |
| Cancellation / timeout isolation | SOFTWARE_TESTED | Retired canceled stream; bounded late-prompt quarantine; missing boundary retires session |
| Session and producer ownership | SOFTWARE_TESTED | Generation-bound callbacks; one session producer; EOF invalidates Connected |
| Classic Bluetooth | DEVICE_REQUIRED | Exact MAC; runtime permission admission; bounded discovery cleaned on lifecycle stop |
| BLE | DEVICE_REQUIRED | Exact MAC; CCCD acknowledgement; notify/indicate; immutable API33 callbacks; stale callback guard |
| Wi-Fi TCP | DEVICE_REQUIRED | Explicit host/port; bounded connect; EOF/write failure closes link |
| USB | PARTIAL / DEVICE_REQUIRED | Exact target/interface; acknowledged generic CDC configuration; specialized FTDI/CH34x drivers not implemented |
| Protocol selection | SOFTWARE_TESTED | AUTO and codes0–9/A with observed readback; B/C/unknown/profile forcing fail closed |
| Connection test | SOFTWARE_TESTED | Exclusive read-only stage evidence; physical pass requires ECU response and observed protocol; test session closes |
| Permissions | DEVICE_REQUIRED | Typed denied/permanent/unsupported/off; actionable controls; persisted request history; bench dialogs unexecuted |
| Foreground ownership | DEVICE_REQUIRED | Token-bound start/ack/failure/stop; stale callback denied; notification visibility admission; no automatic reconnect |
| Mode01 live telemetry | PARTIAL | Capability-gated bounded pages; supported numeric decoders only; unknown not zero |
| Readiness0101 | SOFTWARE_TESTED | Exact four-byte status; ignition type; malformed/no-data/negative/timeout remain distinct; explicit read UI |
| Mode02 | PARTIAL | Selected frame index0–255 and its base PID page; capability-gated metrics, raw unknowns, per-responder outcomes; no all-frame enumeration |
| Mode03 /07 /0A | SOFTWARE_TESTED | Stored/pending/permanent distinct; malformed/timeout not No Codes; multi-responder evidence |
| Mode05 | NOT_IMPLEMENTED / SOURCE_EVIDENCE_REQUIRED | No live product dispatch or invented oxygen-monitor semantics |
| Mode06 | PARTIAL / SOURCE_EVIDENCE_REQUIRED | Observed CAN only; capability-gated MID pages; nine-byte raw test records; unit/signedness/scaling/component/pass-fail unknown |
| Mode09 | PARTIAL | Advertised VIN/count/CALID/CVN/ECU-name; strict CAN layouts and legacy VIN sequence; partial CVN retained; broader non-CAN info formats unsupported |
| CAF1 numbered messages | PARTIAL | Strict numbered assembly for Mode06/09; raw FF/CF live stream remains unsupported |
| Raw ISO-TP / UDS / KWP / J1939 helpers | FOUNDATION_ONLY | Standalone models/codecs are not unrestricted live OEM workflows or certified protocol stacks |
| Responder identity | SOFTWARE_TESTED | Observed ID preserved; unknown nullable; no fabricated7E8 or OEM module role |
| Simulator | SOFTWARE_TESTED | Explicit SIMULATOR label; deterministic faults/multi-ECU/delay fixtures; no physical delegate |
| Recording | SOFTWARE_TESTED / DEVICE_REQUIRED | Production transport wrapper, explicit start/stop, active indicator, atomic private store, list/detail/delete, metadata/raw/typed events |
| Recording limits | PARTIAL | Starts when requested, not retroactive initialization; bounded events/bytes; process kill recovery not guaranteed to last millisecond |
| JSON/CSV export and JSON import | SOFTWARE_TESTED / DEVICE_REQUIRED | Structural bounded JSON, control/CR/LF preservation, CSV escaping; SAF UI requires device exercise |
| Replay | SOFTWARE_TESTED | Isolated simulator-only transport; expected TX correlation, mismatch/late prompt/quarantine/disconnect/negative/no-data evidence; no physical delegate |
| DTC history | SOFTWARE_TESTED / DEVICE_REQUIRED | Complete scans persisted with exact category/responder and nullable observed MIL/protocol; legacy values marked unverified; Android migration test compiled, not run |
| Dashboard voltage | SOFTWARE_TESTED | ATRV adapter supply and0142 control-module voltage distinct |
| Dashboard freshness | SOFTWARE_TESTED | Per-PID timestamps; measured/stale/unavailable; failed reads do not refresh old measurement; real zero retained |
| Dashboard session | SOFTWARE_TESTED / DEVICE_REQUIRED | Selected/connecting/active profile distinct; observed protocol; actual SIMULATOR and recording status |
| Poll mode | SOFTWARE_TESTED | Persisted ECO/NORMAL/FAST; disk recreation and live pacing tests |
| Theme | SOFTWARE_TESTED / DEVICE_REQUIRED | Four applied palettes; Compose visual contrast requires device review |
| Units | SOFTWARE_TESTED | Display-only speed/distance/temp/pressure conversion; canonical evidence unchanged |
| Language | PARTIAL / DEVICE_REQUIRED | Persisted EN/FA; locale applied before activity creation; RTL/LTR; both languages bundled offline; technical/catalog strings remain |
| Timeouts | SOFTWARE_TESTED | Profile100–30000ms bounds; connection watchdog/command upper bound; telemetry may use shorter deadline |
| Auto reconnect | NOT_IMPLEMENTED | Control hidden; legacy field is not a runtime promise |
| Adapter inspector | PARTIAL | Reported/observed identity and heuristic metadata; ATI is not proof of authenticity |
| Manual trip capture | PARTIAL | Shared metric evidence and measured cadence; session-token boundary; unknown fuel; bounded in-memory CSV capture, no new durable manual-trip claim |
| Automatic trip evidence | SOFTWARE_TESTED / DEVICE_REQUIRED | Canonical per-metric nullable values, origin/source/method/range/count/coverage; additive v3 persistence; legacy rows remain unverified; Android migration execution required |
| Custom PID/header routing | SAFETY_DISABLED / NOT_IMPLEMENTED | Static sensor catalog is not arbitrary custom command support |
| Maintenance / vehicle catalog | PARTIAL / SOURCE_EVIDENCE_REQUIRED | Reference metadata, not observed vehicle identification or OEM compatibility certification |
| Health engine / post-drive report | NOT_IMPLEMENTED | No health inference from absent DTCs; needs validated provenance-aware trip/evidence model |

Sprint5 adds explicit setup bench capture using the canonical transcript, paged recording lists, import bound regressions and source-backed legacy VIN sequence assembly. Mode06 admission requires observed ISO15765-4; scaling/pass-fail remain unknown. Dashboard fuel/range/cost no longer use stale inputs or a fabricated default average. See [device/vehicle execution checklist](docs/SPRINT5_BENCH_CHECKLIST.md).

## Competitor-gap checkpoint (2026-10-30)

- Diagnostic reports: SOFTWARE_TESTED / EMULATOR_TESTED for executed 03/07/0A scope, including partial/malformed/no-data outcomes, source identity, ordered raw lines, categorized codes and SQLite v4 recreation/rollback. Readiness/MIL, Mode02/09/06 are **not collected in this report**; standalone readers retain their prior limitations. No diagnostic depth levels or health verdicts were added.
- Dashboard: SOFTWARE_TESTED for capability-gated canonical selection, persisted order/style/size and per-PID bounded graphs. Live candidates are the seven existing PidPollingPlan Mode01 PIDs plus observed ATRV. A catalog definition alone does not enable selection or dispatch; no extra polling loop is created.
- Vehicle setup: unentered metadata stays absent. No sample Peugeot/TU5, displacement, fuel capacity/price or odometer is supplied. Existing persisted owner metadata is retained; setup does not select an ECU/vehicle routing profile.
- Physical adapter and vehicle validation are still required. See [competitor-gap report](docs/COMPETITOR_GAP_DEVELOPMENT.md) for current test totals and remaining scope, rather than interpreting historical rows above as newly executed checks.

Overnight additions remain SOFTWARE/EMULATOR scoped: generation-locked foreground actions; ongoing runtime admission; structured captured-report JSON export via explicit SAF destination; cloud backup exclusion; notification unit/freshness presentation. No physical service or PID expansion accompanies them. See [safety status](docs/CAROBD_SAFETY_STATUS.md), [remaining gaps](docs/CAROBD_REMAINING_GAPS.md) and the canonical [agent handoff](docs/AGENT_CONTINUATION_HANDOFF.md).
