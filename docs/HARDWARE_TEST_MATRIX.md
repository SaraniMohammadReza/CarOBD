# Hardware Test Matrix

This matrix documents physical diagnostic adapter hardware categories, microcontrollers, connection interfaces, known clone quirks, and validation status.

| Hardware Family | Microcontroller / Chipset | Interface | Supported Protocols | Clone Detection Heuristics | Status | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **ELM327 Genuine (v1.3a - v2.3)** | Microchip PIC18F25K80 | Bluetooth SPP / Wi-Fi / USB | ISO 15765-4, KWP2000, ISO 9141-2, J1850 PWM/VPW | Responds to full AT command set (`ATAL`, `ATCAF0`, `ATAT2`, `ATSP0`, etc.) | `IMPLEMENTED_IN_SOFTWARE` | Industry standard genuine reference. High stability. |
| **ELM327 Clone v1.5 (High Quality)** | Microchip PIC18F25K80 (Real crystal) | Bluetooth SPP / BLE / Wi-Fi | ISO 15765-4, KWP2000, ISO 9141-2 | Correctly answers `ATAL`, `ATAT1/2`, supports 500k CAN & K-Line | `HARDWARE_TEST_REQUIRED` | Most common functional clone in Iranian automotive market. |
| **ELM327 Clone v2.1 (Low Quality Fake)** | Cheap ARM / BK3231 / Fake MCU | Bluetooth Classic / BLE | Partial ISO 15765-4 CAN only (often fails KWP2000 & K-Line) | Rejects or returns `?` for `ATAL`, `ATAT`, `ATCAF0`, drops multi-frame packets | `IMPLEMENTED_IN_SOFTWARE` | Prober flags this as `POOR_CLONE`. Disables KWP2000 fast init with clear user warning. |
| **OBDLink SX / EX / LX / MX+** | STN1110 / STN1170 / STN2120 | Bluetooth Classic / BLE / USB | All standard OBD + SW-CAN + MS-CAN + High-speed CAN (up to 10 Mbps) | Responds to `STPBR`, `STPX`, `STPRS`, `STI` | `IMPLEMENTED_IN_SOFTWARE` | Premium adapter. Supported in `StnAdapter.kt`. |
| **Vgate iCar Pro / vLinker FS / MC+** | ARM Cortex-M / STN clone | BLE 4.0 / Bluetooth SPP / USB | ISO 15765-4, KWP2000, ISO 9141, UDS | Fast buffer (up to 2048 bytes), supports high baud rates (115200 - 500000 bps) | `HARDWARE_TEST_REQUIRED` | Excellent compatibility with both Android and iOS. |
| **Viecar BLE 4.0 / Dual Mode** | BK3432 / Realtek BLE | BLE (Service `FFF0` / Char `FFF1`) | ISO 15765-4 CAN, partial K-Line | Standard ELM command set over custom BLE GATT service | `IMPLEMENTED_IN_SOFTWARE` | Supported via `BleTransport.kt`. |
| **Generic USB-to-UART FTDI / CH340 / CP2102** | FTDI FT232R, WCH CH340, Silicon Labs CP210x | Android USB Host (OTG) | Protocol dictated by attached ELM/STN controller | USB Host permissions and baud rate configuration | `IMPLEMENTED_IN_SOFTWARE` | Low-latency serial streaming. |
| **Virtual Isolated Simulator** | Software Engine (Deterministic) | Internal Coroutines Channel | All protocols, simulated Iranian ECU PID set | Labeled `SIMULATOR_ONLY` | `SIMULATED_VERIFIED` | For offline UI testing and unit testing. |
