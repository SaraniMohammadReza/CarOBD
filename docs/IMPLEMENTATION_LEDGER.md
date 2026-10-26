# Implementation Ledger

This document is an append-only chronological record of all architectural modifications, fixes, and feature additions in CarObd.

---

### [2026-10-25] Foundation Rebuild & Multi-Module Architecture
- **Action**: Extracted shared domain, protocol, adapter, vehicle, and telemetry logic into standalone JVM modules.
- **Modules Created**:
  - `:core:model`
  - `:core:protocol`
  - `:core:adapter`
  - `:core:vehicle`
  - `:core:telemetry`
- **Protocols Implemented**:
  - ISO 15765-2 (ISO-TP Reassembly & Flow Control)
  - SAE J1979 Modes 01 through 0A & Bitmask PID decoders
  - ISO 14230-4 KWP2000 parser, checksum validator, NRC handling
  - ISO 14229-1 UDS diagnostic client
  - SAE J1939 29-bit CAN ID DM1 parser
  - DTC lookup engine for standard powertrain, body, chassis, network and Iranian ECU codes
- **Adapter Layer Implemented**:
  - `Elm327Adapter` with synchronized pipeline, prompt parsing (`>`), and hardware error categorization
  - `Elm327Prober` with AT command probes (`ATI`, `ATRV`, `ATCAF`, `ATAT`, `ATDPN`)
  - `StnAdapter` extending ELM with STN-specific commands (`STPBR`, `STPX`, `STPRS`)
- **Vehicle Layer Implemented**:
  - `IranianEcuCatalog` for Bosch ME7.4.4, ME7.4.5, ME7.4.9, Siemens SIM2K/VDO, SSAT, Valeo S2000, EasyU 2.5, Crouse/Kesens, EMS3132, Delphi MT22, Euro 5 Motorcycle ISO 19689
  - `VehicleCatalog` and `VehicleSpecDatabase` with oil viscosity, spark plug gaps, timing belts
- **Telemetry Engine Implemented**:
  - `BoundedRingBuffer`
  - `FuelEconomyCalculator` (MAF and Speed-Density formulas)
  - `TripComputer` with numerical integration and eco scoring
  - `TelemetryScheduler` with dynamic RTT pacing
- **Transports in `:app` Implemented**:
  - `BluetoothClassicTransport` (RFCOMM SPP)
  - `WifiTcpTransport` (TCP socket)
  - `BleTransport` (Android BLE GATT NUS/FFF0)
  - `IsolatedSimulatorTransport` (Deterministic virtual engine)
  - `CarObdDatabaseHelper` (SQLite storage)
  - `ObdRepositoryImpl` (Unified repository orchestrator)
- **Toolchain Alignment**: Configured `jvmToolchain(17)` on core modules and aligned Gradle daemon JVM with Java 24 to prevent Foojay external network download failures.

---

### [2026-10-26] Phase 0 Documentation & Phase 1 Truthfulness Remediation
- **Action**: Initialized Git repository with clean `.gitignore`.
- **Action**: Established mandatory engineering documentation suite in `docs/`:
  - `AGENT_HANDOFF.md`
  - `IMPLEMENTATION_LEDGER.md`
  - `PROTOCOL_SUPPORT_MATRIX.md`
  - `VEHICLE_SUPPORT_MATRIX.md`
  - `HARDWARE_TEST_MATRIX.md`
  - `DATA_PROVENANCE.md`
  - `DEPENDENCY_LICENSES.md`
  - `COMPETITOR_FEATURE_MATRIX.md`
- **Action**: Removed fake Bluetooth mock devices in `DashboardMvi.kt` and hooked real Bluetooth adapter discovery.
- **Action**: Removed simulated auto-scan fake delay loop in `VehicleProfileMvi.kt`.
- **Action**: Removed hardcoded adapter inspector benchmark claims and replaced with real probe state.
- **Action**: Enforced nullable telemetry models with `—` unmeasured displays in Compose UI.
