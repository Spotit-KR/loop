import { findIncompletePlanMdPaths } from "./lib/plan-scanner"
import type { HookCtx, HookResult } from "./types"

const PR_CREATE_RE = /\bgh\s+pr\s+create\b/
const GIT_PUSH_RE = /\bgit\s+push\b/

export function check(ctx: HookCtx): HookResult {
  if (ctx.tool !== "Bash" && ctx.tool !== "bash") return { kind: "allow" }
  const command = (ctx.args?.command as string) ?? ""
  if (!PR_CREATE_RE.test(command) && !GIT_PUSH_RE.test(command)) return { kind: "allow" }

  const incomplete = findIncompletePlanMdPaths(ctx.cwd)
  if (incomplete.length === 0) return { kind: "allow" }

  const list = incomplete.map((p) => `  - ${p}`).join("\n")
  return {
    kind: "block",
    reason:
      `[plan-guard] 미완료 plan.md가 있어 PR 생성/push를 차단합니다.\n` +
      `다음 plan.md의 모든 항목을 완료([x])하거나 불필요한 계획을 정리하세요:\n${list}`,
  }
}
