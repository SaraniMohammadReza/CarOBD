# Competitor Feature Parity Matrix

This matrix tracks CarObd's feature capabilities against market-leading automotive applications: **Torque Pro**, **Car Scanner ELM OBD2**, and **Infocar**.

| Feature Category | Specific Capability | Torque Pro | Car Scanner ELM | Infocar | CarObd Current Status | CarObd Target Advantage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Connectivity** | Bluetooth Classic (SPP) | Supported | Supported | Supported | **Supported** (`BluetoothClassicTransport`) | Multi-device auto-reconnect |
| | Bluetooth Low Energy (BLE) | Limited | Supported | Supported | **Supported** (`BleTransport`) | Multi-service UUID auto-discovery |
| | Wi-Fi (TCP Socket) | Supported | Supported | Supported | **Supported** (`WifiTcpTransport`) | Low-latency non-blocking TCP |
| | USB Host (FTDI / CH340 / CP2102) | Supported | Supported | No | **In Progress** (`UsbSerialTransport`) | Plug-and-play OTG enumeration |
| | Isolated Virtual Simulator | Third-party plugin | Basic | No | **Implemented** (`IsolatedSimulatorTransport`) | Explicitly tagged simulator |
| **Protocol Support** | ISO 15765-4 CAN (11/29-bit, 500k/250k) | Supported | Supported | Supported | **Supported** (`IsoTpEngine`, `PidCodec`) | Flow Control up to 4095 bytes |
| | ISO 14230-4 KWP2000 (Fast & 5-Baud) | Supported | Supported | Partial | **Supported** (`Kwp2000Engine`) | Custom Iranian ECU checksums |
| | ISO 9141-2 | Supported | Supported | Partial | **Supported** (`Kwp2000Engine`) | Tested timing |
| | ISO 14229 UDS over CAN | Limited | Supported (Profiles) | No | **Supported** (`UdsEngine`) | Routine & Security session client |
| | SAE J1939 (Heavy Duty Trucks / Bus) | Third-party plugin | Limited | No | **Supported** (`J1939Engine`) | 29-bit PGN & DM1 DTC parser |
| | ISO 19689 (Euro 5 Motorcycles) | No | Partial | No | **Supported** (`IranianEcuCatalog`) | Iranian & international bikes |
| **Diagnostics (DTC)** | Read Confirmed DTCs (Mode 03) | Supported | Supported | Supported | **Supported** (`PidCodec`, `DtcParser`) | 10,000+ codes database |
| | Read Pending DTCs (Mode 07) | Supported | Supported | Supported | **Supported** (`PidCodec`, `DtcParser`) | Detailed explanation & causes |
| | Read Permanent DTCs (Mode 0A) | Supported | Supported | Partial | **Supported** (`PidCodec`, `DtcParser`) | Real ECU permanence indicator |
| | Clear DTCs (Mode 04) | Immediate send | Immediate send | Confirmation | **Safe Workflow** (Snapshot + Confirm + Rescan) | Safety gate preventing accidental loss |
| | Freeze Frame Data (Mode 02) | Supported | Supported | Supported | **Supported** (`PidCodec`) | Associates exact sensor state at fault |
| | Readiness Monitors (Mode 01 PID 01) | Supported | Supported | Supported | **Supported** (`PidCodec`) | Full MIL and emissions readiness |
| **Live Telemetry & Dashboard** | Customizable Gauge Grid | Fully Custom | Fully Custom | Fixed / Semi | **In Progress** (Jetpack Compose Dynamic Grid) | Material 3 + Custom Dial / Digital |
| | Live Graphing / Charting | Basic Line Chart | Advanced Charts | Modern Line Chart | **In Progress** (Ring buffer + dynamic scaling) | High-FPS canvas, zero allocation |
| | Fuel Economy (Instant & Average) | MAF only | MAF + MAP | Estimated | **Physical Dual Engine** (MAF & Speed-Density) | Automatic fallback if MAF absent |
| | Trip Computer & Eco Scoring | Plugin | Supported | Supported | **Supported** (`TripComputer`) | Harsh brake/accel, numerical integral |
| **Specialized Domestic Focus** | Iranian Vehicles (IKCO, SAIPA, Pars Khodro) | Generic OBD only (Poor) | Custom config files needed | Generic OBD only | **Deep Native Support** (Bosch ME7.4.x, Siemens VDO/SIM2K, SSAT, Kesens, EasyU) | Exact OEM ECU profiles & parameters |
| | Maintenance & Fluids Database | None | None | Generic | **Built-in Verified Specs** (`VehicleSpecDatabase`) | Oil specs, spark plug gaps, belt intervals |
| | Persian (Farsi) Localization & RTL | Community (Broken) | Community | No | **100% Native Persian RTL & LTR Dual-Mode** | Automotive terminology translated accurately |
| | AMOLED Black Mode | Basic Dark | Supported | Dark Mode | **Pure AMOLED Black (`#000000`)** | Battery saving for in-car mounts |
