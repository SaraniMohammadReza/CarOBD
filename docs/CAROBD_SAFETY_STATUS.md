# CarOBD safety status — software scope

2026-10-30. Authoritative continuation: [AGENT_CONTINUATION_HANDOFF.md](AGENT_CONTINUATION_HANDOFF.md). These are implemented/tested software constraints, not physical adapter or vehicle certification.

| Required status | Current behavior |
|---|---|
| PHYSICAL_MODE04_STATUS | DENIED; simulator-only clearing has no physical delegate |
| RAW_TERMINAL_STATUS | Narrow read-only typed authorization; unknown/arbitrary input denied |
| COMMAND_AUTHORIZATION_STATUS | Mandatory adapter dispatch; Mode08, security, programming, writes and speculative manufacturer commands blocked |
| CANCELLATION_ISOLATION_STATUS | Generation ownership retained; cancelled in-flight owners retire before reuse |
| TIMEOUT_QUARANTINE_STATUS | Serialized wire ownership, timeout resynchronization/quarantine and late-response isolation retained |
| TRANSPORT_PERMISSION_STATUS | Classic/BLE/Wi-Fi admission rechecked on connect/write/passive link checks; revoked generation cannot revive. USB retains exact-target permission/presence checks. Physical OS revocation requires bench testing |
| REAL_VEHICLE_READ_ONLY_STATUS | Software restricted; no autonomous vehicle operations performed or vehicle-safety PASS claimed |

`core/adapter/.../safety/AuthorizedDiagnosticCommand.kt` and CommandSafetyClassifier gate the existing serialized adapter. DiagnosticSession admits at most eight pending requests and owns one telemetry producer. BLE requires acknowledged CCCD subscription; USB does not fall back to another device/interface. Source/protocol mismatch rejection, bounded raw evidence, nullable unknown responders and separate03/07/0A outcomes remain intact.

Service teardown checks its session ID inside the repository session lock. Delayed notification/service intents cannot disconnect a replacement generation. Notification unit conversion and report export use existing snapshots only. Report persistence, JSON export and backup policy do not issue diagnostic commands. Missing measurements remain unknown; no DTCs does not establish vehicle health. Simulator and transcript replay remain isolated from physical transports.

Before physical release, execute the [bench checklist](SPRINT5_BENCH_CHECKLIST.md) on authorized devices/adapters. Actual phone revocation/regrant, USB detach, BLE callback timing, TCP loss, protocol behavior and sustained stationary-vehicle measurements remain unverified by emulator/JVM results. Keep physical Mode04 and all writes disabled during that validation.
