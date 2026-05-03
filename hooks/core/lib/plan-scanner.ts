import { existsSync, readFileSync, readdirSync, statSync } from "node:fs"
import { join, resolve } from "node:path"

export const REQUIRED_PLAN_FILES = ["plan.md", "checklist.md"] as const

export type ActivePlan = {
  name: string
  planMdPath: string
  updatedAtMs: number
}

function planBaseDir(directory: string): string {
  return resolve(directory, "docs", "plan")
}

function hasUncheckedItems(content: string): boolean {
  return content.includes("- [ ]")
}

function hasRequiredPlanFiles(planDir: string): boolean {
  return REQUIRED_PLAN_FILES.every((file) => existsSync(join(planDir, file)))
}

function latestPlanTimestamp(planDir: string): number {
  let latest = 0
  for (const file of REQUIRED_PLAN_FILES) {
    const filePath = join(planDir, file)
    if (!existsSync(filePath)) continue
    const mtime = statSync(filePath).mtimeMs
    if (mtime > latest) latest = mtime
  }
  return latest
}

function readPlanDirEntries(directory: string): string[] {
  const planBase = planBaseDir(directory)
  if (!existsSync(planBase)) return []
  try {
    return readdirSync(planBase, { withFileTypes: true })
      .filter((d) => d.isDirectory())
      .map((d) => d.name)
  } catch {
    return []
  }
}

export function findActivePlans(directory: string): ActivePlan[] {
  const entries = readPlanDirEntries(directory)
  const planBase = planBaseDir(directory)
  const active: ActivePlan[] = []

  for (const name of entries) {
    const planDir = join(planBase, name)
    if (!hasRequiredPlanFiles(planDir)) continue
    const planMdPath = join(planDir, "plan.md")
    const content = readFileSync(planMdPath, "utf-8")
    if (!hasUncheckedItems(content)) continue
    active.push({
      name,
      planMdPath,
      updatedAtMs: latestPlanTimestamp(planDir),
    })
  }

  active.sort((a, b) => b.updatedAtMs - a.updatedAtMs)
  return active
}

export function findPrimaryActivePlan(directory: string): ActivePlan | null {
  const active = findActivePlans(directory)
  return active.length > 0 ? active[0] : null
}

export function hasActivePlan(directory: string): boolean {
  return findPrimaryActivePlan(directory) !== null
}

export function findIncompletePlanMdPaths(directory: string): string[] {
  return findActivePlans(directory).map((p) => p.planMdPath)
}
