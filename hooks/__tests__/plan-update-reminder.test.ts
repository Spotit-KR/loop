import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import {
  check,
  handleTaskCreate,
  handleTaskUpdate,
  isExemptTask,
} from "../core/plan-update-reminder"

let tmp: string

function makePlan(name: string, plan = "- [ ] step 1") {
  const dir = join(tmp, "docs", "plan", name)
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(dir, "plan.md"), plan)
  writeFileSync(join(dir, "checklist.md"), "# checklist")
}

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "plan-update-"))
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

describe("isExemptTask", () => {
  test("doc-related subject", () => expect(isExemptTask({ subject: "문서 정리" })).toBe(true))
  test("hook keyword", () => expect(isExemptTask({ subject: "hook 추가" })).toBe(true))
  test("yml file mention", () =>
    expect(isExemptTask({ description: "application.yml 수정" })).toBe(true))
  test("normal feature task is not exempt", () =>
    expect(isExemptTask({ subject: "회원 가입 API 구현" })).toBe(false))
})

describe("handleTaskCreate", () => {
  test("reminds when no active plan", () => {
    const msg = handleTaskCreate({ subject: "새 기능" }, tmp)
    expect(msg).not.toBeNull()
    expect(msg!.includes("새 기능")).toBe(true)
    expect(msg!.includes("plan.md")).toBe(true)
  })
  test("silent when active plan exists", () => {
    makePlan("existing")
    expect(handleTaskCreate({ subject: "추가 작업" }, tmp)).toBeNull()
  })
  test("silent for exempt task even without plan", () => {
    expect(handleTaskCreate({ subject: "문서 정리" }, tmp)).toBeNull()
  })
})

describe("handleTaskUpdate", () => {
  test("reminds on completed with active plan", () => {
    makePlan("feature-x")
    const msg = handleTaskUpdate({ taskId: "1", status: "completed" }, tmp)
    expect(msg).not.toBeNull()
    expect(msg!.includes("plan.md")).toBe(true)
    expect(msg!.includes("[x]")).toBe(true)
  })
  test("silent on in_progress", () => {
    makePlan("feature-x")
    expect(handleTaskUpdate({ taskId: "1", status: "in_progress" }, tmp)).toBeNull()
  })
  test("silent when no active plan", () => {
    expect(handleTaskUpdate({ taskId: "1", status: "completed" }, tmp)).toBeNull()
  })
  test("silent when all plans done", () => {
    makePlan("done", "- [x] done")
    expect(handleTaskUpdate({ taskId: "1", status: "completed" }, tmp)).toBeNull()
  })
})

describe("check", () => {
  test("TaskCreate without plan returns block", () => {
    const r = check({ tool: "TaskCreate", args: { subject: "X" }, cwd: tmp })
    expect(r.kind).toBe("block")
  })
  test("TaskUpdate completed with plan returns block", () => {
    makePlan("p")
    const r = check({ tool: "TaskUpdate", args: { status: "completed" }, cwd: tmp })
    expect(r.kind).toBe("block")
  })
  test("Other tools allowed", () => {
    expect(check({ tool: "Edit", args: { file_path: "/x" }, cwd: tmp }).kind).toBe("allow")
  })
})
