import { findPrimaryActivePlan } from "../lib/work-plan-utils.js"

export const PlanUpdateReminder = async ({ directory }) => {
  return {
    event: async (input) => {
      if (input.event.type !== "todo.updated") return

      const todo = input.event.properties
      const status = todo?.status

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
