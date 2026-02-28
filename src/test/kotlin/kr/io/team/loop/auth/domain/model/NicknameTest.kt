package kr.io.team.loop.auth.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NicknameTest :
    BehaviorSpec({

        Given("Nickname 생성 시") {
            When("유효한 값이면") {
                val nickname = Nickname("홍길동")

                Then("정상 생성된다") {
                    nickname.value shouldBe "홍길동"
                }
            }

            When("빈 값이면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("")
                    }
                }
            }

            When("공백만 있으면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("   ")
                    }
                }
            }

            When("30자를 초과하면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("가".repeat(31))
                    }
                }
            }

            When("30자이면") {
                val nickname = Nickname("가".repeat(30))

                Then("정상 생성된다") {
                    nickname.value.length shouldBe 30
                }
            }
        }
    })
