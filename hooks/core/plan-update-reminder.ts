import { findActivePlans } from "./lib/plan-scanner"
import type { HookCtx, HookResult } from "./types"

const EXEMPT_FILE_PATTERNS = [
  "/docs/",
  "docs/",
  "/.claude/",
  ".claude/",
  "/.opencode/",
  ".opencode/",
  "README",
  "CLAUDE.md",
  ".gradle.kts",
  ".gradle",
  ".yml",
  ".yaml",
  ".properties",
  ".toml",
  ".xml",
  "Dockerfile",
  "docker-compose",
  ".github/workflows",
] as const

const EXEMPT_KEYWORD_RE = /문서|설정\s*파일|빌드|CI\/?CD|배포|deploy|hook|훅/i

function textOfTask(args: Record<string, unknown>): string {
  const subject = (args.subject as string) ?? ""
  const description = (args.description as string) ?? ""
  const content = (args.content as string) ?? ""
  return `${subject} ${description} ${content}`
}

export function isExemptTask(args: Record<string, unknown>): boolean {
  const text = textOfTask(args)
  for (const pattern of EXEMPT_FILE_PATTERNS) {
    if (text.includes(pattern)) return true
  }
  if (EXEMPT_KEYWORD_RE.test(text)) return true
  return false
}

export function handleTaskCreate(args: Record<string, unknown>, cwd: string): string | null {
  if (isExemptTask(args)) return null
  const subject = (args.subject as string) ?? (args.content as string) ?? ""
  const active = findActivePlans(cwd)
  if (active.length === 0) {
    return (
      `[plan] TaskCreate 감지: "${subject}"\n` +
      `docs/plan/{작업명}/ 에 plan.md, checklist.md 를 생성했는지 확인하세요. ` +
      `(docs/work-planning-rules.md 참고)`
    )
  }
  return null
}

export function handleTaskUpdate(args: Record<string, unknown>, cwd: string): string | null {
  if (isExemptTask(args)) return null
  if (args.status !== "completed") return null
  const active = findActivePlans(cwd)
  if (active.length === 0) return null
  const list = active.map((p) => p.planMdPath).join(", ")
  return `[plan] Task 완료 감지. 다음 plan.md 의 해당 단계를 [x]로 업데이트하세요: ${list}`
}

export function check(ctx: HookCtx): HookResult {
  let message: string | null = null
  if (ctx.tool === "TaskCreate") message = handleTaskCreate(ctx.args, ctx.cwd)
  else if (ctx.tool === "TaskUpdate") message = handleTaskUpdate(ctx.args, ctx.cwd)
  if (message) return { kind: "block", reason: message }
  return { kind: "allow" }
}
