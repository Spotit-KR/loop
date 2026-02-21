package kr.io.team.loop.auth.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AccessTokenTest :
    BehaviorSpec({
        given("비어있지 않은 토큰 문자열") {
            `when`("AccessToken 생성") {
                then("성공") {
                    val token = AccessToken("eyJhbGciOiJIUzI1NiJ9.token")
                    token.value shouldBe "eyJhbGciOiJIUzI1NiJ9.token"
                }
            }
        }

        given("빈 문자열") {
            `when`("AccessToken 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        AccessToken("")
                    }
                }
            }
        }

        given("공백만 있는 문자열") {
            `when`("AccessToken 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        AccessToken("   ")
                    }
                }
            }
        }
    })
