# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Before 1.0 the public API may change in
any release.

## [Unreleased]

## [0.1.0] - 2026-07-16

Initial release.

### Added

- `Diorama { }` composable: wrap a Compose app to run it inside a simulated device on a real device,
  with a runtime panel for switching the device, orientation, font scale and dark mode while the app
  keeps its state.
- Configuration override that reports the simulated device correctly: explicit `densityDpi`,
  `screenWidthDp`, window size class and font scale, through constrain-then-scale rather than a fit
  ratio.
- An editable custom device (width, height, density) and a device catalog transcribed from the
  Android SDK's own device definitions.
- Two modules: `:diorama-frame` (multiplatform device model, catalog and scaling primitive) and
  `:diorama` (Android override engine, panel and state).

[Unreleased]: https://github.com/matheuslutero/diorama-compose/compare/0.1.0...HEAD
[0.1.0]: https://github.com/matheuslutero/diorama-compose/releases/tag/0.1.0
