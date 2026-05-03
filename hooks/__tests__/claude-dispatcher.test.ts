import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { spawnSync } from "node:child_process"
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { dirname, join, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const HERE = dirname(fileURLToPath(import.meta.url))
const HOOKS_DIR = resolve(HERE, "..")
const PROJECT_DIR = resolve(HOOKS_DIR, "..")
const DISPATCHER = join(HOOKS_DIR, "adapters", "claude", "run.mjs")

let tmp: string

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "claude-dispatcher-"))
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

function runDispatcher(hookName: string, payload: unknown, inputOverride?: string) {
  return spawnSync("bun", [DISPATCHER, hookName], {
    cwd: PROJECT_DIR,
    env: { ...process.env, CLAUDE_PROJECT_DIR: tmp },
    input: inputOverride ?? JSON.stringify(payload),
    encoding: "utf8",
  })
}

function makeActivePlan(name = "active") {
  const dir = join(tmp, "docs", "plan", name)
  mkdirSync(dir, { recursive: true })
  const planMdPath = join(dir, "plan.md")
  writeFileSync(planMdPath, "- [ ] step")
  writeFileSync(join(dir, "checklist.md"), "# checklist")
  return planMdPath
}

describe("Claude dispatcher", () => {
  test("blocks dangerous bash command with Claude exit code 2", () => {
    const result = runDispatcher("block-dangerous", {
      hook_event_name: "PreToolUse",
      tool_name: "Bash",
      tool_input: { command: "rm -rf /" },
      cwd: tmp,
      session_id: "session-danger",
    })

    expect(result.status).toBe(2)
    expect(result.stderr).toContain("Safety Hook")
    expect(result.stderr).toContain("rm -rf /")
  })

  test("allows safe bash command with exit code 0", () => {
    const result = runDispatcher("block-dangerous", {
      hook_event_name: "PreToolUse",
      tool_name: "Bash",
      tool_input: { command: "ls -la" },
      cwd: tmp,
      session_id: "session-safe",
    })

    expect(result.status).toBe(0)
    expect(result.stderr).toBe("")
  })

  test("passes cwd and tool_input to plan-update-reminder", () => {
    const planMdPath = makeActivePlan()
    const result = runDispatcher("plan-update-reminder", {
      hook_event_name: "PostToolUse",
      tool_name: "TaskUpdate",
      tool_input: { status: "completed" },
      cwd: tmp,
      session_id: "session-plan",
    })

    expect(result.status).toBe(2)
    expect(result.stderr).toContain(planMdPath)
  })

  test("ignores malformed JSON input", () => {
    const result = runDispatcher("block-dangerous", {}, "{not-json")

    expect(result.status).toBe(0)
    expect(result.stderr).toBe("")
  })

  test("unknown hook name exits without blocking", () => {
    const result = runDispatcher("unknown-hook", {
      hook_event_name: "PreToolUse",
      tool_name: "Bash",
      tool_input: { command: "rm -rf /" },
      cwd: tmp,
      session_id: "session-unknown",
    })

    expect(result.status).toBe(0)
    expect(result.stderr).toContain("unknown hook")
  })
})
