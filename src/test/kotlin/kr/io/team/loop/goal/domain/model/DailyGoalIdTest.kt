package kr.io.team.loop.goal.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class DailyGoalIdTest :
    BehaviorSpec({

        Given("DailyGoalId 생성 시") {
            When("양수이면") {
                val id = DailyGoalId(1L)

                Then("정상 생성된다") {
                    id.value shouldBe 1L
                }
            }

            When("0이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        DailyGoalId(0L)
                    }
                }
            }

            When("음수이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        DailyGoalId(-1L)
                    }
                }
            }
        }
    })
