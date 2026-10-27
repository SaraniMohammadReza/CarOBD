# OBD-II Support Matrix (Phase F audit)

- `core/protocol` (Obd2Command / PidCodec / DtcParser) + `core/adapter` (DtcScanValidator) +
  `:core:protocol:test` / `:core:adapter:test` (98 unit tests green at Phase F+ gate; 89 Phase F + 9 §9/#10).

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
| 09 | Vehicle info (VIN/CALID/CVN/ECU name) | ✅ (mode09 + 0900 discovery + decodeVin/decodeAsciiString) | ✅ | ✅ | ⛔ | DEVICE TEST REQUIRED |
| 0A | Permanent DTC | ✅ (requestPermanentDtcs) | ✅ (DtcCategory.PERMANENT, same parse path) | ✅ | ⛔ | DEVICE TEST REQUIRED |

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

## §9 negative-response model (7F <service> <NRC>)
- `NegativeResponse(requestedService, nrc, nrcCode, rawLine, responder)` in core/model.
- `ObdNrc` enum (J1979/ISO 14229 common NRCs); unknown NRCs retained numerically.
- `PidCodec.isNegativeResponse` / `parseNegativeResponse`.
- `EcuRejectionException` (core/model) models "ECU reachable but rejected (7F <svc> <NRC>)" — distinct from `NotConnectedException`.
- `DtcScanValidator.scanFailure`: returns null = clean positive (scan proceeds); `NotConnectedException` = ECU silent / transport error; `EcuRejectionException` = 7F rejection. 7F NEVER collapses to "not connected" or "no codes found."
- `DtcParser.parseMode03Payload` now validates the positive service echo explicitly (43/47/4A per category) + J1979 count byte; returns empty on 7F or mismatched echo (no parity heuristic, no echo-byte fabrication).
- `scanDiagnosticTroubleCodes` (Mode 03/07/0A): 7F surfaces as `EcuRejectionException` (abort); Mode 03 is the truth gate.
- `clearDiagnosticTroubleCodes` (Mode 04): requires a real 0x44 echo; 7F → `EcuRejectionException`; failure → truthful "ECU did not acknowledge."
- Per-protocol 7F models already present: UDS (`UdsNrc`/`UdsResponse.Negative`) and KWP (`KwpNrc`/`KwpResponse.Negative`).
- Status: IMPLEMENTED / PARSED / UNIT TESTED (DtcScanValidatorTest: 6 tests incl. 7F-across-services NRC preservation; ProtocolEnginesUnitTest: 4 framing-regression tests + echo-category fixes). UI truthful (clear-ack message + source/category subtitle). DEVICE TEST REQUIRED for physical 7F behavior.

## §13 multi-ECU / responder identity
- `DiagnosticCode` carries `responderAddressHex`+`category` threaded through `parseMode03Payload`
  / `parseStandardObd2Dtc` / `parseUdsDtc` (no longer hardcoded `7E8`); identity preserved end-to-end
  via `dtcList.addAll(...)` in ObdRepositoryImpl (Mode 03/07/0A) — not dropped on the model→domain mapping.
- `J1939CanId` parses 29-bit CAN IDs so multi-ECU frames are addressable.
- Per §13: standard OBD engine addresses (7E8/7E9) map to "Engine ECU"; all other responder
  IDs are preserved as `ECU <id>`. No ABS/SRS/BCM manufacturer diagnostics are claimed from a
  generic OBD responder.
- Adapter-level 29-bit CAN arbitration-ID capture per responder is a DEVICE-verified follow-up.

## Known gaps (not fabricated as supported)
- Mode 02 freeze-frame decoder.
- Mode 05/06/08 response decoders.
- Mode 0A permanent-DTC `DtcCategory` bucket — CLOSED (4A echo + PERMANENT category, tested).
- ISO-TP receive timeout (no timer in reassembler).
