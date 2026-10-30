# Sprint5 device and vehicle evidence checklist

Physical bench and vehicle items below remain NOT_EXECUTED unless explicitly annotated. Android emulator counterparts are recorded separately in [runtime QA](RUNTIME_QA_CONTINUATION.md); an emulator PASS does not sign off a physical callback/adapter/vehicle scenario. Record actual outcome, Android/API/device/adapter firmware, exact target/profile, APK hash, start/end time and exported transcript ID. SOFTWARE_TESTED and ANDROID_BUILD_VERIFIED do not establish DEVICE_PASS or VEHICLE_PASS. Keep physical04,08, security and programming disabled.

## Capture preparation

- [ ] Install the exact recorded APK on a controlled bench; retain its SHA256 and device/API details.
- [ ] Enable “Bench capture on next connection” in recording screen. It applies to the next attempt without adding diagnostic commands.
- [ ] Save screen observations for permission dialogs before capture starts; they are not retroactively recorded.
- [ ] Use an explicit profile/target. Keep each transport/permission scenario in a separate capture.
- [ ] After stop/disconnect, refresh recordings, inspect raw/parsed/bench events and export JSON plus CSV to a deliberate SAF destination.
- [ ] Retain reference-tool captures separately. Do not infer ECU bus acceptance from a host TX event.

## Bluetooth Classic

- [ ] Fresh install: deny CONNECT/SCAN/location as applicable; verify actionable denial and zero unauthorized connection/diagnostic writes.
- [ ] Grant; verify Bluetooth-off/unavailable and permanent-denial/settings paths.
- [ ] Select exact adapter MAC. Repeat scan, navigate away, rotate/STOP and restart; verify old receiver/discovery cleanup before replacement.
- [ ] Connect RFCOMM; inspect ATI, observed protocol,0100 and capability evidence. Connection alone is not ECU verification.
- [ ] Disconnect explicitly; remove adapter power/trigger EOF; revoke permission while connected. Verify failure reason, no surviving producer and stale callback rejection.

## BLE

- [ ] Denied/granted scan permissions and exact MAC selection; bound scan duration and scan-error state.
- [ ] Verify GATT service/characteristic discovery, notify versus indicate and acknowledged CCCD subscription before readiness.
- [ ] Check API33+ writes/callbacks and supported legacy branch; inspect exact TX/RX.
- [ ] Disconnect/reconnect; deliver delayed old-generation callbacks on the bench. They must not mark the replacement ready or inject old responses.

## Wi-Fi

- [ ] Explicit host/port; unreachable endpoint produces bounded actionable timeout.
- [ ] Successful TCP link, ATI and0100; verify observed protocol and read-only stage evidence.
- [ ] EOF/socket loss invalidates Connected, cancels producer and records closure.

## USB

- [ ] Permission denied then granted; exact device and interface selection.
- [ ] Wrong/absent target fails closed. Reattach with changed identity requires valid selection.
- [ ] Verify CDC line coding acknowledgement, ATI and0100 with a supported generic-CDC device.
- [ ] Detach during read/connect; no fallback to another attached device. Do not claim FTDI/CH34x support from generic CDC success.

## Storage and recording

- [x] EMULATOR_ONLY: executed DtcHistoryMigrationInstrumentedTest and TripEvidenceInstrumentedTest via connectedDebugAndroidTest; retained passing runner XML. Physical-device repetition remains required.
- [x] EMULATOR_ONLY: actual v1→v3 and v2→v3 upgrades, fresh database, helper/file recreation, retained legacy rows and NULL new fuel passed. Host migration rollback also passed. OS process-death capture-tail recovery remains a separate unchecked scenario.
- [ ] Legacy DTC categories/MIL/protocol/responders remain unverified; new scans retain exact observed distinctions.
- [ ] Start/stop capture; verify active indicator, atomic saved session, delete restrictions and paging across more than50 sessions.
- [ ] Export/import JSON with CR/LF/spacing/prompts and CSV quoting; corrupt/oversized/deep input fails gracefully.
- [ ] Replay imported physical transport metadata while hardware is disconnected and while a controlled physical session exists. Confirm no physical writes or session replacement.
- [ ] Exercise timeout/late prompt/disconnect/multi-responder/negative/no-data replay. Cancel long playback.
- [ ] Process kill/recreate during capture: verify restored durable snapshots and report any unsaved tail accurately.

## Presentation

- [ ] EN then FA across connection/profile/wizard/settings/diagnostics/recording/readiness/vehicle info/freeze frame/monitor tests/trip/maintenance/inspector/terminal/onboarding.
- [ ] Offline Activity/process recreation retains language, RTL/LTR, theme and units. Inspect all four palettes and actual text contrast.
- [ ] VIN/CVN/DTC/PID/CAN IDs/hex remain unchanged and readable LTR in Persian surroundings.
- [ ] Fresh/stale/unavailable values differ, real zero remains zero, ATRV is distinct from0142 and actual simulator source is visible.
- [ ] Measured/derived/legacy/unavailable trip evidence and coverage survive automatic-trip storage recreation. Missing fuel/average never becomes zero or a range estimate.

## Vehicle read-only progression

Use a controlled stationary test and a trusted reference tool. For each step retain request/raw response, observed protocol/responder, typed outcome, comparison and limitations. An unsupported field is not a vehicle failure.

- [ ] Adapter identity observation → protocol observation →0100 → advertised capabilities.
- [ ] Sparse advertised Mode01 values; compare fresh values and disconnect/staleness timing.
- [ ] Readiness0101: ignition type/MIL/monitor distinctions; no health claim from completion.
- [ ] DTC03,07,0A: exact stored/pending/permanent, multi-responder identities, nullable unknown MIL and truthful zero-stored-DTC wording.
- [ ] Mode09: advertised fields, CAN numbered VIN and applicable non-CAN sequence/count; CALID/CVN/name padding; preserve unsupported/raw layouts.
- [ ] Selected Mode02 frame/base page; compare frame index, responder, unknown PID and metric bytes. Do not claim all-frame enumeration.
- [ ] Mode06 ISO15765 CAN advertised MID/raw records; compare raw measured/min/max with the reference tool. Assign no OEM interpretation without versioned source evidence.
- [ ] Stop session; inspect timeout quarantine, old callback exclusion and producer shutdown under controlled interruption.

No Mode04, Mode08, arbitrary headers, security/write/programming, proprietary unknown commands or state-changing retry belongs in this checklist.
