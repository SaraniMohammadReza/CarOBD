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
