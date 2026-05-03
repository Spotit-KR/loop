import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import { check } from "../core/plan-completion-guard"

let tmp: string

function makePlan(name: string, complete = true, extras: string[] = []) {
  const dir = join(tmp, "docs", "plan", name)
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(dir, "plan.md"), `- ${complete ? "[x]" : "[ ]"} step 1`)
  writeFileSync(join(dir, "checklist.md"), "# c")
  for (const e of extras) writeFileSync(join(dir, e), "# x")
}

function bash(command: string) {
  return check({ tool: "Bash", args: { command }, cwd: tmp })
}

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "plan-guard-"))
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

describe("plan-completion-guard", () => {
  test("non-Bash always allowed", () => {
    expect(check({ tool: "Edit", args: { file_path: "x" }, cwd: tmp }).kind).toBe("allow")
  })
  test("non PR/push allowed", () => {
    expect(bash("git status").kind).toBe("allow")
  })
  test("gh pr create with incomplete plan blocked", () => {
    makePlan("wip", false)
    const r = bash('gh pr create --title "x"')
    expect(r.kind).toBe("block")
    expect(r.kind === "block" && r.reason.includes("plan-guard")).toBe(true)
    expect(r.kind === "block" && r.reason.includes("wip")).toBe(true)
  })
  test("git push with incomplete plan blocked", () => {
    makePlan("wip", false)
    const r = bash("git push -u origin main")
    expect(r.kind).toBe("block")
  })
  test("gh pr create with complete plan allowed", () => {
    makePlan("done", true)
    expect(bash('gh pr create --title "x"').kind).toBe("allow")
  })
  test("no plans at all allowed", () => {
    mkdirSync(join(tmp, "docs", "plan"), { recursive: true })
    expect(bash('gh pr create --title "x"').kind).toBe("allow")
  })
  test("any incomplete blocks even with completed siblings", () => {
    makePlan("done", true)
    makePlan("wip", false)
    const r = bash('gh pr create --title "x"')
    expect(r.kind).toBe("block")
    expect(r.kind === "block" && r.reason.includes("wip")).toBe(true)
  })
  test("incomplete with extra files still blocks", () => {
    makePlan("extras-wip", false, ["context.md", "notes.md"])
    expect(bash("git push origin main").kind).toBe("block")
  })
  test("plan.md without checklist.md skipped", () => {
    const dir = join(tmp, "docs", "plan", "no-checklist")
    mkdirSync(dir, { recursive: true })
    writeFileSync(join(dir, "plan.md"), "- [ ] step")
    expect(bash('gh pr create --title "x"').kind).toBe("allow")
  })
})
