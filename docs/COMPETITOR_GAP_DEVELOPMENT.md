# CarOBD Competitor-Gap Development Report

2026-10-30. This is a focused continuation, not completion of the entire competitor roadmap. Comparator concepts come from the owner's brief; current competitor editions, screenshots and proprietary assets were not independently inspected or reproduced. No new wire commands or dependency versions were introduced.

## Current Git State

Branch: `feat/car-obd-development`. Starting local/remote checkpoint: `3da28167af627faa49df8113c301a72595071860`. Every tested source checkpoint was committed and immediately pushed without force. Delivery-only documentation follows the tested source commit; the final chat response records the exact delivery HEAD after its push.

- `f657191`: evidence-qualified competitor matrix, before implementation.
- `91795ef`: absent vehicle/fuel/odometer metadata stays absent.
- `a5dc754`: persistent partial diagnostic reports, SQLite v4 and category grouping.
- `dabdc81`: capability-gated widgets, persisted layout and independent bounded graphs.
- `7ef5476`: accessible sensor configuration at large RTL font sizes and plural diagnostic counts.
- `d4c41d5`: generation-locked delayed foreground-service teardown.
- `dd35fc7`: early agent-independent live handoff publication.
- `9ed09ea`: ongoing runtime admission retires denied physical transport generations.
- `70704aa`: captured-report structured JSON export and shared escaping.
- `98953bb`: automatic OS cloud-backup exclusions and real document-provider Save/readback.
- `08ff3e8`: foreground notification follows saved units/language and freshness.
- `1f32763`: English/Persian/Arabic numeric metadata input without guessed grouping/defaults.

Only the pre-existing, unrelated `report.md` is excluded from Git. Its SHA256 remains `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325`. No cache, SDK, JDK, APK or distribution is committed. The source checkout has no dependency on that untracked report.

## InfoCar Feature Comparison

[Feature matrix](COMPETITOR_FEATURE_MATRIX.md) covers all requested concepts with scoped statuses. This continuation closes part of the recent-diagnosis workflow and makes stored/pending/permanent results clearer. A CarOBD report is explicitly a DTC-scope report, not InfoCar's diagnosis-depth selector or a vehicle-health verdict. Manufacturer data, maintenance provenance, manual fuel logs and broader combined reports remain gaps.

## Torque Feature Comparison

Live dashboard configuration now uses the existing producer and capability discovery rather than assuming a catalog entry is supported. Add/remove, style at creation, compact/full width and earlier/later ordering persist globally across process recreation. Per-PID graphs replace the incorrect fallback to oxygen history. Existing-widget style editing, broader proven decoder coverage and more dashboard layout options remain partial.

## Features Implemented

- `DiagnosticSessionReport`: scan start/end, connection generation, selected vehicle/profile identity, simulation/physical origin, observed protocol, adapter-reported identity, individual responder outcomes, distinct categorized codes, raw lines and failure detail.
- SQLite v4 stores report header/outcomes/codes/raw lines in one transaction, separately from the older success-only code history. Recent reports show the latest 100 and expand to details; older records are not silently deleted.
- Partial, malformed and no-data scan reports are persisted before returning the existing scan failure. A storage failure surfaces a warning and does not resend commands or conceal the live result.
- Dashboard selection checks the actual producer capability map and canonical polling plan. The supported candidates are the existing seven Mode01 live PIDs and ATRV after an actual adapter measurement. No polling set was expanded.
- Layout preferences preserve order, style, size and an intentionally empty layout. The schema rejects malformed/oversized/duplicate entries and limits layouts to 24 widgets.
- Graph histories have at most 120 observations per canonical PID. Missing/non-finite values, stale periods, clock rollback and gaps over 10 seconds cannot become continuous graph lines or fabricated zero measurements.
- Explicit SAF export of an existing report emits schema1 JSON with null identities, source/scope/raw outcomes and unread services. No scan is triggered by export. Actual Save to Downloads was parsed back independently.
- Automatic Android cloud-backup rules now exclude every app data domain. Modern owner-selected device transfer retains data except device-local permission-request history; legacy rules exclude both modes. Actual cloud/transfer execution is not claimed. See [privacy policy](DATA_PRIVACY_AND_EXPORT.md).

## Features Improved

- Vehicle onboarding begins without a selected make/model/fuel or sample tank/odometer. Blank numeric inputs remain null; invalid supplied numbers cannot be saved through the form. No hidden 1.6L assumption is written. Existing persisted owner metadata is retained because its historical origin cannot be reliably inferred.
- Missing tank/price do not supply range/cost assumptions; unknown odometer produces no due-service reminders or service-reset value. Onboarding metadata never selects a diagnostic routing profile.
- Unknown sensor IDs no longer fall back to RPM. Catalog entries outside the canonical live decoder/poll path are disabled in the picker. Search now actually matches the displayed PID as well as names.
- Compact numeric cards format stale values, put the unit and stale state on separate lines, and retain accessible widget controls. Graphs explicitly show current/stale/unavailable state.
- Sensor picker body scrolls at constrained height; size choices use separate rows. This fixes a real Persian 130% font failure, not a skipped assertion. Diagnostic counts use plural resources.

## Features Intentionally Not Copied

No logos, graphics, icons, fonts, proprietary wording, premium markers, paywalls or exact competitor screen layout. No giant new tile menu, duplicate architecture, fake diagnostic levels, fabricated history, invented driving scores, GPS requirement or automatic cloud upload.

## Safety-Rejected Competitor Features

Physical Mode04 remains disabled, Mode08 remains blocked, and unknown/raw manufacturer commands, writes, security/programming, physical scripts and unrestricted plugins remain unavailable. Isolated simulator clearing remains local. No authorization, response ownership, timeout quarantine or session generation rule was relaxed.

## Diagnostic UX

Trace: Diagnostics UI → existing MVI → `ScanDtcUseCase` → `ObdRepositoryImpl` → `DiagnosticSession` → `DiagnosticScanRunner` → serialized adapter/transport → envelope/DTC parsers → transactional report store → repository history reader → history ViewModel → expandable report UI.

The source/profile metadata is captured before scanning. Identified responders are counted without assigning an invented ID to headerless results. Stored/pending/permanent codes remain distinct even when their textual code is identical. Each request/responder keeps its outcome. Zero codes with malformed/timeout/no-data evidence is not presented as a successful no-codes scan. Legacy empty history no longer implies a completed scan.

**Report limitation:** readiness/MIL, selected freeze frame, Mode09 and Mode06 are explicitly **not collected in this DTC report**. Their existing standalone screens remain available. Canceled work or a failure before a scan report is returned does not manufacture a persistent completed report. No fake depth selector was added. A future broader scope must use existing typed readers, expected session identity, real support and per-read outcomes.

## Dashboard / Sensors / Graphing

Trace: existing producer/discovery → `LiveTelemetry.pidSupport` and measurement timestamps → existing observer use case → Dashboard MVI → capability-gated picker/admission → layout DataStore → rendering from the same StateFlow. Graphs use the selected sensor PID as their map key; unit conversion remains a display boundary.

The seven Mode01 candidates are RPM `010C`, speed `010D`, throttle `0111`, coolant `0105`, load `0104`, MAP `010B` and MAF `0110`. ATRV is adapter-reported supply voltage, not ECU PID0142. Its existing cadence is every 15 producer cycles. A producer cycle is not an exact guaranteed frequency; serial command duration affects timing. Unknown capability disables new selection. Fresh values, stale values and missing values remain distinct.

## Adapter Inspector

Existing inspector preserved. Reports add only the already captured adapter-reported identity and observed protocol. ATI does not establish authenticity. No new synthetic counters, latency claims, initialization-feature claims or physical compatibility certification.

## Recording / Replay

Existing bounded passive capture, session metadata, SAF JSON/CSV export/import and isolated local replay preserved. No upload, cloud or replay-to-physical path added. Existing isolation/admission tests remain in the full gate. No new recording/replay product capability is claimed this continuation.

## Trips / Fuel / Maintenance

Existing nullable per-metric evidence, coverage and MAF-estimation provenance preserved. Removing sample vehicle metadata prevents initial fuel range/cost and maintenance calculations from inheriting fabricated inputs. Manual tank-to-tank fuel ledger, expanded service-source provenance and additional driving metrics remain partial/unimplemented. Existing owner-entered historical values are not silently erased.

## Settings / Alerts / Help

Existing language/theme/units/poll mode retained. Foreground notifications now observe units/language and format only fresh shared measurements. Actual Settings imperial→metric transitions update NotificationManager without replacing the diagnostic generation. Localized capacity input accepts۵۰٫۵; ambiguous grouping, overflow/nonfinite values and fractional odometers are rejected; blank remains unknown. Widget layout now persists without new commands or producers. Threshold alerts, full application backup/restore, unrestricted plugins and new contextual-help coverage were not implemented. Transcript export remains distinct from application backup.

## Tests

Final fresh offline host results: **464 tests**, zero failures/errors/skips: app147, model57, adapter115, protocol125, telemetry19, vehicle1. The earlier competitor checkpoint had441;23 further regressions cover generation-bound teardown(3), runtime admission(6), report JSON/export(6), notification presentation(4) and localized input(4). The earlier fourteen competitor tests cover absent/explicit vehicle metadata and recreation; partial/failed report evidence; report storage failure without command retry; graph gaps/bounds/rollback/non-finite handling; capability admission; malformed layout rejection and persistent ordering/style/size/empty layout. Existing two-observer producer test additionally verifies exposed support evidence without changing its eight-command assertion.

Three added Android SQLite tests cover report recreation with exact raw strings and nullable identity, v3→v4 upgrade without fabricated reports, and transaction rollback after an injected child-row failure. Existing v1 upgrade test now executes through v4. The runtime workflow now adds a canonical MAF graph, checks its persisted configuration, opens recent diagnosis and expands captured report details.

- Final English API37 emulator run on1f32763: **10/10 passed**, exit0,3m12s (`/tmp/carobd-overnight-final-en-ui.log`), zero failures/errors/skips. Final debug APK reinstalled/launched on emulator after tests; fresh Persian onboarding, no active diagnostic session.
- Final Persian/RTL API37,900×2000,font130 on1f32763: **10/10 passed**, exit0,3m22s, including localized numeric validation, successful SAF Save/readback, notification units, stale service intents and unchanged full-width/persistence assertions. Earlier dashboard-only8/8 also passed.
- Initial Persian run failed because the size choice was outside the unscrollable dialog body. The production dialog was corrected; the test retains its full-width and persisted-graph assertions and now requests accessibility show-on-screen before physical fallback scrolling.
- Localization resource validation: 448 EN/FA entries, matching arguments and unique keys.
- Existing production-SQL host migration/recreation/rollback checks pass. Actual Android SQLite evidence comes from the emulator tests, not that host script.

Local evidence directory: `/home/mohammad/.local/share/carobd-build/competitor-gap-qa/`. Logs/screenshots are not required source inputs or Git dependencies. Commands and portable result summaries are recorded here so another checkout can reproduce the checks.

## Full Gradle Gate

The final fresh offline gate on `1f327633e3375e1ea20d7d324c0f93f772694703` passed: **exit0,1m28s,107/107 tasks executed** (`/tmp/carobd-overnight-final-offline-gate.log`). Clean, application/all-core unit tests, debug app/test assembly, duplicate classes and lint all succeeded.

```bash
JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12 \
ANDROID_HOME=/home/mohammad/Android/Sdk \
./gradlew --offline clean :app:assembleDebug :app:testDebugUnitTest \
  :app:checkDebugDuplicateClasses :app:lintDebug :core:model:test \
  :core:adapter:test :core:protocol:test :core:telemetry:test :core:vehicle:test \
  :app:assembleDebugAndroidTest --console=plain
```

Lint: **84 warnings,0 errors**. UnusedResources53, TypographyEllipsis15, ModifierParameter7, UseKtx5, RedundantLabel1, ConfigurationScreenWidthHeight1, PluralsCandidate1, ObsoleteSdkInt1. No warnings were suppressed. Existing deprecation/Unsafe JDK warnings are retained, not hidden.

APK: `app/build/outputs/apk/debug/app-debug.apk`,22,114,928 bytes. SHA256 `f4f7a8a90c0eacfe6ac4155fb3a509902cf0284a6d3834ad92058e2dcfbcb65b`. Build-tools37 apksigner verify exit0: v2 signature true, one debug signer. This is a debug build, not a signed production release; APK is ignored and not in Git.

### Persistent Android Studio environment

```text
GRADLE_USER_HOME: /home/mohammad/.gradle (effective normal user home)
GRADLE_JDK: Temurin24.0.2+12 /home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12
ANDROID_SDK: /home/mohammad/Android/Sdk; platforms;android-37.0, build-tools37.0.0
WRAPPER_CACHED: YES — 9.8.1 binary and prior Studio Kotlin DSL source archive
PLUGIN_DEPENDENCIES_CACHED: YES — unchanged AGP9.3.3, Kotlin/Compose2.2.10
PROJECT_DEPENDENCIES_CACHED: YES — 133 artifact-bearing configurations,2331 artifact occurrences,56 metadata buckets,0 failures
OFFLINE_HELP_STATUS: PASS exit0,921ms
OFFLINE_BUILD_STATUS: PASS exit0,1m28s,107 executed tasks
ANDROID_STUDIO_SYNC_READY: YES on this home machine; earlier actual online/offline GUI imports succeeded; not a new GUI run
REPOSITORY_ROUTING_USED: persistent user init.d/carobd-repositories.gradle; scoped Aliyun Google mirror and existing fallbacks; no CLI init argument
MISSING_ARTIFACTS: NONE reported by offline all-configuration resolution
```

Fresh cache check: `/tmp/carobd-overnight-final-cache.log`,exit0,3s. Offline help: `/tmp/carobd-overnight-final-offline-help.log`,exit0. Warm-up used Gradle-managed artifact population only. SDK android.jar and required build tools are present. Gradle runtime24 remains separate from JVM target17/core JBR17.0.14.

The machine uses the same user-level repository routing for CLI and Android Studio, without `--init-script`. No project repository/version changes were made. [Android Studio setup](ANDROID_STUDIO_OFFLINE_SETUP.md) explains the exact Gradle JDK and SDK selections. Actual Studio imports previously succeeded on this same machine/project at 14:38:40 and 16:35:12 on 2026-10-30, including an offline import, with `onSuccess` and `onImportFinished` log evidence. A new GUI sync was not claimed merely because the CLI gate passed. This continuation does not change Gradle scripts or dependency coordinates.

For another computer, fetch the branch and use its own persistent user cache/compatible SDK/JDK. Machine caches and user init scripts are intentionally not transferred through Git; install equivalent routing there if that machine needs it.

## Remaining Competitor Gaps

Combined readiness/02/09/06 session reports; real diagnostic scopes; existing-widget style editing; more proven canonical sensors; detailed inspector classification; manual fuel ledger; maintenance source metadata; alerts; validated full application backup; broader contextual help. HUD and parking are deferred. Dashcam and monetization are outside this sprint.

## Device Validation Required

Physical Classic/BLE/Wi-Fi/USB admission, actual permission/recovery flows, lifecycle behavior and sustained graph cadence on target phones still need bench work. Emulator execution is not adapter/device compatibility certification. Studio Run/Device Manager's separate adb5037 issue is not represented as fixed by Gradle Sync readiness; emulator QA uses isolated adb5038.

## Vehicle Validation Required

No physical vehicle operations were performed. Verify supported read-only services/protocols/responders and sustained measurement quality on authorized stationary vehicles. Keep physical clear, actuation, security/write/programming and speculative manufacturer definitions blocked. No vehicle health or genuine-ELM claim follows from software tests.

## Recommended Next Phase

1. Add one supported combined report scope through the existing typed evidence readers and session guard; retain partial evidence without auto-retrying state changes.
2. Add report detail paging/lazy loading for large archives and preserve original evidence.
3. Expand the canonical sensor plan only with existing authorized services, correct decoder/unit/source metadata and support tests; add existing-widget style editing.
4. Run physical adapter/device bench checks before enabling any new physical scope.
5. Extend maintenance/fuel/help only with explicit source/estimate distinctions. Preserve all safety and replay isolation invariants.

## Overnight lifecycle and permissions

Delayed service START/DISCONNECT/STOP are generation bound; teardown checks inside repository sessionMutex. Runtime-admission guard rechecks Classic/BLE/Wi-Fi before connect/write and passive link readiness; denial retires generation and regrant cannot resurrect it. USB keeps existing exact targeting/permission/presence checks. Five guard unit tests and one actual repository/session regression cover no rejected bytes, closure and fresh reconnect. Physical OS revocation remains device-required. Actual LAN permission UI observed shared-group pregrant without a separate prompt, not an explicit denial/regrant.

Earlier failed new-test assumptions are preserved in logs: a LAN test demanded a dialog although Nearby Devices can pregrant; backup resource test initially referenced hidden ApplicationInfo fields; both were corrected with actual public/runtime evidence. The initial FA picker failure led to a production scrolling fix. An emulator no-device failure was repaired by relaunching the same AVD, not reported as a passing test.

Canonical next-agent state and morning commands: [AGENT_CONTINUATION_HANDOFF.md](AGENT_CONTINUATION_HANDOFF.md). Safety fields: [CAROBD_SAFETY_STATUS.md](CAROBD_SAFETY_STATUS.md). Priorities: [CAROBD_REMAINING_GAPS.md](CAROBD_REMAINING_GAPS.md).

## Delivery and morning continuation

Overall continuation baseline:3da28167af627faa49df8113c301a72595071860; final tested source:1f327633e3375e1ea20d7d324c0f93f772694703, verified pushed. The final documentation commit is necessarily above that source snapshot; resolve actual delivery HEAD with Git rather than resetting to this hash. This is an execution-budget checkpoint before07:00, not completion of the entire overnight roadmap. No unverified source is hidden or shipped. The only intentionally untracked file is the pre-existing unrelated report.md, unchanged.

```bash
git fetch origin
git switch feat/car-obd-development
git pull --ff-only origin feat/car-obd-development
git rev-parse HEAD origin/feat/car-obd-development
cat docs/AGENT_CONTINUATION_HANDOFF.md
```

Preserve local changes before switching/pulling. All required caches/toolchains are machine-local; follow ANDROID_STUDIO_OFFLINE_SETUP.md on the company machine. Final delivery procedure fetches origin, checks short status/branch/both heads/log20 and verifies equality before sleep. Sleep is requested only on the verified physical local Linux host with no active builds/tests after successful push; the final chat records the command outcome. No shutdown/hibernate fallback or forced inhibitor bypass is authorized.

CAROBD_COMPETITOR_GAP_HANDOFF

HEAD: tested source1f327633e3375e1ea20d7d324c0f93f772694703; documentation publication follows
REMOTE_HEAD: same tested source at pre-publication verification; final delivery equality checked after push
TREE: final publication includes completed documentation; report.md remains unrelated and untracked

INFOCAR_GAPS_CLOSED:
- Partial/raw/source-preserving recent DTC report and category details; optional vehicle metadata; explicit report export

TORQUE_GAPS_CLOSED:
- Capability-gated widgets, persistent order/style/size, correct per-PID bounded graphs; notification units

PARTIAL:
- DTC-only report03/07/0A; broader diagnostic scope, widget editing, lazy archives, fuel/maintenance provenance

OUT_OF_SCOPE:
- Dashcam/monetization; HUD/parking deferred

SAFETY_REJECTED:
- Physical04/08, writes/security/programming, arbitrary manufacturer commands/scripts/plugins

NEW_TESTS:
- Fourteen original competitor JVM regressions plus23 overnight JVM regressions; total464 passes
- SQLite report persistence/rollback, Android JSON/output, packaged backup policy and broad simulator UI flow; final EN10/10 and FA10/10

BUILD_STATUS: clean offline full gate PASS; lint84warnings/0errors; duplicate classes/debug app/test APK PASS; offline cache/help PASS

DEVICE_REQUIRED:
- Real permission revocation/regrant, foreground lifecycle, Classic/BLE/Wi-Fi/USB loss/recovery and migration behavior

VEHICLE_REQUIRED:
- Authorized stationary read-only protocol/responders/measurement validation; no vehicle PASS claimed

NEXT_PRIORITY:
1. Combine existing typed evidence readers with generation-bound persistent reports
2. Lazy report archive details without evidence loss
3. Existing-widget style editing with conservative support gating
4. Authorized device/adapter bench verification
5. Provenance-aware maintenance/fuel/help

DO_NOT_BREAK:
- typed command authorization
- Mode04 physical disabled
- Mode08 blocked
- response ownership isolation
- session generation
- one producer
- conservative capability discovery
- truthful unknown/stale telemetry
- responder preservation
- simulator isolation
- replay isolation
