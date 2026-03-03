package kr.io.team.loop.review.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.exception.InvalidInputException

class PeriodKeyTest :
    BehaviorSpec({

        Given("PeriodKey.daily 팩토리 메서드") {
            When("날짜를 전달하면") {
                val periodKey = PeriodKey.daily(LocalDate(2026, 2, 20))

                Then("DAILY:{날짜} 형식으로 생성된다") {
                    periodKey.value shouldBe "DAILY:2026-02-20"
                }
            }
        }

        Given("PeriodKey 생성 시") {
            When("빈 문자열이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        PeriodKey("")
                    }
                }
            }

            When("공백 문자열이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        PeriodKey("   ")
                    }
                }
            }
        }
    })
