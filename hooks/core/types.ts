export type HookCtx = {
  tool: string
  args: Record<string, unknown>
  cwd: string
  sessionId?: string
}

export type HookResult =
  | { kind: "allow" }
  | { kind: "block"; reason: string }
  | { kind: "context"; message: string }
  | { kind: "modify"; updatedInput: Record<string, unknown> }

export type HookModule = {
  events: ReadonlyArray<"PreToolUse" | "PostToolUse">
  check: (ctx: HookCtx) => HookResult | Promise<HookResult>
}
