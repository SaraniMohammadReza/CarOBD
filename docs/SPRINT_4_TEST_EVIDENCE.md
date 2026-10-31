# Sprint 4 actual test evidence

## A — source reassessment

Recovered14f924ea60d3ff350537fae143faf334ecf79691, fetched origin, confirmed same branch/head. Working tree only pre-existing report.md before documentation. No code changed and no fresh runtime/build result asserted. Previous464 JVM/10EN/10FA/84lint warnings are historical. Next checkpoint requires actual new regression results.

## B — ownership persistence

Company-machine isolated worktree based on remote `5ed1d92`. `:core:model:test :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain` passed, exit0,1m2s; 68 actionable tasks (50 executed,18 up-to-date). Two new model regressions cover nullable unknowns/normalization and invalid numeric facts. `OwnedVehiclePersistenceInstrumentedTest` and the v1→current migration assertion compile into the Android test APK but were not executed on an emulator/device in this checkpoint. Initial compile attempt failed only because the new isolated worktree lacked ignored `local.properties`; the company SDK path was added locally and remains untracked/ignored.

## C — My Vehicle UI and ownership isolation

Final command:

```powershell
.\gradlew.bat :core:model:test :app:testDebugUnitTest :app:assembleDebug `
  :app:assembleDebugAndroidTest :app:lintDebug :app:checkDebugDuplicateClasses --console=plain
```

Result: exit 0, BUILD SUCCESSFUL. The generated XML reports contain 210 JVM tests with zero failures, errors or skips. Both debug APKs compiled, lint passed and duplicate-class checking passed. Targeted `OwnedVehiclesViewModelTest` and `VehicleSetupNumbersTest` execution also passed after the final test fix.

Coverage added: localized create/edit and nullable normalization; rejected grouped odometer input; selection/deletion isolation; strict English/Persian/Arabic four-digit model years. The compiled Android SQLite test covers save/select, same-id edit, a second isolated vehicle, database reopen, selected-id persistence, selected deletion and preservation of the other vehicle while legacy garage remains untouched.

Android runtime result is `NOT_VERIFIABLE_ON_BLUESTACKS`, not pass or fail. Local-only BlueStacks Pie64 (`127.0.0.1:5555`, API 28, x86_64) accepted installation of both APKs but repeatedly became offline before `am instrument` could run. `connectedDebugAndroidTest` also encountered an unavailable `com.android.tools.utp:android-test-plugin-host-additional-test-output:32.3.3` artifact. No instrumentation assertion executed, and no Bluetooth/BLE/USB/Wi-Fi adapter or vehicle behavior was tested.
