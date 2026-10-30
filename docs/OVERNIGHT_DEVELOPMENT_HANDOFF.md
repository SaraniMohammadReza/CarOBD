# Overnight development continuation

Actual overnight starting checkpoint: `7ef5476af15aa1fb9a6fa8e3382af72bed27e3d3`, already equal to origin/feat/car-obd-development. This continues the earlier same-session work; do not reset to the historical f3024d7 baseline. Pre-existing `report.md` remains excluded and unchanged.

## Recovered verification

- Latest pre-overnight exact clean offline gate: exit0, 1m24s, 107/107 tasks executed, 441 JVM tests with zero failures/errors/skips. Lint84 warnings, zero errors. Log: `/tmp/carobd-competitor-final-offline-gate.log`.
- Latest English Android run after the RTL dialog correction: exit0, 3m3s, 8 tests, zero failures/errors/skips. Log: `/tmp/carobd-competitor-final-ui-en.log`.
- Persian/RTL at 900×2000 and font130: exit0, 3m7s, 8 tests, zero failures/errors/skips. The initial failure and corrected rerun remain documented in [competitor-gap report](COMPETITOR_GAP_DEVELOPMENT.md).
- The emulator subsequently exited. The first service-fix instrumentation attempt failed with **No connected devices**, not an application test failure. Its JVM task passed. Relaunched the existing Pixel_10 AVD without wiping user data; ro.kernel.qemu=1. Retried actual Android execution successfully.

## P0: generation-bound foreground-service teardown

A notification disconnect previously checked a session token before launching a coroutine and later called unconditional disconnect. A replacement connection could win in between. Stale start/stop intents could also stop an already-bound replacement service.

`ObdRepository.disconnectIfCurrent` now compares the token while holding the existing session mutex and tears down only that generation. It does not cancel a pending replacement connection. Service disconnect/stop actions use this boundary, and repository owns service shutdown. A stale start does not stop an already-bound service.

Production files: `ObdRepository.kt`, `ObdRepositoryImpl.kt`, `ConnectionForegroundService.kt`. Regression tests: three repository tests (replacement token, duplicate/unknown teardown, and queued teardown behind an in-progress replacement). RuntimeNavigationInstrumentedTest delivers stale start/stop/disconnect intents to the real Android service after simulator reconnect, verifies the new token and foreground notification remain alive, then verifies the current-token action disconnects.

Verification: `./gradlew --offline :app:testDebugUnitTest` exit0, 3s, app135 tests; `./gradlew --offline :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.qaLocale=en --console=plain` with isolated adb5038 and the established JDK/SDK, exit0, 2m54s, 8/8 Android tests passed. Local logs `/tmp/carobd-overnight-service-unit.log`, `/tmp/carobd-overnight-service-ui.log`. Full integration gate will be refreshed after subsequent changes.

## Platform research

- [Runtime permission workflow](https://developer.android.com/training/permissions/requesting): check permission on each protected operation, and degrade gracefully on denial/revocation. Existing admission/UI is implemented; continued-operation revocation is the next review point.
- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions): Android12+ scan/connect runtime permissions are separate; legacy scan location requirements remain version-scoped. No additional permission was added.
- [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission): target37 local TCP access requires runtime local-network admission. Existing manifest and AndroidAccess already gate it.
- [Connected-device foreground-service type](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device) and [Service lifecycle API](https://developer.android.com/reference/android/app/Service): service start/lifecycle identifiers do not replace the application's diagnostic-generation lock. The atomic teardown decision is CarOBD's inference/application of those lifecycle constraints, covered by the actual service regression above.

No physical Android transport, adapter or vehicle validation was performed. Physical04 stays denied,08 stays blocked, terminal commands stay narrowly authorized, and cancellation/quarantine/one-producer rules remain in force. No dependency versions or Gradle configuration changed.

## P0: ongoing runtime admission

AdmissionGuardedTransport in core:adapter reuses the same injected platform policy before connect/write and during passive link checks. The canonical TransportFactory wraps Classic/BLE/Wi-Fi only; simulator and USB ownership remain unchanged. Denial retires a generation, closes on owner teardown/failed operation, sends no rejected bytes, and cannot be revived by permission regrant. No extra command/reader/producer or permission was introduced.

Tests: five guard regressions (initial denial, denied write/regrant, passive revocation/cleanup, revocation during connect, cancellation) and one actual repository-session revocation/reconnect test. Targeted app136/adapter115 passed, exit0,11s. Android8/8 passed, exit0,2m58s (`/tmp/carobd-overnight-revocation-ui-final.log`). The new LAN test observed PREGRANTED_ON_REQUEST_NO_DIALOG, consistent with shared Nearby Devices permission behavior. An earlier new-test failure expected a separate deny button; this was a test assumption, not proof of a product/network failure. It now handles actual pregrant or blocked results explicitly. Actual OS revocation on physical transports is still unverified. Canonical live continuation is now [AGENT_CONTINUATION_HANDOFF.md](AGENT_CONTINUATION_HANDOFF.md).

## P1: structured report export

DiagnosticReportExport emits schema1 JSON from an already captured DTC-scope report: identities (nullable), source, times, categories/responders, raw lines, request outcomes, computed completeness and explicit unread services/no health assertion. Shared JSON escaping is reused by transcript export; existing serialization/replay tests pass. DtcHistoryViewModel writes through the selected SAF document on IO, guards duplicate export actions, and reports success only after the writer closes; missing selection and write failure remain failures without any diagnostic command.

Targeted app139/model57/adapter115 passed, exit0,9s (`/tmp/carobd-overnight-export-tests.log`). Six added JVM tests cover escaping/partial evidence/nulls and ViewModel selection/write failure/duplicate admission. Independent actual Android JSONObject parses persisted UTF8 output exactly, including CR/LF, quotes and Persian text. Android9/9 passed, exit0,2m59s (`/tmp/carobd-overnight-export-ui.log`), including actual SAF picker launch/cancel. Successful SAF provider Save is not yet claimed by that cancellation test; it is the next verification. No automatic cloud upload or application-backup importer was added.
