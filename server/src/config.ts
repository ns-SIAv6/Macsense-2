import "dotenv/config";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0 || value === "MY_GEMINI_API_KEY") {
    throw new Error(
      `Missing required environment variable: ${name}. Copy server/.env.example to server/.env and set a real value.`
    );
  }
  return value;
}

function optionalEnv(name: string, fallback: string | undefined = undefined): string | undefined {
  const value = process.env[name];
  if (!value || value.trim().length === 0) return fallback;
  return value;
}

export const config = {
  port: Number(optionalEnv("PORT", "8787")),
  geminiApiKey: requireEnv("GEMINI_API_KEY"),
  corsOrigins: optionalEnv("CORS_ORIGINS")
    ?.split(",")
    .map((origin) => origin.trim())
    .filter(Boolean),
  clientToken: optionalEnv("MACSENSE_CLIENT_TOKEN"),
  rateLimitMax: Number(optionalEnv("RATE_LIMIT_MAX", "30")),
  rateLimitWindowMs: Number(optionalEnv("RATE_LIMIT_WINDOW_MS", "60000")),
} as const;
