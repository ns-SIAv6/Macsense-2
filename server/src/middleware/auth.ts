import { NextFunction, Request, Response } from "express";
import { config } from "../config";

/**
 * Optional bearer-token gate. If MACSENSE_CLIENT_TOKEN is unset, this is a no-op so the
 * gateway keeps working during local development / initial rollout. Once the Android
 * client is updated to send `Authorization: Bearer <token>`, set the env var to enforce it.
 */
export function requireClientToken(req: Request, res: Response, next: NextFunction): void {
  if (!config.clientToken) {
    next();
    return;
  }

  const header = req.header("authorization") || "";
  const expected = `Bearer ${config.clientToken}`;

  if (header !== expected) {
    res.status(401).json({ error: "Unauthorized", code: "unauthorized" });
    return;
  }

  next();
}
