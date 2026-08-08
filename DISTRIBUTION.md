# MACSENSE AI — Distribution Guide

This document covers how to push MACSENSE AI to music distribution platforms
and what to prepare before submitting.

---

## Platforms

### Google Play Store (primary)
- **Track**: Internal → Closed Testing → Production
- **Console**: https://play.google.com/console
- **Required assets**: 512x512 icon, 1024x500 feature graphic, 2 screenshots per form factor
- **API integration**: Use the [Google Play Developer API](https://developers.google.com/android-publisher)
  for automated APK uploads (see `scripts/upload_to_play.py` placeholder)

### DistroKit
- **API**: `https://api.distrokid.com/vx/`
- **Auth**: Bearer token from `DISTROKIT_API_KEY` secret (add in GitHub Settings → Secrets)
- **Endpoint**: `POST /releases` with multipart APK + metadata
- **Required**: UPC code, release date, genre tags, track metadata

### TuneCore (alternative / additional)
- **Portal**: https://www.tunecore.com
- **API docs**: Contact TuneCore for API partner access
- **Manual submission** is the current path until API access is granted

---

## CI Release Flow

1. Developer merges all features into `main` and runs the full test suite locally
2. Run the production sign-off checklist (shown in `release.yml`)
3. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
4. Push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
5. GitHub Actions `release.yml` fires:
   - Runs unit tests (`testReleaseUnitTest`)
   - Builds signed APK using keystore secrets
   - Creates GitHub Release with APK attached
6. Download APK from GitHub Release and upload to Play Console / DistroKit

---

## Required GitHub Secrets

| Secret | Description |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
| `SENTRY_DSN` | Sentry DSN for crash reporting |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase anon/public API key |
| `DISTROKIT_API_KEY` | DistroKit API bearer token (for automated upload) |

---

## Store Release Checklist

- [ ] `versionCode` incremented from previous release
- [ ] `versionName` updated (e.g. `1.0.1`)
- [ ] All CI tests passing on `main`
- [ ] Sentry DSN set and crash reporting confirmed active in release build
- [ ] ProGuard/R8 output inspected: serialization classes not stripped
- [ ] APK signed with production keystore (not debug)
- [ ] Manual smoke-test on physical device:
  - [ ] DAW screen loads, playback works
  - [ ] FlowCapture records and shows take in shelf
  - [ ] Breeding Lab: breed + resurrect complete without crash
  - [ ] Vocal Scanner: all 3 modes scan and show plugin chain
  - [ ] Supabase sync: project uploads when online, queues when offline
- [ ] Privacy policy URL current in Play Store listing
- [ ] Screenshots/metadata updated for this version
- [ ] APK uploaded to Play Console Internal track
- [ ] Internal testers sign off
- [ ] Promote to Production
