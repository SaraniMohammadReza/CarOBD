# Remaining CarOBD work

Read [the canonical handoff](AGENT_CONTINUATION_HANDOFF.md) for actual current Git state, test evidence and exact next steps. [Competitor matrix](COMPETITOR_FEATURE_MATRIX.md) qualifies product scope. This list does not authorize physical vehicle operations.

- P0 device/adapter verification: real permission revocation/regrant, USB detach/exact interface, BLE subscription/late callbacks, TCP loss and foreground lifecycle. Existing guards are software tested; no physical compatibility claim.
- P1 combined diagnostic scope: persistent reports currently retain03/07/0A only. Integrate existing typed readiness/MIL,02,09,06 readers with expected-session ownership and independent partial outcomes. No fake depth selector or health verdict.
- P1 report archive scale: currently loads up to100 full reports, including raw evidence. Add lazy details/paging without deleting historical evidence or silently truncating export.
- P1 dashboard: edit style of existing widgets; broaden selection only after canonical decoder, units, support evidence and polling tests. No catalog-to-wire shortcuts or additional producer.
- P2 provenance-aware maintenance, explicit manual fuel ledger, measured/estimated alerts and contextual help. Existing MAF fuel estimates are not direct fuel measurements.
- P2 full application backup/restore is not implemented. Report/transcript JSON exports are evidence documents. Automatic OS cloud backup is excluded; modern user-directed device transfer is a separate OS mechanism and requires device validation.
- P3 HUD/parking deferred; dashcam/monetization outside current scope. Physical clear, actuation, security/programming, arbitrary manufacturer commands and unrestricted scripts/plugins remain rejected.

Company-machine dependencies are not in Git: prepare its own persistent cache, compatible JDK and SDK with [offline setup](ANDROID_STUDIO_OFFLINE_SETUP.md). Home Android Studio Sync success does not establish company-machine cache readiness. The separate Studio adb5037 Device Manager/Run issue remains open; emulator QA uses adb5038.
