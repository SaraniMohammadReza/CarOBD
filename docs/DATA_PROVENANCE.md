# Data Provenance & Evidence Ledger

This document catalogs every external technical specification, SAE/ISO standard, workshop manual, and ECU formula used in CarObd to ensure integrity, verifiable evidence, and strict conflict quarantine.

---

## 1. International Diagnostic Standards
- **ISO 15765-2:2016 / 2024**: Road vehicles — Diagnostic communication over Controller Area Network (DoCAN) — Part 2: Transport protocol and network layer services.
  - Used in: `IsoTpEngine.kt` (Single Frame, First Frame, Consecutive Frame, Flow Control frame reassembly and PCI extraction).
- **ISO 15765-4:2021**: Road vehicles — Diagnostic communication over Controller Area Network (DoCAN) — Part 4: Requirements for emissions-related systems.
  - Used in: `ObdProtocol.kt`, CAN baud configuration (500 kbps / 250 kbps, 11-bit / 29-bit CAN identifiers).
- **SAE J1979 / ISO 15031-5**: Diagnostic Test Modes (Mode 01 - Request Current Powertrain Diagnostic Data, Mode 02 - Freeze Frame, Mode 03 - Show Stored DTCs, Mode 04 - Clear Diagnostic Information, Mode 07 - Pending DTCs, Mode 09 - Vehicle Information).
  - Used in: `PidCodec.kt`, `Obd2Command.kt`. Formulas: RPM `((A * 256) + B) / 4.0`, Speed `A`, Coolant Temp `A - 40`, MAF `((A * 256) + B) / 100.0`, MAP `A` kPa.
- **ISO 14230-4:2000 (KWP2000)**: Keyword Protocol 2000 — Part 4: Requirements for emission-related systems.
  - Used in: `Kwp2000Engine.kt` (Format byte, Target Address, Source Address, Length, Service ID, Data Bytes, 8-bit Sum Checksum).
- **ISO 14229-1:2020 (UDS)**: Unified Diagnostic Services — Part 1: Application layer.
  - Used in: `UdsEngine.kt` (DiagnosticSessionControl 0x10, ECUReset 0x11, ReadDataByIdentifier 0x22, WriteDataByIdentifier 0x2E, SecurityAccess 0x27, RoutineControl 0x31).
- **SAE J1939-71 / J1939-73**: Heavy Duty Vehicle Applications and Diagnostics.
  - Used in: `J1939Engine.kt` (Parameter Group Numbers, 29-bit CAN identifiers, DM1 DTC parsing: Suspect Parameter Number [SPN] and Failure Mode Identifier [FMI]).
- **ISO 19689:2016**: Motorcycles and mopeds — Communication between vehicle and external equipment for emissions-related diagnostics.
  - Used in: `IranianEcuCatalog.kt`, `VehicleCatalog.kt` for Euro 5 motorcycle OBD-II 6-pin diagnostic interfaces.

---

## 2. Iranian Vehicle Technical Specifications & Workshop Manuals
- **IKCO (Iran Khodro Industrial Group)**:
  - *Technical Service Bulletin 206/207 TU3/TU5 Electrical Architecture* (IKCO Engineering Dept): Bosch ME7.4.4 / ME7.4.5 pinouts and K-Line diagnostic address `0x11`.
  - *EF7 NA and TC Engine Workshop Manual* (IPCO / IKCO): Bosch ME7.4.9 CNG/Gasoline bi-fuel map, 1.645L displacement, Total Quartz 7000 / 10W-40 SL/CF lubricant specification, Bosch FR8DE+ / Champion OE004 spark plug specifications (0.85mm - 0.90mm gap).
  - *XU7 / XU7P Engine Diagnostic & Maintenance Manual* (IKCO): Siemens VDO and SSAT pinouts, 1.761L displacement, 20W-50 SJ/SL (classic XU7) and 10W-40 SL (XU7P) oil grades, Eyquem RFC58LZ2E / NGK BKR6E spark plugs (0.90mm gap).
  - *TU5P & EC5 Engine Technical Guide* (IKCO Tara / Dena+): Kesens/Crouse CAN diagnostic ID `0x7E0`, Total Quartz INEO ECS / 5W-30 or 10W-40 SN specification, NGK LZKR6B-10E spark plugs (1.0mm gap).
- **SAIPA Group**:
  - *Pride & Tiba Service Training Manual* (Mega Motor / SAIPA): 1.3L M13 and 1.5L M15 engines, Siemens SIM2K and SSAT K-Line address `0x12`, 10W-40 SJ/SL engine oil (3.4L capacity), Champion RN9YC / NGK BPR6ES spark plugs (0.80mm gap).
  - *Shahin M15-TC Turbocharger Maintenance Manual* (SAIPA): 10W-40 SN Full Synthetic (high thermal stability), Torch / NGK Iridium spark plugs (0.75mm gap for boosted combustion chambers).
- **Pars Khodro / Renault**:
  - *Renault Megane & Logan K4M Workshop Manual (MR-388-LOGAN)*: Siemens EMS3132 ECU, K-Line address `0x7A`, ELF Evolution 700 ST / Total 10W-40 SL, Renault 7700500168 / Eyquem RFC58LZ2E (0.90mm gap).

---

## 3. Conflict Quarantine Policy
When conflict arises between community forum claims and official OEM documentation:
1. Official OEM workshop manuals take absolute precedence.
2. The contradictory claim is documented in the code comment with both sources cited.
3. The specification is tagged `COMMUNITY_REPORTED` or `OEM_VERIFIED` explicitly in `IranianEcuCatalog.kt` and `VehicleSpecDatabase.kt`.
4. The user-facing UI displays the provenance badge so the vehicle owner can make an informed decision.
