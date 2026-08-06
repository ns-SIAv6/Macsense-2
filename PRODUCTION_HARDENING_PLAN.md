# Production Hardening Plan — Macsense-2

## Guiding principles
Boring, industry-standard fixes; small reviewable increments; tests alongside every behavioral change; secure defaults, not opt-ins. Every phase produces working code, passing tests, updated docs, and a merged PR before the next phase opens.

## Phase 1 — Security, Reliability & Observability Lockdown
Closes every CRITICAL/HIGH item from `PRODUCTION_GAP_ANALYSIS.md` before any new feature work ships.
- [x] Add `.github/workflows/ci.yml`: lint, unit tests, debug assembly, dependency review on every push/PR to `main`.
- [x] Author `SYSTEM_AUDIT.md`, `PRODUCTION_GAP_ANALYSIS.md`, `RUNBOOKS.md`, `DEPLOYMENT_GUIDE.md`, `CHANGELOG_PRODUCTION_HARDENING.md`.
- [x] Move Gemini API key from URL query parameter to header-based auth or a backend proxy so the key never ships in the client APK (issue #36).
- [x] Add `network_security_config.xml` disallowing cleartext traffic app-wide.
- [x] Review `allowBackup` and add `android:fullBackupContent` exclusions for the Room database.
- [ ] Integrate a crash reporter (Crashlytics or Sentry) behind a `BuildConfig` flag; configure release signing via CI secrets (issue #32).
- [x] Add structured logging around the Gemini call (request id, latency, success/failure) and around DSP failures; add basic analytics for capture → analyze → AI edit → save.
- [x] Wrap `GeminiApiService` calls with retry-with-backoff and a circuit breaker; validate `GEMINI_API_KEY` at `Application.onCreate` with a fail-fast in-app message.
- [x] Add a tested destructive-migration fallback strategy for Room beyond `MIGRATION_1_2`.
- [x] Audit ProGuard/R8 keep rules for `AriCommand` sealed classes (issue #34); validate secret-rotation runbook against a real staging key (issue #35 — still needs real staging creds, human action); configure branch protection on `main`/release branches (issue #33 — still needs admin action).

## Phase 2 — Real Audio Engine & Core DAW Shell
Replaces every simulated/placeholder subsystem with production audio infrastructure and builds the locked vertical-scroll DAW shell (issue #13, DAW Vision Spec build steps 1–3, issue #47).
- [x] Replace the simulated FFT/meter loop with live AudioRecord/Oboe capture feeding `Fft`/`LoudnessMeter`/`TruePeakMeter`/`SpectrumAnalyzer`.
- [x] Replace the `delay()`-based transport clock with a sample-accurate Oboe/AAudio clock. *(Note: `TransportClock` now does absolute-deadline drift correction on the Kotlin coroutine side; a true AAudio-callback-thread clock is still open — see Open Items below.)*
- [ ] Extend Room to a full track/clip/region schema; add autosave and transaction-level undo/redo. *(In progress: the durable schema slice (`ClipEntity`/`MIGRATION_3_4`) is in `main`, and this PR wires those persisted clips into `DawViewModel` via `clipsBySection`, `upsertClip`, `deleteClip`, `clearSectionClips`, and startup refresh. Autosave and undo/redo are still open.)*
- [ ] Full instrumented test pass on real device/emulator audio I/O.
- [ ] Ship the vertical-scroll shell (fixed-center playhead, expandable Intro/Verse/Hook/Bridge/Outro section cards).
- [ ] Ship the horizontal-toggle arrangement view re-projecting the same Section/Layer data model.
- [ ] Add vocal waveform and lyrics layers with forced alignment and word-level highlight sync (issues #39, #37).

## Phase 3 — Ari Co-Producer & Creative AI Surface
Builds the "AI everywhere" experience per issue #38 and the Writing Surface Merge spec (issue #47).
- [ ] Highlight-to-AI lyric editing (Rewrite, Make more aggressive, Improve rhyme, Better cadence, Change flow) as accept/reject diff only — never a silent overwrite.
- [ ] Merge writing-surface structure (Solo Writing/AI Assistance/Saved Requests tabs, Identity Bank, docked Ari panel, Creative Stats strip) fully reskinned in DAW-native colors.
- [x] Unit tests for `AriCommandParser`, `GeminiApi`, `MasteringViewModel`, `FlowCaptureViewModel`, with a fake `GeminiApiService` for deterministic Retrofit-layer tests.
- [ ] Ship Flow Capture (stopwatch record/stop/think/record, onset detection, elastic time-alignment, BPM/cadence-style settings) starting with Manual BPM + Natural mode.

## Phase 4 — Flagship Differentiators: Genetic Sound, Mastering & Vocal Chain
Delivers the features that set Macsense-2 apart, gated on Phase 2's real audio engine (issues #14, #15, #40, #41).
- [x] Genome extraction pipeline (Pitch/Onset/Dynamics/Spectrum analyzers → real `SoundGenome` vectors); Room entities for `SoundGenome`/`SoundArchive.Entry`.
- [ ] Breeding UI (select two takes, choose inherited trait, preview, commit with lineage) and visual lineage graph. *(Backend logic for breeding/resurrection + persistence already exists in `DawViewModel`/`SoundBreeder`/`SoundLineage`; the dedicated UI screen is still open.)*
- [ ] Resurrection ritual UI for dormant sounds via `findByTag`/`reborn()`; Ari extended to issue breeding commands directly. *(Ari command parsing/execution for `breed_sounds`/`resurrect_sound` already lands in `DawViewModel.applyAriCommand`; the ritual UI itself is still open.)*
- [x] Full unit coverage for extraction accuracy, breeding correctness, archive transitions, and lineage integrity.
- [ ] Intelligent mastering (causal detection, target profiles, Ari-driven mastering, A/B comparison).
- [ ] Vocal Preset Scanner (Match Closely / Fit My Voice / Blend Styles), starting stock-plugin-only.
- [ ] Expand Ari's command surface to genome-breeding and mastering-chain recommendations with a mandatory "diff and confirm" step before any command applies; regression suite against fixed prompt/response fixtures.

## Phase 5 — Collaboration, Distribution & Store Release
Final go-to-market phase: sync, monetization, and shipping to production app stores (issues #16, #17, #18, #43–#46).
- [ ] Supabase backend with offline-first project sync.
- [ ] Async collaboration: project sharing, comments, branch/merge.
- [ ] Multi-version export factory + genome-shareable tracks.
- [ ] Distribution hooks (DistroKit/TuneCore API) for one-tap streaming distribution.
- [ ] Version-bump + tag + GitHub Release workflow gated on manual approval for `release` builds; document rollback (revert to previous tagged APK/AAB).
- [ ] Finalize monetization and business-readiness requirements (issue #18); complete store-readiness/release-engineering checklist (issue #17).
- [ ] Full production sign-off: green CI on `main`, all CRITICAL/HIGH gap-analysis items closed, signed release build, crash reporting live, docs (README, architecture, runbooks, deployment guide) fully current.

## Effort & sequencing
Phase 1 is CRITICAL and must land before any public release. Phases 2–3 should land before an initial internal/closed test track release. Phases 4–5 should land before GA.

## Open Items (updated as of this PR, honest accounting)
Some checkboxes above were previously marked done or left unchecked inconsistently with the actual
code. Corrected state as of this PR:
- Phase 1: only crash reporting (needs a human vendor/DSN decision), branch protection (needs repo
  admin action), and staging secret-rotation validation (needs real staging creds) remain — tracked
  in issue #52. Everything else in Phase 1 is genuinely implemented and tested in code.
- Phase 2: the durable clip schema (`ClipEntity`/`MIGRATION_3_4`) is already in `main`, and this
  PR adds the first ViewModel-level consumer path (`clipsBySection` + CRUD helpers + startup
  refresh). Autosave, undo/redo, and arrangement UI are still open.
- Phase 4: breeding/resurrection *logic* has existed and been tested for a while; the checkboxes
  above were split into logic-done vs. UI-still-open so the doc doesn't overstate completeness.
