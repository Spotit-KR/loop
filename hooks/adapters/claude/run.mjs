#!/usr/bin/env bun
import { dirname, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const HERE = dirname(fileURLToPath(import.meta.url))
const CORE_DIR = resolve(HERE, "..", "..", "core")

const CORES = {
  "block-dangerous": "block-dangerous.ts",
  "work-plan-enforcer": "work-plan-enforcer.ts",
  "plan-completion-guard": "plan-completion-guard.ts",
  "plan-update-reminder": "plan-update-reminder.ts",
  "layer-doc-reminder": "layer-doc-reminder.ts",
}

async function readStdin() {
  let data = ""
  for await (const chunk of process.stdin) data += chunk
  return data
}

async function main() {
  const hookName = process.argv[2]
  if (!hookName || !CORES[hookName]) {
    process.stderr.write(`unknown hook: ${hookName}\n`)
    process.exit(0)
  }

  let payload
  try {
    payload = JSON.parse((await readStdin()) || "{}")
  } catch {
    process.exit(0)
  }

  const cwd =
    payload.cwd ?? process.env.CLAUDE_PROJECT_DIR ?? process.cwd()

  const ctx = {
    tool: payload.tool_name ?? "",
    args: payload.tool_input ?? {},
    cwd,
    sessionId: payload.session_id,
  }

  const mod = await import(resolve(CORE_DIR, CORES[hookName]))
  const result = await mod.check(ctx)

  switch (result.kind) {
    case "block":
      process.stderr.write(result.reason)
      process.exit(2)
    case "context":
      process.stdout.write(
        JSON.stringify({
          hookSpecificOutput: {
            hookEventName: payload.hook_event_name,
            additionalContext: result.message,
          },
        }),
      )
      process.exit(0)
    case "modify":
      process.stdout.write(
        JSON.stringify({
          hookSpecificOutput: {
            hookEventName: payload.hook_event_name,
            updatedInput: result.updatedInput,
          },
        }),
      )
      process.exit(0)
    case "allow":
    default:
      process.exit(0)
  }
}

main().catch((err) => {
  process.stderr.write(`hook dispatcher error: ${err?.stack ?? err}\n`)
  process.exit(0)
})
