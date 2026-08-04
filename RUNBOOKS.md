# Runbooks — Macsense-2

## Startup / shutdown
The app has no server process; "startup" is `MacSenseApplication.onCreate()` initializing `AppContainer` (Room DB + repository). If the app fails to start, check for a Room migration exception in the crash log (once crash reporting is integrated per the hardening plan) — this is the most likely fatal-startup cause given the versioned schema.

## Incident triage (AI assistant / Gemini outage)
1. Confirm whether the Gemini API is down (check Google Cloud status page) or the client's `GEMINI_API_KEY` is invalid/rotated.
2. If key-related, rotate the key (see Secret Rotation below) and cut a new release; the app has no server-side kill switch since the key lives client-side.
3. If provider-side outage, communicate to users that the "Ari" AI features are degraded; core DSP/mastering features are unaffected since they run fully on-device.

## Dependency outage handling
Gemini is the only external dependency. Because there is currently no retry/backoff (tracked in the hardening plan, Phase 4), a transient outage will surface as an immediate error to the user; there is no cached/offline fallback for AI commands by design.

## Degraded mode behavior
All DSP, mastering, capture, and persistence features function fully offline. Only the Ari AI assistant chat/command flow requires network connectivity to the Gemini API.

## Rollback
This is a client-distributed Android app with no server component. Rollback means re-publishing the previous known-good tagged APK/AAB build (via Play Console "halt rollout" + revert to prior release, or redistributing the prior signed artifact for sideload/internal distribution). No database rollback is needed since Room migrations are additive and local to each device.

## Secret rotation (GEMINI_API_KEY)
1. Generate a new key in Google AI Studio / Google Cloud console.
2. Update the CI secret store value used to populate `.env` at build time (never commit the key to source).
3. Cut a new release build and roll it out; the old key can then be revoked.
4. Until Phase 2 hardening (header-based auth / backend proxy) lands, treat the key as sensitive-but-necessarily-client-embedded, and rotate proactively on any suspected leak (e.g., APK decompilation).

## Database issue response
If users report crashes on app update, check whether `Migrations.MIGRATION_1_2` matches the schema exported at `app/schemas/com.macsense.ai.data.local.MacSenseDatabase/2.json`. A mismatch indicates a schema/migration drift that must be fixed in a patch release before wider rollout.
