package kr.io.team.loop.goal.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

class DailyGoalTest :
    BehaviorSpec({

        Given("DailyGoal 생성 시") {
            When("유효한 값이면") {
                val dailyGoal =
                    DailyGoal(
                        id = DailyGoalId(1L),
                        goalId = GoalId(1L),
                        memberId = MemberId(1L),
                        date = LocalDate(2026, 3, 24),
                        createdAt = Instant.now(),
                    )

                Then("정상 생성된다") {
                    dailyGoal.id.value shouldBe 1L
                    dailyGoal.goalId.value shouldBe 1L
                    dailyGoal.memberId.value shouldBe 1L
                    dailyGoal.date shouldBe LocalDate(2026, 3, 24)
                    dailyGoal.createdAt shouldNotBe null
                }
            }
        }

        Given("DailyGoal의 소유자 확인 시") {
            val dailyGoal =
                DailyGoal(
                    id = DailyGoalId(1L),
                    goalId = GoalId(1L),
                    memberId = MemberId(1L),
                    date = LocalDate(2026, 3, 24),
                    createdAt = Instant.now(),
                )

            When("본인이면") {
                Then("true를 반환한다") {
                    dailyGoal.isOwnedBy(MemberId(1L)) shouldBe true
                }
            }

            When("본인이 아니면") {
                Then("false를 반환한다") {
                    dailyGoal.isOwnedBy(MemberId(2L)) shouldBe false
                }
            }
        }
    })
