import { existsSync, readFileSync, readdirSync } from "fs"
import { resolve, join } from "path"

const EXEMPT_PATTERNS = ["/docs/", "/.claude/", "/.opencode/", "README", "CLAUDE.md"]

const EXEMPT_EXTENSIONS = [
  ".gradle.kts",
  ".gradle",
  ".yml",
  ".yaml",
  ".properties",
  ".toml",
  ".xml",
]

function normalizePath(filePath) {
  return filePath.replace(/\\/g, "/")
}

function isExempt(filePath) {
  const normalized = normalizePath(filePath)

  for (const pattern of EXEMPT_PATTERNS) {
    if (normalized.includes(pattern)) return true
  }

  if (normalized.startsWith("docs/")) return true
  if (normalized.startsWith(".claude/")) return true
  if (normalized.startsWith(".opencode/")) return true

  for (const ext of EXEMPT_EXTENSIONS) {
    if (normalized.endsWith(ext)) return true
  }
  return false
}

function hasActivePlan(directory) {
  const planBase = resolve(directory, "docs", "plan")
  if (!existsSync(planBase)) return false

  let dirs
  try {
    dirs = readdirSync(planBase, { withFileTypes: true }).filter((d) => d.isDirectory())
  } catch {
    return false
  }

  for (const dir of dirs) {
    const planMd = join(planBase, dir.name, "plan.md")
    if (!existsSync(planMd)) continue
    const content = readFileSync(planMd, "utf-8")
    if (content.includes("- [ ]")) return true
  }

  return false
}

function extractPathsFromPatchText(patchText) {
  const paths = []
  const regex = /^\*\*\* (?:Add|Update|Delete) File: (.+)$/gm
  let match
  while ((match = regex.exec(patchText)) !== null) {
    paths.push(match[1].trim())
  }
  return paths
}

function collectFilePaths(input, output) {
  const directPath =
    output.args?.file_path || output.args?.filePath || output.args?.path || ""
  const paths = []

  if (directPath) paths.push(directPath)

  if (input.tool === "apply_patch" && typeof output.args?.patchText === "string") {
    paths.push(...extractPathsFromPatchText(output.args.patchText))
  }

  return paths
}

export const WorkPlanEnforcer = async ({ directory }) => {
  return {
    "tool.execute.before": async (input, output) => {
      const filePaths = collectFilePaths(input, output)
      if (filePaths.length === 0) return

      for (const filePath of filePaths) {
        const normalized = normalizePath(filePath)
        if (isExempt(normalized)) continue
        if (!/(^|\/)src\//.test(normalized)) continue

        if (!hasActivePlan(directory)) {
          throw new Error(
            `[work-plan] 소스 코드 수정이 차단되었습니다. ` +
              `docs/plan/{작업명}/ 에 plan.md, context.md, checklist.md 를 먼저 생성하세요. ` +
              `(docs/work-planning-rules.md 참고)`
          )
        }
      }
    },
  }
}
