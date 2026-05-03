import { describe, expect, it } from "bun:test"
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import { LoopHooks } from "../loop-hooks.js"

function makeDir() {
  return mkdtempSync(join(tmpdir(), "loop-hooks-"))
}

function makeActivePlan(dir, name = "active") {
  const planDir = join(dir, "docs", "plan", name)
  mkdirSync(planDir, { recursive: true })
  writeFileSync(join(planDir, "plan.md"), "- [ ] step")
  writeFileSync(join(planDir, "checklist.md"), "# c")
}

describe("LoopHooks adapter", () => {
  it("blocks rm -rf / via block-dangerous", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.before"](
        { tool: "bash" },
        { args: { command: "rm -rf /" } },
      ),
    ).rejects.toThrow(/Safety Hook/)
  })

  it("blocks src write without active plan", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.before"](
        { tool: "write" },
        { args: { filePath: "src/main/kotlin/Task.kt" } },
      ),
    ).rejects.toThrow(/work-plan/)
  })

  it("allows src write with active plan", async () => {
    const dir = makeDir()
    makeActivePlan(dir)
    const plugin = await LoopHooks({ directory: dir })
    // layer-doc-reminder will fire on first domain edit, so use a non-layer src path
    await expect(
      plugin["tool.execute.before"](
        { tool: "write" },
        { args: { filePath: "src/main/kotlin/util/X.kt" } },
      ),
    ).resolves.toBeUndefined()
  })

  it("blocks first domain layer edit via layer-doc-reminder", async () => {
    const dir = makeDir()
    makeActivePlan(dir)
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.before"](
        { tool: "edit" },
        { args: { filePath: "src/main/kotlin/task/domain/model/Task.kt" } },
      ),
    ).rejects.toThrow(/domain/)
  })

  it("blocks gh pr create with incomplete plan via plan-completion-guard", async () => {
    const dir = makeDir()
    makeActivePlan(dir, "wip")
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.before"](
        { tool: "bash" },
        { args: { command: 'gh pr create --title "x"' } },
      ),
    ).rejects.toThrow(/plan-guard/)
  })

  it("blocks apply_patch on src without plan", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.before"](
        { tool: "apply_patch" },
        { args: { patchText: "*** Update File: src/main/kotlin/Task.kt\n" } },
      ),
    ).rejects.toThrow(/work-plan/)
  })

  it("plan-update-reminder fires on TaskCreate without plan", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.after"](
        { tool: "task_create" },
        { args: { subject: "신규 기능" } },
      ),
    ).rejects.toThrow(/plan/)
  })
})
