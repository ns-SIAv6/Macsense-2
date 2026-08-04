# Production Hardening Plan — Macsense-2

## Guiding principles
Boring, industry-standard fixes; small reviewable increments; tests alongside every behavioral change; secure defaults, not opt-ins.

## Phase 1 (this PR) — Foundations
- [x] Add `.github/workflows/ci.yml`: lint, unit tests, debug assembly, dependency review on every push/PR to `main`.
- [x] Author `SYSTEM_AUDIT.md`, `PRODUCTION_GAP_ANALYSIS.md`, `PRODUCTION_HARDENING_PLAN.md`, `RUNBOOKS.md`, `DEPLOYMENT_GUIDE.md`, `CHANGELOG_PRODUCTION_HARDENING.md`.

## Phase 2 — Security (next PR, CRITICAL)
- Move Gemini API key from URL query parameter to an `Authorization`/header-based mechanism, or introduce a thin backend proxy so the key never ships in the client APK.
- Add `network_security_config.xml` disallowing cleartext traffic app-wide.
- Review `allowBackup` and add `android:fullBackupContent` exclusions for the Room database if project data is considered sensitive.

## Phase 3 — Observability (CRITICAL)
- Integrate a crash reporter (Crashlytics or Sentry) behind a `BuildConfig` flag so debug builds don't spam production dashboards.
- Add structured logging around the Gemini call (request id, latency, success/failure) and around DSP failures.
- Add basic analytics events for the critical flow: capture → analyze → AI edit → save.

## Phase 4 — Reliability
- Wrap `GeminiApiService` calls with retry-with-backoff (e.g., 2 retries, exponential backoff) and a client-side timeout-driven circuit breaker for repeated failures.
- Validate `GEMINI_API_KEY` at `Application.onCreate`; fail fast with a clear in-app message rather than a deep-stack Retrofit exception.
- Add a tested destructive-migration fallback strategy for Room beyond `MIGRATION_1_2`.

## Phase 5 — Testing
- Add unit tests for `AriCommandParser` (valid command, malformed JSON, missing tags, boundary values for bpm/reverb/etc.).
- Add unit tests for `MasteringViewModel` and `FlowCaptureViewModel` state transitions.
- Add a fake/mock `GeminiApiService` for deterministic Retrofit-layer tests.

## Phase 6 — Release engineering
- Add a release build type signing configuration wired to CI secrets (keystore, alias, passwords) — never committed to source control.
- Add a version-bump + tag + GitHub Release workflow gated on manual approval for `release` builds.
- Document rollback: revert to previous tagged APK/AAB, no server-side rollback needed (client-only app).

## Effort & sequencing
Phases 2 and 3 are CRITICAL and should land before any public release; Phases 4–6 should land before GA but do not block an initial internal/closed test track release once Phases 1–3 are done.
