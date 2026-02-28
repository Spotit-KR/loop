import { findPrimaryActivePlan } from "../lib/work-plan-utils.js"

// 비코드(예외) 작업 판별 패턴 (work-planning-rules.md 예외 기준)
// 태스크 subject/description에 이 문자열이 포함되면 계획 문서 리마인더를 건너뜀
const EXEMPT_FILE_PATTERNS = [
  "/docs/", "docs/",
  "/.claude/", ".claude/",
  "/.opencode/", ".opencode/",
  "README", "CLAUDE.md",
  ".gradle.kts", ".gradle",
  ".yml", ".yaml",
  ".properties", ".toml", ".xml",
  "Dockerfile", "docker-compose",
  ".github/workflows",
]

const EXEMPT_KEYWORD_RE = /문서|설정\s*파일|빌드|CI\/?CD|배포|deploy|hook|훅/i

function isExemptTask(todo) {
  const content = todo?.content || ""
  const text = content

  for (const pattern of EXEMPT_FILE_PATTERNS) {
    if (text.includes(pattern)) return true
  }

  if (EXEMPT_KEYWORD_RE.test(text)) return true

  return false
}

export const PlanUpdateReminder = async ({ directory }) => {
  return {
    event: async (input) => {
      if (input.event.type !== "todo.updated") return

      const todo = input.event.properties
      const status = todo?.status

      if (isExemptTask(todo)) return

      // Todo 완료 시: plan.md 체크 표시 업데이트 리마인드
      if (status === "completed") {
        const activePlan = findPrimaryActivePlan(directory)
        if (activePlan) {
          throw new Error(
            `[plan] Task 완료 감지. ` +
              `다음 plan.md 의 해당 단계를 [x]로 업데이트하세요: ${activePlan.planMdPath}`
          )
        }
      }

      // Todo 생성 시: plan 디렉토리·문서 생성 여부 확인
      if (status === "pending") {
        const activePlan = findPrimaryActivePlan(directory)
        if (!activePlan) {
          const subject = todo?.content || ""
          throw new Error(
            `[plan] Todo 생성 감지: "${subject}"\n` +
              `docs/plan/{작업명}/ 에 plan.md, context.md, checklist.md 를 생성했는지 확인하세요. ` +
              `(docs/work-planning-rules.md 참고)`
          )
        }
      }
    },
  }
}
