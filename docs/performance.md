# Performance Notes — CarOBD (Phase D)

Source-level audit of startup and first-render path (no real device required to read this).

## Known bottlenecks (source-verified)

| Area | Finding | Severity |
|------|---------|----------|
| App startup | No `runBlocking` on startup. No `Thread.sleep`. No `blockingGet`/`GlobalScope`. | — none |
| Koin DI | Fast (~6 ms, prior measurement). Not a bottleneck. | OK |
| DataStore | Preferences read is single, cached, on `lifecycleScope.launch` (non-blocking). Keys: `car_obd_prefs`. | OK |
| Vehicle catalog | `IranianEcuRegistry` is a Kotlin `object` (lazy-init), 8 small `VehicleProfile` entries + `ALL_PROTOCOLS`. Not eagerly built into Compose state. | OK |
| DashboardMvi collectors | 4 cheap `setState`-only collectors (`isOnboardingDone`, `userVehicleSetup`, `telemetry`, `connectionState`, `activeProfile`). No `sortedBy`/heavy map in collection path. `scanNearbyDevices()` is NOT started at init. | OK |
| Bluetooth scanner | `sortedByDescending { it.isObdCandidate }` only on a small in-memory device list, post-scan — not a startup path. | OK |
| Main-thread stalls (Samsung J7 Pro log) | `Skipped 280 / 92 / 38 / 31 frames` — root cause is **UI composition**, not startup blocking: giant car vector illustration, large gauge canvas drawing, unbounded recomposition in Dashboard. → Phase E target. | UI (Phase E) |

## Changes made (Phase C→D)

- Removed hardcoded maintenance `currentOdometerKm = 115000`; odometer now sourced from persisted `userVehicleSetup` (single source).
- Maintenance reminders rebuilt from `ServiceType.defaultIntervalKm` / `VehicleSpecSheet.timingBeltIntervalKm` (no duplicated literals).
- `UnitConverter` pure helper (+10 tests) — presentation-boundary conversions.
- Settings now persist & apply language/theme/units (no local-`remember` no-ops).

## How to reproduce (device)

1. Cold start (process killed) + warm start → measure first frame.
2. Dashboard open → scroll → watch skipped frames (Studio `Profileinstaller` / `systrace`).
3. Onboarding latency → first Dashboard render.
4. ConnectionForegroundService start latency.

## Device test checklist (§53)

### APP STARTUP
- [ ] cold start — no crash
- [ ] warm start — no crash
- [ ] onboarding behavior
- [ ] selected vehicle restores after kill+reopen

### ACTIVE VEHICLE (B3 single source)
- [ ] choose Dena → Dashboard = Dena
- [ ] choose Dena → Diagnostics = Dena
- [ ] choose Dena → Maintenance = Dena
- [ ] choose Dena → Trips = Dena
- [ ] choose Dena → ECU screen = Dena
- [ ] kill app → reopen → Dena persists

### BLUETOOTH OFF
- [ ] Bluetooth OFF → attempt connect → explicit Persian "بلوتوث خاموش است"
- [ ] never renders Connected
- [ ] Diagnostics never shows "ECU Normal"

### REAL ELM
- [ ] pair → connect → initialize with valid Mode01 PID00
- [ ] only then Connected
- [ ] adapter identity/firmware shown in Settings → Adapter Diagnostics

### DTC
- [ ] disconnected scan = Failure
- [ ] connected, 0 DTC = SuccessNoCodes
- [ ] actual DTC = SuccessWithCodes
- [ ] clear only when connected

### SIMULATOR
- [ ] persistent "SIMULATION MODE" badge (Persian)
- [ ] `TransportType.SIMULATOR`
- [ ] never shown as physical transport

### SETTINGS
- [ ] language global (Persian ↔ English)
- [ ] RTL / LTR
- [ ] theme global (persists + restart)
- [ ] units converted (km↔miles, °C↔°F, kPa↔psi)

### MAINTENANCE
- [ ] opens without crash
- [ ] shows active vehicle (Dena, not Peugeot/TU5)
- [ ] reminders use active-vehicle odometer
- [ ] reset persists across restart

### PERFORMANCE
- [ ] cold startup observation
- [ ] skipped frames (Dashboard scroll, gauge scroll, onboarding)

> DEVICE VERIFIED / VEHICLE VERIFIED labels require an actual phone + ELM + vehicle. Source/build/unit-test work only is marked SOURCE VERIFIED / BUILD VERIFIED / UNIT TEST VERIFIED.
