import fetch from "node-fetch";
import { config } from "./config.js";

const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
const GEMINI_MODEL = "gemini-3.5-flash";

export interface GeminiPart {
  text?: string;
}

export interface GeminiContent {
  role?: string;
  parts: GeminiPart[];
}

export interface GeminiGenerateContentRequest {
  contents: GeminiContent[];
  systemInstruction?: GeminiContent;
  generationConfig?: {
    temperature?: number;
    topP?: number;
    maxOutputTokens?: number;
  };
}

export interface GeminiCandidate {
  content?: GeminiContent;
}

export interface GeminiGenerateContentResponse {
  candidates?: GeminiCandidate[];
}

export class GeminiUpstreamError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly retryable: boolean
  ) {
    super(message);
    this.name = "GeminiUpstreamError";
  }
}

const MAX_RETRIES = 2;
const INITIAL_BACKOFF_MS = 400;
const REQUEST_TIMEOUT_MS = 20000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Calls Gemini's generateContent endpoint with the API key attached server-side only.
 * Retries transient failures (429 / 5xx / network errors) with exponential backoff,
 * mirroring the retry semantics the Android client used to implement itself.
 */
export async function callGemini(
  request: GeminiGenerateContentRequest
): Promise<GeminiGenerateContentResponse> {
  let attempt = 0;
  let lastError: unknown;

  while (attempt <= MAX_RETRIES) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(
        `${GEMINI_BASE_URL}/v1beta/models/${GEMINI_MODEL}:generateContent`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": config.geminiApiKey,
          },
          body: JSON.stringify(request),
          signal: controller.signal,
        }
      );

      const retryable = response.status === 429 || response.status >= 500;

      if (!response.ok) {
        if (retryable && attempt < MAX_RETRIES) {
          attempt++;
          await sleep(INITIAL_BACKOFF_MS * 2 ** (attempt - 1));
          continue;
        }
        const bodyText = await response.text().catch(() => "");
        throw new GeminiUpstreamError(
          `Gemini request failed with status ${response.status}: ${bodyText.slice(0, 500)}`,
          response.status,
          retryable
        );
      }

      return (await response.json()) as GeminiGenerateContentResponse;
    } catch (error) {
      lastError = error;
      if (error instanceof GeminiUpstreamError) throw error;
      if (attempt >= MAX_RETRIES) {
        throw new GeminiUpstreamError(
          `Gemini request failed after ${attempt + 1} attempts: ${(error as Error).message}`,
          502,
          false
        );
      }
      attempt++;
      await sleep(INITIAL_BACKOFF_MS * 2 ** (attempt - 1));
    } finally {
      clearTimeout(timeout);
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new GeminiUpstreamError("Gemini request failed for an unknown reason", 502, false);
}
