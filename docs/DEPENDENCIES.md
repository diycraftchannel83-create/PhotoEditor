# Dependency and License Inventory

**Status: source inventory only — not a verified legal/licensing report.** Build, dependency resolution, SBOM generation, license scanning and vulnerability scanning have not been run during the feature implementation pass.

## Direct dependencies from `app/build.gradle.kts`

| Dependency | Version | Role | License status |
|---|---:|---|---|
| AndroidX Core KTX | 1.15.0 | Android compatibility helpers | Confirm from official artifact metadata before release |
| AndroidX Activity Compose | 1.10.0 | Activity and Compose integration | Confirm from official artifact metadata before release |
| AndroidX Lifecycle Runtime KTX | 2.8.7 | Lifecycle/coroutines integration | Confirm from official artifact metadata before release |
| AndroidX Lifecycle ViewModel Compose | 2.8.7 | Compose ViewModel integration | Confirm from official artifact metadata before release |
| Compose BOM | 2025.01.00 | Aligns Compose library versions | BOM is a version platform; audit resolved artifacts |
| Compose UI | BOM-managed | UI primitives | Confirm resolved artifact metadata |
| Compose UI Tooling Preview | BOM-managed | Preview support | Development dependency status; confirm resolved artifacts |
| Compose Foundation | BOM-managed | Layout, gestures, scrolling | Confirm resolved artifact metadata |
| Compose Material 3 | BOM-managed | Material components | Confirm resolved artifact metadata |
| Compose Material Icons Extended | BOM-managed | Icon set | Icon licensing/attribution review required |
| Compose Runtime | BOM-managed | State/runtime | Confirm resolved artifact metadata |
| Compose UI Graphics | BOM-managed | Graphics | Confirm resolved artifact metadata |
| Compose UI Tooling | BOM-managed | Debug tooling | Development-only; confirm resolved artifact metadata |
| ML Kit Selfie Segmentation | 16.0.0-beta6 | On-device person segmentation | Review Google ML Kit terms and beta status before production |
| JUnit | 4.13.2 | Unit testing | Test-only |
| Robolectric | 4.14.1 | Android unit-test simulation | Test-only |
| Compose UI Test Manifest | BOM-managed | Debug test support | Debug-only |

## Build plugins

- Android Gradle Plugin: 8.7.3
- Kotlin Android: 2.0.21
- Kotlin Compose plugin: 2.0.21
- Java/Kotlin target: 17

## Release audit actions

- [ ] Resolve the full dependency graph (including transitive dependencies).
- [ ] Export SBOM (CycloneDX or SPDX).
- [ ] Run automated license scan and review every result.
- [ ] Confirm Material Icons Extended attribution/licensing obligations for included icon artwork.
- [ ] Confirm ML Kit beta dependency is production-suitable and document its terms.
- [ ] Run vulnerability scanning against resolved versions.
- [ ] Pin/lock dependency resolution where the build setup supports it.
- [ ] Generate and ship a third-party notices file if required.
- [ ] Repeat the audit on the exact release build.

## Network/privacy note

The current Android manifest contains no explicit Internet permission. The app's stated design is offline-first; however, this must be verified against the resolved dependency graph, Android runtime behavior and release build before making a definitive privacy claim.



## 20 November 2026 — release audit remains open

No resolved dependency graph, SBOM, license scan or vulnerability scan has been executed. The source-level list above is not sufficient for production approval. ML Kit Selfie Segmentation remains a beta dependency that requires terms and suitability review. Privacy claims must remain limited until runtime, packaged permissions and third-party behavior are checked against the release artifact.
