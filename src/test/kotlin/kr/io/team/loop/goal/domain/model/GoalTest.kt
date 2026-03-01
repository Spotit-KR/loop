package kr.io.team.loop.goal.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

class GoalTest :
    BehaviorSpec({

        Given("Goal 생성 시") {
            When("유효한 값이면") {
                val goal =
                    Goal(
                        id = GoalId(1L),
                        title = GoalTitle("영어 공부"),
                        memberId = MemberId(1L),
                        createdAt = Instant.now(),
                        updatedAt = null,
                    )

                Then("정상 생성된다") {
                    goal.id.value shouldBe 1L
                    goal.title.value shouldBe "영어 공부"
                    goal.memberId.value shouldBe 1L
                    goal.createdAt shouldNotBe null
                    goal.updatedAt shouldBe null
                }
            }
        }

        Given("Goal의 소유자 확인 시") {
            val goal =
                Goal(
                    id = GoalId(1L),
                    title = GoalTitle("영어 공부"),
                    memberId = MemberId(1L),
                    createdAt = Instant.now(),
                    updatedAt = null,
                )

            When("본인이면") {
                Then("true를 반환한다") {
                    goal.isOwnedBy(MemberId(1L)) shouldBe true
                }
            }

            When("본인이 아니면") {
                Then("false를 반환한다") {
                    goal.isOwnedBy(MemberId(2L)) shouldBe false
                }
            }
        }
    })
