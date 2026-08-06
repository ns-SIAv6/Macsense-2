# Macsense Gateway

A thin Node.js + TypeScript backend that proxies Ari's chat requests to the Gemini API. Its
only job is to keep `GEMINI_API_KEY` off the Android client: the app never sees the key, and
the gateway never persists user audio or long-term chat history.

## Why this exists

The Android app previously called Gemini directly using `BuildConfig.GEMINI_API_KEY`. Any
key embedded in a shipped APK can be extracted, so this gateway moves the call server-side.
The Android client keeps its offline Ari fallback for when the gateway is unreachable.

## Endpoints

- `GET /health` — liveness check, no auth required.
- `POST /v1/ari/chat` — accepts `{ contents, systemInstruction? }` in the same shape Gemini's
  `generateContent` expects, calls Gemini server-side, and returns the raw Gemini response
  (`{ candidates: [...] }`). The Android `AriCommandParser` continues to parse the returned
  text client-side, so no Ari command logic had to move server-side.

## Running locally

```bash
cd server
cp .env.example .env
# edit .env and set a real GEMINI_API_KEY
npm install
npm run dev
```

The gateway listens on `http://localhost:8787` by default (`PORT` in `.env`).

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `GEMINI_API_KEY` | yes | Real Gemini API key. Never commit this. |
| `PORT` | no | Defaults to `8787`. |
| `CORS_ORIGINS` | no | Comma-separated allowed origins. Omit to allow all (fine for a mobile-only client). |
| `MACSENSE_CLIENT_TOKEN` | no | If set, clients must send `Authorization: Bearer <token>`. Recommended for any deployed (non-local) environment. |
| `RATE_LIMIT_MAX` | no | Max requests per IP per window. Defaults to `30`. |
| `RATE_LIMIT_WINDOW_MS` | no | Rate limit window in ms. Defaults to `60000`. |

## Example request

```bash
curl -X POST http://localhost:8787/v1/ari/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MACSENSE_CLIENT_TOKEN" \
  -d '{
    "contents": [{ "role": "user", "parts": [{ "text": "critique my structure" }] }],
    "systemInstruction": { "parts": [{ "text": "you are ari, an executive producer." }] }
  }'
```

## Production notes

- Deploy behind HTTPS only; never accept the client token or Gemini traffic over plaintext.
- Rotate `GEMINI_API_KEY` and `MACSENSE_CLIENT_TOKEN` independently of app releases.
- The in-memory rate limiter resets on restart and does not share state across instances;
  swap in a Redis-backed limiter before running more than one gateway instance.
- No user data is persisted by this service today. If you add logging, redact `contents`
  bodies (they may include lyrics) and never log `GEMINI_API_KEY` or the client token.

## Out of scope for this milestone

- User accounts / per-user API keys.
- Persisting chat history server-side.
- Audio upload or processing endpoints.
