export interface GatewayPart {
  text?: string;
}

export interface GatewayContent {
  role?: string;
  parts: GatewayPart[];
}

export interface AriChatRequestBody {
  /**
   * Full conversation history in Gemini content format. The caller (Android app)
   * is responsible for enriching the last user turn with serialized DAW context,
   * exactly as it already does today when calling Gemini directly.
   */
  contents: GatewayContent[];
  /**
   * Optional system prompt override. If omitted, the gateway does not inject one;
   * the caller must supply the full Ari persona/system instruction, matching the
   * existing client behavior in DawViewModel.getAriSystemPrompt().
   */
  systemInstruction?: GatewayContent;
}

export interface GatewayErrorBody {
  error: string;
  code: string;
}
