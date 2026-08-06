import cors from "cors";
import express from "express";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import { config } from "./config";
import { requireClientToken } from "./middleware/auth";
import { ariChatRouter } from "./routes/ariChat";

const app = express();

app.disable("x-powered-by");
app.use(helmet());
app.use(
  cors({
    origin: config.corsOrigins.length > 0 ? config.corsOrigins : true,
  })
);
app.use(express.json({ limit: "256kb" }));

const limiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.rateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

app.use("/v1/ari/chat", requireClientToken, ariChatRouter);

// Fallback 404 in the same error shape as everything else.
app.use((_req, res) => {
  res.status(404).json({ error: "Not found", code: "not_found" });
});

app.listen(config.port, () => {
  // eslint-disable-next-line no-console
  console.log(`macsense-gateway listening on port ${config.port}`);
});
