# Sprint5 localization inventory

Scope: Dashboard, Diagnostics/history, readiness, Mode09, Mode02, Mode06, recordings/replay, connection/profile editor, settings, terminal, inspector, simulator options, trip, maintenance, onboarding, runtime access controls, foreground notification and shared chart/connection components.

`python3 tools/check_localization.py` verifies matching EN/FA string/plural/array keys and format signatures, with no duplicate keys. `python3 tools/inventory_ui_strings.py` produces a repeatable heuristic JSON inventory for code review; it does not assert that every runtime catalog string is translated. Final validation found410 matched resource entries (including plural variants and array items).

| Surface | Current handling | Remaining validation |
|---|---|---|
| Ordinary controls/status/error guidance | EN/FA resources; typed diagnostic/connection status; developer details expandable | EN/default-size and FA/900×2000/font1.3 emulator navigation executed; full TalkBack and exhaustive contrast remain partial |
| Onboarding/fixture choices | Resource arrays or bilingual fixture metadata; editable selections are user configuration | Existing user text stays as entered, not silently translated |
| Shared chart/connection animation | Resource labels; no inference of headlights/ignition/failed fuse from connection state | Emulator rendering inspected; animation is illustrative, not physical evidence |
| VIN/CVN/DTC/PID/hex/responder IDs | Kept unchanged; diagnostic identifier component uses LTR text direction | Emulator screenshots include technical identifiers; physical-device/large-catalog bidi remains partial |
| Vehicle names | Existing bilingual names selected by current locale | Catalog names/notes without verified translations remain partial |
| Raw parser/adapter/driver details and derivation methods | Developer evidence, source text preserved | Not presented as localized ordinary guidance or verified manufacturer diagnosis |
| Compatibility DashboardState.vehicleDisplayLabel helper | Historical Persian fallback retained for existing non-UI callers/tests; dashboard uses localized resource fallback | Future helper cleanup is optional |
| Units | Live display speed/distance/temp/pressure, derived fuel US units, trip and maintenance distance | Device consistency; reference specifications can retain explicit canonical source units |

Activity/process recreation and offline language resources were retained from Sprint4. Host checks alone do not validate rendering. The runtime continuation executes EN/FA emulator permission, theme, locale recreation and process restart flows; it does not establish physical-device or vehicle compatibility. See [device checklist](SPRINT5_BENCH_CHECKLIST.md).
