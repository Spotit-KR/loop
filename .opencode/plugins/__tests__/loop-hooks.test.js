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
  const planMdPath = join(planDir, "plan.md")
  writeFileSync(planMdPath, "- [ ] step")
  writeFileSync(join(planDir, "checklist.md"), "# c")
  return planMdPath
}

function todoUpdatedEvent(todo, sessionID = "session-1") {
  return {
    type: "todo.updated",
    properties: {
      sessionID,
      todos: [todo],
    },
  }
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
        { tool: "task_create", args: { subject: "신규 기능" } },
        { title: "", output: "", metadata: {} },
      ),
    ).rejects.toThrow(/plan/)
  })

  it("plan-update-reminder reads TaskUpdate status from after-hook input args", async () => {
    const dir = makeDir()
    const planMdPath = makeActivePlan(dir)
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin["tool.execute.after"](
        { tool: "task_update", args: { status: "completed" } },
        { title: "", output: "", metadata: {} },
      ),
    ).rejects.toThrow(planMdPath)
  })

  it("todo.updated pending event reminds when no active plan exists", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: todoUpdatedEvent({ content: "신규 기능", status: "pending", priority: "medium" }),
      }),
    ).rejects.toThrow(/TaskCreate/)
  })

  it("todo.updated pending event is silent when active plan exists", async () => {
    const dir = makeDir()
    makeActivePlan(dir)
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: todoUpdatedEvent({ content: "추가 작업", status: "pending", priority: "medium" }),
      }),
    ).resolves.toBeUndefined()
  })

  it("todo.updated completed event reminds with active plan path", async () => {
    const dir = makeDir()
    const planMdPath = makeActivePlan(dir)
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: todoUpdatedEvent({ content: "1단계", status: "completed", priority: "medium" }),
      }),
    ).rejects.toThrow(planMdPath)
  })

  it("todo.updated in_progress event does not masquerade as TaskCreate", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: todoUpdatedEvent({ content: "신규 기능", status: "in_progress", priority: "medium" }),
      }),
    ).resolves.toBeUndefined()
  })

  it("todo.updated pending event skips exempt task", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: todoUpdatedEvent({ content: "CLAUDE.md 문서 수정", status: "pending", priority: "medium" }),
      }),
    ).resolves.toBeUndefined()
  })

  it("ignores non-todo events", async () => {
    const dir = makeDir()
    const plugin = await LoopHooks({ directory: dir })
    await expect(
      plugin.event({
        event: {
          type: "session.updated",
          properties: { status: "pending", content: "신규 기능" },
        },
      }),
    ).resolves.toBeUndefined()
  })
})
