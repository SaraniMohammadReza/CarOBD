# CarOBD Autonomous Sprint 5 — evidence quality

Date:2026-10-30. Start HEAD:`4145491c106ff0afe100909b3eb0b35d3c13ccad`. Branch:`feat/car-obd-development`. No master modification, reset, stash or force push. Untracked user `report.md` is preserved; its SHA256 remains `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325`.

## Trip provenance contract

Canonical core/model `TripMetricEvidence` records metric/unit, nullable value, origin, quality, source, first/last observation, sample count, covered milliseconds and derivation method. Manual TripLogger and automatic TripComputer use this same model. Automatic persistence includes duration, distance, maximum/covered-average speed, maximum RPM, MAF-derived fuel, fuel economy, maximum coolant and sample-average engine load. Unavailable values are nullable. No missing fuel is passed as zero to the trip computer.

TripComputer keeps bounded aggregate state. Speed, RPM, fuel, coolant and load availability are independent. Adjacent valid intervals up to10s are integrated; missing inputs, duplicate/backward times and longer gaps break coverage. No imaginary initial zero-speed sample is integrated. One sample can establish a sample maximum but not integrated distance/fuel. Coverage is a sum of accepted adjacent intervals, not a claimed exact whole-drive percentage. Arithmetic load means and observed maxima are identified as such.

All aggregates are DERIVED, not ECU-reported totals. Fuel's source is0110; its method states gasoline AFR14.7/density745g/L assumptions and trapezoidal integration. It is an estimate, not direct ECU fuel-flow measurement. Economy requires matching speed/fuel availability and coverage; unmatched intervals remain unknown. Simulator aggregates carry a SIMULATOR source prefix. Duration is host wall time, not evidence of engine-running time. Clock discontinuities limit evidence; no monotonic whole-drive accuracy claim is made.

Legacy TripRecord numeric values remain available with `legacyUnverified=true` and per-metric LEGACY_UNVERIFIED provenance. Their source, counts and covered time are not invented. Old score columns remain compatibility data and must not be consumed as health evidence. New automatic TripRecord.drivingStyle is null because no such metric is derived; compatibility column zeros are never returned as new scores or event observations. Legacy score objects remain explicitly under the legacy-unverified trip flag. New records have complete metric evidence; raw old NOT NULL numeric columns are compatibility storage only. The v3 evidence table is authoritative for new nullable values. Readers fail on incomplete/invalid new evidence instead of falling back to old zeros. New evidence is restored in canonical metric order, so helper recreation preserves the complete aggregate model.

Manual trip display reads canonical distance/average evidence, shows unknown without sufficient intervals, and presents source/coverage details. Manual capture stops across session-token changes and marks simulator sources. It remains a bounded in-memory capture with CSV preview, not a newly claimed durable manual-trip database. Automatic trip database persistence is durable, bounded to100 records per query; write failures are exposed as a history warning.

## Database version3

Version2 DTC migration is preserved. Version3 adds a LEGACY_UNVERIFIED trip provenance column, a normalized trip_metric_evidence table keyed by trip/metric, and DTC session/trip vehicle-time indices. No historical table is dropped. New trip row and metric evidence are written transactionally; replacement clears old metric rows in that transaction. Unknown fuel is SQL NULL in metric evidence. Legacy MIL/protocol/responder/category ambiguity remains unverified. Invalid observed MIL values return unknown, not OFF.

`onCreate` uses the baseline schema plus both additive migrations. `onUpgrade` checks old and requested new versions, preserving1→2 behavior as well as1→3/2→3. SQLiteOpenHelper supplies the upgrade transaction; exceptions propagate rather than falling back to destructive recreation. Published migration steps must not be edited after release.

Host check `python3 tools/check_dtc_history_migration.py` executes production SQL with representative stored/pending/permanent legacy rows and a legacy trip. It checks unchanged legacy numbers, NULL observed fields, absent invented evidence, file close/reopen and rollback of a deliberately failed upgrade. This is host SQLite evidence, not Android SQLite runtime proof.

Prepared Android instrumentation covers actual SQLiteOpenHelper1→3,2→3, fresh3 creation, domain DTC-history interpretation, retained legacy trip values and exact new-trip evidence after helper close/reopen. These classes compile; they were not executed on a device.

## Bench capture

Recording screen offers explicit “Bench capture on next connection”. It reuses DiagnosticTranscriptBuilder, RecordingTransport, FileTranscriptStore and existing export/import/replay; no second transcript format or diagnostic dispatch path exists. It starts before admission/transport creation and therefore retains setup failure, initialization TX/RX, parser outcomes, adapter/protocol/ECU/capability stages, transport loss and session end. Stages use structured metadata, not a fabricated physical PASS.

Bench phase events retain capture session ID, transport/profile context, wall/monotonic event timing and observed state. Wire TX/RX and parsed responder outcomes retain their existing fields. Admission snapshots distinguish policy acceptance from an actual OS grant, and failed pre-transport admission produces no fake disconnect. A denial regression asserts zero transport opens and zero writes. Permission requests/results can be observed while a capture is active; there is no retroactive pre-capture permission-dialog history. Capture session IDs identify recording generations, not a newly invented ECU identity. It is a local developer tool; exports can contain VIN/adapter addresses and should be shared deliberately. No credentials, stack dumps or automatic uploads were added.

Physical driver/adapter validation remains DEVICE_NOT_TESTED. Emulator permission and foreground-service execution is recorded separately in RUNTIME_QA_CONTINUATION.md. ValidationEvidenceState separates SOFTWARE_TESTED / ANDROID_BUILD_VERIFIED from DEVICE_PASS / VEHICLE_PASS. A unit-test stage PASS never sets physical validation state. The production-path bench test compares exact captured TX with transport writes and verifies that setup capture adds no commands. Observer failures are caught and cannot change transport writes, RX or close behavior. Capture limits stop observation rather than authorize extra traffic.

## Presentation and localization

EN/FA resource coverage, unique keys, plural variants and format-placeholder compatibility are checked by `tools/check_localization.py`. Remaining catalog names/notes and technical evidence are distinguished from ordinary controls. Developer evidence expands explicitly and diagnostic identifiers use LTR presentation without altering their stored contents. No visual RTL or theme contrast validation is claimed.

Trip, diagnostics, connection, recording, inspector and maintenance text was migrated or corrected. No-code/complete-scan wording does not imply vehicle health. DTC categories are localized without changing codes. Adapter troubleshooting no longer guesses a failed fuse, specific header or universal PIN from a generic symptom. ATI is reported identity, not authenticity proof. Catalog selection is reference metadata and does not configure headers. The old “scan all protocols” action now truthfully describes an ATDPN observation and guards replacement-session results.

Four existing theme palettes and persisted language/unit behavior remain intact. Fuel volume and economy add explicit US gallon/mpg conversion. Maintenance distance displays, dashboard range and cost-per-distance use the selected unit system; canonical protocol evidence is untouched. Some catalog reference specifications retain their explicit source units.

The fuel dashboard now requires fresh measured MAF and speed. It no longer uses a fabricated default7.5L/100km average for range/cost. Those need an explicitly supplied average; unknown values remain null. No extreme valid number is silently clipped into a plausible fuel estimate. Freshness is reevaluated by the dashboard clock. Tests distinguish absent/stale MAF, unavailable average, actual zero volume and undefined reciprocal economy.

## Diagnostic review

Mode09: ELM's documented non-CAN VIN sequence byte now controls assembly of unique chunks even when arrival order differs. Original evidence order remains intact. Duplicate/missing chunks, count mismatch, invalid VIN ASCII and bad padding still fail. A wrong PID cannot be relabeled as a partial requested VIN. Added CALID padding and binary CVN/multi-responder regressions. Broader non-CAN CALID/CVN/name formats and raw live FF/CF remain unsupported pending format-specific evidence.

Mode02: explicit selected-frame/base-page scope remains. Empty capability evidence is Malformed and triggers no speculative metric reads. No all-frame enumeration API was invented; reviewed sources did not establish one for this implementation. Existing frame/PID/responder and truncated-value regressions remain.

Mode06: only exact observed ISO15765-4 protocol names admit its CAN layout; arbitrary CAN/UDS names and non-CAN/J1939 do not. Empty capability evidence is Malformed. Raw records remain raw: no new OEM naming/scaling/signedness or pass/fail mapping was inferred.

Mode05: NOT_IMPLEMENTED / SOURCE_EVIDENCE_REQUIRED. Vendor documentation supports its historical non-CAN oxygen-monitor applicability but does not supply enough verified byte-layout/scaling evidence for a complete product decoder here. No generic UI or unsafe dispatch expansion was added.

Health engine/post-drive report/custom PID/auto-reconnect/specialized USB drivers were not implemented. Health/reporting remains deliberately deferred until device/vehicle evidence confirms input quality and explicit source/rule mappings exist; no score or mechanical diagnosis was added.

## Recording, replay and performance

Existing CR/LF/spacing/prompt preservation, atomic snapshots, cancellation, quarantine and responder handling remain. Replay imported with Classic/BLE/Wi-Fi/USB metadata is tested while an active physical-session test double exists: it adds zero physical writes and leaves that session connected. ReplayTransport has no physical delegate or TransportFactory dependency.

Import tests enforce16MiB text,50,000 events,65,536-character raw frames, bounded depth, no duplicate JSON keys, ordered nonnegative bounded elapsed times and safe IDs. SAF input bytes are separately bounded. User-visible errors use localized context with expandable technical evidence. Tests prove recording/provider-error-handler failures cannot alter wire behavior.

Recording list uses50-item pages ordered by file update time and stable ID; only selected-page transcript files are parsed. Original files are retained. Corruption remains an actionable error when its page is selected, not a silently skipped capture. Event/replay UI previews show the first100 entries; frame text is bounded to2048 characters and12 bounded metadata fields. Bench phases/outcomes/responders are visible without altering stored data; exports retain stored events. Database and transcript IO stays off the main thread. Paging can shift if new files arrive during navigation; it is not a database snapshot cursor.

## Sources reviewed on2026-10-30

- [SQLiteOpenHelper](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper): additive upgrade paths, framework transaction rollback and immutable released migration steps informed the migration design. Host transaction tests do not replace Android execution.
- [Android SQLite storage](https://developer.android.com/training/data-storage/sqlite): schema/data-object conversion and background IO require explicit care with this existing helper; no dependency migration was added.
- [ELM327L datasheet, Multiline Responses p43](https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327L_Data_Sheet.pdf): VIN chunks have explicit sequence numbers, filler and count/layout differences from formatted CAN. This supports sequence-based assembly, not arbitrary VIN repair or generalized legacy layouts.
- [Snap-on OBDII Service Modes](https://www.snapon.com/DiagnosticsManuals/Help/OBDII/Content/Common/Help%20Topics/OBD/OBDII%20Service%20Modes.htm): Mode05 applicability, freeze-frame meaning and the need for vehicle/OEM-specific Mode06 interpretation support retaining conservative scope. No manufacturer mapping or universal pass/fail was inferred.

## Build reproduction and status

Versions preserved: Gradle9.8.1, AGP9.3.3, Kotlin/Compose2.2.10, coroutines1.10.1, compile/target37, min24, JVM target17. Known JDK24 launcher and persistent user-level same-coordinate Google mirror routing remain local environment requirements, not dependency substitutions. User root Kotlin JVM plugin change remains intact.

```bash
export JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12
export ANDROID_HOME=/home/mohammad/Android/Sdk
./gradlew --offline clean --console=plain
./gradlew --offline \
  :app:assembleDebug :app:testDebugUnitTest :app:checkDebugDuplicateClasses :app:lintDebug \
  :core:model:test :core:adapter:test :core:protocol:test :core:telemetry:test :core:vehicle:test \
  --console=plain
```

Final fresh results, actual Android emulator execution, runtime repairs and APK evidence are recorded in [RUNTIME_QA_CONTINUATION.md](RUNTIME_QA_CONTINUATION.md). Device checklist: [SPRINT5_BENCH_CHECKLIST.md](SPRINT5_BENCH_CHECKLIST.md). Status remains SOFTWARE_BUILD_GO_READ_ONLY__DEVICE_VALIDATION_REQUIRED unless a failing final gate requires correction. No physical adapter/vehicle PASS has been obtained. The subsequent runtime continuation executes Android emulator tests; these are separate from hardware validation.

## Additional mandatory environment validation

See [Android Studio setup](ANDROID_STUDIO_SETUP.md). Routing is now persistent under the normal user Gradle init.d directory, not dependent on a CLI-only init argument. All artifacts remain Gradle-managed in ~/.gradle. The installed API37 package is platforms;android-37.0, with build tools37.0.0. Offline help and broad external-artifact resolution pass; actual IDE sync succeeded twice. Final offline clean gate results are recorded in the runtime continuation report.
