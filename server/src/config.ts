import * as dotenv from "dotenv";

dotenv.config();

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function optionalEnv(name: string, fallback: string): string {
  const value = process.env[name];
  return value && value.trim().length > 0 ? value : fallback;
}

export const config = {
  port: parseInt(optionalEnv("PORT", "8787"), 10),
  geminiApiKey: requireEnv("GEMINI_API_KEY"),
  geminiModel: optionalEnv("GEMINI_MODEL", "gemini-2.0-flash"),
  corsOrigins: optionalEnv("CORS_ORIGINS", "")
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0),
  clientToken: process.env.MACSENSE_CLIENT_TOKEN?.trim() || null,
  rateLimitMax: parseInt(optionalEnv("RATE_LIMIT_MAX", "30"), 10),
  rateLimitWindowMs: parseInt(optionalEnv("RATE_LIMIT_WINDOW_MS", "60000"), 10),
};
