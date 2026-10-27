# CarOBD Connection Profiles

## Conceptual model (§6)

`ConnectionProfile` bundles everything a connection needs: persistent, named,
user-selectable. At minimum one selected profile is persisted (multiple
profiles supported where the architecture allows).

```
ConnectionProfile(
    id, name,
    transportType: TransportType,        // BLUETOOTH_CLASSIC | BLE | WIFI_TCP | USB_SERIAL | SIMULATOR
    transportConfig,                     // BT address / BLE dev / host+port / USB dev / scenario
    adapterConfig,                       // connectTimeout, commandTimeout, autoReconnect, ecoReset, ...
    protocolMode: Auto|Profile|Manual,   // 0..C manual choices; Profile uses vehicle hint
    vehicleProfileId?,                   // drives recommended protocol + ordered init
    adapterIdentity?,                    // cached last ATI / reported identity
    vehicleProtocol?,                    // last detected protocol number
    lastSuccessfulAt?,                   // epoch ms of last full physical handshake
    connectionState                      // truthful levels (§15)
)
```

## Truthful states (§7–10)

Each transport UI shows explicit states — never a single "Connected" boolean:

- **Bluetooth**: `ON / OFF / PERMISSION MISSING`; paired+available compatible devices;
  selected device name + address; paired/bonded state; last connection result. OFF does
  **not** fall through to a fake connection.
- **BLE**: `scan → select → connect → service discovery → RX/TX char status →
  notification/indication → MTU`. AUTO first; advanced characteristic override only for
  clones, isolated behind an advanced toggle. Never "connected" before notifications are
  armed.
- **Wi-Fi**: layered — `Wi-Fi/TCP connected → adapter responded → ELM/STN identified →
  vehicle protocol detected → ECU responded`. DNS/IP failure / connection refused /
  socket timeout / adapter silent / ECU unavailable are each reported where they occur.
- **USB**: device + permission state (explicit on denial); VID/PID; selected port;
  baud only where the backend requires it; advanced serial framing gated.
- **Simulator**: always visibly `SIMULATOR`; never presented as physical ECU data.

## Connection test wizard (§14)

`Test Connection` stages, each PASS/FAIL with a distinct reason:

1. Transport (e.g. Bluetooth transport — PASS / Bluetooth OFF / Permission denied)
2. Adapter (adapter responded / Adapter not responding / timeout)
3. Adapter identity (ATI → reported firmware; clones not labelled genuine)
4. Vehicle protocol (Protocol — ISO 15765-4 CAN 11/500 / BUS INIT ERROR / UNABLE TO CONNECT)
5. ECU handshake (Mode 01 PID 00 → positive `41 00` required before `ConnectedPhysical`)
6. Optional supported-PID discovery (`0100/0120/...`)

A generic "Connection failed" is never the only message.

## Persistence

- Default/selected profile id persisted in `AppPreferencesManager` (proto DataStore).
- Transport config, timeouts, auto-reconnect, protocol mode survive process-death and
  cold restart.
- Language, units, theme, simulator scenario, and selected vehicle profile persist and
  apply before Compose resource resolution (§17).

## Profile templates (suggestions only — never assumed universal)

- "My Bluetooth ELM" (Classic, last selected BT device)
- "Garage Wi-Fi Adapter" (TCP, custom host/port)
- "USB Bench Adapter" (USB serial, VID/PID)
- "Simulator" (synthetic / deterministic scenario / recorded-session replay)

## Connection-profile initialization (§16)

On connect, a verified profile drives the **complete ordered** init sequence:

recommended protocol → ELM/eco reset → echo off → (`ATSH`/`ATCRA` header if required) →
(receive filter if required) → safe custom PID pack → vehicle protocol selection →
ECU handshake (`41 00`). If any init command fails, the adapter surfaces a
**profile initialization failure** and the profile is not reported active.
