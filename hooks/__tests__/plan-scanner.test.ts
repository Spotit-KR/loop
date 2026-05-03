import { afterEach, beforeEach, describe, expect, test } from "bun:test"
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs"
import { tmpdir } from "node:os"
import { join } from "node:path"

import {
  findActivePlans,
  findIncompletePlanMdPaths,
  findPrimaryActivePlan,
  hasActivePlan,
} from "../core/lib/plan-scanner"

let tmp: string

function makePlan(name: string, opts: { plan?: string; checklist?: boolean } = {}) {
  const dir = join(tmp, "docs", "plan", name)
  mkdirSync(dir, { recursive: true })
  writeFileSync(join(dir, "plan.md"), opts.plan ?? "- [ ] step 1")
  if (opts.checklist !== false) {
    writeFileSync(join(dir, "checklist.md"), "# checklist")
  }
}

beforeEach(() => {
  tmp = mkdtempSync(join(tmpdir(), "plan-scanner-"))
})

afterEach(() => {
  rmSync(tmp, { recursive: true, force: true })
})

describe("plan-scanner", () => {
  test("returns nothing when docs/plan does not exist", () => {
    expect(findActivePlans(tmp)).toEqual([])
    expect(hasActivePlan(tmp)).toBe(false)
  })

  test("ignores plans missing checklist.md", () => {
    makePlan("incomplete-only", { checklist: false })
    expect(findActivePlans(tmp)).toEqual([])
  })

  test("ignores plans whose plan.md has no unchecked items", () => {
    makePlan("done", { plan: "- [x] done" })
    expect(findActivePlans(tmp)).toEqual([])
  })

  test("finds active plans with both files and unchecked items", () => {
    makePlan("active")
    const plans = findActivePlans(tmp)
    expect(plans.length).toBe(1)
    expect(plans[0].name).toBe("active")
    expect(plans[0].planMdPath.endsWith("plan.md")).toBe(true)
    expect(hasActivePlan(tmp)).toBe(true)
    expect(findPrimaryActivePlan(tmp)?.name).toBe("active")
  })

  test("findIncompletePlanMdPaths returns all incomplete paths", () => {
    makePlan("a")
    makePlan("b")
    makePlan("done", { plan: "- [x] done" })
    const paths = findIncompletePlanMdPaths(tmp)
    expect(paths.length).toBe(2)
    expect(paths.every((p) => p.endsWith("plan.md"))).toBe(true)
  })
})
