import cors from "cors";
import express, { type NextFunction, type Request, type Response } from "express";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import { config } from "./config.js";
import { requireClientToken } from "./authMiddleware.js";
import { GeminiUpstreamError, callGemini } from "./geminiClient.js";
import { ariChatRequestSchema } from "./schemas.js";

const app = express();

app.disable("x-powered-by");
app.use(helmet());
app.use(
  cors({
    origin: config.corsOrigins && config.corsOrigins.length > 0 ? config.corsOrigins : true,
  })
);
app.use(express.json({ limit: "256kb" }));

const ariLimiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.rateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Too many requests, slow down.", code: "rate_limited" },
});

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "macsense-gateway", time: new Date().toISOString() });
});

app.post(
  "/v1/ari/chat",
  ariLimiter,
  requireClientToken,
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const parsed = ariChatRequestSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: "Invalid request body",
          code: "invalid_body",
          details: parsed.error.flatten(),
        });
        return;
      }

      const upstream = await callGemini(parsed.data);
      res.json(upstream);
    } catch (error) {
      next(error);
    }
  }
);

// Centralized error handler. Never leaks the Gemini API key or raw upstream bodies
// beyond a truncated snippet already sanitized in geminiClient.ts.
app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  if (error instanceof GeminiUpstreamError) {
    res.status(error.retryable ? 503 : error.status).json({
      error: "Upstream Gemini request failed",
      code: "gemini_upstream_error",
      retryable: error.retryable,
    });
    return;
  }

  // eslint-disable-next-line no-console
  console.error("Unhandled gateway error:", error);
  res.status(500).json({ error: "Internal server error", code: "internal_error" });
});

app.listen(config.port, () => {
  // eslint-disable-next-line no-console
  console.log(`macsense-gateway listening on port ${config.port}`);
});
