# Changelog

All notable changes to this extension are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project uses semantic versioning.

## [1.3] — 2026-04-18

### Added
- AVG and MAX now read directly from Karoo's own `AVERAGE_HR` / `MAX_HR` / `AVERAGE_POWER` / `MAX_POWER` streams, so the values shown on the graph field match other data fields on the page that display the same metric.
- Ride-state awareness: while the ride is paused (manual or auto-pause via `RideState.Paused`), new samples are no longer appended to the curve.

### Changed
- Sample buffer and power smoother are now held on the `DataTypeImpl` instance and collection runs on a service-scoped coroutine. Theme changes (dark/light) no longer restart the collector, so the already-drawn curve survives the switch.
- Re-render trigger is a `StateFlow` signal from the collector to the view, decoupling collection from the view lifecycle.

### Fixed
- Dark/light theme toggle wiped the already-drawn curve (curve was bound to the view's coroutine scope, which was torn down on configuration change).
- Curve continued to be extended during auto-pause, making the "Full" view misrepresent the actual ride.

## [1.2] — 2026-04-18

### Added
- Tap on a zone graph field cycles the time window (1 min → 5 min → Full). Implemented via `RemoteViews.setOnClickPendingIntent` on the ImageView; a broadcast reaches the extension service, which advances the per-field `StateFlow<TimeWindow>`.

### Changed
- HR and Power fields now hold **independent** time-window state — toggling one does not affect the other.
- Graph renderer dynamically shrinks the current value text when a narrow field layout would cause it to collide with AVG/MAX.
- Window label (1min/5min/Full) is centered above the graph between the current value and the AVG/MAX column; it is omitted if the available space is too tight rather than overlapping other elements.

### Removed
- Hardware-button-mapped `BonusAction`s for window toggling (replaced by the tap gesture).

### Fixed
- `onBonusAction` override compile error on SDK < 1.1.7 (now moot — BonusActions removed).
- Overlap of the current value and AVG/MAX at certain field heights/widths.

## [1.1] — 2026-04-18 (internal, never shipped)

### Added
- Karoo service integration: `HrPowerExtension` service + `HrZoneGraphDataType` / `PowerZoneGraphDataType` data types.
- Live streaming of `HEART_RATE`/`POWER` values combined with `HR_ZONE`/`POWER_ZONE` classification.
- 3-second rolling average for power (`PowerSmoother`).
- Experimental `BonusAction` declarations (later dropped in 1.2 — see above).

### Changed
- Bumped karoo-ext SDK from 1.1.3 to 1.1.7.

## [1.0] — initial

- Rendering pipeline: `GraphGeometry`, `GraphRenderer`, `ZoneColors`, `DataBuffer`, `TimeWindow`, `Sample`, `SyntheticData`.
- Two graphical data fields (HR and Power) with Garmin-style layout: current value with icon, AVG/MAX column, window label, zone-colored curve with filled area.
- Compose preview (`MainScreen`) with synthetic data.
- JUnit tests for `GraphGeometry`.
