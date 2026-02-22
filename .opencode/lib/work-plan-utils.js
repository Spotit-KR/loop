import { existsSync, readFileSync, readdirSync, statSync } from "fs"
import { join, resolve } from "path"

const REQUIRED_PLAN_FILES = ["plan.md", "context.md", "checklist.md"]

function hasUncheckedItems(content) {
  return content.includes("- [ ]")
}

function latestPlanTimestamp(planDir) {
  let latest = 0
  for (const file of REQUIRED_PLAN_FILES) {
    const filePath = join(planDir, file)
    if (!existsSync(filePath)) continue
    const mtime = statSync(filePath).mtimeMs
    if (mtime > latest) latest = mtime
  }
  return latest
}

function hasRequiredPlanFiles(planDir) {
  return REQUIRED_PLAN_FILES.every((file) => existsSync(join(planDir, file)))
}

export function findActivePlans(directory) {
  const planBase = resolve(directory, "docs", "plan")
  if (!existsSync(planBase)) return []

  let dirs
  try {
    dirs = readdirSync(planBase, { withFileTypes: true }).filter((d) => d.isDirectory())
  } catch {
    return []
  }

  const activePlans = []

  for (const dir of dirs) {
    const planDir = join(planBase, dir.name)
    if (!hasRequiredPlanFiles(planDir)) continue

    const planMdPath = join(planDir, "plan.md")
    const content = readFileSync(planMdPath, "utf-8")
    if (!hasUncheckedItems(content)) continue

    activePlans.push({
      name: dir.name,
      planMdPath,
      updatedAtMs: latestPlanTimestamp(planDir),
    })
  }

  activePlans.sort((a, b) => b.updatedAtMs - a.updatedAtMs)
  return activePlans
}

export function findPrimaryActivePlan(directory) {
  const activePlans = findActivePlans(directory)
  return activePlans.length > 0 ? activePlans[0] : null
}

export function hasActivePlan(directory) {
  return findPrimaryActivePlan(directory) !== null
}
