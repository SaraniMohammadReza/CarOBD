# Sprint 4 actual test evidence

## A — source reassessment

Recovered14f924ea60d3ff350537fae143faf334ecf79691, fetched origin, confirmed same branch/head. Working tree only pre-existing report.md before documentation. No code changed and no fresh runtime/build result asserted. Previous464 JVM/10EN/10FA/84lint warnings are historical. Next checkpoint requires actual new regression results.

## B — ownership persistence

Company-machine isolated worktree based on remote `5ed1d92`. `:core:model:test :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain` passed, exit0,1m2s; 68 actionable tasks (50 executed,18 up-to-date). Two new model regressions cover nullable unknowns/normalization and invalid numeric facts. `OwnedVehiclePersistenceInstrumentedTest` and the v1→current migration assertion compile into the Android test APK but were not executed on an emulator/device in this checkpoint. Initial compile attempt failed only because the new isolated worktree lacked ignored `local.properties`; the company SDK path was added locally and remains untracked/ignored.
