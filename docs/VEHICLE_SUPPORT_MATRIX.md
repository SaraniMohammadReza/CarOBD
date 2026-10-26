# Vehicle Support Matrix

This matrix documents the ECU architectures, communication protocols, diagnostic addresses, and verification maturity for Iranian domestic, Chinese assembly, and motorcycle platforms.

| Manufacturer | Model / Platform | Engine | ECU Model & Architecture | Hardware Protocol | Diagnostic Address / Header | Maturity Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **IKCO** | Peugeot 206 Type 2/3/5/6 | TU3JP / TU5JP4 | Bosch ME7.4.4 / ME7.4.5 | KWP2000 (ISO 14230-4) Fast Init / CAN | `0x11` (K-Line) / `0x7E0` (CAN) | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Peugeot 206+ / 207i | TU5 / TU5P | Crouse / Kesens CAN / EasyU 2.5 | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Samand / Soren | XU7 / EF7 NA / EF7 TC | Bosch ME7.4.9 (CNG/Petrol Bi-Fuel) / Siemens VDO | KWP2000 / ISO 15765-4 CAN | `0x11` / `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Dena / Dena+ Turbo | EF7 TC / EFP | Bosch ME7.4.9 / Kesens Turbo CAN | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Tara | TU5P | Crouse / Kesens CAN | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Pars | XU7 / XU7P / TU5 | Siemens SIM2K / SSAT / Crouse | KWP2000 / ISO 15765-4 CAN | `0x12` / `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **IKCO** | Paykan / Roa / Arisun | OHV / OHVG / XU7 | Valeo S2000 / Siemens VDO / SSAT | KWP2000 (Fast & 5-Baud) | `0x12` | `IMPLEMENTED_IN_SOFTWARE` |
| **SAIPA** | Pride (111/131/132/141) | M13 NA | Siemens VDO / SSAT / Valeo S2000 | ISO 9141-2 / ISO 14230-4 KWP2000 | `0x12` | `IMPLEMENTED_IN_SOFTWARE` |
| **SAIPA** | Tiba / Saina / Quik | M15 1.5L | Siemens SIM2K / SSAT / EasyU 2.5 | KWP2000 / ISO 15765-4 CAN | `0x12` / `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **SAIPA** | Shahin | M15-TC (1.5L Turbo) | Bosch ME17 / Crouse Kesens Turbo | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **SAIPA** | Atlas / Sahand | M15GSI (VVT) | Crouse / Kesens CAN | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Pars Khodro** | Tondar 90 (L90) / Sandero | K4M 1.6L 16V | Siemens EMS3132 | KWP2000 (ISO 14230-4 Fast Init) | `0x7A` | `IMPLEMENTED_IN_SOFTWARE` |
| **Pars Khodro** | Brillance H320/H330 | BM15L 1.5L | Delphi MT22U / Bosch ME7.8.8 | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Bahman** | Dignity / Fidelity | 1.5L Turbo | Bosch MED17.8.10 / Delphi MT92.1 | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **KMC / JAC** | KMC J7 / T8 / K7 | 1.5L Turbo / 2.0L Turbo | Delphi MT92 / Bosch MED17.8.10 | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Modiran (MVM/Chery)** | Arrizo 5/6, Tiggo 7/8 | 1.5T / 1.6TGDI / 2.0TGDI | Bosch MED17 / ME17.8.8 | ISO 15765-4 11-bit 500k | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Motorcycles** | Honda CG125 EFI / CDI | 125cc - 200cc | Lifan / Yeson / Delphi EFI (Euro 4/5) | ISO 14230-4 K-Line / ISO 19689 6-pin | `0x12` / `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Motorcycles** | Benelli 150/180/250/302S | 150cc - 300cc Twin | Delphi MT05 / MT05.2 | ISO 14230-4 K-Line / Euro 5 ISO 19689 CAN | `0x12` / `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |
| **Motorcycles** | CF Moto 150/250NK | 250cc Single | Bosch MSE6.0 / MSE8.0 | ISO 15765-4 CAN / Euro 5 6-pin OBD | `0x7E0` | `IMPLEMENTED_IN_SOFTWARE` |

### Evidence Status Definitions
- `IMPLEMENTED_IN_SOFTWARE`: Code mappings and parsing rules written in `:core:vehicle` and verified via unit tests.
- `HARDWARE_TEST_REQUIRED`: Physical connection to the physical car/motorcycle port with an ELM327 or STN adapter is needed to validate timing parameters (P1-P4 timing on K-Line).
- `COMMUNITY_REPORTED`: Diagnostic parameters collected from mechanics and field technicians.
- `OEM_VERIFIED`: Cross-referenced with factory wiring schematics and workshop manuals.
