# System Audit — Macsense-2

## What the app does
MacSense ("Master Codex") is a native Android app (Kotlin, Jetpack Compose) positioned as a vertical DAW / sound-breeding studio: audio capture, DSP analysis (FFT, loudness/K-weighting, true-peak, pitch/onset/tempo detection), mastering controls, a lyrics studio, a vocal scanner, and an AI assistant ("Ari") that issues structured commands (BPM/lyrics/preset/effects changes) parsed from Gemini model responses.

## Architecture
- **UI**: Jetpack Compose screens (`FlowCaptureScreen`, `MasteringScreen`, `VerticalDawScreen`, `LyricsStudioScreen`, `VocalScannerScreen`) driven by `ViewModel`s holding `StateFlow` state.
- **Navigation**: Single-activity, Compose Navigation (`MacSenseNavHost`, `Routes`).
- **DSP**: Custom Kotlin signal-processing package (`dsp/`) — FFT, biquad filters, loudness/true-peak meters, pitch/onset/tempo estimators.
- **Persistence**: Room database (`MacSenseDatabase`, versioned schema at v2, with a `MIGRATION_1_2`), a thin `MacSenseRepository`, manual DI via a single `AppContainer` created in `MacSenseApplication`.
- **AI integration**: Retrofit/OkHttp client (`RetrofitClient`) calling the Gemini `generateContent` REST endpoint directly, with the API key passed as a query parameter; responses parsed for an embedded `<ari_command>` JSON block.
- **Build**: Gradle Kotlin DSL, AGP with `compileSdk/targetSdk 35`, `minSdk 26`, Compose BOM, Room + KSP, `secrets` Gradle plugin backed by `.env`/`.env.example`.

## Dependencies & build system
Standard AndroidX/Compose/Room/Retrofit/OkHttp/kotlinx-serialization stack, no dependency version pinning beyond the Gradle version catalog (`libs.versions.toml`), no lockfile.

## Deployment model / environments
No distinct staging/production build flavors; only default `debug`/`release` build types exist, and `release` has no signing config wired up (comment notes it must be added via CI secrets). There is no distribution pipeline (Play Console, internal app sharing, or APK signing) configured anywhere in the repo.

## Data stores
Local-only: Room/SQLite on-device database (`macsense_db`) holding `ProjectEntity` records. No remote backend, no server-side persistence.

## Background jobs / external integrations
No `WorkManager` or scheduled jobs found. The only external integration is the Gemini generative-language API, called synchronously from view models via Retrofit.

## Secrets / config strategy
API key sourced from `.env` (gitignored) via the Gradle `secrets` plugin, exposed at build time and passed as a URL query parameter (`?key=...`) on every Gemini request — this is a hardening-relevant detail we address below.

## Test coverage
Unit tests exist for most DSP primitives (`FftTest`, `KWeightingTest`, `LoudnessMeterTest`, `OnsetDetectorTest`, `PitchDetectorTest`, `QuantizerTest`, `SpectrumAnalyzerTest`, `TempoEstimatorTest`, `TruePeakMeterTest`), the repository, and `DawViewModel`, plus two custom architecture tests (`NoLlmMeasurementTest`, `NoStubTest`). Instrumented tests cover DAO/migration and basic UI navigation/reachability. No tests exist for `MasteringViewModel`, `FlowCaptureViewModel`, `GeminiApi`/`RetrofitClient`, or `AriCommandParser`.

## CI/CD status (before this change)
No `.github/workflows` directory existed — there was no automated build, test, lint, or security scanning on push/PR. This is the single largest production-readiness gap.

## Monitoring/logging status
No structured logging, crash reporting, or analytics SDK is present anywhere in the manifest or dependency list.

## Auth model
None — the app has no user accounts, sign-in, or authorization boundaries; all state is local to the device.

## User-facing critical flows
Audio capture → DSP analysis/mastering → AI-assisted editing (Ari) → project persistence. The Gemini call is the only network dependency and a single point of failure for the "Ari" assistant flow.

## Major risks
1. API key transmitted as a URL query parameter (`?key=`) rather than a header — more likely to leak into logs, proxies, and crash reports.
2. No release signing, no CI, no security scanning prior to this branch.
3. `RECORD_AUDIO` permission is declared but there is no visible runtime permission request/handling code reviewed in this audit — needs verification.
4. No crash reporting or structured logs, so production incidents would be invisible to the team.
5. Single hard-coded model string (`gemini-3.5-flash`) with no fallback/retry/timeout-aware error handling visible in the API layer beyond basic OkHttp timeouts.

## Assumptions
This is an AI Studio–scaffolded Android app (per `metadata.json`'s `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` flag) intended for direct Play Store or sideload distribution rather than a client-server web service; "production" here means a stable, signed, monitored mobile release rather than a hosted backend.

## Unknowns / blockers
Play Console / signing keystore ownership, crash-reporting vendor preference (Crashlytics vs. Sentry), and whether the Gemini key should be proxied through a backend to remove it from the client entirely (recommended, but out of scope for a client-only repo without a backend).
