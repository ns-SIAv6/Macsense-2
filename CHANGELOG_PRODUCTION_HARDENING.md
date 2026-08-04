# Changelog — Production Hardening

## [Unreleased] — Phase 1: Foundations
### Added
- `.github/workflows/ci.yml`: automated lint, unit test, debug-APK assembly, and dependency-review checks on every push and pull request to `main`. This closes the single largest production-readiness gap identified in the audit — there was previously no CI at all.
- `SYSTEM_AUDIT.md`: full repository discovery covering architecture, dependencies, data stores, auth model, and critical flows.
- `PRODUCTION_GAP_ANALYSIS.md`: categorized gap analysis (reliability, security, performance, observability, testing, CI/CD, config, documentation) with a prioritized remediation table.
- `PRODUCTION_HARDENING_PLAN.md`: phased execution plan for the remaining CRITICAL/HIGH items (API key handling, crash reporting, retries, release signing, expanded test coverage).
- `RUNBOOKS.md`: startup/shutdown, incident triage, degraded-mode behavior, rollback, secret rotation, and database issue response procedures.
- `DEPLOYMENT_GUIDE.md`: pre-deploy checklist, release steps, validation, rollback, and emergency hotfix workflow for this client-only Android app.

### Why it matters
Before this change, the repository had zero automated verification and zero operational documentation — any change could reach a release build unreviewed and un-tested, and there was no reference for on-call/incident response. This phase establishes the baseline safety net and knowledge base that all subsequent hardening work builds on.

### Still open (see PRODUCTION_HARDENING_PLAN.md)
- Gemini API key is still sent as a URL query parameter (CRITICAL, Phase 2).
- No crash reporting/observability integrated yet (CRITICAL, Phase 3).
- No release signing configuration wired up yet (CRITICAL, Phase 6).
- AI-command parsing and mastering/flow-capture view models remain untested (HIGH, Phase 5).
