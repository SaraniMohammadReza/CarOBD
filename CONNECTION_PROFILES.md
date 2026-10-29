# CarOBD Connection Profiles

Status: SOFTWARE VERIFIED by supplemental tests/source compilation; Android UI rendering, permissions and all physical drivers are DEVICE TEST REQUIRED. Exact Android BUILD VERIFIED is pending.

Connection Center creates, edits, selects and deletes named profiles using ConnectionProfileRepository backed by a Preferences DataStore. Connect and Test use the saved transport type; there is no BLE-to-Classic fallback. New physical profiles require explicit targets. Saving edits does not reroute an existing live session.

| Transport | Required runtime fields | Fail-closed behavior |
|---|---|---|
| Classic | Exact MAC address | Missing/invalid address, permission, adapter or link fails; no other device selected |
| BLE | Exact MAC address | NUS/FFF0 services only; ready after successful CCCD acknowledgement; closed generation cannot revive from a late callback |
| Wi-Fi | Host and TCP port | Blank/malformed endpoints fail; legacy host:port path never substitutes a port |
| USB | Device ID or VID+PID, explicit interface; optional CDC baud | Every supplied identity field must match exactly one device; ambiguous/missing target/interface fails; selected interface must acknowledge CDC line coding |
| Simulator | Explicit scenario in editor | Separate synthetic transport; unknown scenario IDs fail; legacy absent scenario means the simulator default |

USB is a generic CDC-style implementation requiring DEVICE TEST REQUIRED validation. Specialized FTDI/CH34x driver support is NOT IMPLEMENTED; no success is claimed from ignored serial configuration errors. Device ID may change after reattachment; reconnect then requires selecting a valid current target. Interface index is not claimed to be a usb-serial library port number.

AUTO and manual codes 0–9/A are supported. A is J1939 and sends ATSPA. B/C/unknown codes fail; rejected selection or unknown/mismatched readback fails. VEHICLE_PROFILE protocol mode is DISABLED FOR SAFETY until validated. Selecting vehicle metadata never applies headers, filters or manufacturer command packs.

Connect timeout bounds initial link acquisition through session ownership; command timeout caps adapter write/receive deadlines. Existing profile format retains some legacy fields for compatibility: autoReconnect and vehicle recommendations do not implement runtime reconnect or forced profile initialization. The editor does not offer these features and saves autoReconnect=false. Custom BLE UUID/MTU overrides and transcript profiles are rejected as NOT IMPLEMENTED.

The visible Disconnect/cancel action invalidates generation, cancels connection/request jobs, stops polling, closes hardware, and clears telemetry. Explicit producer stop is terminal for a session; reconnect creates its replacement.

Test Connection takes ownership of the current session, executes controlled read-only initialization and retains all six stage outcomes/evidence. Physical Pass requires positive 41 00 and valid observed protocol. The initial PID 00 bitmap supplies capability evidence; later capability pages are discovered conservatively by normal telemetry. The test session closes before returning. Simulator reports are labeled SIMULATOR. A passed software test is not device or vehicle certification.
