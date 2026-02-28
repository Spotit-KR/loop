package kr.io.team.loop.auth.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class LoginIdTest :
    BehaviorSpec({

        Given("LoginId 생성 시") {
            When("유효한 값이면") {
                val loginId = LoginId("testuser")

                Then("정상 생성된다") {
                    loginId.value shouldBe "testuser"
                }
            }

            When("빈 값이면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        LoginId("")
                    }
                }
            }

            When("공백만 있으면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        LoginId("   ")
                    }
                }
            }

            When("50자를 초과하면") {
                Then("예외가 발생한다") {
                    shouldThrow<InvalidInputException> {
                        LoginId("a".repeat(51))
                    }
                }
            }

            When("50자이면") {
                val loginId = LoginId("a".repeat(50))

                Then("정상 생성된다") {
                    loginId.value.length shouldBe 50
                }
            }
        }
    })
