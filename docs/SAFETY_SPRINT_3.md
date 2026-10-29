# Sprint 3 exact Android build verification — 2026-10-30

Status: **SOFTWARE_BUILD_GO_READ_ONLY__DEVICE_VALIDATION_REQUIRED**.
Physical Mode04 remains **DISABLED_FOR_SAFETY**. No Android installation, physical adapter or vehicle test occurred. This report supersedes Sprint 2's build-blocked status; it does not supersede its historical evidence.

## Git and repairs

Started at `85aafa546ad8350c201a4c9e30c15fd974037e52` on `feat/car-obd-development`, fetched equal to origin. Remote: `git@github.com:SaraniMohammadReza/CarOBD.git`. Historical master object `0cf39bd48984a2a0cb49a035def1726ff5b89514` was verified; master was not changed or pushed. The preexisting untracked `report.md` was preserved byte-for-byte (SHA256 `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325`).

- `0b3ed7b`: correct daemon download assets to Temurin 24.0.2+12 while keeping the intentional Java 24 daemon criterion. The old Foojay URLs redirected to Java 25 despite that criterion. Only supported published platform assets are listed; other platforms require an installed matching JDK.
- `497eea6`: preserve the user's removal of the root Kotlin JVM alias. AGP supplies KGP 2.2.10 on the shared classpath; the five core modules reuse the unversioned plugin and explicitly reject divergence from catalog 2.2.10. This fixes Gradle's already-on-classpath/unknown-version error without changing compiler versions.
- `b4fecff`: retain persisted ECO coverage, add persisted FAST recreation feeding the real producer without a UI subscriber, and add NORMAL → ECO cycle times 0/1000/3000 ms. No safety assertion was weakened.

## Environment and blocker forensics

The Java 24 request comes from committed `gradle/gradle-daemon-jvm.properties`, not application language requirements or an IDE override. That criterion takes precedence over JAVA_HOME. All core toolchains and app Java compatibility remain 17.

The old download lock was held by a live Android Studio process (PID 248008 at inspection), with a growing partial archive. It was not a dead-process lock. No IDE process was killed, lock deleted or Gradle home wiped. A separate official Temurin 24.0.2+12 archive was verified against Adoptium SHA256 `aea1cc55e51cf651c85f2f00ad021603fe269c4bb6493fa97a321ad770c9b096` and installed outside the repository.

| Item | Actual evidence |
|---|---|
| Wrapper | Gradle 9.8.1, revision 39daf39feb60a18b1b6ddccde53366522fef516f |
| Gradle embedded Kotlin / Groovy / Ant | 2.4.10 / 4.0.33 / 1.10.17; embedded Kotlin is not the app compiler |
| Launcher and daemon | Temurin 24.0.2+12 |
| JAVA_HOME for these commands | `/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12` |
| Core compile and test toolchain | JetBrains JBR 17.0.14 (`/home/mohammad/.jdks/jbr-17.0.14`) |
| App compiler host / unit-test JVM | Temurin 24.0.2+12; app does not declare a Java toolchain |
| App and every core Kotlin JVM target | 17, queried from actual Gradle tasks |
| Android Java source/target | 17 / 17 |
| AGP / Kotlin and Compose compiler plugin | 9.3.3 / 2.2.10; actual plugin version guard passes |
| SDK | `/home/mohammad/Android/Sdk`, platform android-37.0, build tools 37.0.0; min 24, target/compile 37 |
| Coroutines | Declared and resolved app runtime 1.10.1 (Gradle dependencyInsight) |
| OS | Linux 7.0.0-38-generic amd64 |

System JAVA_HOME was initially unset; default java/javac were Ubuntu 25.0.4.1. The verified commands explicitly selected JDK 24.

Official compatibility references: [Gradle daemon criteria](https://docs.gradle.org/current/userguide/gradle_daemon.html), [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html), [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin). AGP's published 9.3.3 POM/module also identifies runtime KGP 2.2.10.

## Exact wrapper execution

Direct Google Maven requests returned 404 here even for known older AGP coordinates and repository metadata. An external, uncommitted init script prepended `https://maven.aliyun.com/repository/google` for Android/Google/AndroidX groups. AGP POM content was cross-checked against Huawei's mirror. No version, variant, dependency substitution, Maven-local repository or test runner replacement was introduced. Locally reused artifacts were checked by Gradle against remote checksums.

Evidence and the complete mirror init script are retained at `/home/mohammad/.local/share/carobd-build/sprint3-evidence/`. A checkout on this same network needs that routing workaround or restored Google Maven access. Repository settings were not changed to impose the regional workaround on other users.

`--version`: exit 0. Configuration `tasks`: exit 0. Primary checkout `clean`: **exit 0**, six tasks up-to-date because it had no generated build outputs. Then the complete gate executed from that checkout:

```bash
JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12 \
ANDROID_HOME=/home/mohammad/Android/Sdk \
./gradlew --init-script /home/mohammad/.local/share/carobd-build/sprint3-evidence/carobd-s3-repositories.gradle \
  -Dorg.gradle.internal.http.connectionTimeout=30000 \
  -Dorg.gradle.internal.http.socketTimeout=30000 \
  :app:assembleDebug :app:testDebugUnitTest :app:checkDebugDuplicateClasses \
  :app:lintDebug :core:model:test :core:adapter:test :core:protocol:test \
  :core:telemetry:test :core:vehicle:test --console=plain --info
```

**Exit 0, BUILD SUCCESSFUL in 6m45s, 71 actionable tasks executed.** No tasks were excluded. Logs were redirected to files with the actual exit status retained. An earlier isolated-checkout dependency warm-up was interrupted (exit 130); it is not used as passing evidence. A concurrent inspection attempt hit a configuration-cache lock and was retried sequentially; it is not counted as a gate failure or test result.

## Fresh Gradle XML totals

| Module | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| app | 98 | 0 | 0 | 0 |
| core:model | 30 | 0 | 0 | 0 |
| core:adapter | 100 | 0 | 0 | 0 |
| core:protocol | 90 | 0 | 0 | 0 |
| core:telemetry | 13 | 0 | 0 | 0 |
| core:vehicle | 1 | 0 | 0 | 0 |
| **Total** | **332** | **0** | **0** | **0** |

Computed from 43 current-run TEST-*.xml files under app/build/test-results/testDebugUnitTest and core/*/build/test-results/test, also copied to the evidence directory. No historical supplemental totals were reused.

`tools/check_safety_jvm.py` remains a supplemental runner. It compiles all five core modules' main/tests plus app's Android-free session sources/tests using installed kotlinc and catalog coroutine/JUnit versions. It does not validate other Android app sources, resources, manifests, lint, dependency resolution, packaging or platform callbacks. It was inspected, not substituted for this Gradle gate.

## Lint and duplicate classes

Actual lint XML/HTML: `app/build/reports/lint-results-debug.{xml,html}`. **0 errors, 76 warnings**: UnusedResources 47, NewerVersionAvailable 8, TypographyEllipsis 8, ModifierParameter 7, and one each AppBundleLocaleChanges, RedundantLabel, AndroidGradlePluginVersion, PluralsCandidate, ObsoleteSdkInt, UseKtx.

No permission/BLE/FGS/API correctness errors or missing-translation errors were emitted. Existing MissingPermission suppressions in Classic/BLE remain; a clean lint result does not prove runtime permission handling. Source search found no runtime Bluetooth permission request/check flow. That is concrete device-readiness work, especially on a fresh Android 12+ install. The dynamic-locale warning is relevant to a future Play App Bundle: language split delivery must support switching between Persian and English; this debug APK includes both. Pluralization and inline UI strings also need localization review. No lint rules or tests were disabled in this sprint.

Duplicate-class task passed. The canonical ServiceType/ServiceRecord/VehicleSpecSheet remain in core:model; app MaintenanceModels holds only its reminder model/constant. DuplicateFqcnGuardTest also passed. Compiler deprecations (legacy BLE APIs, Compose tabs/icons/system-bar APIs), D8 informational toolchain warnings, DataStore Unsafe warnings on Java 24 and Gradle 10 deprecations remain non-fatal maintenance items.

## APK static evidence

- Path: `app/build/outputs/apk/debug/app-debug.apk` in the primary repository.
- Size: **21,614,825 bytes**; mtime **2026-10-30 02:26:39.575366232 +03:30**.
- SHA256: `193c42a0a0ff7d5d312a3b8dd244123c6632e6abc4b2552347de42c9f2264cf6`.
- aapt badging/binary manifest: `com.sarani.carobd`, versionName 1.0.0, code 1, minSdk 24, target/compileSdk 37.
- Permissions: Bluetooth/ADMIN capped at 30, SCAN with neverForLocation, CONNECT; fine/coarse location; INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE; FOREGROUND_SERVICE, FOREGROUND_SERVICE_CONNECTED_DEVICE, POST_NOTIFICATIONS; AndroidX's signature-level dynamic-receiver permission.
- Non-exported ConnectionForegroundService declares connectedDevice. USB host is optional. No USB attach filter/device-filter metadata is packaged; explicit selection and dynamic permission handling are used.
- apksigner verification passed; signer is CN=Android Debug, O=Android, C=US. No release signing credentials created.

## Post-build safety and Sprint 2 verification

These are software source/JVM regression conclusions, not physical guarantees.

| Invariant | Current evidence |
|---|---|
| A Single command/no injection | SingleCommandParser rejects controls/non-ASCII before normalization; WireEnvelopeGate/classifier tests pass |
| B Physical Mode04 unavailable | readOnly authorization excludes 04; simulator clear checks SIMULATOR transport; adapter/repository/raw tests pass |
| C Control/write/security/programming unavailable | strict read-service allowlist; privileged service regression corpus passes |
| D Unknown denied | private authorized-command constructor, deny-by-default adapter path; direct attacks pass |
| E Canceled response cannot satisfy next command | cancellation retires/disconnects the generation; CommandOwnership/adversarial/session tests pass |
| F Timeout quarantine | bounded prompt drain; missing boundary compromises/retires stream; late-prompt tests pass |
| G One producer | session owns one producer; restart cancels old job; observer/cadence tests pass |
| H Loss invalidates Connected | session watchdog and inbound completion retire generation; transport-loss tests pass |
| I Exact USB target | unique identity and explicit interface required; absent/ambiguous target tests pass |
| J Manual protocol mismatch fails | 0–9/A exact command/readback; unknown/rejected/mismatch tests pass |
| K Simulation/replay isolation | isolated simulator has no physical delegate; ReplayTransport ignores writes; routing/isolation tests pass |
| L Malformed cannot mean no codes | envelope and scan validation preserve failed/mixed outcomes; malformed/DTC-category tests pass |

Connection Center's five editors, navigation, resources, ViewModels and DI registration source compile with the actual Android toolchain. This does not establish runtime Koin resolution, Android UI rendering or platform lifecycle behavior.

The actual repository wizard tests pass: six evidence stages, read-only physical handshake, rejected unknown protocol, retained ECU evidence, closed session and zero forbidden writes. Persisted ECO and FAST survive disk DataStore recreation; the real producer consumes the repository state without a UI collector. NORMAL → ECO produces 0/1000/3000 ms samples with one discovery and one poll stream. Simulator tests pass for healthy, stored/pending/permanent after initialization, malformed, multi-ECU, independent late prompt after the receive deadline, timeout, NO DATA, BUS INIT ERROR, CAN ERROR and BUFFER FULL. Session/cancellation/disconnect tests also pass.

## Remaining software and next-sprint backlog

1. Before physical use: implement/review runtime Bluetooth/notification permission admission, fresh-install/denial/revocation behavior, then bench all transport and service lifecycle paths in HARDWARE_VALIDATION_CHECKLIST.md. No device results are claimed.
2. Readiness: PidCodec currently returns false MIL/zero count/empty monitors for a payload shorter than four bytes. A later typed result should preserve malformed/unknown state rather than allowing consumers to interpret this as healthy.
3. Mode09: preserve per-responder framing/count/completeness through product integration. Tighten decodeVin's permissive fallback (any printable text of length >=10) and explicit partial-CVN reporting before presenting identity as verified.
4. Mode02: a command builder exists, but no complete freeze-frame decoder/product integration. Add typed frame/PID support and malformed/unsupported/multi-responder fixtures in a later feature sprint.
5. Record/replay: raw frame recording trims whitespace and replay is a cold timed stream independent of command matching; validate chunk fidelity, subscriber timing and disconnect semantics before calling it deterministic session reproduction. It has no physical delegate. Add adversarial transcript fixtures before product exposure.
6. Locale delivery, pluralization/inline strings and remaining deprecations need focused follow-up. No new major product features were implemented here.

After controlled bench acceptance, authorized read-only vehicle validation must compare CAN/non-CAN protocol and stored/pending/permanent results against a reference tool, retain responder identity and bus-load traces, and verify zero privileged writes. Physical Mode04 remains disabled regardless of this software build result.
