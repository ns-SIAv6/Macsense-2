# Phase 2 & 3 Closeout — Security Hardening + Observability & Reliability

Status: closing out per `macsense-2-full-readiness-plan-2.md`. This document maps each numbered
requirement from Phase 2 and Phase 3 to the commit/file that satisfies it, per Phase 0 Ground Rule 1
("every phase produces working code, passing tests, updated docs, and a merged PR").

## Phase 2 — Security Hardening

| # | Requirement | Status | Where |
|---|---|---|---|
| 1 | Move Gemini API key off URL query param | Done | `GeminiApiService` sends key via `x-goog-api-key` header instead of a query parameter |
| 2 | `network_security_config.xml` blocking cleartext | Done | `app/src/main/res/xml/network_security_config.xml`, wired via `android:networkSecurityConfig` in `AndroidManifest.xml` |
| 3 | Validate `GEMINI_API_KEY` at startup, fail fast | Done | `StartupValidator`, invoked from `MacSenseApplication.onCreate()` |
| 4 | Review `allowBackup` / add `dataExtractionRules` exclusions | Done | `app/src/main/res/xml/data_extraction_rules.xml` referenced from manifest; Room DB path excluded from cloud backup |
| 5 | Promote dependency-review CI job from advisory to blocking on release branches | Tracked | Requires branch-protection change in repo settings (human action — see Open Questions below) |
| 6 | Secret-rotation runbook validated end-to-end | Tracked | `RUNBOOKS.md` (Phase 1) contains the rotation steps; a staging run-through requires real staging credentials (human action) |

## Phase 3 — Observability & Reliability

| # | Requirement | Status | Where |
|---|---|---|---|
| 1 | Crash reporting behind `BuildConfig` flag | Tracked into Phase 4/backlog — requires choosing Crashlytics vs Sentry and adding `google-services.json`/DSN (human decision, see Open Questions) |
| 2 | Structured logging around every Gemini call | Done | `AppLogger` facade; request id, latency, success/failure, retry count logged around `GeminiApiService` calls in `DawViewModel` |
| 3 | Retry-with-backoff / circuit breaker on `generateContent` | Done | `RetryInterceptor` (OkHttp-level) + `withGeminiRetry()` coroutine helper wired into `DawViewModel`'s Ari call path |
| 4 | Tested destructive-migration fallback beyond `MIGRATION_1_2` | Done | `fallbackToDestructiveMigration()` path added and covered by a Room migration test |
| 5 | Unit tests for previously-untested surfaces | Done | `AriCommandParserTest`, `GeminiApiServiceTest` (MockWebServer), `StartupValidatorTest`, `AppLoggerTest` |
| 6 | Close-out gate | In progress | CI green on this branch; crash-reporting item requires the human decision in item 1 above before it can close |

## Branding pass (folded into this phase per user direction)

Applied the Macsense/Ari visual identity from the reference screens (dark purple-black canvas,
gold/amber accent, "MACSENSE AI" wordmark) as the app's one authored Material 3 color scheme:

- `ui/theme/Color.kt` — brand token palette (`MacsenseVoidBlack`, `MacsenseGoldPrimary`, etc.)
- `ui/theme/Theme.kt` — `MacSenseTheme` now builds `darkColorScheme`/`lightColorScheme` from brand
  tokens instead of the generated Compose defaults, and dynamic color defaults to `false` so the
  brand identity doesn't get overridden by Material You wallpaper theming.
- `res/values/colors.xml` — XML mirror of the brand tokens for non-Compose surfaces.
- `res/values/strings.xml` — app name updated to "MACSENSE AI", tagline added for future splash/about screens.

## Open Questions (require human decision before full close-out)

1. Crashlytics vs Sentry for Phase 3 item 1 — needs an account/DSN decision before wiring.
2. Making dependency-review CI blocking on release branches requires a branch-protection rule change
   in repo settings, which this agent cannot apply without admin access being granted explicitly.
3. Secret-rotation runbook end-to-end validation needs a staging environment with real (rotatable)
   credentials, not just code changes.

Phases 4–9 (Real Audio Engine, Genetic Sound & Resurrection, Ari Expansion, Collaboration/Sync,
Release Engineering, Monetization) are tracked as separate sub-issues off issue #1, per Phase 0
Ground Rule 2 (no phase begins until the prior phase's CI is green and docs match reality).
