# Deployment Guide — Macsense-2

## Overview
MacSense is a native Android application with no backend service; "deployment" means producing a signed release build (APK/AAB) and distributing it via Google Play or an internal distribution channel.

## Pre-deploy checklist
1. CI is green on `main` (lint, unit tests, debug assembly — see `.github/workflows/ci.yml`).
2. `GEMINI_API_KEY` is present and valid in the CI secret store used to populate `.env` at build time.
3. Room schema/migration reviewed: confirm any new schema version has a corresponding tested migration.
4. Version bump: increment `versionCode` and `versionName` in `app/build.gradle.kts`.

## Release steps
1. Cut a release branch or tag from `main`.
2. CI builds and tests the commit.
3. Build a signed release bundle: `./gradlew bundleRelease` using a release signing configuration supplied via CI secrets (keystore, alias, store/key passwords) — this must be added to `app/build.gradle.kts` `signingConfigs` before this step is usable; it is intentionally not present in source control today.
4. Upload the AAB to Google Play Console (internal/closed/open track as appropriate) or distribute via the chosen internal channel.
5. Manual approval gate: a human must approve promotion from internal/closed testing to production — do not auto-promote.

## Validation steps (post-deploy)
- Install the released build on a clean device/emulator and verify: app launches, DAW screen loads, audio capture permission prompt appears, a mastering preset applies, and the Ari assistant returns a response (network-dependent).
- Confirm no crash-reporting alerts fire in the first hour of rollout once crash reporting (Phase 3 of the hardening plan) is integrated.

## Rollback steps
Halt the Play Console staged rollout, or revert to the previous tagged release artifact for sideload/internal distribution. No data migration rollback is required since the local Room DB is additive/backward-compatible per its migration chain.

## Emergency hotfix workflow
1. Branch from the last known-good release tag (not necessarily `main` if `main` has since diverged).
2. Apply the minimal fix, let CI run, and cut a new patch version.
3. Fast-track through internal testing track before production promotion; do not skip the manual approval gate even for hotfixes.
