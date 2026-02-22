import { existsSync, readFileSync, readdirSync } from "fs"
import { resolve, join } from "path"

function findActivePlanDirs(directory) {
  const planBase = resolve(directory, "docs", "plan")
  if (!existsSync(planBase)) return []

  let dirs
  try {
    dirs = readdirSync(planBase, { withFileTypes: true }).filter((d) => d.isDirectory())
  } catch {
    return []
  }

  const active = []
  for (const dir of dirs) {
    const planMd = join(planBase, dir.name, "plan.md")
    if (!existsSync(planMd)) continue
    const content = readFileSync(planMd, "utf-8")
    if (content.includes("- [ ]")) {
      active.push(planMd)
    }
  }
  return active
}

export const PlanUpdateReminder = async ({ directory }) => {
  return {
    event: async (input) => {
      if (input.event.type !== "todo.updated") return

      const todo = input.event.properties
      const status = todo?.status

      // Todo 완료 시: plan.md 체크 표시 업데이트 리마인드
      if (status === "completed") {
        const activePlans = findActivePlanDirs(directory)
        if (activePlans.length > 0) {
          throw new Error(
            `[plan] Task 완료 감지. ` +
              `다음 plan.md 의 해당 단계를 [x]로 업데이트하세요: ${activePlans.join(", ")}`
          )
        }
      }

      // Todo 생성 시: plan 디렉토리·문서 생성 여부 확인
      if (status === "pending") {
        const activePlans = findActivePlanDirs(directory)
        if (activePlans.length === 0) {
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
