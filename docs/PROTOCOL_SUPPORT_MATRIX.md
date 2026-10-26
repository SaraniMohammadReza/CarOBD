# Protocol Support Matrix

This matrix tracks every automotive communication protocol implemented in CarObd, detailing standard references, module location, functional status, and test coverage.

| Protocol | Standard | Implementation Class | Multi-Frame Support | Baud Rates | Status | Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **ISO 15765-4 (CAN 11-bit / 500k)** | ISO 15765-4:2021 | `:core:protocol` `IsoTpEngine` & `PidCodec` | Flow Control, Consecutive Frames (Reassembly up to 4095 bytes) | 500 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 15765-4 (CAN 29-bit / 500k)** | ISO 15765-4:2021 | `:core:protocol` `IsoTpEngine` & `PidCodec` | Flow Control, Consecutive Frames | 500 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 15765-4 (CAN 11-bit / 250k)** | ISO 15765-4:2021 | `:core:protocol` `IsoTpEngine` & `PidCodec` | Flow Control, Consecutive Frames | 250 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 15765-4 (CAN 29-bit / 250k)** | ISO 15765-4:2021 | `:core:protocol` `IsoTpEngine` & `PidCodec` | Flow Control, Consecutive Frames | 250 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 14230-4 KWP2000 (Fast Init)** | ISO 14230-4:2000 | `:core:protocol` `Kwp2000Engine` | Header + Length + Service + Data + Checksum (NRC mapping) | 10.4 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 14230-4 KWP2000 (5 Baud Init)** | ISO 14230-4:2000 | `:core:protocol` `Kwp2000Engine` | Header + Length + Service + Data + Checksum | 10.4 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 9141-2** | ISO 9141-2:1994 | `:core:protocol` `Kwp2000Engine` & ELM AT SP 3 | Byte stream checksum verification | 10.4 kbps | `IMPLEMENTED_IN_SOFTWARE` | Parser verified |
| **SAE J1850 PWM** | SAE J1850 | Handled via ELM327 AT SP 1 | 3-byte header, CRC | 41.6 kbps | `HARDWARE_TEST_REQUIRED` | Hardware dependent |
| **SAE J1850 VPW** | SAE J1850 | Handled via ELM327 AT SP 2 | 3-byte header, CRC | 10.4 kbps | `HARDWARE_TEST_REQUIRED` | Hardware dependent |
| **ISO 14229-1 (UDS over CAN)** | ISO 14229-1:2020 | `:core:protocol` `UdsEngine` | DiagnosticSessionControl (0x10), SecurityAccess (0x27), ReadDataByIdentifier (0x22), WriteDataByIdentifier (0x2E), RoutineControl (0x31) | 500k / 250k | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **SAE J1939 (Heavy Duty)** | SAE J1939-71/73 | `:core:protocol` `J1939Engine` | 29-bit CAN ID parsing: Priority, PGN, Source Address. DM1 active DTC decoder (SPN & FMI) | 250 kbps / 500 kbps | `IMPLEMENTED_IN_SOFTWARE` | `ProtocolEnginesUnitTest.kt` (Tested) |
| **ISO 19689 (Euro 5 Motorcycles)** | ISO 19689:2016 | `:core:protocol` `PidCodec` & `IsoTpEngine` | 11-bit CAN 500 kbps / K-Line motorcycle diagnostic interface | 500 kbps / 10.4k | `IMPLEMENTED_IN_SOFTWARE` | Mapped in IranianEcuCatalog |
