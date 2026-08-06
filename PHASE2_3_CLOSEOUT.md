# Phase 2 & 3 Closeout — Security Hardening + Observability & Reliability

Status: closing out per `macsense-2-full-readiness-plan-2.md`. This document maps each numbered
requirement from Phase 2 and Phase 3 to the commit/file that satisfies it, per Phase 0 Ground Rule 1
("every phase produces working code, passing tests, updated docs, and a merged PR").

Remaining Phase 1 items (crash reporting, branch protection, staging secret-rotation validation,
final review sign-off) are tracked in issue #52.

## Phase 2 — Security Hardening

| # | Requirement | Status | Where |
|---|---|---|---|
| 1 | Move Gemini API key off URL query param | Done | `GeminiApiService` sends key via `x-goog-api-key` header instead of a query parameter |
| 2 | `network_security_config.xml` blocking cleartext | Done | `app/src/main/res/xml/network_security_config.xml`, wired via `android:networkSecurityConfig` in `AndroidManifest.xml` |
| 3 | Validate `GEMINI_API_KEY` at startup, fail fast | Done | `StartupValidator`, invoked from `MacSenseApplication.onCreate()` |
| 4 | Review `allowBackup` / add `dataExtractionRules` exclusions | Done | `app/src/main/res/xml/data_extraction_rules.xml` referenced from manifest; Room DB path excluded from cloud backup |
| 5 | Promote dependency-review CI job from advisory to blocking on release branches | Tracked (issue #33) | Requires branch-protection change in repo settings (human action — see Open Questions below) |
| 6 | Secret-rotation runbook validated end-to-end | Tracked (issue #35) | `RUNBOOKS.md` (Phase 1) contains the rotation steps; a staging run-through requires real staging credentials (human action) |
| 7 | ProGuard/R8 keep-rule audit for `AriCommand` sealed/serializable classes | Done (issue #34) | `app/proguard-rules.pro` — explicit `@Serializable` companion/serializer keep rules, `AriCommand`-specific keep, Retrofit interface + Signature/Exceptions attribute keep rules |

## Phase 3 — Observability & Reliability

| # | Requirement | Status | Where |
|---|---|---|---|
| 1 | Crash reporting behind `BuildConfig` flag | Tracked (issue #32) — requires choosing Crashlytics vs Sentry and adding `google-services.json`/DSN (human decision, see Open Questions) |
| 2 | Structured logging around every Gemini call | Done | `AppLogger` facade; request id, latency, success/failure, retry count logged around `GeminiApiService` calls in `DawViewModel` |
| 3 | Retry-with-backoff / circuit breaker on `generateContent` | Done | `RetryInterceptor` (OkHttp-level) + `withGeminiRetry()` coroutine helper wired into `DawViewModel`'s Ari call path |
| 4 | Tested destructive-migration fallback beyond `MIGRATION_1_2` | **Corrected: actually Done now** | This table previously marked this item "Done," but `AppContainer.kt`'s `Room.databaseBuilder` had no fallback configured — only `MIGRATION_1_2`/`MIGRATION_2_3`. Fixed: `.fallbackToDestructiveMigration()` + `.fallbackToDestructiveMigrationOnDowngrade()` now wired in `AppContainer.kt`, proven by `DestructiveMigrationFallbackTest` (Robolectric) which reproduces the exact missed-migration crash and confirms the fallback prevents it |
| 5 | Unit tests for previously-untested surfaces | Done | `AriCommandParserTest`, `GeminiApiServiceTest` (MockWebServer), `StartupValidatorTest`, `AppLoggerTest`, `DestructiveMigrationFallbackTest` |
| 6 | Close-out gate | In progress (issue #52) | CI green on this branch; crash-reporting item requires the human decision in item 1 above before it can close |

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

Phases 2–5 of the consolidated plan (Real Audio Engine & Core DAW Shell, Ari Co-Producer & Creative
AI Surface, Genetic Sound/Mastering/Vocal Chain, Collaboration/Distribution/Store Release) are
tracked as separate issues per `PRODUCTION_HARDENING_PLAN.md`, per Phase 0 Ground Rule 2 (no phase
begins until the prior phase's CI is green and docs match reality).
