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

## Export checkpoint published

`70704aa05071ee5208c1aac3a0efd4ef21742ccb` is pushed. Report export targeted JVM tests app139/model57/adapter115 and Android9/9 passed; actual SAF cancellation was covered there. The next checkpoint additionally tests successful provider Save/readback and replaces permissive template OS cloud-backup policy. No physical vehicle operations or new dependency coordinates.

## Privacy policy and successful document Save

Automatic Android cloud backup now excludes every app data domain; modern device transfer keeps owner data but excludes runtime permission-request history. Legacy devices exclude both backup modes. Local stored evidence is unchanged. See [privacy policy](DATA_PRIVACY_AND_EXPORT.md) and official [Android backup rules](https://developer.android.com/identity/data/autobackup). Actual cloud/transfer execution is not claimed. Packaged-policy instrumentation and actual SAF Save to Downloads, parsed back with matching report id/scope/source/outcome count, passed in Android10/10, exit0,3m6s. Log `/tmp/carobd-overnight-backup-save-ui-retry.log`; initial test compile failure from hidden ApplicationInfo fields was corrected without weakening the shipped policy checks.

## Foreground notification respects settings

NotificationMeasurements formats fresh shared telemetry with saved units, retaining unknowns, measured zero, negative temperatures and independent PID timestamps. Service observes units/language in its existing notification collector; no additional producer or commands. App143 tests passed, exit0,3s. Complete Android10/10 passed, exit0,3m11s, including real Settings imperial→metric change reflected in NotificationManager with the same diagnostic generation. EN/FA resources447 matched. Logs `/tmp/carobd-overnight-notification-unit.log`, `/tmp/carobd-overnight-notification-ui.log`. Physical notification/lifecycle bench remains required.

## Localized metadata numbers

VehicleSetupNumbers accepts English/Persian/Arabic digits and decimal separator٫ for positive finite tank litres, and localized nonnegative integer odometers. Ambiguous grouping, exponent/nonfinite/overflow and fractional odometer values are rejected, not guessed or rounded. Blank input remains null. Onboarding shares parsed values between validation/save, uses decimal keyboard for capacity and localized error support. Four new JVM regressions passed in app147, exit0,3s. Actual form accepts۵۰٫۵, rejects۵۰٬۵ and returns to valid unknown after clearing. Complete FA/RTL900×2000/font130 Android10/10 passed, exit0,3m22s (`/tmp/carobd-overnight-numeric-fa-ui.log`). Resource validation448 EN/FA keys passes. No vehicle data defaults or commands added.

## Final execution-budget checkpoint

Tested source1f327633e3375e1ea20d7d324c0f93f772694703 pushed; final documentation follows. Fresh offline gate exit0,1m28s,107 executed tasks: JVM464/0fail/error/skip; lint84warnings/0errors; duplicate classes and app/test APK assemblies pass. EN10/10 exit0,3m12s; FA/RTL900×2000/font13010/10 exit0,3m22s. Localization448 keys and host production SQL checks pass. Offline all-configuration resolution133/2331/56/0failures exit0,3s; offline help exit0,921ms,normal ~/.gradle,JDK24.0.2+12. Earlier actual Studio online/offline GUI proof retained, no new GUI run claimed.

Debug APK22,114,928 bytes,SHA256 f4f7a8a90c0eacfe6ac4155fb3a509902cf0284a6d3834ad92058e2dcfbcb65b,v2 signature verifies. Final app reinstalled/launched on emulator only; no physical operation. Full portable report [COMPETITOR_GAP_DEVELOPMENT.md](COMPETITOR_GAP_DEVELOPMENT.md); canonical [agent handoff](AGENT_CONTINUATION_HANDOFF.md); exact environment/routing recipe [ANDROID_STUDIO_OFFLINE_SETUP.md](ANDROID_STUDIO_OFFLINE_SETUP.md). Report.md remains unchanged/untracked. Ending at a safe execution-budget checkpoint before07:00; broader scope/physical validation remains incomplete. Final sleep is conditional on successful documentation/source push, head equality, no active build/test and physical-local-host verification. Actual invocation result belongs to final chat; no later development may follow sleep.
