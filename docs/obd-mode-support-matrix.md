# OBD-II Support Matrix (Phase F audit)

Audited against `core/protocol` (Obd2Command / PidCodec / DtcParser) and
`core/adapter` (Elm327Adapter / Elm327BufferParser) + `:core:protocol:test` /
`:core:adapter:test` (89 unit tests green at Phase F gate; 72 Phase E + 17 Phase F).

Legend: ✅ = Implemented • ✅ parsed • ✅ tested • ✅ UI • ⛔ = not supported • DEVICE = hardware-blocked.

| Mode | Purpose | Implemented | Parsed | Tested | UI | Hardware |
|------|---------|-------------|--------|--------|----|----------|
| 01 | Live data + PID discovery + readiness | ✅ (mode01, decodeMode01Metric, decodeSupportedPids, decodeReadinessStatus) | ✅ | ✅ | ✅ | DEVICE TEST REQUIRED |
| 02 | Freeze frame | ✅ (mode02 command builder) | ⛔ (no decodeMode02) | ⛔ | ⛔ | DEVICE TEST REQUIRED |
| 03 | Stored DTC | ✅ (requestStoredDtcs + DtcParser.parseMode03Payload) | ✅ | ✅ | ✅ | DEVICE TEST REQUIRED |
| 04 | Clear DTC | ✅ (clearDtcs + PidCodec.verifyClearDtcResponse) | ✅ | ✅ | ⛔ | DEVICE TEST REQUIRED |
| 05 | O2 sensor monitor tests | ✅ (mode05 command builder, added) | ⛔ (no decoder) | ⛔ | ⛔ | DEVICE TEST REQUIRED |
| 06 | Onboard monitoring | ✅ (mode06 command builder, added) | ⛔ (no decoder) | ⛔ | ⛔ | DEVICE TEST REQUIRED |
| 07 | Pending DTC | ✅ (requestPendingDtcs + parseMode03Payload(PENDING)) | ✅ | ✅ | ✅ | DEVICE TEST REQUIRED |
| 08 | Control operation | ✅ (mode08 command builder, added) | ⛔ (no decoder) | ⛔ | ⛔ | DEVICE TEST REQUIRED |
| 09 | Vehicle info (VIN/CALID/CVN) | ✅ (mode09 + decodeVin/decodeAsciiString) | ✅ | ✅ | ⛔ | DEVICE TEST REQUIRED |
| 0A | Permanent DTC | ✅ (requestPermanentDtcs command) | ⚠️ (parseMode03Payload works; DtcCategory lacks PERMANENT bucket) | ⛔ | ⛔ | DEVICE TEST REQUIRED |

## ISO-TP (ISO 15765-2) coverage
- Single Frame ✅ (tested)
- First Frame ✅ (tested — starts assembly, returns null)
- Consecutive Frame ✅ (tested — completes reassembly)
- Multi-frame reassembly ✅ (tested)
- Sequence rollover ✅ (tested, >16 CF)
- Wrong sequence ✅ (tested — resets assembly)
- Length mismatch ✅ (tested — stays incomplete)
- Malformed/empty ✅ (tested — ignored)
- Flow Control ✅ (tested — ignored by receiver)
- Timeout ⚠️ N/A (reassembler is synchronous; no receive timer — documented gap)

## ELM327 / STN robustness (Elm327BufferParser)
All 14 directives parsed into `AdapterError` (never empty success):
NO DATA ✅ / STOPPED ✅ / `?` ✅ (UnknownCommand) / SEARCHING... ✅ (stripped) /
UNABLE TO CONNECT ✅ / BUS INIT: ERROR ✅ / CAN ERROR ✅ / BUFFER FULL ✅ /
prompt `>` ✅ (stripped) / command echo ✅ (stripped) / CR/LF ✅ / multi-line ✅.
`Timeout` / `TransportFault` / `Other` error types exist for transport-level failures.

## §13 multi-ECU / responder identity
- `DiagnosticCode` now carries `responderAddressHex` threaded through `parseMode03Payload` /
  `parseStandardObd2Dtc` / `parseUdsDtc` (instead of hardcoded `7E8`).
- `J1939CanId` parses 29-bit CAN IDs so multi-ECU frames are addressable.
- Per §13: standard OBD engine addresses (7E8/7E9) map to "Engine ECU"; all other responder
  IDs are preserved as `ECU <id>`. No ABS/SRS/BCM manufacturer diagnostics are claimed from a
  generic OBD responder.
- Adapter-level 29-bit CAN arbitration-ID capture per responder is a DEVICE-verified follow-up.

## Known gaps (not fabricated as supported)
- Mode 02 freeze-frame decoder.
- Mode 05/06/08 response decoders.
- Mode 0A permanent-DTC `DtcCategory` bucket (command + parser exist; category enum gap).
- ISO-TP receive timeout (no timer in reassembler).
