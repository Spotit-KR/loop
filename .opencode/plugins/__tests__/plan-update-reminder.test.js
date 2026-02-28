import { describe, expect, it } from "bun:test"
import { mkdtempSync, mkdirSync, utimesSync, writeFileSync } from "fs"
import { tmpdir } from "os"
import { join } from "path"
import { PlanUpdateReminder } from "../plan-update-reminder.js"

function createWorkspace() {
  return mkdtempSync(join(tmpdir(), "wp-reminder-test-"))
}

function createPlan(workspace, name, { content = "- [ ] 1단계\n", withRequiredDocs = true, updatedAt } = {}) {
  const planDir = join(workspace, "docs", "plan", name)
  mkdirSync(planDir, { recursive: true })

  const planMdPath = join(planDir, "plan.md")
  writeFileSync(planMdPath, content, "utf-8")

  if (withRequiredDocs) {
    writeFileSync(join(planDir, "context.md"), "context", "utf-8")
    writeFileSync(join(planDir, "checklist.md"), "checklist", "utf-8")
  }

  if (updatedAt) {
    const targetTime = new Date(updatedAt)
    utimesSync(planMdPath, targetTime, targetTime)
    if (withRequiredDocs) {
      utimesSync(join(planDir, "context.md"), targetTime, targetTime)
      utimesSync(join(planDir, "checklist.md"), targetTime, targetTime)
    }
  }

  return planMdPath
}

async function runEvent(directory, status, content = "task") {
  const plugin = await PlanUpdateReminder({ directory })
  return plugin.event({
    event: {
      type: "todo.updated",
      properties: { status, content },
    },
  })
}

describe("PlanUpdateReminder", () => {
  it("warns on pending todo when no active plan exists", async () => {
    const workspace = createWorkspace()
    await expect(runEvent(workspace, "pending", "새 작업")).rejects.toThrow("Todo 생성 감지")
  })

  it("does not warn on pending todo when active plan exists", async () => {
    const workspace = createWorkspace()
    createPlan(workspace, "active")
    await expect(runEvent(workspace, "pending", "새 작업")).resolves.toBeUndefined()
  })

  it("ignores plan directories missing required docs", async () => {
    const workspace = createWorkspace()
    createPlan(workspace, "broken", { withRequiredDocs: false })
    await expect(runEvent(workspace, "pending", "새 작업")).rejects.toThrow("Todo 생성 감지")
  })

  it("warns on completed todo with plan.md path", async () => {
    const workspace = createWorkspace()
    const planPath = createPlan(workspace, "active")

    await expect(runEvent(workspace, "completed", "1단계")).rejects.toThrow(planPath)
  })

  it("chooses most recently updated active plan", async () => {
    const workspace = createWorkspace()
    const oldTime = Date.now() - 60_000
    const newTime = Date.now()
    createPlan(workspace, "old-plan", { updatedAt: oldTime })
    const latestPlanPath = createPlan(workspace, "latest-plan", { updatedAt: newTime })

    await expect(runEvent(workspace, "completed", "1단계")).rejects.toThrow(latestPlanPath)
  })

  it("does nothing on completed todo when no active plan exists", async () => {
    const workspace = createWorkspace()
    await expect(runEvent(workspace, "completed", "1단계")).resolves.toBeUndefined()
  })

  describe("exempt tasks", () => {
    it("skips pending reminder for docs-related task", async () => {
      const workspace = createWorkspace()
      await expect(runEvent(workspace, "pending", "CLAUDE.md 문서 수정")).resolves.toBeUndefined()
    })

    it("skips pending reminder for config file task", async () => {
      const workspace = createWorkspace()
      await expect(runEvent(workspace, "pending", "application.yml 설정 변경")).resolves.toBeUndefined()
    })

    it("skips pending reminder for build/CI task", async () => {
      const workspace = createWorkspace()
      await expect(runEvent(workspace, "pending", "CI/CD 파이프라인 수정")).resolves.toBeUndefined()
    })

    it("skips pending reminder for hook task", async () => {
      const workspace = createWorkspace()
      await expect(runEvent(workspace, "pending", ".claude/ 훅 설정 수정")).resolves.toBeUndefined()
    })

    it("skips completed reminder for exempt task even with active plan", async () => {
      const workspace = createWorkspace()
      createPlan(workspace, "active")
      await expect(runEvent(workspace, "completed", "docs/ 문서 업데이트")).resolves.toBeUndefined()
    })

    it("still warns for non-exempt code task", async () => {
      const workspace = createWorkspace()
      await expect(runEvent(workspace, "pending", "인증 서비스 구현")).rejects.toThrow("Todo 생성 감지")
    })
  })
})
