# CarOBD Hardware Validation Checklist

Everything below is **SOFTWARE VERIFIED** (builds, unit-tested, lint-clean) unless it
reads **DEVICE / VEHICLE TEST REQUIRED**. Physical hardware is absent from this
environment, so only software architecture, parsers, UI, tests, fake transports,
replay, simulator, settings and configuration logic are marked verified.

## Physical adapters — DEVICE TEST REQUIRED
- [ ] Real Bluetooth Classic ELM (e.g. OBDLink LX/MX+) — `BluetoothClassicTransport`
- [ ] Real BLE clone adapter — `BleTransport` (AUTO + characteristic override path)
- [ ] Real Wi-Fi ELM (TCP) — `WifiTcpTransport` host/port/MTU
- [ ] Real USB-OTG serial adapter — `UsbSerialTransport` permission + VID/PID + baud
- [ ] ELM327 vs STN clone identity reporting (ATI/ATRV/ATDP) — positive detection
  must not label clones as genuine STN/OBDLink unless reported.

## Vehicle verification — VEHICLE TEST REQUIRED
- [ ] Real CAN vehicle (ISO 15765-4) — Modes 01/03/07/09/0A, readiness monitors
- [ ] Pre-CAN K-Line vehicle (ISO 9141-2 / KWP slow / fast) — profile protocol hint
  actually affects init; echo + BUS INIT handling
- [ ] Multi-ECU CAN vehicle — responder identity `7E8`/`7E9`/`7EA` preserved end-to-end
  (transport → ISO-TP → domain → UI → export), no cross-contamination
- [ ] Mode 04 clear on real ECU — positive `0x44` confirmed, `7F`/`NO DATA`/timeout
  treated as failure, readiness reset observed in follow-up read
- [ ] Mode 02 freeze-frame — full end-to-end fetch (frame index preserved)
- [ ] Mode 05 / Mode 06 — O2 monitor + monitor results on a supported vehicle
- [ ] Custom PID pack — profile-driven ATSH/ATCRA against a real ECU, no guessed
  Iranian proprietary PIDs

## Never fabricated (§61)
The following are left **DEVICE / VEHICLE TEST REQUIRED** and are **not** claimed
software-verified:

- DEVICE VERIFIED — NO (no physical adapter tested)
- VEHICLE VERIFIED — NO (no real ECU tested)
- CAN VERIFIED ON REAL VEHICLE — NO
- K-LINE VERIFIED — NO
- MULTI-ECU VERIFIED ON REAL VEHICLE — NO
- REAL MODE 04 CLEAR — NO
