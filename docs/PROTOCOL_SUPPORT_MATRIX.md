# Protocol support and boundaries

Current product truth is [the Sprint4 matrix](../OBD_SUPPORT_MATRIX.md). A helper engine or catalog entry is not verified vehicle compatibility.

| Protocol/family | Current runtime / limitation |
|---|---|
|ELM AUTO, ISO9141-2, ISO14230-4, J1850, CAN, J1939 selector A|Explicit supported ELM selector/readback; read-only supported OBD payload paths; DEVICE_REQUIRED / VEHICLE_REQUIRED|
|CAN11/29-bit responder envelopes|Observed responders retained, nullable without headers; strict framing; no live arbitrary header routing|
|ISO-TP|Standalone reassembly helpers tested; raw FF/CF live path unsupported; bounded numbered CAF1 Mode06/09 supported|
|UDS|Standalone parser/builders only; security/write/programming/session-changing live commands denied|
|KWP2000|Standalone engine plus ELM read-only path; timing/checksum helpers do not prove ECU compatibility|
|J1939|Selector A and standalone ID/DM1 helpers; no complete heavy-duty diagnostic workflow claim|
|Motorcycles / Iranian OEM catalogs|Reference metadata only; no vehicle-specific protocol or manufacturer diagnostic certification|

No physical bench measurements were obtained in Sprint4. Unknown protocol readback and unsupported selectors fail closed. Manufacturer labels, baud-rate tables and source code alone do not validate an adapter/vehicle pairing.
