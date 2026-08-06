import { Router } from "express";
import { callGeminiGenerateContent, GeminiUpstreamError } from "../geminiClient";
import { AriChatRequestBody } from "../types";

export const ariChatRouter = Router();

function isValidBody(body: unknown): body is AriChatRequestBody {
  if (!body || typeof body !== "object") return false;
  const b = body as Record<string, unknown>;
  if (!Array.isArray(b.contents) || b.contents.length === 0) return false;
  return b.contents.every(
    (c) =>
      c &&
      typeof c === "object" &&
      Array.isArray((c as Record<string, unknown>).parts)
  );
}

ariChatRouter.post("/", async (req, res) => {
  const body = req.body;

  if (!isValidBody(body)) {
    res.status(400).json({
      error: "Request must include a non-empty 'contents' array with Gemini-style parts.",
      code: "invalid_request",
    });
    return;
  }

  try {
    const upstream = await callGeminiGenerateContent(body);
    res.status(200).json(upstream);
  } catch (err) {
    if (err instanceof GeminiUpstreamError) {
      // Map upstream failures to a stable shape; never leak the API key or raw headers.
      const status = err.status === 429 ? 429 : 502;
      res.status(status).json({
        error: "Upstream Gemini request failed.",
        code: err.status === 429 ? "rate_limited" : "upstream_error",
      });
      return;
    }

    res.status(502).json({
      error: "Unexpected error contacting Gemini.",
      code: "unexpected_error",
    });
  }
});
