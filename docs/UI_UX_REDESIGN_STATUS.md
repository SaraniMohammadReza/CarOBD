# CAROBD — UI/UX RECONSTRUCTION STATUS

## 1. Executive Summary & Defect Remediation

Prior to this UI/UX reconstruction sprint, CarOBD suffered from several structural interface defects:
- Arbitrary per-screen design drift: inconsistent paddings, hardcoded colors, and lack of standardized typography/radii tokens.
- Navigation clutter: diagnostics, raw terminal, and secondary utilities competed equally with primary ownership and maintenance features.
- False health assertions: screens previously displayed misleading "Vehicle Healthy" claims in the absence of verified schedules.
- Text overflow and broken RTL: dense English and Persian strings wrapped into tall multi-line button labels on narrow mobile screens (320dp/360dp) and high font scales (130%).
- Runtime crash risk: unhandled constructors in spec sheets (`VehicleSpecSheet`) and missing compose-context scoping in DI.

## 2. What Was Redesigned (Checkpoint UI-1 & UI-2)

### A. Canonical Design System (`CarObdDesignSystem.kt`)
- **Spacing Tokens (`Spacing`)**: Standardized scale (`micro = 4.dp`, `compact = 8.dp`, `medium = 12.dp`, `standard = 16.dp`, `wide = 20.dp`, `section = 24.dp`, `major = 32.dp`).
- **Corner Radii (`CornerRadius`)**: Standardized corner radii and shapes (`small = 8.dp`, `medium = 12.dp`, `large = 16.dp`, `xlarge = 24.dp`, `shapePill = CircleShape`).
- **Semantic Colors (`SemanticColors`)**: Restrained automotive color hierarchy:
  - Critical / Red: Genuine errors and overdue critical services only.
  - Warning / Amber: Services due soon and unverified safety items.
  - Success / Green: Confirmed valid/completed items (never fabricated vehicle health).
  - Info / Blue: Navigation states and active information pills.
  - Neutral / Gray: Unknown or unconfigured items.
- **Elevation Tokens (`Elevation`)**: Canonical surface elevation scale (`none = 0.dp`, `card = 1.dp`, `elevated = 2.dp`, `dialog = 6.dp`).
- **Component Sizing (`CarObdSizes`)**: Canonical minimum touch targets (48dp), standardized icon sizes (14dp micro, 18dp small, 24dp medium, 32dp large, 48dp hero), and 48dp button height.

### B. Reusable Shared UI Components (`SharedCarObdComponents.kt`)
- `CarObdTopBar`: Standardized header with back navigation and action slot.
- `VehicleContextHeader`: Active owned vehicle hero with known facts (make, model, year, engine, odometer) and vehicle switcher action.
- `SectionHeader`: Clean semantic category headers with optional action link.
- `PrimaryFeatureCard`: Prominent destination card with icon, title, multi-line subtitle, status chip, and chevron.
- `SecondaryActionCard`: Compact 2-column grid cards for secondary diagnostic and utility surfaces.
- `CompactStat`: KPI badge counter for overdue/due soon/parts statistics.
- `StatusChip`: Semantic status indicator chip.
- `EmptyState`: Truthful empty container with actionable button.
- `ErrorState`: Standardized error presentation with retry action.
- `CarObdCard`: Standardized surface wrapper with elevation and clip.
- `CarObdPrimaryButton` & `CarObdSecondaryButton`: Accessible buttons respecting 48dp touch targets and progress state.
- `ProvenanceBadge`: Auditable data provenance badge distinguishing OEM verified from community and unverified claims.

### C. Ownership Home Screen (`HomeScreen.kt`, `HomeMvi.kt`)
- **Top Area**: Active vehicle context header displaying only factual known metadata (year, make/model, engine code, odometer in km).
- **Primary Product Area**:
  1. *My Vehicle*: Specifications, profile details, and variant.
  2. *Maintenance Center*: Dynamic summary of overdue, due soon, or upcoming services; shows truthful empty state (`UNSCHEDULED` / "No maintenance schedules configured yet") when no schedules exist—never claims "Everything is healthy".
  3. *Parts & Consumables*: Compatible parts counter and verified confidence indicator (`VERIFIED_OEM` or `COMMUNITY`).
- **Secondary Diagnostics & Live Data Area**:
  - Diagnostics (Scan & view DTCs)
  - Live Gauges (RPM, speed, telemetry)
  - Connection Center (Adapter pairing & setup)
  - Diagnostic Reports (Saved scan reports & history)
- **Bottom Resources Area**:
  - User Guide (Automotive knowledge base)
  - Settings (Language, units & preferences)
- **Vehicle Switcher Modal Dialog**: Quick dialog to switch between registered owned vehicles.

### D. Offline Automotive User Guide (`GuideScreen.kt`, `DefaultGuideRepository.kt`)
- Bilingual knowledge base organizing automotive topics into sections: Getting Started, Ownership, Maintenance, Parts, Diagnostics, Telemetry, and Records.
- Expandable topic cards with rich content blocks (paragraphs, alerts, bullet lists, key-value specifications).
- Search filter supporting English and Persian queries.

## 3. Responsive & RTL Behavior

- **Widths Handled**: Explicitly designed and smoke-tested on 360dp phone hardware (Samsung Galaxy Pie 9.0) and tested against 320dp, 393dp, 411dp constraints.
- **RTL Support**: Full bidirectional localization using Android resources (`values` and `values-fa`). All primary headers, subtitles, badges, and icons mirror properly in Persian RTL (`ldrtl`).
- **Font Scaling (130%)**: Text elements enforce single-line or controlled multi-line constraints with `TextOverflow.Ellipsis`, preventing tall wrapped button labels.
- **Touch Accessibility**: All buttons, chips, and cards maintain minimum 48dp touch target boundaries.

## 4. Verification Evidence

- **Unit Tests**: Full test suite passed across all 6 modules (525+ JVM unit tests, 0 failures, 0 errors, 0 skipped).
- **Compilation & Dex**: Clean assembly `:app:assembleDebug` completed with 0 errors.
- **Lint**: `:app:lintDebug` completed with 0 errors.
- **Duplicate Class Check**: `:app:checkDebugDuplicateClasses` passed with 0 conflicts.
- **Live Device Smoke Test**: Verified interactively on connected Android device (id `9889db474842494948`, Android 9 Pie, 360dp RTL). Navigated through Home, My Vehicle, Maintenance, Diagnostics, and More dialog. Confirmed 0 crashes and complete absence of the `VehicleSpecSheet` NoSuchMethodError.

## 5. Remaining Roadmap

- **UI-3**: My Vehicle + Vehicle Details (full specs viewer, vehicle editing flow, variant picker).
- **UI-4**: Maintenance Center (redesign schedule cards, service history, and interval manager with design system tokens).
- **UI-5**: Parts Center + Part Details (redesign parts catalog list, fitment warning cards, and illustration banners).
- **UI-6**: Diagnostics (redesign DTC scan results, severity badges, and read-only safety guard notices).
- **UI-7**: Live Dashboard / Sensor Picker / Telemetry Graphs (redesign gauges, widget configuration, and real-time cadence).
- **UI-8**: Connection Center (redesign adapter scanner, transport cards, and stage visualizer).
- **UI-9**: Settings / User Guide / History / Reports (complete knowledge base expansion, SAF report viewer).
- **UI-10**: Global Responsive + RTL + Accessibility Audit Pass.

## UI-2 Checkpoint Update (2026-10-31)
- **Status:** COMPLETED and LIVE-DEVICE VERIFIED
- **Key Deliverables:**
  1. Dashboard text collapse & layout defects repaired: CircularGauge, DigitalMetricCard, and HorizontalBarGauge now enforce maxLines = 1 with TextOverflow.Ellipsis and display titles cleanly outside needle pivots; custom widgets pair into clean 2-column rows on 360dp phone; raw unstyled text removed from top of Dashboard.
  2. Trip Screen layout repaired: Unstyled raw texts encapsulated in structured warning/info card; bottom clearance spacer added to prevent bottom navigation bar overlap.
  3. Connection Center reconstructed: Prominent 100% Read-Only Safety Guarantee banner added; multi-step connection progress visualizer; explicit '+ Add Diagnostic Adapter' wizard with transport type hints and Wi-Fi default configuration (192.168.0.10:35000).
  4. Diagnostics Disconnected Recovery: Tapping scan while disconnected presents an actionable alert dialog; disconnected empty state offers 3 direct recovery paths (Connect Adapter, Use Simulator, Learn OBD-II).
  5. Simulator Polish: Prominent synthetic simulation explanation banner added; connection button integrated directly into status card.
  6. Automotive Splash & First-Run 3-Step Onboarding: Headlights illumination reveal animation; 3-step progressive onboarding (Overview, Iranian Vehicle Selection, Safety & Diagnostic Options) with persistence via AppPreferencesManager.
- **Device Verification:** Live on Samsung Galaxy (9889db474842494948) running Android 9 Pie in Persian RTL. 0 crashes, 0 NoSuchMethodError, responsive layouts across all 360dp screens.

## HOME-R1 — reconciliation

Home draft preserved locally at `73e0dd8` on `home/ownership-preservation`; development based on company `d8b1f2d` in sibling worktree. See HOME_STABILIZATION.md for every file decision. No duplicate ownership architecture ported. Removed unsupported 100% safety claims and invalid percent formatting; physical safety code unchanged. Correctness stabilization is now active; earlier completion/runtime claims are historical.

## HOME-R3/R5 — durable reminders and transactional history

SQLite v8 adds nullable record.schedule_id (old records never guessed), original manual baseline, persisted snooze bound to schedule revision, and owned-vehicle archive flag. Latest linked event by performed date drives schedule; editing/deleting it recomputes baseline, reverting to manual baseline only when all linked events are gone. Odometer is monotonic and is not rewound by historical corrections. Record completion writes history, baseline and odometer in one SQLite transaction; injected update failure rolls everything back.

Normal reminder reconciliation respects persisted snooze across reload/recreation. Completion invalidates the old snooze and schedules every recalculated schedule, including future time warnings. Mileage-only OK schedules keep their persisted facts but have no invented wall-clock trigger; owner odometer edits reevaluate. WorkManager timing is best effort, not exact. Worker rereads current vehicle/schedule/snooze and closes its helper.

Deletion policy: archive owner, clear selection, retain schedules/history under the original owner id, delete snooze metadata, cancel tagged work and visible schedule notifications. Archived vehicles cannot be selected or resurrected by stale edits; workers ignore them. Deleting a schedule unlinks retained history rather than guessing another association.

Validation: targeted Reminder/Maintenance JVM PASS; Android API37 emulator actual SQLite/repository/migration/WorkManager tests 11/11 PASS, exit0. Final follow-up reran 5 stabilization Android tests plus targeted JVM, exit0. Localization 619 entries PASS; diff check PASS. No physical adapter/vehicle operation.

## HOME-R4 — canonical onboarding and cold-start ownership

First-run setup now saves/selects canonical OwnedVehicleRepository only; it never writes legacy UserVehicleSetup or supplies sample engine/mileage/tank/fuel/price. Skip creates nothing. Shared draft parsing is used by both My Vehicle and onboarding; invalid supplied numbers fail, localized real zero remains zero, optional blanks remain null. Saved draft identity is stable across retries/recreation. Persistence failure cannot complete onboarding.

Home hydrates persistence as its first entry point, passes nullable odometer through unchanged, suppresses invented remaining mileage, observes maintenance revisions, and respects explicitly cleared selection. Maintenance and Parts also hydrate independently. Targeted Home/Onboarding/OwnedVehicle/Maintenance/Parts JVM PASS, exit0; UI runtime verification follows in R6. No physical diagnostic behavior changed.

## HOME-R6 — green stabilization gate and emulator evidence

Full offline gate PASS exit0, 1m05s: all six JVM modules, duplicate classes, both debug APKs and lint. Added actual UI smoke under API37 emulator emulator-5554 (sdk_gphone16k_x86_64): EN ownership flow PASS; FA/RTL 360dp/font130 ownership flow plus five database/reminder stabilization tests 6/6 PASS. Covered skip, canonical create with unknowns, real persisted Home hydration using a new repository, ambiguous 206 maintenance denial, unknown-engine search empty, explicit TU3 fitment, and unchanged diagnostic routing/disconnected state. Screenshots in machine-local home-stabilization-qa; tests are committed/reproducible. No physical hardware/vehicle validation. Earlier failed compile/lint attempts are retained in /tmp logs; final gate was rerun after fixes.

Resolution now also rejects ambiguous family prefixes (M15 does not imply M15 TC); only full codes or explicitly curated identity aliases may resolve. Legacy app/core/model/MaintenanceModels.kt is retained because MaintenanceDueCalculationTest still references it; it is not a duplicate of VehicleSpecSheet. No unsafe removal or physical diagnostic changes. Remaining UI polish and broad legacy navigation fixture modernization are separate from these targeted runtime checks.

## HOME-R6 follow-up — unknown is visible

Home no longer labels schedules with missing service evidence as up-to-date. Unknown parts context displays selection-required rather than a measured zero; Parts uses the same explanation. Vehicle header displays unknown odometer as — and preserves actual zero. Home targeted tests, English actual UI assertions and lint PASS (exit0, 1m06s); localization624 and diff check PASS. This completes visible truthfulness alongside nullable domain facts.
