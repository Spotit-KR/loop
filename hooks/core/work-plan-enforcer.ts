import { hasActivePlan } from "./lib/plan-scanner"
import { normalizePath, shouldBlockSrcPath } from "./lib/path"
import type { HookCtx, HookResult } from "./types"

const BASH_WRITE_PATTERNS: RegExp[] = [
  /\b(touch|mkdir|cp|mv|rm|install|truncate|dd)\b/i,
  /\b(sed|perl)\b[\s\S]*\s-i\b/i,
  /\btee\b/i,
  /\b(cat|echo|printf)\b[\s\S]*>{1,2}/i,
  />{1,2}\s*["']?[^\s"']*src\//i,
  /\bpython(?:3)?\b[\s\S]*open\(\s*["'][^"']*src\/[^"']*["']\s*,\s*["'][wa]/i,
]

function isBashSourceMutation(command: string): boolean {
  if (!/(^|\W)src\//.test(command)) return false
  return BASH_WRITE_PATTERNS.some((p) => p.test(command))
}

function extractSrcPathCandidates(command: string): string[] {
  const paths: string[] = []
  const regex = /(?:^|[\s"'])([^\s"'`]*src\/[^\s"'`]+)/g
  let m: RegExpExecArray | null
  while ((m = regex.exec(command)) !== null) {
    paths.push(m[1].replace(/[;|,&]+$/g, ""))
  }
  return paths
}

function extractPathsFromPatchText(patchText: string): string[] {
  const paths: string[] = []
  const regex = /^\*\*\* (?:Add|Update|Delete) File: (.+)$/gm
  let m: RegExpExecArray | null
  while ((m = regex.exec(patchText)) !== null) {
    paths.push(m[1].trim())
  }
  return paths
}

function collectFilePaths(args: Record<string, unknown>, tool: string): string[] {
  const direct = (args.file_path ?? args.filePath ?? args.path) as string | undefined
  const paths: string[] = []
  if (direct) paths.push(direct)
  if ((tool === "apply_patch") && typeof args.patchText === "string") {
    paths.push(...extractPathsFromPatchText(args.patchText as string))
  }
  return paths
}

function blockResult(): HookResult {
  return {
    kind: "block",
    reason:
      "[work-plan] 소스 코드 수정이 차단되었습니다. " +
      "docs/plan/{작업명}/ 에 plan.md, checklist.md 를 먼저 생성하세요. " +
      "(docs/work-planning-rules.md 참고)",
  }
}

export function check(ctx: HookCtx): HookResult {
  const tool = ctx.tool

  if (tool === "Bash" || tool === "bash") {
    const command = (ctx.args?.command as string) ?? ""
    if (!command) return { kind: "allow" }
    if (!isBashSourceMutation(command)) return { kind: "allow" }
    const candidates = extractSrcPathCandidates(command)
    const hasNonExempt = candidates.some((p) => shouldBlockSrcPath(p))
    if (!hasNonExempt) return { kind: "allow" }
    if (hasActivePlan(ctx.cwd)) return { kind: "allow" }
    return blockResult()
  }

  if (!["Edit", "Write", "edit", "write", "apply_patch"].includes(tool)) {
    return { kind: "allow" }
  }

  const paths = collectFilePaths(ctx.args, tool)
  if (paths.length === 0) return { kind: "allow" }

  for (const filePath of paths) {
    if (!filePath) continue
    const normalized = normalizePath(filePath)
    if (!shouldBlockSrcPath(normalized)) continue
    if (hasActivePlan(ctx.cwd)) return { kind: "allow" }
    return blockResult()
  }

  return { kind: "allow" }
}
