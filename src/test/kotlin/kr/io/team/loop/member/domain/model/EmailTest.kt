package kr.io.team.loop.member.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.Email

class EmailTest :
    BehaviorSpec({
        given("유효한 이메일") {
            `when`("Email 생성") {
                then("성공") {
                    val email = Email("test@example.com")
                    email.value shouldBe "test@example.com"
                }
            }
        }

        given("빈 문자열") {
            `when`("Email 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Email("")
                    }
                }
            }
        }

        given("@ 없는 문자열") {
            `when`("Email 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Email("invalidemail")
                    }
                }
            }
        }

        given("255자 초과 이메일") {
            `when`("Email 생성") {
                then("IllegalArgumentException 발생") {
                    val longEmail = "a".repeat(250) + "@b.com"
                    shouldThrow<IllegalArgumentException> {
                        Email(longEmail)
                    }
                }
            }
        }
    })
