# Home stabilization

## HOME-R1 — safe reconciliation

Remote verified `d8b1f2d4bae7ef61e1b369ceb96ef3a8687082de`. Original home HEAD `5ed1d92017224dd2df1113b2d8d14a902049b7e2`. All 21 development files were preserved in LOCAL ONLY branch `home/ownership-preservation`, commit `73e0dd8`; this is an unverified WIP snapshot, not a green release. Unrelated report.md remains untracked and unchanged. Patch and untracked archive also exist outside Git in /tmp.

Development worktree `/home/mohammad/Desktop/CarOBD/CarOBD-home-dev` starts exactly at remote on `feat/car-obd-development`. No rebase/reset/clean or preservation-branch push. The company schema/model is canonical; the incompatible home v5 migration is never applied.

| Home file | Decision | Reason |
|---|---|---|
| `app/src/androidTest/java/com/sarani/carobd/DiagnosticReportPersistenceInstrumentedTest.kt` | OBSOLETE | Old DB v5 expectations superseded by company v7 and subsequent additive migrations. |
| `app/src/androidTest/java/com/sarani/carobd/DtcHistoryMigrationInstrumentedTest.kt` | OBSOLETE | Old DB v5 expectations superseded by company v7 and subsequent additive migrations. |
| `app/src/androidTest/java/com/sarani/carobd/OwnershipPersistenceInstrumentedTest.kt` | MERGE SELECTIVELY | Port test concepts (unknown/zero, recreation, routing isolation), adapting to canonical APIs; do not copy obsolete routes. |
| `app/src/androidTest/java/com/sarani/carobd/RuntimeNavigationInstrumentedTest.kt` | MERGE SELECTIVELY | Port test concepts (unknown/zero, recreation, routing isolation), adapting to canonical APIs; do not copy obsolete routes. |
| `app/src/main/java/com/sarani/carobd/MainActivity.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/core/di/AppModule.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/core/storage/VehicleSetupNumbers.kt` | KEEP ONLY ON PRESERVATION BRANCH | Fractional mileage is incompatible with canonical integer-km model; company localized parser retained. |
| `app/src/main/java/com/sarani/carobd/data/db/CarObdDatabaseHelper.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/db/OwnedVehicleStore.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/db/OwnershipMigration.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/data/repository/OwnershipRepository.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/ui/screens/dashboard/DashboardScreen.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnedFieldLabels.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnershipScreen.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/java/com/sarani/carobd/ui/screens/ownership/OwnershipViewModel.kt` | MERGE SELECTIVELY | Retain usability ideas (scrolling body, stable actions, unknown values); no parallel screen/ViewModel. |
| `app/src/main/res/values-fa/strings.xml` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/main/res/values/strings.xml` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `app/src/test/kotlin/com/sarani/carobd/OwnedVehicleInputTest.kt` | KEEP ONLY ON PRESERVATION BRANCH | Fractional mileage is incompatible with canonical integer-km model; company localized parser retained. |
| `core/model/src/main/kotlin/com/sarani/carobd/core/model/OwnedVehicle.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `core/model/src/test/kotlin/com/sarani/carobd/core/model/OwnedVehicleTest.kt` | OBSOLETE | Equivalent company model/repository/store/DI/routes/resources already exist; retain canonical OwnedVehicleRepository. |
| `docs/SPRINT_4_IMPLEMENTATION_REPORT.md` | KEEP ONLY ON PRESERVATION BRANCH | Historical unfinished home draft, not evidence for company implementation. |

No duplicate home architecture is ported. Home integration-test concepts will be adapted during stabilization. Source fixes begin after this reconciliation checkpoint. Physical diagnostic core is outside scope.

R1 validation: localization checker PASS (618 resources); offline lintDebug PASS, exit0, 1m13s. git diff --check PASS. No runtime verification claimed.
