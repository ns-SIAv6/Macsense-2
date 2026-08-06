import type { NextFunction, Request, Response } from "express";
import { config } from "./config.js";

/**
 * Optional shared-token auth. When MACSENSE_CLIENT_TOKEN is unset (local dev), this is a
 * no-op so the gateway stays easy to run without extra setup. In any deployed environment,
 * set MACSENSE_CLIENT_TOKEN and configure the Android client to send it as a Bearer token.
 */
export function requireClientToken(req: Request, res: Response, next: NextFunction): void {
  if (!config.clientToken) {
    next();
    return;
  }

  const header = req.header("authorization") ?? "";
  const expected = `Bearer ${config.clientToken}`;

  if (header !== expected) {
    res.status(401).json({ error: "Unauthorized", code: "invalid_client_token" });
    return;
  }

  next();
}
