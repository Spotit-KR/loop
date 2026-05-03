import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { mkdtempSync, rmSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import {
  __testing,
  check,
  detectLayer,
  isSecurityRelated,
} from "../core/layer-doc-reminder"

let tmp: string

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "layer-reminder-"))
  __testing.setReminderDir(tmp)
  __testing.resetInProcess()
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

function edit(filePath: string, sessionId?: string) {
  return check({ tool: "Edit", args: { file_path: filePath }, cwd: "/proj", sessionId })
}

describe("detectLayer", () => {
  test("domain", () => expect(detectLayer("/p/task/domain/model/Task.kt")).toBe("domain"))
  test("application", () =>
    expect(detectLayer("/p/task/application/service/TaskService.kt")).toBe("application"))
  test("infrastructure", () =>
    expect(detectLayer("/p/task/infrastructure/persistence/TaskTable.kt")).toBe("infrastructure"))
  test("presentation", () =>
    expect(detectLayer("/p/task/presentation/controller/TaskController.kt")).toBe("presentation"))
  test("unrelated", () => expect(detectLayer("/p/build.gradle.kts")).toBeNull())
  test("common/config not a layer", () =>
    expect(detectLayer("/p/common/config/SecurityConfig.kt")).toBeNull())
})

describe("isSecurityRelated", () => {
  test("SecurityConfig.kt under common/config", () =>
    expect(isSecurityRelated("/p/common/config/SecurityConfig.kt")).toBe(true))
  test("case insensitive", () =>
    expect(isSecurityRelated("/p/common/config/SECURITY.kt")).toBe(true))
  test("non-security under common/config", () =>
    expect(isSecurityRelated("/p/common/config/WebMvcConfig.kt")).toBe(false))
  test("security outside common/config", () =>
    expect(isSecurityRelated("/p/auth/domain/SecurityToken.kt")).toBe(false))
})

describe("session-keyed reminders", () => {
  test("first domain edit blocks", () => {
    const r = edit("/p/task/domain/model/Task.kt", "s1")
    expect(r.kind).toBe("block")
    expect(r.kind === "block" && r.reason.includes("domain")).toBe(true)
    expect(r.kind === "block" && r.reason.includes("docs/layers/domain.md")).toBe(true)
  })
  test("second domain edit in same session passes", () => {
    edit("/p/task/domain/model/Task.kt", "s1")
    expect(edit("/p/task/domain/model/Other.kt", "s1").kind).toBe("allow")
  })
  test("different session blocks again", () => {
    edit("/p/task/domain/model/Task.kt", "sA")
    expect(edit("/p/task/domain/model/Task.kt", "sB").kind).toBe("block")
  })
  test("different layer in same session still blocks", () => {
    edit("/p/task/domain/model/Task.kt", "s1")
    expect(edit("/p/task/application/TaskService.kt", "s1").kind).toBe("block")
  })
  test("docs path always passes", () => {
    expect(edit("/p/docs/layers/domain.md", "s1").kind).toBe("allow")
  })
  test("Test.kt suffix passes", () => {
    expect(edit("/p/task/domain/model/TaskTest.kt", "s1").kind).toBe("allow")
  })
  test("src/test/ passes", () => {
    expect(edit("/p/src/test/kotlin/task/domain/model/Task.kt", "s1").kind).toBe("allow")
  })
  test("BC named test still triggers reminder", () => {
    expect(edit("/p/test/domain/model/Task.kt", "s1").kind).toBe("block")
  })
  test("unrelated file passes", () => {
    expect(edit("/p/build.gradle.kts", "s1").kind).toBe("allow")
  })
  test("security config blocks", () => {
    const r = edit("/p/common/config/SecurityConfig.kt", "s1")
    expect(r.kind).toBe("block")
    expect(r.kind === "block" && r.reason.toLowerCase().includes("security")).toBe(true)
    expect(r.kind === "block" && r.reason.includes("spring-security-7.md")).toBe(true)
  })
  test("empty file_path passes", () => {
    expect(edit("", "s1").kind).toBe("allow")
  })
  test("non Edit/Write tool passes", () => {
    expect(check({ tool: "Bash", args: { command: "x" }, cwd: "/p", sessionId: "s1" }).kind).toBe(
      "allow",
    )
  })
  test("in-process fallback when no sessionId", () => {
    expect(edit("/p/task/domain/model/A.kt").kind).toBe("block")
    expect(edit("/p/task/domain/model/B.kt").kind).toBe("allow")
  })
})
