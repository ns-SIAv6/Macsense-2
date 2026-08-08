# Macsense Gateway (Milestone 1)

A minimal, production-shaped Node.js/TypeScript backend that proxies Ari/Gemini requests so
the `GEMINI_API_KEY` never ships inside the Android client (APK). This directly addresses the
Phase 2 security gap already called out in `PRODUCTION_HARDENING_PLAN.md` at the repo root.

## Why this exists

Today, `DawViewModel` in the Android app calls Gemini directly using
`BuildConfig.GEMINI_API_KEY`. Even with the header-based key transport and redaction already
implemented in `GeminiApi.kt`, the key itself is still embedded in the compiled APK and can be
extracted. This gateway moves the key server-side; the Android app talks to this service instead
of Google directly for live Ari responses, keeping the existing offline fallback untouched.

## Endpoints

### `GET /health`
Liveness check. Returns `{ "status": "ok" }`.

### `POST /v1/ari/chat`
Proxies to Gemini's `generateContent`. Request body must match:

```json
{
  "contents": [
    { "role": "user", "parts": [{ "text": "..." }] }
  ],
  "systemInstruction": { "parts": [{ "text": "..." }] }
}
```

This is the same shape as `GenerateContentRequest` in the Android app's `GeminiApi.kt`, so the
client payload construction (chat history + serialized DAW context + Ari system prompt) does not
need to change â€” only the destination URL and headers change.

Response body is the raw Gemini `generateContent` response on success (`200`). On failure:

```json
{ "error": "human readable message", "code": "machine_readable_code" }
```

Possible `code` values: `invalid_request` (400), `unauthorized` (401), `rate_limited` (429),
`upstream_error` / `unexpected_error` (502).

## Running locally

```bash
cd server
cp .env.example .env
# edit .env and set a real GEMINI_API_KEY
npm install
npm run dev
```

The server listens on `PORT` (default `8787`).

## Example request

```bash
curl -X POST http://localhost:8787/v1/ari/chat \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [
      { "role": "user", "parts": [{ "text": "critique my structure" }] }
    ],
    "systemInstruction": { "parts": [{ "text": "you are ari..." }] }
  }'
```

If `MACSENSE_CLIENT_TOKEN` is set in `.env`, add:

```bash
  -H "Authorization: Bearer <token>"
```

## Environment variables

See `.env.example`. Required: `GEMINI_API_KEY`. Everything else has a safe default. The default
model is `gemini-2.0-flash`; do not configure obsolete `gemini-3.5-*` identifiers.

## Security notes

- The Gemini API key is read from the environment only and is never logged, echoed, or included
  in any response body.
- `helmet()` sets standard security headers; CORS is locked to `CORS_ORIGINS` when provided.
- A per-IP rate limiter (`RATE_LIMIT_MAX` per `RATE_LIMIT_WINDOW_MS`) protects against abuse and
  runaway Gemini billing.
- Request bodies are capped at 256kb.
- Optional bearer-token auth (`MACSENSE_CLIENT_TOKEN`) is scaffolded but off by default so this
  can be deployed and validated before the Android client is updated to send the header.

## What this milestone deliberately does NOT include

- User accounts / OAuth / sessions beyond the optional shared bearer token.
- A database. Nothing is persisted server-side.
- Audio upload or processing. This proxies text/JSON chat requests only.
- Changes to `DawViewModel`'s offline Ari fallback, which continues to work unchanged when the
  gateway or network is unavailable.

## Deploying

Any standard Node.js host works (Cloud Run, Render, Fly.io, a small VM, etc.). Set `GEMINI_API_KEY`
and (recommended) `MACSENSE_CLIENT_TOKEN` as platform secrets, never in source control. Run:

```bash
npm install
npm run build
npm start
```

## Next steps (tracked separately)

- Point the Android app's Ari networking at this gateway's base URL instead of calling Gemini
  directly, gated behind a `BuildConfig` field so debug builds can still target either endpoint
  during rollout.
- Enforce `MACSENSE_CLIENT_TOKEN` once the Android client sends it.
- Add automated tests (unit tests for request validation, integration test against a mocked
  Gemini upstream).
