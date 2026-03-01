package kr.io.team.loop.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class TaskTitleTest :
    BehaviorSpec({

        Given("TaskTitle 생성 시") {
            When("유효한 값이면") {
                val title = TaskTitle("할일 제목")

                Then("정상 생성된다") {
                    title.value shouldBe "할일 제목"
                }
            }

            When("빈 값이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        TaskTitle("")
                    }
                }
            }

            When("공백만 있으면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        TaskTitle("   ")
                    }
                }
            }

            When("200자 이하이면") {
                val title = TaskTitle("가".repeat(200))

                Then("정상 생성된다") {
                    title.value.length shouldBe 200
                }
            }

            When("200자 초과이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        TaskTitle("가".repeat(201))
                    }
                }
            }
        }
    })
