import { z } from "zod";

const partSchema = z.object({
  text: z.string().max(20000).optional(),
});

const contentSchema = z.object({
  role: z.string().optional(),
  parts: z.array(partSchema).min(1),
});

/**
 * Payload the Android client sends. Deliberately close to the Gemini wire format
 * (contents + systemInstruction) so the gateway stays a thin, low-risk pass-through
 * rather than a second place business logic can drift from the client.
 */
export const ariChatRequestSchema = z.object({
  contents: z.array(contentSchema).min(1).max(20),
  systemInstruction: contentSchema.optional(),
});

export type AriChatRequest = z.infer<typeof ariChatRequestSchema>;
