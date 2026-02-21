package kr.io.team.loop.member.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.Nickname

class NicknameTest :
    BehaviorSpec({
        given("유효한 닉네임") {
            `when`("Nickname 생성") {
                then("성공") {
                    val nickname = Nickname("testuser")
                    nickname.value shouldBe "testuser"
                }
            }
        }

        given("빈 문자열") {
            `when`("Nickname 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("")
                    }
                }
            }
        }

        given("50자 초과 닉네임") {
            `when`("Nickname 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("a".repeat(51))
                    }
                }
            }
        }
    })
