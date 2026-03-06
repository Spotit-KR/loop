package kr.io.team.loop.task.application.dto

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.GoalId

class GoalTaskStatsDtoTest :
    BehaviorSpec({

        Given("GoalTaskStatsDto 달성률 계산 시") {
            When("할일이 있으면") {
                val stats = GoalTaskStatsDto(goalId = GoalId(1L), totalCount = 10, completedCount = 7)

                Then("달성률을 백분율로 반환한다") {
                    stats.achievementRate shouldBe 70.0
                }
            }

            When("모든 할일이 완료되면") {
                val stats = GoalTaskStatsDto(goalId = GoalId(1L), totalCount = 5, completedCount = 5)

                Then("달성률 100.0을 반환한다") {
                    stats.achievementRate shouldBe 100.0
                }
            }

            When("할일이 없으면") {
                val stats = GoalTaskStatsDto(goalId = GoalId(1L), totalCount = 0, completedCount = 0)

                Then("달성률 0.0을 반환한다") {
                    stats.achievementRate shouldBe 0.0
                }
            }

            When("완료된 할일이 없으면") {
                val stats = GoalTaskStatsDto(goalId = GoalId(1L), totalCount = 3, completedCount = 0)

                Then("달성률 0.0을 반환한다") {
                    stats.achievementRate shouldBe 0.0
                }
            }
        }
    })
