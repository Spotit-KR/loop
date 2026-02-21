package kr.io.team.loop.auth.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RefreshTokenTest :
    BehaviorSpec({
        given("비어있지 않은 토큰 문자열") {
            `when`("RefreshToken 생성") {
                then("성공") {
                    val token = RefreshToken("refresh-token-value-123")
                    token.value shouldBe "refresh-token-value-123"
                }
            }
        }

        given("빈 문자열") {
            `when`("RefreshToken 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        RefreshToken("")
                    }
                }
            }
        }

        given("공백만 있는 문자열") {
            `when`("RefreshToken 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        RefreshToken("   ")
                    }
                }
            }
        }
    })
