import { describe, expect, test } from "bun:test"

import { isExempt, isSrcPath, normalizePath, shouldBlockSrcPath } from "../core/lib/path"

describe("normalizePath", () => {
  test("converts backslashes to forward slashes", () => {
    expect(normalizePath("C:\\src\\main\\Task.kt")).toBe("C:/src/main/Task.kt")
  })
})

describe("isExempt", () => {
  test("matches docs path", () => {
    expect(isExempt("/proj/docs/plan/a.md")).toBe(true)
  })
  test("matches .claude path", () => {
    expect(isExempt("/proj/.claude/hooks/x.py")).toBe(true)
  })
  test("matches .opencode path", () => {
    expect(isExempt("/proj/.opencode/plugins/x.js")).toBe(true)
  })
  test("matches README", () => {
    expect(isExempt("/proj/README.md")).toBe(true)
  })
  test("matches CLAUDE.md", () => {
    expect(isExempt("/proj/CLAUDE.md")).toBe(true)
  })
  test("matches build.gradle.kts", () => {
    expect(isExempt("/proj/build.gradle.kts")).toBe(true)
  })
  test("matches application.yml", () => {
    expect(isExempt("/proj/src/main/resources/application.yml")).toBe(true)
  })
  test("does not match plain kotlin source", () => {
    expect(isExempt("/proj/src/main/kotlin/Task.kt")).toBe(false)
  })
})

describe("isSrcPath", () => {
  test("detects src/ at root", () => {
    expect(isSrcPath("src/main/kotlin/Task.kt")).toBe(true)
  })
  test("detects src/ in absolute path", () => {
    expect(isSrcPath("/proj/src/main/kotlin/Task.kt")).toBe(true)
  })
  test("normalizes backslashes", () => {
    expect(isSrcPath("C:\\proj\\src\\Task.kt")).toBe(true)
  })
  test("rejects non-src", () => {
    expect(isSrcPath("/proj/docs/x.md")).toBe(false)
  })
})

describe("shouldBlockSrcPath", () => {
  test("blocks src kotlin file", () => {
    expect(shouldBlockSrcPath("/proj/src/main/kotlin/Task.kt")).toBe(true)
  })
  test("does not block exempt yml even under src", () => {
    expect(shouldBlockSrcPath("/proj/src/main/resources/application.yml")).toBe(false)
  })
})
