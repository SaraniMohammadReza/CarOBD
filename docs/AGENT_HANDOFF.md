# CarObd Engineering Agent Handoff Document

## 1. Project Overview & Architecture
CarObd is a high-performance, evidence-governed automotive diagnostics and telemetry SuperApp for Android. It is engineered with a strict multi-module clean architecture:
- `:core:model`: Pure Kotlin JVM domain models, 18-state connection machine, provenance taxonomy, telemetry, DTC, maintenance, and vehicle definitions.
- `:core:protocol`: Pure Kotlin JVM automotive protocol engines:
  - ISO 15765-2 (ISO-TP) multi-frame reassembly and Flow Control generation.
  - SAE J1979 classic OBD-II Modes 01-0A codec and bitmask PID support discovery.
  - ISO 14230-4 KWP2000 stateful frame parsing, checksum calculation, and NRC mapping.
  - ISO 14229-1 UDS diagnostic client, session control, security access, and routine control.
  - SAE J1939 29-bit CAN ID decoders and DM1 active DTC parsing.
  - Standard SAE and proprietary Iranian/OEM DTC lookup database.
- `:core:adapter`: Pure Kotlin JVM adapter state machine:
  - `Elm327Adapter`: Synchronized AT command pipeline, prompt detection, hardware error categorization (`CAN_ERROR`, `BUS_BUSY`, `BUFFER_FULL`, `NO_DATA`, etc.).
  - `Elm327Prober`: Probe routines (`ATI`, `ATRV`, `ATCAF`, `ATAT`, `ATDPN`), clone detection heuristics (PIC18F25K80 genuine vs clone chips).
  - `StnAdapter`: OBDLink / STN extended command suite (`STPBR`, `STPX`, `STPRS`).
  - `ObdTransport`: Hardware-agnostic transport abstraction.
- `:core:vehicle`: Pure Kotlin JVM vehicle definitions:
  - `IranianEcuCatalog`: Hardware & diagnostic specifications for IKCO, SAIPA, Pars Khodro (Bosch ME7.4.4, ME7.4.5, ME7.4.9, Siemens SIM2K/VDO, SSAT, Valeo S2000, EasyU 2.5, Crouse/Kesens CAN, Siemens EMS3132, Delphi MT22, Euro 5 Motorcycle ISO 19689).
  - `VehicleCatalog`: Canonical vehicle profiles for domestic and international vehicles.
  - `VehicleSpecDatabase`: Maintenance specifications, engine oil grades, capacities, spark plug part numbers and electrode gaps, gearbox lubricants, and timing belt intervals.
- `:core:telemetry`: Pure Kotlin JVM telemetry engine:
  - `BoundedRingBuffer`: Zero-allocation ring buffer for live charting and statistical analysis.
  - `FuelEconomyCalculator`: Dual-mode physical fuel consumption engine (MAF sensor and Speed-Density approximation).
  - `TripComputer`: Numerical integration of speed and fuel over elapsed time, eco scoring, and harsh event detection.
  - `TelemetryScheduler`: Priority-paced query queue dynamically adjusted to round-trip latency.
- `:app`: Android application layer:
  - Jetpack Compose Material 3 UI with Persian RTL and English LTR support.
  - Transport layer: `BluetoothClassicTransport` (RFCOMM SPP), `WifiTcpTransport` (TCP socket), `BleTransport` (GATT client), `UsbSerialTransport` (USB Host), `IsolatedSimulatorTransport` (clearly labeled virtual test engine).
  - Data persistence: SQLite storage with Room migration in progress.
  - Android 14+ Foreground Service (`ConnectionForegroundService`) for uninterrupted background logging.

## 2. Evidence Taxonomy
Every diagnostic fact and vehicle specification is tagged with its empirical verification level:
- `IMPLEMENTED_IN_SOFTWARE`: Code/parser is written and passed unit tests, but not yet verified against vehicle hardware.
- `SIMULATED_VERIFIED`: Verified against the isolated virtual simulator.
- `HARDWARE_TEST_REQUIRED`: Known specification requiring physical test with an ELM327/STN adapter on physical vehicle OBD port.
- `VEHICLE_VERIFIED`: Confirmed working on a physical production vehicle with documented logs.
- `COMMUNITY_REPORTED`: Provided by mechanic logs or community forums; awaiting physical verification.
- `OEM_VERIFIED`: Sourced directly from OEM workshop manual (e.g. IKCO/SAIPA official service documentation).

## 3. Strict Truthfulness Policy
1. No fake Bluetooth devices are generated. Real discovery and paired lists only.
2. No simulated connection progress animations masquerading as physical hardware.
3. No plausible non-zero defaults (`12.6V`, `90°C`, `800 RPM`). Unmeasured signals are represented by `null` and rendered as `—`.
4. Simulator mode is strictly labeled with prominent visual indicators and logs to an isolated database/table.
5. No ECU is claimed "healthy" merely because 0 DTCs were found; Readiness Monitors and Mil status must be reported truthfully.

## 4. Current Phase & Progress
- **Phase 0 (Foundation & Audit Cleanup)**: Complete. Multi-module refactoring done, build and tests passing.
- **Phase 1 (Truthfulness Enforcement & Defect Remediation)**: In Progress.
- **Phase 2 (Canonical Architecture Unification)**: In Progress.
- **Phase 3 (Connection Engine & Transports)**: Transports built, service underway.
- **Phase 4 & 5 (Supported PIDs, DTCs & Room Database)**: In Progress.
- **Phase 6 to 9 (Dashboard Customization, Charts, Persian Localization, Export)**: Planned.

## 5. Verification Commands
```bash
./gradlew test
./gradlew :app:assembleDebug
```
