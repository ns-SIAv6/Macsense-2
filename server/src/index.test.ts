import { describe, expect, it } from "vitest";
import { ariChatRequestSchema } from "./schemas.js";

describe("ariChatRequestSchema", () => {
  it("accepts a minimal valid chat request", () => {
    const result = ariChatRequestSchema.safeParse({
      contents: [{ role: "user", parts: [{ text: "hello" }] }],
    });
    expect(result.success).toBe(true);
  });

  it("rejects a request with no contents", () => {
    const result = ariChatRequestSchema.safeParse({ contents: [] });
    expect(result.success).toBe(false);
  });

  it("rejects a content entry with no parts", () => {
    const result = ariChatRequestSchema.safeParse({
      contents: [{ role: "user", parts: [] }],
    });
    expect(result.success).toBe(false);
  });

  it("accepts an optional systemInstruction", () => {
    const result = ariChatRequestSchema.safeParse({
      contents: [{ role: "user", parts: [{ text: "hi" }] }],
      systemInstruction: { parts: [{ text: "be nice" }] },
    });
    expect(result.success).toBe(true);
  });
});
