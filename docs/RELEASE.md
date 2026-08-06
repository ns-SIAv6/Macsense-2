# Release Build Guide — Macsense-2

This is Milestone 2 of the production hardening sequence (see `PRODUCTION_HARDENING_PLAN.md`).
It makes the Android `release` build type actually production-shaped: shrunk, obfuscated,
non-debuggable, with project-specific ProGuard/R8 keep rules instead of a blanket keep.

## What changed

- `app/build.gradle.kts` `release` build type now sets:
  - `isMinifyEnabled = true` (was already on)
  - `isShrinkResources = true` (new — requires minify, strips unused resources)
  - `isDebuggable = false`, `isJniDebuggable = false` (explicit, defense-in-depth)
- `app/proguard-rules.pro` replaced the old `-keep class com.macsense.ai.** { *; }` blanket
  rule (which disabled shrinking/obfuscation for the entire app) with targeted rules for:
  - kotlinx.serialization `@Serializable` DTOs (Gemini/Ari request+response models)
  - Retrofit/OkHttp service interfaces and annotations
  - Room entities/DAOs/database
  - Jetpack Compose runtime internals
  - JNI-bound classes (`NativePlaybackEngine`, `LiveMeterEngine`) and any class with native methods
- Added a `debug` build type block with `applicationIdSuffix = ".debug"` and
  `versionNameSuffix = "-debug"` so debug and release builds can be installed side by side and
  are trivially distinguishable in `adb shell pm list packages` / app drawer.

## Building a release APK/AAB locally

```bash
./gradlew :app:assembleRelease
# or, for Play Store upload:
./gradlew :app:bundleRelease
```

Output lands in `app/build/outputs/apk/release/` or `app/build/outputs/bundle/release/`.

> Release signing is intentionally not configured in source control (see the comment in
> `app/build.gradle.kts`). Wire a `signingConfigs { release { ... } }` block from CI secrets
> (keystore path, alias, store/key passwords) before publishing — never commit a keystore or
> passwords to this repo.

## Pre-release checklist

- [ ] `GEMINI_API_KEY` is **not** referenced by any release-path client code for live network
      calls. (Tracked: Milestone 1 introduces `server/`, a backend gateway; the Android client
      cutover to call it instead of Gemini directly is the next follow-up PR.)
- [ ] `./gradlew :app:assembleRelease` completes with no R8 "missing classes" warnings that
      reference app code (warnings about optional third-party classes not present at compile
      time are expected and fine).
- [ ] Manually smoke-test a release build install:
  - Ari chat still opens and responds (offline brain is acceptable if no gateway/network).
  - Applying an Ari command (bpm/lyrics/preset/effects) still updates the DAW state.
  - Transport play/pause, BPM change, section reorder, and step-grid toggles all work.
  - Stem split button and Ari feedback engine on the right rail still function.
  - Phase 4 horizontal/vertical view toggle still switches layouts correctly.
- [ ] `versionCode` / `versionName` bumped from the previous release.
- [ ] `android:allowBackup` / `android:fullBackupContent` reviewed if the Room database now
      contains anything considered sensitive (see `PRODUCTION_HARDENING_PLAN.md` Phase 2).
- [ ] Network security config (`app/src/main/res/xml/network_security_config.xml`) still
      disallows cleartext traffic app-wide — already enforced, re-verify after any networking
      changes.

## Rollback

This is a client-only app with no server-side release state (aside from the new optional
gateway, which is stateless). Rollback = reinstall the previous tagged APK/AAB; no data
migration or server rollback is required. See `RUNBOOKS.md` for the broader incident process.

## Known follow-ups (not in this milestone)

- Wire a real `signingConfigs` block from CI secrets.
- Add a version-bump + tag + GitHub Release workflow (Phase 6 in `PRODUCTION_HARDENING_PLAN.md`).
- Once the Android client is updated to call the Milestone 1 gateway, add a CI assertion that
  fails the build if `BuildConfig.GEMINI_API_KEY` is referenced outside of local/offline-only
  code paths.
