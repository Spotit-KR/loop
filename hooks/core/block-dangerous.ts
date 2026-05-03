import type { HookCtx, HookModule, HookResult } from "./types"

const BLOCKED_PATTERNS: Array<[RegExp, string]> = [
  [/git\s+reset\s+--hard/i, "git reset --hard는 커밋되지 않은 작업을 삭제합니다"],
  [/git\s+push\s+.*--force/i, "git push --force는 원격 히스토리를 덮어씁니다"],
  [/git\s+push\s+.*-f\b/i, "git push -f는 원격 히스토리를 덮어씁니다"],
  [/git\s+clean\s+-.*f/i, "git clean -f는 추적되지 않은 파일을 영구 삭제합니다"],
  [/git\s+checkout\s+\.\s*$/i, "git checkout .은 모든 커밋되지 않은 변경사항을 삭제합니다"],
  [/git\s+restore\s+\.\s*$/i, "git restore .은 모든 커밋되지 않은 변경사항을 삭제합니다"],
  [/git\s+stash\s+drop/i, "git stash drop은 스태시된 변경사항을 영구 삭제합니다"],
  [/git\s+stash\s+clear/i, "git stash clear는 모든 스태시를 삭제합니다"],
  [/\brm\s+-rf\s+\/\s*(;|&&|\|\||$)/i, "rm -rf /는 매우 위험합니다"],
  [/\brm\s+-rf\s+~/i, "rm -rf ~는 홈 디렉토리를 삭제합니다"],
  [/\brm\s+-rf\s+\.\./i, "rm -rf ..은 상위 디렉토리를 삭제할 수 있습니다"],
  [/\brm\s+-rf\s+\*/i, "rm -rf *는 위험합니다"],
  [/DROP\s+DATABASE/i, "DROP DATABASE는 파괴적입니다"],
  [/DROP\s+TABLE/i, "DROP TABLE은 파괴적입니다"],
  [/TRUNCATE\s+TABLE/i, "TRUNCATE TABLE은 모든 데이터를 삭제합니다"],
]

export function check(ctx: HookCtx): HookResult {
  if (ctx.tool !== "Bash" && ctx.tool !== "bash") return { kind: "allow" }

  const command = (ctx.args?.command as string) ?? ""
  if (!command) return { kind: "allow" }

  for (const [pattern, reason] of BLOCKED_PATTERNS) {
    if (pattern.test(command)) {
      return {
        kind: "block",
        reason: `🚫 [Safety Hook] 차단됨: ${reason}\n\n시도된 명령어: ${command}\n정말 실행이 필요하다면, 터미널에서 직접 실행하세요.`,
      }
    }
  }
  return { kind: "allow" }
}

export const module: HookModule = {
  events: ["PreToolUse"] as const,
  check,
}
