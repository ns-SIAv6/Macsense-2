import fetch from "node-fetch";
import { config } from "./config";
import { AriChatRequestBody } from "./types";

const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";

class GeminiUpstreamError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "GeminiUpstreamError";
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Calls Gemini's generateContent endpoint on behalf of the client, retrying transient
 * failures (429/5xx) with exponential backoff. The API key never leaves this process:
 * it is attached here as a header and is never echoed back in any response or log line.
 */
export async function callGeminiGenerateContent(
  body: AriChatRequestBody,
  maxRetries = 2
): Promise<unknown> {
  const url = `${GEMINI_BASE_URL}/v1beta/models/${config.geminiModel}:generateContent`;

  let attempt = 0;
  let lastError: unknown = null;

  while (attempt <= maxRetries) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": config.geminiApiKey,
        },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        return await response.json();
      }

      const shouldRetry = response.status === 429 || response.status >= 500;
      if (!shouldRetry || attempt === maxRetries) {
        const text = await response.text().catch(() => "");
        throw new GeminiUpstreamError(
          response.status,
          `Gemini upstream error ${response.status}: ${text.slice(0, 300)}`
        );
      }
    } catch (err) {
      lastError = err;
      if (attempt === maxRetries) {
        throw err;
      }
    }

    const backoffMs = 400 * 2 ** attempt;
    await sleep(backoffMs);
    attempt++;
  }

  throw lastError ?? new Error("Gemini request failed after retries");
}

export { GeminiUpstreamError };
