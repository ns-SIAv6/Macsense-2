# Production Readiness Gap Analysis — Macsense-2

## A. Architecture and reliability
- No retries/backoff/circuit breaker around the Gemini call; a transient network failure surfaces as a raw exception to the UI. **HIGH**
- Single `AppContainer` with no interface abstraction for the repository/database makes unit testing view models that depend on it harder and increases risk of tight coupling. **MEDIUM**
- Room migration strategy exists (`MIGRATION_1_2`) but has no destructive-migration fallback declared, so a missed migration in a future release would crash on upgrade. **HIGH**

## B. Security hardening
- Gemini API key sent as a URL query parameter — increases exposure via logs/proxies/analytics. Should move to a request header where the API supports it, or proxy through a backend. **CRITICAL**
- No `usesCleartextTraffic=false` explicit hardening or network security config restricting traffic to HTTPS-only pinned hosts. **HIGH**
- `android:allowBackup="true"` combined with local DB storage of user project data means device backups could exfiltrate project data; should be reviewed against data-sensitivity requirements. **MEDIUM**
- No dependency vulnerability scanning in CI prior to this change. **HIGH** (mitigated by new workflow's dependency-review job)

## C. Performance and scalability
- DSP work (FFT, loudness, true-peak) appears to run on view-model scopes; needs verification that heavy computation is dispatched off the main thread consistently across all view models, not just `DawViewModel`. **MEDIUM**
- No caching/backoff for repeated identical Gemini prompts (cost and latency risk at scale). **LOW**

## D. Observability and operability
- No crash reporting, no structured logging, no metrics for AI-call latency/error rate or DSP failures. **CRITICAL**
- No health/readiness concept applicable to a mobile client, but startup failure handling (e.g., DB open failure) is not explicitly guarded. **MEDIUM**

## E. Testing and quality
- No tests for `MasteringViewModel`, `FlowCaptureViewModel`, `GeminiApi`, or `AriCommandParser` — the AI command path (highest-risk, user-facing logic) is untested. **HIGH**
- No static analysis / lint enforcement was running in CI before this change. **HIGH** (mitigated by new CI workflow)

## F. CI/CD and release engineering
- No CI pipeline existed at all before this branch — no automated build/test/lint on every push or PR. **CRITICAL** (fixed: `.github/workflows/ci.yml` added)
- No release signing configuration, no versioning/tagging strategy, no changelog process before this change. **CRITICAL**

## G. Configuration and infrastructure readiness
- No environment-variable validation at startup; a missing `GEMINI_API_KEY` fails silently at call time rather than being caught early with a clear error. **MEDIUM**
- No ProGuard/R8 keep rules reviewed for Retrofit/kotlinx-serialization models beyond the default file — risk of release-build serialization breakage. **MEDIUM**

## H. Documentation and support readiness
- No `README.md`, architecture docs, runbooks, or deployment guide existed before this change. **CRITICAL** (mitigated by this document set)

---

## Prioritized Remediation Table

| Priority | Issue | Risk | Impact | Recommended Fix | Effort | Dependencies | Blocking? |
|---|---|---|---|---|---|---|---|
| 1 | No CI/CD pipeline | CRITICAL | No safety net against regressions | Add GitHub Actions workflow (lint, test, assemble, dependency review) | S | None | Yes |
| 2 | Gemini API key in URL query param | CRITICAL | Key leakage via logs/proxies | Move to header-based auth or backend proxy | M | Gemini API support confirmation | Yes |
| 3 | No crash reporting/observability | CRITICAL | Blind to production incidents | Integrate Firebase Crashlytics or Sentry Android SDK | M | Vendor decision, API keys | Yes |
| 4 | No release signing | CRITICAL | Cannot ship a verifiable release build | Configure signing via CI secret store (keystore + passwords) | S | Keystore provisioning | Yes |
| 5 | Untested AI-command path | HIGH | Silent parsing/logic failures reach users | Add unit tests for `AriCommandParser`, `GeminiApi`, `MasteringViewModel`, `FlowCaptureViewModel` | M | None | No (should fix before GA) |
| 6 | No destructive-migration fallback | HIGH | Crash on DB upgrade if migration missed | Add `fallbackToDestructiveMigrationOnDowngrade`/tested migration path | S | None | No |
| 7 | No network security config | HIGH | Cleartext/MITM exposure | Add `network_security_config.xml` enforcing HTTPS + optional pinning | S | None | No |
| 8 | No env var validation at startup | MEDIUM | Confusing runtime failures | Validate `GEMINI_API_KEY` presence in `Application.onCreate` | S | None | No |
| 9 | `allowBackup=true` with local project data | MEDIUM | Data exfiltration via backup | Review and restrict backup rules for sensitive DB tables | S | Product decision | No |
| 10 | No documentation set | CRITICAL | No team handoff/onboarding path | This document set + README + runbooks | S | None | Yes |
