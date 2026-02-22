import { describe, expect, it } from "bun:test"
import { mkdtempSync, mkdirSync, writeFileSync } from "fs"
import { tmpdir } from "os"
import { join } from "path"
import { WorkPlanEnforcer } from "../work-plan-enforcer.js"

function createWorkspace({
  planName = "task-a",
  withActivePlan = false,
  withRequiredDocs = true,
  planContent = "- [ ] 1단계\n",
} = {}) {
  const workspace = mkdtempSync(join(tmpdir(), "wp-enforcer-test-"))
  if (!withActivePlan) return workspace

  const planDir = join(workspace, "docs", "plan", planName)
  mkdirSync(planDir, { recursive: true })
  writeFileSync(join(planDir, "plan.md"), planContent, "utf-8")

  if (withRequiredDocs) {
    writeFileSync(join(planDir, "context.md"), "context", "utf-8")
    writeFileSync(join(planDir, "checklist.md"), "checklist", "utf-8")
  }

  return workspace
}

async function runHook(directory, input, output) {
  const plugin = await WorkPlanEnforcer({ directory })
  return plugin["tool.execute.before"](input, output)
}

describe("WorkPlanEnforcer", () => {
  it("blocks src file edit when active plan is missing", async () => {
    const workspace = createWorkspace()
    await expect(
      runHook(
        workspace,
        { tool: "write" },
        { args: { filePath: "src/main/kotlin/TaskService.kt" } }
      )
    ).rejects.toThrow("[work-plan]")
  })

  it("allows src file edit when active plan exists", async () => {
    const workspace = createWorkspace({ withActivePlan: true })
    await expect(
      runHook(
        workspace,
        { tool: "write" },
        { args: { filePath: "src/main/kotlin/TaskService.kt" } }
      )
    ).resolves.toBeUndefined()
  })

  it("treats plan as inactive when required docs are missing", async () => {
    const workspace = createWorkspace({ withActivePlan: true, withRequiredDocs: false })
    await expect(
      runHook(
        workspace,
        { tool: "write" },
        { args: { filePath: "src/main/kotlin/TaskService.kt" } }
      )
    ).rejects.toThrow("[work-plan]")
  })

  it("blocks bash-based src mutation without active plan", async () => {
    const workspace = createWorkspace()
    await expect(
      runHook(
        workspace,
        { tool: "bash" },
        { args: { command: 'echo "x" > src/main/kotlin/TaskService.kt' } }
      )
    ).rejects.toThrow("[work-plan]")
  })

  it("allows bash command when it only reads src files", async () => {
    const workspace = createWorkspace()
    await expect(
      runHook(workspace, { tool: "bash" }, { args: { command: "cat src/main/kotlin/Task.kt" } })
    ).resolves.toBeUndefined()
  })

  it("allows bash mutation for exempt src config files", async () => {
    const workspace = createWorkspace()
    await expect(
      runHook(
        workspace,
        { tool: "bash" },
        { args: { command: 'echo "spring: test" > src/main/resources/application.yml' } }
      )
    ).resolves.toBeUndefined()
  })

  it("blocks apply_patch src edit without active plan", async () => {
    const workspace = createWorkspace()
    await expect(
      runHook(
        workspace,
        { tool: "apply_patch" },
        {
          args: {
            patchText: "*** Begin Patch\n*** Update File: src/main/kotlin/Task.kt\n*** End Patch",
          },
        }
      )
    ).rejects.toThrow("[work-plan]")
  })
})
