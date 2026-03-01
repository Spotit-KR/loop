package kr.io.team.loop.common.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class GoalIdTest :
    BehaviorSpec({

        Given("GoalId 생성 시") {
            When("양수 값이면") {
                val goalId = GoalId(1L)

                Then("정상 생성된다") {
                    goalId.value shouldBe 1L
                }
            }

            When("0이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        GoalId(0L)
                    }
                }
            }

            When("음수이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        GoalId(-1L)
                    }
                }
            }
        }
    })
