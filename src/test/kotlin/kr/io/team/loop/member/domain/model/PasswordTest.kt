package kr.io.team.loop.member.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PasswordTest :
    BehaviorSpec({
        given("8자 이상 비밀번호") {
            `when`("Password 생성") {
                then("성공") {
                    val password = Password("password1")
                    password.value shouldBe "password1"
                }
            }
        }

        given("7자 이하 비밀번호") {
            `when`("Password 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Password("short")
                    }
                }
            }
        }

        given("100자 초과 비밀번호") {
            `when`("Password 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Password("a".repeat(101))
                    }
                }
            }
        }
    })
