# OBD mode support

The current authoritative product matrix is [OBD_SUPPORT_MATRIX.md](../OBD_SUPPORT_MATRIX.md). Older Phase F helper coverage is not evidence of live product support.

| Mode | Product scope |
|---|---|
|01|Capability-gated live values and explicit readiness UI; per-reading evidence|
|02|Selected frame/base-page freeze-frame UI; partial supported metrics|
|03|Stored DTCs, responder/category evidence and complete-scan history|
|04|Physical disabled for safety; isolated simulator-local operation only|
|05|Not implemented; source evidence required|
|06|CAN capability-gated raw monitor records; no invented scaling/pass-fail|
|07|Pending DTCs, distinct category|
|08|Disabled for safety|
|09|Advertised information fields; strict layouts; broader non-CAN formats partial|
|0A|Permanent DTCs, distinct category|

All physical paths require device and vehicle validation. Raw live FF/CF reassembly is unsupported. Strict ELM CAF1 numbered responses are assembled only for supported Mode06/09 decoding. Standalone IsoTpEngine tests do not establish live raw routing support.
