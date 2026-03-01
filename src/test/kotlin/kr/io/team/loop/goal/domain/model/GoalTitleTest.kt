package kr.io.team.loop.goal.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class GoalTitleTest :
    BehaviorSpec({

        Given("GoalTitle 생성 시") {
            When("유효한 값이면") {
                val title = GoalTitle("목표 제목")

                Then("정상 생성된다") {
                    title.value shouldBe "목표 제목"
                }
            }

            When("빈 값이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        GoalTitle("")
                    }
                }
            }

            When("공백만 있으면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        GoalTitle("   ")
                    }
                }
            }

            When("200자를 초과하면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        GoalTitle("가".repeat(201))
                    }
                }
            }

            When("200자이면") {
                val title = GoalTitle("가".repeat(200))

                Then("정상 생성된다") {
                    title.value.length shouldBe 200
                }
            }
        }
    })
